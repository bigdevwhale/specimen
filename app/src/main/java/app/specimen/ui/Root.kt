package app.specimen.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.data.Settings
import app.specimen.domain.Lab
import app.specimen.ui.atlas.AtlasScreen
import app.specimen.ui.atlas.BehaviourPage
import app.specimen.ui.components.LocalAbove
import app.specimen.ui.components.R8
import app.specimen.ui.components.Tip
import app.specimen.ui.components.TipOf
import app.specimen.ui.components.desk
import app.specimen.ui.course.CourseScreen
import app.specimen.ui.course.LessonPage
import app.specimen.ui.dossier.DossierPage
import app.specimen.ui.dossier.FoodsPage
import app.specimen.ui.dossier.ProtocolsPage
import app.specimen.ui.dossier.SpecimenEdit
import app.specimen.ui.log.AddMenu
import app.specimen.ui.log.LogSheet
import app.specimen.ui.log.ShotEdit
import app.specimen.ui.log.ShotPage
import app.specimen.ui.log.TrialEdit
import app.specimen.ui.log.TrialPage
import app.specimen.ui.onboarding.Onboarding
import app.specimen.ui.report.ReportPage
import app.specimen.ui.report.ReportsScreen
import app.specimen.ui.settings.SettingsPage
import app.specimen.ui.station.StationScreen
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T

enum class Tab { STATION, ATLAS, REPORTS, COURSE }

sealed interface Overlay {
    data class Dossier(val id: Long) : Overlay
    /** A new specimen (id 0) or editing one. */
    data class SpecimenEdit(val id: Long = 0) : Overlay
    data class Shot(val id: Long) : Overlay
    /** A new field shot of [specimen]; [camera] opens the camera straight away, otherwise the picker. */
    data class ShotEdit(val specimen: Long, val camera: Boolean) : Overlay
    data class Trial(val id: Long) : Overlay
    data class TrialEdit(val specimen: Long, val kind: Int = -1) : Overlay
    data class Report(val specimen: Long, val week: Long) : Overlay
    data class Behaviour(val key: String) : Overlay
    data class Lesson(val key: String) : Overlay
    data class Protocols(val specimen: Long) : Overlay
    data class Foods(val specimen: Long) : Overlay
    data object Settings : Overlay
}

/** An observation sheet over whatever is open: [kind] is an [app.specimen.data.Ob] kind, 0 lets the researcher pick. */
data class LogRequest(val specimen: Long, val kind: Int = 0)

class Nav(tab: Tab, first: Overlay?) {
    var tab by mutableStateOf(tab)
    val stack = mutableStateListOf<Overlay>().apply { if (first != null) add(first) }
    var menu by mutableStateOf(false)
    var log by mutableStateOf<LogRequest?>(null)
    /** The specimen the station is looking at. */
    var current by mutableLongStateOf(0L)
    fun open(o: Overlay) { menu = false; stack.add(o) }
    fun close() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(o: Overlay) { close(); open(o) }
    fun home(t: Tab) { menu = false; tab = t; stack.clear() }
}

