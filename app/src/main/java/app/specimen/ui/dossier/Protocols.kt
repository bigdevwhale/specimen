package app.specimen.ui.dossier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.data.Protocol
import app.specimen.domain.Food
import app.specimen.domain.Lab
import app.specimen.domain.Outcome
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.Field
import app.specimen.ui.components.Kind
import app.specimen.ui.components.Label
import app.specimen.ui.components.Paper
import app.specimen.ui.components.R8
import app.specimen.ui.components.SectionTitle
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.Tag
import app.specimen.ui.components.Tone
import app.specimen.ui.components.TopBar
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import app.specimen.ui.tx
import java.time.Instant
import java.time.ZoneOffset

private val EVERY = listOf(7, 14, 30, 60, 90, 180, 365)

/** All of a specimen's life-support protocols, their log, and new ones. */
@Composable
fun ProtocolsPage(id: Long, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val sp = lab.specimen(id) ?: run { nav.close(); return }
    var open by remember { mutableStateOf<Long?>(null) }
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.support_title), over = "${Words.num(ctx, sp)} · ${sp.name}", onBack = { nav.close() })
        Text(stringResource(R.string.protocols_b), style = T.Small, modifier = Modifier.padding(horizontal = 18.dp))
        Paper(Modifier.padding(18.dp).fillMaxWidth(), pad = 6.dp) {
            val dues = lab.dues(sp.id)
            if (dues.isEmpty()) Text(stringResource(R.string.no_protocols), style = T.Typed.copy(color = C.muted), modifier = Modifier.padding(8.dp))
            dues.forEach { d -> DueRow(d, lab) { open = d.p.id } }
        }
        Btn(stringResource(R.string.protocol_add), { adding = true }, Modifier.padding(horizontal = 18.dp).fillMaxWidth(), kind = Kind.SOFT)
        val log = lab.done.filter { it.specimen == sp.id }.sortedByDescending { it.day }
        if (log.isNotEmpty()) {
            SectionTitle(stringResource(R.string.protocol_log))
            Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
                log.take(30).forEach { d ->
                    val p = lab.protocols.firstOrNull { it.id == d.protocol }
                    val name = p?.let { if (it.key == "u") it.title else lab.cat.proto(it.key)?.name?.let { n -> tx(n) } ?: it.key } ?: "—"
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(Words.dateFull(ctx, d.day), style = T.TypedSmall, modifier = Modifier.size(width = 100.dp, height = 18.dp))
                        Text(name, style = T.Typed.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
                        if (d.late) Text(stringResource(R.string.late), style = T.TypedSmall.copy(color = C.stamp))
                    }
                }
            }
        }
        Spacer(Modifier.size(24.dp))
    }
    open?.let { pid -> lab.protocols.firstOrNull { it.id == pid }?.let { ProtocolSheet(it, lab) { open = null } } ?: run { open = null } }
    if (adding) AddProtocol(sp.id, lab) { adding = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProtocolSheet(p: Protocol, lab: Lab, onDone: () -> Unit) {
    val ctx = ctx()
    val c = ctx.container
    val def = lab.cat.proto(p.key)
    var picking by remember { mutableStateOf(false) }
    Sheet(if (p.key == "u") p.title else def?.name?.let { tx(it) } ?: p.key, onDone) {
        def?.let { Text(tx(it.why), style = T.Typed) }
        Text(if (p.last > 0) stringResource(R.string.done_on, Words.dateFull(ctx, p.last)) else stringResource(R.string.due_unknown), style = T.TypedSmall, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.done_today), { c.write { markDone(p) }; onDone() }, Modifier.weight(1f))
            Btn(stringResource(R.string.done_other_day), { picking = true }, Modifier.weight(1f), kind = Kind.SOFT)
        }
        Label(stringResource(R.string.every))
        Chips {
            Chip(stringResource(R.string.as_needed), p.every == 0) { c.write { save(p.copy(every = 0)) } }
            (EVERY + listOfNotNull(p.every.takeIf { it > 0 && it !in EVERY })).sorted().forEach { d -> Chip(Words.days(ctx, d), p.every == d) { c.write { save(p.copy(every = d)) } } }
        }
        Btn(stringResource(if (p.key == "u") R.string.delete else R.string.protocol_off), {
            c.write { if (p.key == "u") drop(p) else save(p.copy(off = true)) }; onDone()
        }, Modifier.padding(top = 18.dp).fillMaxWidth(), kind = Kind.GHOST)
    }
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = lab.day * 86_400_000L)
        DatePickerDialog(onDismissRequest = { picking = false }, confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { ms -> val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay().coerceAtMost(lab.day); c.write { markDone(p, d) } }
                picking = false; onDone()
            }) { Text(stringResource(R.string.ok)) }
        }, dismissButton = { TextButton({ picking = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state) }
    }
}

