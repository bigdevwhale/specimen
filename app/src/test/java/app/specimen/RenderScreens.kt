package app.specimen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.specimen.data.DemoSeed
import app.specimen.data.Ob
import app.specimen.data.Settings
import app.specimen.domain.weekOf
import app.specimen.domain.today
import app.specimen.ui.LogRequest
import app.specimen.ui.Overlay
import app.specimen.ui.StationRoot
import app.specimen.ui.Tab
import app.specimen.ui.onboarding.Onboarding
import app.specimen.ui.theme.BureauTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The main screens over the demo station, drawn to PNGs (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    open val theme = 0
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }
    private val app get() = ApplicationProvider.getApplicationContext<SpecimenApp>()

    private fun seed(): Settings = runBlocking {
        val c = app.container
        c.seeding.first { !it }
        if (c.dao.specimensNow().isEmpty()) DemoSeed.seedIfEmpty(c)
        c.seeding.value = false
        c.settings.update { it.copy(tipsOff = true) }
        c.settings.now()
    }

    private fun save(name: String) {
        // A frame caught between two compositions comes out blank: wait a little and draw again.
        repeat(8) { attempt ->
            var blank = false
            rule.runOnUiThread {
                val view = rule.activity.window.decorView
                val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(image))
                val probe = listOf(image.getPixel(view.width / 2, view.height / 3), image.getPixel(view.width / 4, view.height / 2), image.getPixel(view.width / 2, view.height * 3 / 4))
                blank = probe.all { it == probe[0] } && android.graphics.Color.red(probe[0]) > 240
                if (!blank || attempt == 7) File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
            if (!blank) return
            rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle(); Thread.sleep(500)
        }
    }

    private fun settle(waitFor: String?) {
        rule.waitUntil(30_000) { rule.mainClock.advanceTimeBy(100); waitFor == null || rule.onAllNodesWithText(waitFor, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        Thread.sleep(700)
        rule.mainClock.advanceTimeBy(800)
    }

    data class Sc(val name: String, val tab: Tab, val o: Overlay? = null, val menu: Boolean = false, val log: LogRequest? = null)

    @Test fun screens() {
        val st = seed()
        val c = app.container
        val (cat, trial, shot) = runBlocking { Triple(c.dao.specimensNow().first().id, c.dao.trialsNow().first { it.open }.id, c.dao.shotsNow().firstOrNull()?.id ?: 0L) }
        val week = weekOf(today()) - 7
        val list = listOfNotNull(
            Sc("1-station", Tab.STATION),
            Sc("2-dossier", Tab.STATION, Overlay.Dossier(cat)),
            Sc("3-report", Tab.REPORTS, Overlay.Report(cat, week)),
            Sc("4-atlas", Tab.ATLAS),
            Sc("5-reports", Tab.REPORTS),
            Sc("6-course", Tab.COURSE),
            Sc("7-trial", Tab.STATION, Overlay.Trial(trial)),
            Sc("8-new-trial", Tab.STATION, Overlay.TrialEdit(cat)),
            Sc("9-mood", Tab.STATION, log = LogRequest(cat, Ob.MOOD)),
            Sc("9b-appetite", Tab.STATION, log = LogRequest(cat, Ob.APPETITE)),
            Sc("9c-weight", Tab.STATION, log = LogRequest(cat, Ob.WEIGHT)),
            Sc("g-shot-new", Tab.STATION, Overlay.ShotEdit(cat, camera = false)),
            Sc("a-menu", Tab.STATION, menu = true),
            if (shot > 0) Sc("b-shot", Tab.STATION, Overlay.Shot(shot)) else null,
            Sc("c-register", Tab.STATION, Overlay.SpecimenEdit()),
            Sc("d-protocols", Tab.STATION, Overlay.Protocols(cat)),
            Sc("e-settings", Tab.STATION, Overlay.Settings),
            Sc("f-lesson", Tab.COURSE, Overlay.Lesson("cat_body")),
        )
        var which by mutableStateOf(list[0])
        rule.mainClock.autoAdvance = false
        rule.setContent { BureauTheme(theme) { key(which.name) { StationRoot(st.copy(theme = theme), which.tab, which.o, which.menu, which.log) } } }
        for (sc in list) {
            rule.runOnUiThread { which = sc }
            settle(if (sc.o == null && !sc.menu && sc.log == null) app.getString(R.string.tab_course) else null)
            save(sc.name)
        }
    }

    @Test fun welcome() {
        rule.mainClock.autoAdvance = false
        rule.setContent { BureauTheme(theme) { Onboarding() } }
        settle(null)
        save("0-welcome")
    }
}

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRuDay : ScreensBase() { override val lang = "ru-day"; override val theme = 1 }
