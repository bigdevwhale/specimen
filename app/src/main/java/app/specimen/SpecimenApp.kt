package app.specimen

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import app.specimen.data.DemoSeed
import app.specimen.data.Done
import app.specimen.data.Obs
import app.specimen.data.Photos
import app.specimen.data.Protocol
import app.specimen.data.SettingsStore
import app.specimen.data.Shot
import app.specimen.data.Specimen
import app.specimen.data.StationDb
import app.specimen.data.Trial
import app.specimen.data.now
import app.specimen.domain.Catalog
import app.specimen.domain.Lab
import app.specimen.domain.ProtoDef
import app.specimen.domain.today
import app.specimen.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = StationDb.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val photos = Photos(app)
    val catalog: Catalog get() = Catalog.get(app)
    /** On the main thread: Room and DataStore are main-safe. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** True while the demo build fills its station, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)

    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    private fun minute() = LocalTime.now().let { it.hour * 60 + it.minute }

    suspend fun lab(): Lab = Lab(settings.now(), catalog, dao.specimensNow(), dao.obsNow(), dao.trialsNow(), dao.protocolsNow(), dao.doneNow(), dao.shotsNow())

    // ── specimens ──

    /** Files a new specimen with the protocols it starts with; [lasts] gives the day each was last done (0 = unknown). */
    suspend fun addSpecimen(sp: Specimen, protos: List<ProtoDef>, lasts: Map<String, Long> = emptyMap()): Long {
        val id = dao.add(sp.copy(num = dao.lastNum() + 1))
        protos.forEach { dao.add(Protocol(specimen = id, key = it.key, every = it.every, last = lasts[it.key] ?: 0L)) }
        return id
    }

    suspend fun save(sp: Specimen) = dao.save(sp.copy(updated = now()))

    suspend fun endMission(sp: Specimen, note: String) = save(sp.copy(gone = today(), goneNote = note.trim()))

    suspend fun resume(sp: Specimen) = save(sp.copy(gone = 0, goneNote = ""))

    suspend fun delete(sp: Specimen) {
        dao.shotsNow().filter { it.specimen == sp.id }.forEach { dao.drop(it); photos.delete(it.id) }
        dao.dropObs(sp.id); dao.dropTrials(sp.id); dao.dropDone(sp.id); dao.dropProtocols(sp.id)
        dao.drop(sp)
    }

    // ── observations ──

    suspend fun log(o: Obs): Long = dao.add(o.copy(at = if (o.at == 0) minute() else o.at))

    suspend fun drop(o: Obs) = dao.drop(o)

    // ── field shots ──

    suspend fun addShot(specimen: Long, uri: Uri, tags: List<String>, note: String, day: Long = today()): Long {
        val bmp = photos.read(uri) ?: return 0
        val id = dao.add(Shot(specimen = specimen, day = day, tags = tags.joinToString(","), note = note.trim()))
        photos.keep(id, bmp)
        // The first photo of a specimen becomes its portrait.
        dao.specimen(specimen)?.takeIf { it.portrait == 0L }?.let { save(it.copy(portrait = id)) }
        return id
    }

    suspend fun save(s: Shot) = dao.save(s.copy(updated = now()))

    suspend fun drop(s: Shot) {
        dao.drop(s)
        photos.delete(s.id)
        dao.specimensNow().filter { it.portrait == s.id }.forEach { sp ->
            save(sp.copy(portrait = dao.shotsNow().firstOrNull { it.specimen == sp.id }?.id ?: 0))
        }
    }

    // ── experiments ──

    suspend fun start(t: Trial): Long = dao.add(t)

    suspend fun conclude(t: Trial, outcome: Int, reactions: List<String>, note: String) =
        dao.save(t.copy(outcome = outcome, reactions = reactions.joinToString(","), note = note.trim(), ended = today(), updated = now()))

    suspend fun save(t: Trial) = dao.save(t.copy(updated = now()))

    suspend fun drop(t: Trial) = dao.drop(t)

    // ── life support ──

    suspend fun markDone(p: Protocol, day: Long = today(), note: String = "") {
        val late = p.due?.let { it < day } ?: false
        dao.add(Done(protocol = p.id, specimen = p.specimen, day = day, late = late, note = note.trim()))
        if (day >= p.last) dao.save(p.copy(last = day, updated = now()))
    }

    suspend fun addProtocol(p: Protocol): Long = dao.add(p)

    suspend fun save(p: Protocol) = dao.save(p.copy(updated = now()))

    suspend fun drop(p: Protocol) {
        dao.dropDoneOf(p.id)
        dao.drop(p)
    }

    suspend fun drop(d: Done) = dao.drop(d)
}

class SpecimenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.LANG.isNotEmpty()) AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(BuildConfig.LANG))
        container = AppContainer(this)
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container)
            container.seeding.value = false
        }
        Reminders.schedule(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as SpecimenApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"

/** A context in the app's own language, for text made outside an activity. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}
