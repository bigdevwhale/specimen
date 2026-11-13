package app.specimen.sync

import android.graphics.BitmapFactory
import android.util.Base64
import app.specimen.data.Done
import app.specimen.data.Obs
import app.specimen.data.Photos
import app.specimen.data.Protocol
import app.specimen.data.Shot
import app.specimen.data.Specimen
import app.specimen.data.StationDao
import app.specimen.data.Tomb
import app.specimen.data.Trial
import org.json.JSONArray
import org.json.JSONObject

/**
 * The station's rows as they travel between two paired phones. Local ids differ from phone to phone, so every row
 * goes by its [uid], and links to other rows (a protocol's specimen, a done mark's protocol) go by their uids too.
 * Each row's [updated] stamp decides: the newer edit wins. A deleted row travels as a tombstone, so the other phone
 * deletes it too instead of sending it back. Photos ride with their shots, an avatar with its specimen.
 */
object Rows {
    /** uid → stamp: positive for a live row (its updated), negative for a tombstone (−deleted at). */
    suspend fun inventory(dao: StationDao): Map<String, Long> {
        val m = HashMap<String, Long>()
        dao.specimensNow().forEach { m[it.uid] = it.updated }
        dao.obsNow().forEach { m[it.uid] = it.updated }
        dao.trialsNow().forEach { m[it.uid] = it.updated }
        dao.protocolsNow().forEach { m[it.uid] = it.updated }
        dao.doneNow().forEach { m[it.uid] = it.updated }
        dao.shotsNow().forEach { m[it.uid] = it.updated }
        dao.tombsNow().forEach { t -> if ((m[t.uid] ?: 0L) <= t.at) m[t.uid] = -t.at }
        return m
    }

    /** Whether a stamp [mine] is newer than [theirs] (a tombstone wins a tie). */
    fun newer(mine: Long, theirs: Long?): Boolean {
        if (theirs == null) return true
        val a = kotlin.math.abs(mine); val b = kotlin.math.abs(theirs)
        return a > b || (a == b && mine < 0 && theirs > 0)
    }

    /** Of everything here, what the other phone (holding [theirs]) lacks or has older, ready to send. */
    suspend fun missing(dao: StationDao, photos: Photos, theirs: Map<String, Long>): List<JSONObject> {
        val mine = inventory(dao)
        val want = mine.filter { (uid, st) -> newer(st, theirs[uid]) }.keys
        if (want.isEmpty()) return emptyList()
        val sp = dao.specimensNow(); val spUid = sp.associate { it.id to it.uid }
        val pr = dao.protocolsNow(); val prUid = pr.associate { it.id to it.uid }
        val sh = dao.shotsNow(); val shUid = sh.associate { it.id to it.uid }
        val out = mutableListOf<JSONObject>()
        sp.filter { it.uid in want }.forEach { s ->
            out += JSONObject().put("tb", "sp").put("u", s.uid).put("up", s.updated).put("num", s.num).put("name", s.name).put("planet", s.planet)
                .put("breed", s.breed).put("sex", s.sex).put("born", s.born).put("arrived", s.arrived).put("neut", s.neutered)
                .put("gone", s.gone).put("goneNote", s.goneNote).apply {
                    when {
                        s.portrait < 0 -> { put("pa", true); file(photos.photo(s.portrait))?.let { put("av", it) } }
                        s.portrait > 0 -> shUid[s.portrait]?.let { put("ps", it) }
                    }
                }
        }
        pr.filter { it.uid in want }.forEach { p ->
            out += JSONObject().put("tb", "pr").put("u", p.uid).put("up", p.updated).put("sp", spUid[p.specimen] ?: return@forEach)
                .put("key", p.key).put("title", p.title).put("every", p.every).put("last", p.last).put("note", p.note).put("off", p.off)
        }
        dao.obsNow().filter { it.uid in want }.forEach { o ->
            out += JSONObject().put("tb", "ob").put("u", o.uid).put("up", o.updated).put("sp", spUid[o.specimen] ?: return@forEach)
                .put("day", o.day).put("at", o.at).put("kind", o.kind).put("v", o.value.toDouble()).put("note", o.note)
        }
        dao.trialsNow().filter { it.uid in want }.forEach { t ->
            out += JSONObject().put("tb", "tr").put("u", t.uid).put("up", t.updated).put("sp", spUid[t.specimen] ?: return@forEach)
                .put("kind", t.kind).put("subject", t.subject).put("guess", t.guess).put("start", t.start).put("days", t.days)
                .put("out", t.outcome).put("re", t.reactions).put("note", t.note).put("ended", t.ended)
        }
        dao.doneNow().filter { it.uid in want }.forEach { d ->
            out += JSONObject().put("tb", "dn").put("u", d.uid).put("up", d.updated).put("sp", spUid[d.specimen] ?: return@forEach)
                .put("pr", prUid[d.protocol] ?: return@forEach).put("day", d.day).put("late", d.late).put("note", d.note)
        }
        sh.filter { it.uid in want }.forEach { s ->
            out += JSONObject().put("tb", "sh").put("u", s.uid).put("up", s.updated).put("sp", spUid[s.specimen] ?: return@forEach)
                .put("day", s.day).put("tags", s.tags).put("note", s.note).apply { file(photos.photo(s.id))?.let { put("img", it) } }
        }
        dao.tombsNow().filter { it.uid in want && (mine[it.uid] ?: 0) < 0 }.forEach { out += JSONObject().put("tb", "x").put("u", it.uid).put("at", it.at) }
        return out
    }

