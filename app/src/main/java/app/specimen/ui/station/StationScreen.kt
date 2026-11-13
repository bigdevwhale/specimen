package app.specimen.ui.station

import app.specimen.ui.components.Typed
import app.specimen.ui.components.Sheet
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.data.Ob
import app.specimen.data.Specimen
import app.specimen.domain.Due
import app.specimen.domain.Lab
import app.specimen.domain.Mood
import app.specimen.domain.weekOf
import app.specimen.ui.BAR_SPACE
import app.specimen.ui.LogRequest
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Envoy
import app.specimen.ui.components.IconBtn
import app.specimen.ui.components.PILL
import app.specimen.ui.components.Paper
import app.specimen.ui.components.PlanetArt
import app.specimen.ui.components.Portrait
import app.specimen.ui.components.R8
import app.specimen.ui.components.SectionTitle
import app.specimen.ui.components.TopBar
import app.specimen.ui.ctx
import app.specimen.ui.rel
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import app.specimen.ui.tx

/**
 * The station: the specimen in its porthole, four console buttons, and one «today» list with only what needs
 * doing now. Everything else lives one tap away (the file, the reports, the course).
 */
@Composable
fun StationScreen(lab: Lab, nav: Nav) {
    val sp = lab.specimen(nav.current) ?: lab.here.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 14.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { if (sp != null) Pills(lab, nav) }
            IconBtn(Icons.Outlined.Settings, { nav.open(Overlay.Settings) }, desc = stringResource(R.string.settings))
        }
        if (sp == null) Empty(nav) else {
            Hero(sp, lab, nav)
            Today(sp, lab, nav)
        }
        Spacer(Modifier.size(BAR_SPACE))
    }
}

@Composable
private fun Empty(nav: Nav) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Envoy(Modifier.padding(top = 24.dp).size(220.dp))
        Text(stringResource(R.string.empty_title), style = T.Title, modifier = Modifier.padding(top = 12.dp))
        Text(stringResource(R.string.empty_body), style = T.Small, modifier = Modifier.padding(top = 8.dp))
        Btn(stringResource(R.string.register_specimen), { nav.open(Overlay.SpecimenEdit()) }, Modifier.padding(top = 20.dp).fillMaxWidth())
    }
}

/** One pill per specimen and a round «+» to register another. */
@Composable
private fun Pills(lab: Lab, nav: Nav) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        lab.here.forEach { sp ->
            val on = sp.id == nav.current && lab.here.size > 1
            Row(Modifier.clip(PILL).border(1.dp, if (on) C.teal else C.line, PILL)
                .clickable { nav.current = sp.id }.padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Portrait(sp, 26.dp)
                Text(sp.name, style = T.Strong.copy(fontSize = 14.sp, color = if (on || lab.here.size == 1) C.ink else C.muted), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Box(Modifier.size(36.dp).clip(CircleShape).border(1.dp, C.line, CircleShape).clickable { nav.open(Overlay.SpecimenEdit()) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, stringResource(R.string.add_specimen), tint = C.tealText, modifier = Modifier.size(20.dp))
        }
    }
}

/** The porthole, the name, one line under it, and the console. */
@Composable
private fun Hero(sp: Specimen, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val mood = lab.obsOf(sp.id).lastOrNull { it.kind == Ob.MOOD && it.day == lab.day }?.let { Mood.entries.getOrNull(it.value.toInt()) }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            PlanetArt(lab.planet(sp), Modifier.fillMaxSize().alpha(if (C.dark) .32f else .24f))
            Box(Modifier.clip(CircleShape).clickable { nav.open(Overlay.Dossier(sp.id)) }) { Portrait(sp, 184.dp, mood ?: Mood.CALM, glow = true) }
        }
        Text(sp.name, style = T.Hero, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 18.dp, end = 18.dp))
        Text(listOfNotNull(Words.planet(ctx, lab.planet(sp)), tx(lab.breed(sp).name), mood?.let { Words.mood(ctx, it).lowercase() }).joinToString(" · "),
            style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, start = 24.dp, end = 24.dp))
        Row(Modifier.padding(top = 22.dp, start = 18.dp, end = 18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Quick(Icons.Outlined.Mood, stringResource(R.string.q_mood)) { nav.log = LogRequest(sp.id, Ob.MOOD) }
            Quick(Icons.Outlined.Restaurant, stringResource(R.string.q_food)) { nav.log = LogRequest(sp.id, Ob.APPETITE) }
            Quick(Icons.Outlined.MonitorWeight, stringResource(R.string.q_weight)) { nav.log = LogRequest(sp.id, Ob.WEIGHT) }
            Quick(Icons.Outlined.CameraAlt, stringResource(R.string.q_shot)) { nav.open(Overlay.ShotEdit(sp.id, camera = true)) }
        }
    }
}

