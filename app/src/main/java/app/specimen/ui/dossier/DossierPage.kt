package app.specimen.ui.dossier

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import app.specimen.ui.components.PILL
import app.specimen.ui.components.PlanetArt
import androidx.compose.material.icons.rounded.Check
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.data.Ob
import app.specimen.data.Obs
import app.specimen.data.Specimen
import app.specimen.domain.Due
import app.specimen.domain.Lab
import app.specimen.domain.Mood
import app.specimen.domain.Outcome
import app.specimen.domain.Symptom
import app.specimen.domain.Trait
import app.specimen.domain.TrialKind
import app.specimen.export.VetPdf
import app.specimen.ui.LogRequest
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Dashes
import app.specimen.ui.components.Field
import app.specimen.ui.components.IconBtn
import app.specimen.ui.components.KV
import app.specimen.ui.components.Kind
import app.specimen.ui.components.Paper
import app.specimen.ui.components.Portrait
import app.specimen.ui.components.R8
import app.specimen.ui.components.Radar
import app.specimen.ui.components.SectionTitle
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.Stamp
import app.specimen.ui.components.Tag
import app.specimen.ui.components.Tone
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.components.WeightChart
import app.specimen.ui.components.fmtKg
import app.specimen.ui.ctx
import app.specimen.ui.log.ShotTile
import app.specimen.ui.log.kindName
import app.specimen.ui.rel
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import app.specimen.ui.tx

@Composable
fun DossierPage(id: Long, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val sp = lab.specimen(id) ?: run { nav.close(); return }
    val planet = lab.planet(sp)
    val breed = lab.breed(sp)
    var ending by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(if (lab.anomalies(sp).any { it.urgent }) 1 else 0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(sp.name, over = Words.num(ctx, sp), onBack = { nav.close() }) {
            IconBtn(Icons.Outlined.Edit, { nav.open(Overlay.SpecimenEdit(sp.id)) }, desc = stringResource(R.string.edit))
        }
        Row(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(CircleShape).clickable { nav.open(Overlay.SpecimenEdit(sp.id)) }) { Portrait(sp, 84.dp) }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(tx(breed.name), style = T.Heading)
                Text(listOfNotNull(Words.planet(ctx, planet), if (sp.born > 0) Words.age(ctx, sp.born, lab.day) else null).joinToString(" · "), style = T.Small)
                if (!sp.here) Text(stringResource(R.string.stamp_mission_over), style = T.Small.copy(color = C.tealText))
            }
            PlanetArt(planet, Modifier.size(56.dp))
        }
        // Three tabs instead of one long file.
        Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp).fillMaxWidth().clip(PILL).background(C.paper).padding(4.dp)) {
            listOf(R.string.tab_profile, R.string.tab_health, R.string.tab_journal).forEachIndexed { i, l ->
                Text(stringResource(l), style = T.Strong.copy(fontSize = 14.sp, color = if (tab == i) C.onTeal else C.muted), textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(PILL).background(if (tab == i) C.teal else Color.Transparent).clickable { tab = i }.padding(vertical = 9.dp))
            }
        }
        when (tab) {
            0 -> Profile(sp, lab) { ending = true }
            1 -> Health(sp, lab, nav)
            else -> Journal(sp, lab, nav)
        }
        Spacer(Modifier.size(28.dp))
    }
    if (ending) EndMission(sp) { ending = false }
}

@Composable
private fun Profile(sp: Specimen, lab: Lab, onEnd: () -> Unit) {
    val ctx = ctx()
    val c = ctx.container
    val breed = lab.breed(sp)
    Paper(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp).fillMaxWidth(), color = C.screen) {
        Radar(lab.traits(sp), breed.traits, Trait.entries.map { Words.traitShort(ctx, it) }, Modifier.fillMaxWidth().aspectRatio(1.05f))
        Text(stringResource(R.string.profile_legend_short), style = T.TypedSmall.copy(color = C.phosphor.copy(alpha = .7f), fontSize = 11.sp))
    }
    if (tx(breed.lore).isNotBlank()) Text("«" + tx(breed.lore).trimEnd('.') + "»", style = T.Small.copy(fontStyle = FontStyle.Italic),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp))
    val risks = breed.risks.mapNotNull { lab.cat.risk(it) }
    if (risks.isNotEmpty()) {
        SectionTitle(stringResource(R.string.risks))
        Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 4.dp) {
            risks.forEach { r ->
                var open by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().clip(R8).clickable { open = !open }.padding(horizontal = 10.dp, vertical = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tx(r.name), style = T.Strong.copy(fontSize = 14.5.sp), modifier = Modifier.weight(1f))
                        Text(if (open) "–" else "+", style = T.Strong.copy(color = C.tealText))
                    }
                    if (open) Text(tx(r.watch), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    SectionTitle(stringResource(R.string.passport))
    Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
        if (sp.sex > 0) KV(stringResource(R.string.k_sex), Words.sex(ctx, sp.sex) + if (sp.neutered) ", " + stringResource(R.string.neutered).lowercase() else "")
        if (sp.born > 0) KV(stringResource(R.string.k_born), Words.dateFull(ctx, sp.born))
        KV(stringResource(R.string.k_arrived), Words.dateFull(ctx, sp.arrived))
        if (breed.wMax > 0f) KV(stringResource(R.string.k_norm), stringResource(R.string.kg_range, fmtKg(breed.wMin), fmtKg(breed.wMax)))
        if (breed.life.isNotBlank()) KV(stringResource(R.string.k_life), stringResource(R.string.years_range, breed.life))
        if (!sp.here && sp.goneNote.isNotBlank()) KV(stringResource(R.string.k_gone_note), sp.goneNote)
    }
    Text(stringResource(if (sp.here) R.string.end_mission else R.string.resume_mission), style = T.Small,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp).clip(R8).clickable { if (sp.here) onEnd() else c.write { resume(sp) } }.padding(6.dp))
}

