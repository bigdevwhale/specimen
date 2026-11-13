package app.specimen.sync

import android.util.Base64
import android.util.Log
import app.specimen.data.Photos
import app.specimen.data.SettingsStore
import app.specimen.data.StationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.spec.SecretKeySpec

/**
 * What the QR says: the shared secret, the phone that shows it, and its owner's name. Scanning it pairs the scanning
 * phone at once; the showing phone learns who scanned when they first meet.
 */
data class Invite(val secret: ByteArray, val deviceId: String, val name: String) {
    fun encode(): String = listOf(PREFIX, b64(secret), deviceId, b64(name.toByteArray())).joinToString(":")

    override fun equals(other: Any?) = other is Invite && secret.contentEquals(other.secret) && deviceId == other.deviceId && name == other.name
    override fun hashCode() = deviceId.hashCode()

    companion object {
        private const val PREFIX = "specimen1"
        private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

        fun decode(text: String): Invite? = runCatching {
            val p = text.trim().split(":")
            if (p.size != 4 || p[0] != PREFIX) return null
            val secret = Base64.decode(p[1], FLAGS)
            if (secret.size != 32 || p[2].isBlank()) return null
            Invite(secret, p[2], String(Base64.decode(p[3], FLAGS)))
        }.getOrNull()

        private fun b64(b: ByteArray) = Base64.encodeToString(b, FLAGS)
    }
}

/** How the link to the other phone is right now. */
data class Near(
    /** This phone is listening for the other one. */
    val listening: Boolean = false,
    /** The two phones are talking: every entry on one shows up on the other at once. */
    val together: Boolean = false,
    /** Rows that came from the other phone in this conversation. */
    val received: Int = 0,
)

/** Finds the other phone on the network. The real one is [LanFinder]; tests connect by hand. */
interface Finder {
    fun start(tag: String, me: String, port: Int, found: (InetAddress, Int) -> Unit)
    fun stop()
}

/**
 * The link between two paired phones. While it runs, this phone listens on the home network for the other one and
 * calls out for it. When they find each other they open one sealed conversation, swap the inventories, send each
 * other whatever is missing (photos included) and keep talking: an entry made on one phone appears on the other
 * within a second, until one of them leaves.
 *
 * It runs while something has a reason for it ([start]/[stop]): the app being open, the background service, the
 * pairing page. Never paired, it stays quiet whatever the reasons. Built on the same pieces as Fridge's hum.
 */