@Composable
fun StationRoot(s: Settings, tab: Tab = Tab.STATION, overlay: Overlay? = null, menu: Boolean = false, log: LogRequest? = null) {
    val c = LocalContext.current.container
    if (!s.onboarded) { Onboarding(); return }

    val specimens by remember { c.dao.specimens() }.collectAsState(null)
    val obs by remember { c.dao.obs() }.collectAsState(null)
    val trials by remember { c.dao.trials() }.collectAsState(null)
    val protocols by remember { c.dao.protocols() }.collectAsState(null)
    val done by remember { c.dao.done() }.collectAsState(null)
    val shots by remember { c.dao.shots() }.collectAsState(null)
    val nav = remember { Nav(tab, overlay).also { it.menu = menu; it.log = log } }

    val lab = Lab(s, c.catalog, specimens ?: return, obs ?: return, trials ?: return, protocols ?: return, done ?: return, shots ?: return)
    if (lab.here.none { it.id == nav.current }) nav.current = lab.here.firstOrNull()?.id ?: 0L

    Box(Modifier.fillMaxSize().desk()) {
        // Only the top layer is heard by screen readers; the ones under it are still kept, so going back keeps their state.
        Box(Modifier.fillMaxSize().then(if (nav.stack.isNotEmpty()) Modifier.clearAndSetSemantics {} else Modifier)) {
            val above = remember { mutableStateListOf<@Composable () -> Unit>() }
            CompositionLocalProvider(LocalAbove provides above) {
                when (nav.tab) {
                    Tab.STATION -> StationScreen(lab, nav)
                    Tab.ATLAS -> AtlasScreen(lab, nav)
                    Tab.REPORTS -> ReportsScreen(lab, nav)
                    Tab.COURSE -> CourseScreen(lab, nav)
                }
            }
            StatusScrim()
            TabBar(nav, lab, Modifier.align(Alignment.BottomCenter))
            above.forEach { it() }
        }
        if (nav.stack.isNotEmpty()) BackHandler { nav.close() }
        nav.stack.forEachIndexed { i, o -> Box(Modifier.fillMaxSize().desk()
            // A page swallows the touches that land on its empty parts, so they never reach the page under it.
            .clickable(remember { MutableInteractionSource() }, null) {}
            .then(if (i < nav.stack.lastIndex) Modifier.clearAndSetSemantics {} else Modifier)) {
            when (o) {
                is Overlay.Dossier -> DossierPage(o.id, lab, nav)
                is Overlay.SpecimenEdit -> SpecimenEdit(o.id, lab, nav)
                is Overlay.Shot -> ShotPage(o.id, lab, nav)
                is Overlay.ShotEdit -> ShotEdit(o, lab, nav)
                is Overlay.Trial -> TrialPage(o.id, lab, nav)
                is Overlay.TrialEdit -> TrialEdit(o, lab, nav)
                is Overlay.Report -> ReportPage(o.specimen, o.week, lab, nav)
                is Overlay.Behaviour -> BehaviourPage(o.key, lab, nav)
                is Overlay.Lesson -> LessonPage(o.key, lab, nav)
                is Overlay.Protocols -> ProtocolsPage(o.specimen, lab, nav)
                is Overlay.Foods -> FoodsPage(o.specimen, lab, nav)
                Overlay.Settings -> SettingsPage(lab, nav)
            }
            StatusScrim()
        } }
        if (nav.menu) AddMenu(lab, nav)
        nav.log?.let { LogSheet(it, lab, nav) }
        // The first visit to each screen explains it once.
        if (!nav.menu && nav.log == null) {
            val tip = when (val top = nav.stack.lastOrNull()) {
                null -> if (lab.here.isEmpty()) null else when (nav.tab) { Tab.STATION -> TipOf.STATION; Tab.ATLAS -> TipOf.ATLAS; Tab.REPORTS -> TipOf.REPORTS; Tab.COURSE -> TipOf.COURSE }
                is Overlay.Dossier -> TipOf.DOSSIER
                is Overlay.TrialEdit -> TipOf.TRIAL
                is Overlay.ShotEdit -> TipOf.SHOT
                else -> null
            }
            if (tip != null) androidx.compose.runtime.key(tip) { Tip(tip, s) }
        }
    }
}

/** Station, atlas, the teal «+», reports, course. */
@Composable
private fun TabBar(nav: Nav, lab: Lab, modifier: Modifier) {
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, C.desk.copy(alpha = .96f), C.desk), startY = 0f, endY = 60f))) {
        Row(Modifier.navigationBarsPadding().padding(top = 16.dp, bottom = 6.dp).fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            @Composable
            fun item(t: Tab, label: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
                val on = nav.tab == t
                Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { nav.home(t) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = if (on) C.tealText else C.muted, modifier = Modifier.size(24.dp))
                    Text(stringResource(label).uppercase(), style = T.Tab.copy(color = if (on) C.tealText else C.muted, fontSize = 9.5.sp, letterSpacing = 0.8.sp), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item(Tab.STATION, R.string.tab_station, Icons.Outlined.Science)
            item(Tab.ATLAS, R.string.tab_atlas, Icons.Outlined.GridView)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.offset(y = (-8).dp).size(60.dp).shadow(16.dp, androidx.compose.foundation.shape.CircleShape, ambientColor = C.teal, spotColor = C.teal).clip(androidx.compose.foundation.shape.CircleShape).background(C.teal)
                    .clickable { if (lab.here.isEmpty()) nav.open(Overlay.SpecimenEdit()) else nav.menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = C.onTeal, modifier = Modifier.size(30.dp))
                }
            }
            item(Tab.REPORTS, R.string.tab_reports, Icons.Outlined.Description)
            item(Tab.COURSE, R.string.tab_course, Icons.Outlined.School)
        }
    }
}

/** Room to leave at the bottom of a tab's list so the bar doesn't hide its end. */
val BAR_SPACE = 110.dp

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** A strip behind the status bar in the desk's colour so scrolled lists stay readable under it. */
@Composable
fun StatusScrim() {
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars).background(C.desk))
}