@Composable
private fun Health(sp: Specimen, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val breed = lab.breed(sp)
    val anomalies = lab.anomalies(sp)
    if (anomalies.isNotEmpty()) {
        Paper(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp).fillMaxWidth(), color = C.stamp.copy(alpha = .14f)) {
            anomalies.forEach { a ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Words.anomaly(ctx, a), style = T.Strong.copy(fontSize = 14.5.sp), modifier = Modifier.weight(1f))
                    if (a.urgent) Tag(stringResource(R.string.vet_today), Tone.ALARM)
                }
            }
            Text(stringResource(R.string.anomaly_disclaimer), style = T.TypedSmall, modifier = Modifier.padding(top = 6.dp))
        }
    }
    SectionTitle(stringResource(R.string.weight), action = stringResource(R.string.add_weight), onAction = { nav.log = LogRequest(sp.id, Ob.WEIGHT) })
    val w = lab.weights(sp.id)
    Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
        if (w.isEmpty()) Text(stringResource(R.string.weight_empty), style = T.Small)
        else {
            val last = w.last()
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Words.kg(ctx, last.second), style = T.Number)
                Text("  " + rel(last.first, lab.day), style = T.TypedSmall, modifier = Modifier.padding(bottom = 3.dp))
                Spacer(Modifier.weight(1f))
                lab.inNorm(sp, last.second)?.let { Tag(stringResource(if (it) R.string.in_norm else R.string.out_of_norm), if (it) Tone.GOOD else Tone.WARN) }
            }
            if (w.size > 1) WeightChart(w.takeLast(40), breed.wMin, breed.wMax, Modifier.padding(top = 10.dp))
        }
    }
    SectionTitle(stringResource(R.string.support_title), note = lab.integrity(sp.id)?.let { "$it%" }, action = stringResource(R.string.all), onAction = { nav.open(Overlay.Protocols(sp.id)) })
    Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 4.dp) {
        val dues = lab.dues(sp.id)
        if (dues.isEmpty()) Text(stringResource(R.string.no_protocols), style = T.Small, modifier = Modifier.padding(10.dp))
        dues.take(3).forEach { d -> DueRow(d, lab, compact = true) }
    }
    Btn(stringResource(R.string.vet_extract), {
        c.write {
            val f = VetPdf.write(ctx, lab, sp)
            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
            runCatching {
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), ctx.getString(R.string.vet_extract)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }, Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp).fillMaxWidth(), kind = Kind.SOFT)
}