@Composable
private fun AddProtocol(specimen: Long, lab: Lab, onDone: () -> Unit) {
    val c = ctx().container
    val sp = lab.specimen(specimen) ?: return
    val planet = lab.planet(sp)
    val have = lab.protocols.filter { it.specimen == specimen }
    var name by remember { mutableStateOf("") }
    var every by remember { mutableIntStateOf(30) }
    val ctx = ctx()
    Sheet(stringResource(R.string.protocol_add), onDone) {
        val offer = lab.cat.protocols.filter { it.forPlanet(planet) && have.none { h -> h.key == it.key && !h.off } }
        offer.forEach { d ->
            Row(Modifier.fillMaxWidth().clip(R8).clickable {
                c.write { have.firstOrNull { it.key == d.key }?.let { save(it.copy(off = false)) } ?: addProtocol(Protocol(specimen = specimen, key = d.key, every = d.every)) }
                onDone()
            }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tx(d.name), style = T.Strong.copy(fontSize = 14.5.sp))
                    Text(tx(d.why), style = T.TypedSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("+", style = T.Heading.copy(color = C.tealText), modifier = Modifier.padding(start = 10.dp))
            }
        }
        Label(stringResource(R.string.protocol_own))
        Field(name, { name = it }, null, hint = stringResource(R.string.protocol_own_hint))
        Spacer(Modifier.size(8.dp))
        Chips { (listOf(1) + EVERY).forEach { d -> Chip(Words.days(ctx, d), every == d) { every = d } } }
        Btn(stringResource(R.string.add), {
            c.write { addProtocol(Protocol(specimen = specimen, key = "u", title = name.trim(), every = every)) }; onDone()
        }, Modifier.padding(top = 14.dp).fillMaxWidth(), enabled = name.isNotBlank())
    }
}

/** The diet ledger: every food and treat tried, best first, the bad ones marked. */
@Composable
fun FoodsPage(id: Long, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val sp = lab.specimen(id) ?: run { nav.close(); return }
    val foods = lab.foods(sp.id)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.diet_title), over = "${Words.num(ctx, sp)} · ${sp.name}", onBack = { nav.close() })
        Text(stringResource(R.string.diet_b), style = T.Small, modifier = Modifier.padding(horizontal = 18.dp))
        if (foods.isEmpty()) Paper(Modifier.padding(18.dp).fillMaxWidth()) { Text(stringResource(R.string.diet_empty), style = T.Typed.copy(color = C.muted)) }
        else {
            val (bad, rest) = foods.partition { it.bad || it.outcome == Outcome.REFUSED }
            FoodList(stringResource(R.string.diet_good), rest)
            if (bad.isNotEmpty()) FoodList(stringResource(R.string.diet_bad), bad)
        }
        Btn(stringResource(R.string.diet_try), { nav.open(Overlay.TrialEdit(sp.id, app.specimen.domain.TrialKind.FOOD.ordinal)) }, Modifier.padding(18.dp).fillMaxWidth())
    }
}

@Composable
private fun FoodList(title: String, list: List<Food>) {
    val ctx = ctx()
    if (list.isEmpty()) return
    SectionTitle(title)
    Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 8.dp) {
        list.forEach { f ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, style = T.Strong.copy(fontSize = 14.5.sp))
                    Text(Words.kind(ctx, f.kind) + " · " + Words.dateFull(ctx, f.last) + if (f.tries > 1) " · ×${f.tries}" else "", style = T.TypedSmall)
                }
                val o = f.outcome
                if (o == null) Tag(stringResource(R.string.running)) else Tag(Words.outcome(ctx, o), if (f.bad || o == Outcome.BAD) Tone.ALARM else if (o.good) Tone.GOOD else Tone.PLAIN)
            }
        }
    }
}