    private fun file(f: java.io.File): String? = if (f.exists()) Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else null

    private fun bytes(o: JSONObject, k: String): ByteArray? = o.optString(k).takeIf { it.isNotEmpty() }?.let { Base64.decode(it, Base64.NO_WRAP) }

    /** The stamp a travelling row carries. */
    fun stamp(o: JSONObject): Long = if (o.optString("tb") == "x") -o.getLong("at") else o.getLong("up")

    /**
     * Takes in rows from the other phone, keeping whichever is newer. Parents go first (specimens, then protocols),
     * so links resolve; a specimen's portrait shot is set once the shots are in. Returns how many rows changed here.
     */
    suspend fun take(dao: StationDao, photos: Photos, rows: List<JSONObject>): Int {
        if (rows.isEmpty()) return 0
        val mine = inventory(dao)
        val fresh = rows.filter { newer(stamp(it), mine[it.optString("u")]) }
        if (fresh.isEmpty()) return 0
        val order = listOf("x", "sp", "pr", "ob", "tr", "dn", "sh")
        var changed = 0
        val portraits = mutableMapOf<String, String>()
        for (tb in order) for (o in fresh.filter { it.optString("tb") == tb }) {
            val uid = o.getString("u")
            val ok = runCatching {
                when (tb) {
                    "x" -> { bury(dao, photos, uid); dao.bury(Tomb(uid, o.getLong("at"))) }
                    "sp" -> {
                        val old = dao.specimensNow().firstOrNull { it.uid == uid }
                        val num = old?.num ?: o.getInt("num").let { n -> if (dao.specimensNow().any { it.num == n }) dao.lastNum() + 1 else n }
                        val base = Specimen(id = old?.id ?: 0, num = num, name = o.getString("name"), planet = o.getString("planet"), breed = o.getString("breed"),
                            sex = o.optInt("sex"), born = o.optLong("born"), arrived = o.getLong("arrived"), neutered = o.optBoolean("neut"),
                            portrait = old?.portrait ?: 0, gone = o.optLong("gone"), goneNote = o.optString("goneNote"), uid = uid, updated = o.getLong("up"))
                        val id = if (old == null) dao.add(base) else { dao.save(base); old.id }
                        if (o.optBoolean("pa")) {
                            bytes(o, "av")?.let { b -> BitmapFactory.decodeByteArray(b, 0, b.size)?.let { photos.keep(-id, it) } }
                            dao.save(base.copy(id = id, portrait = -id))
                        } else o.optString("ps").takeIf { it.isNotEmpty() }?.let { portraits[uid] = it }
                    }
                    "pr" -> {
                        val sp = specimen(dao, o) ?: return@runCatching false
                        val old = dao.protocolsNow().firstOrNull { it.uid == uid }
                        val p = Protocol(id = old?.id ?: 0, specimen = sp, key = o.getString("key"), title = o.optString("title"), every = o.getInt("every"),
                            last = o.optLong("last"), note = o.optString("note"), off = o.optBoolean("off"), uid = uid, updated = o.getLong("up"))
                        if (old == null) dao.add(p) else dao.save(p)
                    }
                    "ob" -> {
                        val sp = specimen(dao, o) ?: return@runCatching false
                        val old = dao.obsNow().firstOrNull { it.uid == uid }
                        val x = Obs(id = old?.id ?: 0, specimen = sp, day = o.getLong("day"), at = o.optInt("at"), kind = o.getInt("kind"),
                            value = o.optDouble("v").toFloat(), note = o.optString("note"), uid = uid, updated = o.getLong("up"))
                        if (old == null) dao.add(x) else dao.save(x)
                    }
                    "tr" -> {
                        val sp = specimen(dao, o) ?: return@runCatching false
                        val old = dao.trialsNow().firstOrNull { it.uid == uid }
                        val x = Trial(id = old?.id ?: 0, specimen = sp, kind = o.getInt("kind"), subject = o.getString("subject"), guess = o.optInt("guess"),
                            start = o.getLong("start"), days = o.getInt("days"), outcome = o.optInt("out", -1), reactions = o.optString("re"),
                            note = o.optString("note"), ended = o.optLong("ended"), uid = uid, updated = o.getLong("up"))
                        if (old == null) dao.add(x) else dao.save(x)
                    }
                    "dn" -> {
                        val sp = specimen(dao, o) ?: return@runCatching false
                        val pr = dao.protocolsNow().firstOrNull { it.uid == o.getString("pr") }?.id ?: return@runCatching false
                        if (dao.doneNow().none { it.uid == uid })
                            dao.add(Done(protocol = pr, specimen = sp, day = o.getLong("day"), late = o.optBoolean("late"), note = o.optString("note"), uid = uid, updated = o.getLong("up")))
                    }
                    "sh" -> {
                        val sp = specimen(dao, o) ?: return@runCatching false
                        val old = dao.shotsNow().firstOrNull { it.uid == uid }
                        val x = Shot(id = old?.id ?: 0, specimen = sp, day = o.getLong("day"), tags = o.optString("tags"), note = o.optString("note"), uid = uid, updated = o.getLong("up"))
                        val id = if (old == null) dao.add(x) else { dao.save(x); old.id }
                        bytes(o, "img")?.let { b -> BitmapFactory.decodeByteArray(b, 0, b.size)?.let { photos.keep(id, it) } }
                    }
                }
                true
            }.getOrDefault(false)
            if (ok) changed++
        }
        // Portraits that point at a shot, now that the shots are here.
        val shots = dao.shotsNow().associate { it.uid to it.id }
        portraits.forEach { (spUid, shotUid) ->
            val s = dao.specimensNow().firstOrNull { it.uid == spUid } ?: return@forEach
            shots[shotUid]?.let { if (s.portrait != it) dao.save(s.copy(portrait = it)) }
        }
        return changed
    }