@Composable
private fun Journal(sp: Specimen, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val shots = lab.shotsOf(sp.id)
    SectionTitle(stringResource(R.string.field_shots), note = if (shots.isNotEmpty()) shots.size.toString() else null)
    if (shots.isEmpty()) Text(stringResource(R.string.no_shots), style = T.Small, modifier = Modifier.padding(horizontal = 18.dp))
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shots.take(6).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { s -> ShotTile(s, Modifier.weight(1f)) { nav.open(Overlay.Shot(s.id)) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    SectionTitle(stringResource(R.string.trials), action = stringResource(R.string.diet), onAction = { nav.open(Overlay.Foods(sp.id)) })
    val trials = lab.trialsOf(sp.id)
    Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 4.dp) {
        if (trials.isEmpty()) Text(stringResource(R.string.no_trials), style = T.Small, modifier = Modifier.padding(10.dp))
        trials.take(4).forEach { t ->
            Row(Modifier.fillMaxWidth().clip(R8).clickable { nav.open(Overlay.Trial(t.id)) }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t.subject, style = T.Strong.copy(fontSize = 14.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                val o = Outcome.entries.getOrNull(t.outcome)
                if (o == null) Tag(stringResource(R.string.running)) else Tag(Words.outcome(ctx, o), if (o.good) Tone.GOOD else if (o == Outcome.BAD) Tone.ALARM else Tone.PLAIN)
            }
        }
        Text(stringResource(R.string.new_trial_plus), style = T.Small.copy(color = C.tealText),
            modifier = Modifier.fillMaxWidth().clip(R8).clickable { nav.open(Overlay.TrialEdit(sp.id)) }.padding(10.dp))
    }
    SectionTitle(stringResource(R.string.log_recent), action = stringResource(R.string.add), onAction = { nav.log = LogRequest(sp.id) })
    Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 6.dp) {
        val list = lab.obsOf(sp.id).reversed().take(8)
        if (list.isEmpty()) Text(stringResource(R.string.no_obs), style = T.Small, modifier = Modifier.padding(8.dp))
        list.forEach { o -> ObsRow(o, lab) }
    }
}

@Composable
private fun EndMission(sp: Specimen, onDone: () -> Unit) {
    val c = ctx().container
    var note by remember { mutableStateOf("") }
    Sheet(stringResource(R.string.end_mission), onDone) {
        Text(stringResource(R.string.end_mission_b, sp.name), style = T.Typed)
        Spacer(Modifier.size(14.dp))
        Field(note, { note = it }, stringResource(R.string.end_mission_note), hint = stringResource(R.string.end_mission_hint), lines = 2)
        Btn(stringResource(R.string.end_mission_go), { c.write { endMission(sp, note) }; onDone() }, Modifier.padding(top = 16.dp).fillMaxWidth())
    }
}

/** A protocol with when it's due and a tick. */
@Composable
fun DueRow(d: Due, lab: Lab, compact: Boolean = false, onOpen: (() -> Unit)? = null) {
    val ctx = ctx()
    val c = ctx.container
    Row(Modifier.fillMaxWidth().clip(R8).then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tx(d.name), style = T.Strong.copy(fontSize = 14.5.sp))
            val line = when (d.state) {
                Due.State.UNKNOWN -> stringResource(R.string.due_unknown)
                Due.State.ONCE -> if (d.p.last > 0) stringResource(R.string.done_on, Words.dateFull(ctx, d.p.last)) else stringResource(R.string.as_needed)
                else -> stringResource(R.string.due_on, rel(d.day ?: lab.day, lab.day)) + if (!compact && d.p.every > 0) " · " + stringResource(R.string.every_n, Words.days(ctx, d.p.every)) else ""
            }
            Text(line, style = T.TypedSmall.copy(color = if (d.state == Due.State.OVERDUE) C.stamp else C.muted))
        }
        Box(Modifier.size(36.dp).clip(R8).background(when (d.state) { Due.State.OVERDUE -> C.stamp; Due.State.TODAY, Due.State.SOON -> C.teal; else -> C.paper2 })
            .clickable { c.write { markDone(d.p) } }, contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Rounded.Check, stringResource(R.string.done),
                tint = when (d.state) { Due.State.OVERDUE, Due.State.TODAY, Due.State.SOON -> C.onTeal; else -> C.ink }, modifier = Modifier.size(19.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ObsRow(o: Obs, lab: Lab) {
    val ctx = ctx()
    val c = ctx.container
    var confirm by remember { mutableStateOf(false) }
    val value = when (o.kind) {
        Ob.MOOD -> Mood.entries.getOrNull(o.value.toInt())?.let { Words.mood(ctx, it) } ?: ""
        Ob.WEIGHT -> Words.kg(ctx, o.value)
        Ob.APPETITE -> Words.appetite(ctx, o.value.toInt())
        Ob.STOOL -> Words.stool(ctx, o.value.toInt())
        Ob.SYMPTOM -> Symptom.entries.getOrNull(o.value.toInt())?.let { Words.symptom(ctx, it) } ?: ""
        Ob.CONDITION -> stringResource(R.string.condition_n, o.value.toInt())
        else -> o.note
    }
    Row(Modifier.fillMaxWidth().clip(R8).combinedClickable(onLongClick = { confirm = true }) {}.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(Words.date(ctx, o.day), style = T.TypedSmall, modifier = Modifier.size(width = 62.dp, height = 18.dp))
        Text(stringResource(kindName(o.kind)), style = T.TypedSmall, modifier = Modifier.size(width = 92.dp, height = 18.dp), maxLines = 1)
        Text(value, style = T.Typed.copy(fontSize = 13.sp, color = if (o.kind == Ob.SYMPTOM) C.stamp else C.ink), modifier = Modifier.weight(1f))
    }
    if (confirm) Sheet(stringResource(R.string.delete_obs), { confirm = false }) {
        Text("${Words.dateFull(ctx, o.day)} · ${stringResource(kindName(o.kind))}: $value", style = T.Typed)
        Btn(stringResource(R.string.delete), { c.write { drop(o) }; confirm = false }, Modifier.padding(top = 16.dp).fillMaxWidth(), kind = Kind.STAMP)
    }
}