class Link(
    private val settings: SettingsStore,
    private val dao: StationDao,
    private val photos: Photos,
    private val scope: CoroutineScope,
    private val finder: Finder?,
) {
    private val _near = MutableStateFlow(Near())
    val near: StateFlow<Near> = _near.asStateFlow()

    /** Called each time the two phones find each other: wherever this is, it's home. */
    var onMet: () -> Unit = {}

    private val reasons = mutableSetOf<String>()
    private var running: Job? = null
    private var runningSecret: String = ""
    private var server: ServerSocket? = null
    @Volatile private var current: Session? = null
    private val connecting = AtomicBoolean(false)
    private val lastTry = ConcurrentHashMap<String, Long>()
    private val sending = Mutex()

    /** The port this phone listens on; 0 when it isn't. For tests. */
    val port: Int get() = server?.localPort ?: 0

    init {
        // Whatever changes here goes to the other phone while they're talking.
        @OptIn(FlowPreview::class)
        scope.launch {
            merge(dao.specimens(), dao.obs(), dao.trials(), dao.protocols(), dao.done(), dao.shots())
                .debounce(600).collect { current?.let { s -> runCatching { pushTo(s) }.onFailure { s.close() } } }
        }
    }

    @Synchronized fun start(reason: String) { reasons += reason; refresh() }

    @Synchronized fun stop(reason: String) { reasons -= reason; refresh() }

    /** Re-reads the settings: after pairing, unpairing, or a new QR. */
    @Synchronized
    fun refresh() {
        val s = runBlocking { settings.now() }
        val want = reasons.isNotEmpty() && s.secret.isNotEmpty()
        if (want && running != null && runningSecret == s.secret) return
        shutdown()
        if (want) launch(s.secret)
    }

    private fun launch(secretB64: String) {
        val secret = Base64.decode(secretB64, Base64.NO_WRAP)
        runningSecret = secretB64
        val key = Seal.key(secret)
        _near.update { it.copy(listening = true) }
        running = scope.launch(Dispatchers.IO) {
            val me = settings.deviceId()
            val ss = runCatching { ServerSocket(0) }.getOrNull() ?: return@launch
            synchronized(this@Link) {
                if (!isActive) return@launch ss.close()
                server = ss
            }
            finder?.start(Seal.tag(secret), me, ss.localPort) { host, port -> reach(host, port, key) }
            while (isActive && !ss.isClosed) {
                val socket = runCatching { ss.accept() }.getOrNull() ?: break
                launch { converse(socket, outgoing = false, key) }
            }
        }
    }

    private fun shutdown() {
        finder?.stop()
        running?.cancel()
        running = null
        runningSecret = ""
        runCatching { server?.close() }
        server = null
        current?.close()
        current = null
        lastTry.clear()
        _near.value = Near()
    }

    /** Someone answered the call at [host]:[port]. Talk to them, unless already talking. */
    fun reach(host: InetAddress, port: Int, sealKey: SecretKeySpec? = null) {
        val key = sealKey ?: Seal.key(Base64.decode(runningSecret.ifEmpty { return }, Base64.NO_WRAP))
        if (current?.alive == true) return
        val where = "${host.hostAddress}:$port"
        val now = System.currentTimeMillis()
        if (now - (lastTry[where] ?: 0) < RETRY_MS) return
        if (!connecting.compareAndSet(false, true)) return
        lastTry[where] = now
        scope.launch(Dispatchers.IO) {
            val socket = Socket()
            val ok = runCatching { socket.connect(InetSocketAddress(host, port), CONNECT_MS) }.isSuccess
            if (!ok) {
                connecting.set(false)
                runCatching { socket.close() }
                return@launch
            }
            converse(socket, outgoing = true, key)
        }
    }

    /** Sends what the other phone lacks, and remembers that it has it now. */
    private suspend fun pushTo(s: Session) = sending.withLock {
        val rows = Rows.missing(dao, photos, s.theirs)
        if (rows.isEmpty()) return@withLock
        s.sendRows(rows)
        rows.forEach { s.theirs[it.getString("u")] = Rows.stamp(it) }
    }

    private suspend fun converse(socket: Socket, outgoing: Boolean, key: SecretKeySpec) {
        val session = Session(socket, key)
        try {
            val s = settings.now()
            val me = settings.deviceId()
            socket.soTimeout = HANDSHAKE_MS
            session.handshake()
            val inv = JSONObject().apply { Rows.inventory(dao).forEach { (k, v) -> put(k, v) } }
            session.send(JSONObject().put("t", "hello").put("app", PROTOCOL).put("dev", me).put("name", s.researcher).put("inv", inv))
            val hello = session.receive()
            val peer = hello.optString("dev")
            if (hello.optString("t") != "hello" || peer.isEmpty() || peer == me) return
            if (s.partnerId.isNotEmpty() && peer != s.partnerId) return
            val name = hello.optString("name")
            settings.update { it.copy(partnerId = peer, partnerName = name.ifBlank { it.partnerName }, met = System.currentTimeMillis()) }
            session.initiator = if (outgoing) me else peer
            if (!settle(session, me, peer)) return
            if (outgoing) connecting.set(false)
            socket.soTimeout = IDLE_MS
            hello.optJSONObject("inv")?.let { o -> o.keys().forEach { k -> session.theirs[k] = o.getLong(k) } }
            _near.update { it.copy(together = true, received = 0) }
            onMet()
            pushTo(session)

            val pinger = scope.launch(Dispatchers.IO) {
                while (isActive && session.alive) {
                    delay(PING_MS)
                    runCatching { session.send(JSONObject().put("t", "ping")) }.onFailure { session.close() }
                }
            }
            try {
                while (session.alive) {
                    val m = session.receive()
                    when (m.optString("t")) {
                        "rows" -> {
                            val rows = Rows.list(m.getJSONArray("rows"))
                            // They have these now: don't send them back.
                            rows.forEach { session.theirs[it.getString("u")] = Rows.stamp(it) }
                            val n = sending.withLock { Rows.take(dao, photos, rows) }
                            if (n > 0) _near.update { it.copy(received = it.received + n) }
                        }
                        "bye" -> break
                    }
                }
            } finally {
                pinger.cancel()
            }
        } catch (e: Exception) {
            Log.d(TAG, "conversation ended: ${e.message}")
        } finally {
            if (outgoing) connecting.set(false)
            session.close()
            synchronized(this) {
                if (current === session) {
                    current = null
                    _near.update { it.copy(together = false) }
                }
            }
        }
    }

    /** Both phones may call each other at once: each keeps the conversation started by the smaller device id. */
    @Synchronized
    private fun settle(session: Session, me: String, peer: String): Boolean {
        val old = current
        if (old != null && old.alive) {
            val keepNew = old.initiator == session.initiator || session.initiator == minOf(me, peer)
            if (!keepNew) return false
            old.close()
        }
        current = session
        return true
    }

    /**
     * One sealed conversation. Each side opens with a random nonce; every frame after that is sealed with both nonces
     * and its own number as associated data, so frames can't be replayed from another conversation or shuffled.
     */
    private class Session(val socket: Socket, val key: SecretKeySpec) {
        private val out = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        private val inp = DataInputStream(BufferedInputStream(socket.getInputStream()))
        private val mine = Seal.nonce()
        private val theirsNonce = ByteArray(16)
        private var sent = 0L
        private var got = 0L
        var initiator = ""
        /** What the other phone holds: uid → stamp, kept up to date as rows travel. */
        val theirs = ConcurrentHashMap<String, Long>()
        @Volatile var alive = true
            private set

        fun handshake() {
            out.write(MAGIC); out.write(mine); out.flush()
            val magic = ByteArray(MAGIC.size)
            inp.readFully(magic)
            require(magic.contentEquals(MAGIC)) { "not a station" }
            inp.readFully(theirsNonce)
        }

        @Synchronized
        fun send(m: JSONObject) {
            val sealed = Seal.close(key, m.toString().toByteArray(), aad(mine, theirsNonce, sent))
            out.writeInt(sealed.size); out.write(sealed); out.flush()
            sent++
        }

        /** Rows go in batches, so a stack of photos doesn't become one giant frame. */
        fun sendRows(rows: List<JSONObject>) {
            var batch = mutableListOf<JSONObject>()
            var size = 0
            for (r in rows) {
                batch += r
                size += 300 + r.optString("img").length + r.optString("av").length
                if (size > BATCH_BYTES) {
                    send(JSONObject().put("t", "rows").put("rows", Rows.array(batch)))
                    batch = mutableListOf(); size = 0
                }
            }
            if (batch.isNotEmpty()) send(JSONObject().put("t", "rows").put("rows", Rows.array(batch)))
        }

        fun receive(): JSONObject {
            val size = inp.readInt()
            require(size in 13..MAX_FRAME) { "bad frame" }
            val sealed = ByteArray(size)
            inp.readFully(sealed)
            val plain = Seal.open(key, sealed, aad(theirsNonce, mine, got)) ?: error("not sealed with our key")
            got++
            return JSONObject(String(plain))
        }

        fun close() { alive = false; runCatching { socket.close() } }

        private fun aad(from: ByteArray, to: ByteArray, n: Long) = from + to + ByteBuffer.allocate(8).putLong(n).array()
    }

    companion object {
        private const val TAG = "SpecimenLink"
        private const val PROTOCOL = 1
        private val MAGIC = "SPCM1".toByteArray()
        private const val CONNECT_MS = 4_000
        private const val HANDSHAKE_MS = 10_000
        private const val IDLE_MS = 45_000
        private const val PING_MS = 15_000L
        private const val RETRY_MS = 4_000L
        private const val BATCH_BYTES = 512 * 1024
        private const val MAX_FRAME = 32 * 1024 * 1024
    }
}