@Composable
private fun Quick(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(R8).clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).border(1.dp, C.line, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = C.tealText, modifier = Modifier.size(23.dp))
        }
        Text(label, style = T.Tiny.copy(color = C.muted), maxLines = 1, modifier = Modifier.padding(top = 6.dp))
    }
}

/** One thing to do now. */
private class Todo(val title: String, val sub: String, val color: androidx.compose.ui.graphics.Color, val tick: (() -> Unit)?, val open: () -> Unit)

/** The two most urgent things; the rest wait behind «more». A calm line when there is nothing. */
@Composable
private fun Today(sp: Specimen, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val many = lab.here.size > 1
    val last = weekOf(lab.day) - 7
    val todo = buildList {
        lab.here.forEach { s -> lab.anomalies(s).firstOrNull { it.urgent }?.let { a ->
            add(Todo(Words.anomaly(ctx, a), (if (many) s.name + " · " else "") + ctx.getString(R.string.vet_today), C.stamp, null) { nav.open(Overlay.Dossier(s.id)) }) } }
        lab.here.forEach { s -> lab.dues(s.id).filter { it.state == Due.State.OVERDUE || it.state == Due.State.TODAY }.forEach { d ->
            add(Todo(d.name.of(ctx.resources.configuration.locales[0].language == "ru"), (if (many) s.name + " · " else "") + (d.day?.let { Words.rel(ctx, it, lab.day) } ?: ""),
                if (d.state == Due.State.OVERDUE) C.stamp else C.muted, { c.write { markDone(d.p) } }) { nav.open(Overlay.Protocols(s.id)) }) } }
        lab.trials.filter { it.open && lab.day - it.start >= it.days && lab.specimen(it.specimen)?.here == true }.forEach { t ->
            add(Todo(t.subject, ctx.getString(R.string.time_to_conclude), C.tealText, null) { nav.open(Overlay.Trial(t.id)) }) }
        lab.here.filter { last in lab.reportWeeks(it.id) && "${it.id}:$last" !in lab.s.read }.forEach { s ->
            add(Todo(ctx.getString(R.string.report_ready_b, s.name), ctx.getString(R.string.report_n, lab.reportNumber(s.id, last)), C.tealText, null) { nav.open(Overlay.Report(s.id, last)) }) }
    }
    var all by remember { mutableStateOf(false) }
    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 26.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Typed(stringResource(R.string.today_title), Modifier.weight(1f))
            if (todo.size > 2) Text(stringResource(R.string.more_n, todo.size - 2), style = T.Small.copy(color = C.tealText), modifier = Modifier.clip(R8).clickable { all = true }.padding(4.dp))
        }
        Spacer(Modifier.size(8.dp))
        if (todo.isEmpty()) Text(stringResource(R.string.today_clear, sp.name), style = T.Small)
        todo.take(2).forEach { TodoRow(it) }
    }
    if (all) Sheet(stringResource(R.string.today_title), { all = false }) { todo.forEach { TodoRow(it) } }
}

@Composable
private fun TodoRow(t: Todo) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(R8).border(1.dp, C.line, R8).clickable(onClick = t.open).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(t.title, style = T.Strong.copy(fontSize = 14.5.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.sub, style = T.TypedSmall.copy(color = t.color))
        }
        val tick = t.tick
        if (tick != null) Box(Modifier.size(34.dp).clip(CircleShape).border(1.dp, C.teal, CircleShape).clickable(onClick = tick), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, stringResource(R.string.done), tint = C.tealText, modifier = Modifier.size(17.dp))
        } else Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = C.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Item(title: String, sub: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(R8).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = T.Strong.copy(fontSize = 14.5.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, style = T.TypedSmall.copy(color = color))
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = C.muted, modifier = Modifier.size(18.dp))
    }
}