    private suspend fun specimen(dao: StationDao, o: JSONObject): Long? = dao.specimensNow().firstOrNull { it.uid == o.getString("sp") }?.id

    /** Deletes whatever row has this uid, with what hangs on it. */
    private suspend fun bury(dao: StationDao, photos: Photos, uid: String) {
        dao.specimensNow().firstOrNull { it.uid == uid }?.let { sp ->
            dao.shotsNow().filter { it.specimen == sp.id }.forEach { dao.drop(it); photos.delete(it.id) }
            photos.delete(-sp.id)
            dao.dropObs(sp.id); dao.dropTrials(sp.id); dao.dropDone(sp.id); dao.dropProtocols(sp.id)
            dao.drop(sp); return
        }
        dao.obsNow().firstOrNull { it.uid == uid }?.let { dao.drop(it); return }
        dao.trialsNow().firstOrNull { it.uid == uid }?.let { dao.drop(it); return }
        dao.protocolsNow().firstOrNull { it.uid == uid }?.let { dao.dropDoneOf(it.id); dao.drop(it); return }
        dao.doneNow().firstOrNull { it.uid == uid }?.let { dao.drop(it); return }
        dao.shotsNow().firstOrNull { it.uid == uid }?.let { dao.drop(it); photos.delete(it.id); return }
    }

    fun array(list: List<JSONObject>) = JSONArray().apply { list.forEach { put(it) } }
    fun list(a: JSONArray) = (0 until a.length()).mapNotNull { runCatching { a.getJSONObject(it) }.getOrNull() }
}
