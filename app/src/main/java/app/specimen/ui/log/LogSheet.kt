package app.specimen.ui.log

import app.specimen.ui.rel
import app.specimen.ui.components.WeightChart
import app.specimen.ui.components.Typed
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import app.specimen.ui.components.Portrait
import app.specimen.ui.components.R12
import app.specimen.ui.components.PILL
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.data.Ob
import app.specimen.data.Obs
import app.specimen.domain.Lab
import app.specimen.domain.Mood
import app.specimen.domain.Symptom
import app.specimen.ui.LogRequest
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.Field
import app.specimen.ui.components.Glyph
import app.specimen.ui.components.Label
import app.specimen.ui.components.MenuRow
import app.specimen.ui.components.R8
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.fmtKg
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T

/** The «+»: who it is about, then six engraved tiles – what to file. */
@Composable
fun AddMenu(lab: Lab, nav: Nav) {
    var who by remember { mutableStateOf(nav.current) }
    Sheet(stringResource(R.string.menu_title), { nav.menu = false }) {
        Text(stringResource(R.string.menu_sub), style = T.Small, modifier = Modifier.padding(bottom = 14.dp))
        if (lab.here.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                lab.here.forEach { sp ->
                    val on = sp.id == who
                    Row(Modifier.clip(PILL).border(1.dp, if (on) C.teal else C.line, PILL).clickable { who = sp.id; nav.current = sp.id }
                        .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Portrait(sp, 26.dp)
                        Text(sp.name, style = T.Strong.copy(fontSize = 14.sp, color = if (on) C.ink else C.muted), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        val tiles = listOf(
            Triple(R.drawable.menu_0, R.string.menu_obs, R.string.menu_obs_b) to { nav.menu = false; nav.log = LogRequest(who) },
            Triple(R.drawable.menu_1, R.string.menu_shot, R.string.menu_shot_b) to { nav.open(Overlay.ShotEdit(who, camera = true)) },
            Triple(R.drawable.menu_2, R.string.menu_pick, R.string.menu_pick_b) to { nav.open(Overlay.ShotEdit(who, camera = false)) },
            Triple(R.drawable.menu_3, R.string.menu_trial, R.string.menu_trial_b) to { nav.open(Overlay.TrialEdit(who)) },
            Triple(R.drawable.menu_4, R.string.menu_protocol, R.string.menu_protocol_b) to { nav.open(Overlay.Protocols(who)) },
            Triple(R.drawable.menu_5, R.string.menu_note, R.string.menu_note_b) to { nav.menu = false; nav.log = LogRequest(who, Ob.NOTE) },
        )
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (t, go) ->
                    Column(Modifier.weight(1f).clip(R12).border(1.dp, C.line, R12).clickable(onClick = go).padding(14.dp)) {
                        androidx.compose.foundation.Image(painterResource(t.first), null, Modifier.size(64.dp), colorFilter = ColorFilter.tint(C.star))
                        Text(stringResource(t.second), style = T.Heading.copy(fontSize = 16.sp), modifier = Modifier.padding(top = 10.dp))
                        Text(stringResource(t.third), style = T.TypedSmall, maxLines = 2, minLines = 2)
                    }
                }
            }
        }
    }
}

private val KINDS = listOf(Ob.MOOD, Ob.APPETITE, Ob.STOOL, Ob.WEIGHT, Ob.CONDITION, Ob.SYMPTOM, Ob.NOTE)

/** One observation: mood, appetite, stool, weight, body condition, symptoms or a field note. Opened from a console button it asks one question. */
@Composable
fun LogSheet(req: LogRequest, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val sp = lab.specimen(req.specimen) ?: run { nav.log = null; return }
    val planet = lab.planet(sp)
    var kind by remember { mutableIntStateOf(if (req.kind == 0) Ob.MOOD else req.kind) }
    var yesterday by remember { mutableStateOf(false) }
    fun close() { nav.log = null }
    fun put(k: Int, v: Float, note: String = "") {
        val day = lab.day - if (yesterday) 1 else 0
        c.write { log(Obs(specimen = sp.id, day = day, kind = k, value = v, note = note, at = if (yesterday) 20 * 60 else 0)) }
        close()
    }
    val title = when (kind) {
        Ob.MOOD -> stringResource(R.string.ask_mood, sp.name)
        Ob.APPETITE -> stringResource(R.string.ask_appetite, sp.name)
        Ob.WEIGHT -> stringResource(R.string.ask_weight, sp.name)
        Ob.STOOL -> stringResource(R.string.ask_stool)
        Ob.CONDITION -> stringResource(R.string.ask_condition)
        Ob.SYMPTOM -> stringResource(R.string.ask_symptom)
        else -> stringResource(R.string.ask_note)
    }
    Sheet(null, ::close) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Portrait(sp, 36.dp)
            Text(sp.name, style = T.Small, modifier = Modifier.padding(start = 10.dp).weight(1f))
            // Today or yesterday, as one small switch.
            Row(Modifier.clip(PILL).border(1.dp, C.line, PILL).padding(3.dp)) {
                listOf(false to R.string.today, true to R.string.yesterday).forEach { (y, l) ->
                    Text(stringResource(l), style = T.Tiny.copy(color = if (yesterday == y) C.onTeal else C.muted, fontSize = 12.sp),
                        modifier = Modifier.clip(PILL).background(if (yesterday == y) C.teal else Color.Transparent).clickable { yesterday = y }.padding(horizontal = 12.dp, vertical = 5.dp))
                }
            }
        }
        Text(title, style = T.Title.copy(fontSize = 24.sp), modifier = Modifier.padding(top = 14.dp, bottom = 14.dp))
        if (req.kind == 0) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KINDS.forEach { k -> Chip(stringResource(kindName(k)), kind == k) { kind = k } }
            }
        }
        when (kind) {
            Ob.MOOD -> {
                Mood.entries.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        row.forEach { m ->
                            Column(Modifier.width(78.dp).clip(R12).clickable { put(Ob.MOOD, m.ordinal.toFloat()) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(62.dp).clip(CircleShape).border(1.dp, C.line, CircleShape), contentAlignment = Alignment.Center) {
                                    Glyph(planet, m, Modifier.size(46.dp), if (m == Mood.UNWELL) C.stamp else C.tealText)
                                }
                                Text(Words.mood(ctx, m), style = T.Small.copy(color = C.ink), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                                Text(Words.moodSign(ctx, m, planet), style = T.Tiny.copy(fontSize = 10.sp, lineHeight = 12.sp), textAlign = TextAlign.Center, maxLines = 3, minLines = 3)
                            }
                        }
                    }
                }
                Recent(lab, sp.id, Ob.MOOD) { Mood.entries.getOrNull(it.toInt())?.let { m -> Words.mood(ctx, m).lowercase() } ?: "" }
            }
            Ob.APPETITE -> {
                listOf(3, 2, 1, 0).chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { v ->
                            Column(Modifier.weight(1f).clip(R12).border(1.dp, C.line, R12).clickable { put(Ob.APPETITE, v.toFloat()) }.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                androidx.compose.foundation.Image(painterResource(appetiteArt(planet, v)), null, Modifier.size(104.dp), colorFilter = ColorFilter.tint(C.star))
                                Text(Words.appetite(ctx, v), style = T.Strong.copy(fontSize = 14.sp, color = if (v == 0) C.stamp else C.ink), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
                Week(lab, sp.id)
            }
            Ob.STOOL -> {
                (0..3).chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { v ->
                            Text(Words.stool(ctx, v), style = T.Strong.copy(fontSize = 14.5.sp, color = if (v >= 2) C.stamp else C.ink), textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).clip(R12).border(1.dp, C.line, R12).clickable { put(Ob.STOOL, v.toFloat()) }.padding(vertical = 18.dp))
                        }
                    }
                }
            }
            Ob.WEIGHT -> WeightInput(lab, sp) { put(Ob.WEIGHT, it) }
            Ob.CONDITION -> {
                Text(stringResource(R.string.condition_b), style = T.Small)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (1..9).forEach { n ->
                        val col = when (n) { in 4..5 -> C.ok; 3, 6 -> C.warn; else -> C.stamp }
                        Box(Modifier.weight(1f).aspectRatio(0.8f).clip(R8).border(1.dp, col.copy(alpha = .6f), R8).clickable { put(Ob.CONDITION, n.toFloat()) }, contentAlignment = Alignment.Center) {
                            Text("$n", style = T.Number.copy(fontSize = 18.sp, color = col))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(stringResource(R.string.condition_thin), style = T.Tiny, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.condition_ideal), style = T.Tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(stringResource(R.string.condition_heavy), style = T.Tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }
            Ob.SYMPTOM -> {
                val picked = remember { mutableStateOf(setOf<Symptom>()) }
                Chips {
                    Symptom.entries.filter { it.offeredOn(planet) }.forEach { s ->
                        Chip((if (s.urgentOn(planet)) "• " else "") + Words.symptom(ctx, s), s in picked.value, color = if (s.urgentOn(planet)) C.stamp else C.teal) {
                            picked.value = if (s in picked.value) picked.value - s else picked.value + s
                        }
                    }
                }
                Text(stringResource(R.string.symptom_dot), style = T.TypedSmall, modifier = Modifier.padding(top = 10.dp))
                if (picked.value.any { it.urgentOn(planet) }) Text(stringResource(R.string.symptom_urgent), style = T.Body.copy(color = C.stamp), modifier = Modifier.padding(top = 10.dp))
                Btn(stringResource(R.string.save), {
                    val day = lab.day - if (yesterday) 1 else 0
                    picked.value.forEach { s -> c.write { log(Obs(specimen = sp.id, day = day, kind = Ob.SYMPTOM, value = s.ordinal.toFloat())) } }
                    close()
                }, Modifier.padding(top = 16.dp).fillMaxWidth(), enabled = picked.value.isNotEmpty())
            }
            Ob.NOTE -> {
                var text by remember { mutableStateOf("") }
                Field(text, { text = it }, null, hint = stringResource(R.string.note_hint), lines = 4)
                Btn(stringResource(R.string.save), { if (text.isNotBlank()) put(Ob.NOTE, 0f, text.trim()) }, Modifier.padding(top = 16.dp).fillMaxWidth(), enabled = text.isNotBlank())
            }
        }
    }
}

/** The last entry of this kind, in one quiet line. */
@Composable
private fun Recent(lab: Lab, id: Long, kind: Int, value: (Float) -> String) {
    val o = lab.obsOf(id).lastOrNull { it.kind == kind } ?: return
    Text(stringResource(R.string.last_entry, value(o.value), rel(o.day, lab.day)), style = T.TypedSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
}

/** Engraved appetite scenes per kind, in sheet order: asked for more, as usual, ate little, refused. */
private val APPETITE = mapOf(
    app.specimen.domain.Planet.FELIS to listOf(R.drawable.appetite_cat_0, R.drawable.appetite_cat_1, R.drawable.appetite_cat_2, R.drawable.appetite_cat_3),
    app.specimen.domain.Planet.CANIS to listOf(R.drawable.appetite_dog_0, R.drawable.appetite_dog_1, R.drawable.appetite_dog_2, R.drawable.appetite_dog_3),
    app.specimen.domain.Planet.PSITTACUS to listOf(R.drawable.appetite_bird_0, R.drawable.appetite_bird_1, R.drawable.appetite_bird_2, R.drawable.appetite_bird_3),
    app.specimen.domain.Planet.GLIRES to listOf(R.drawable.appetite_rabbit_0, R.drawable.appetite_rabbit_1, R.drawable.appetite_rabbit_2, R.drawable.appetite_rabbit_3),
    app.specimen.domain.Planet.MUSTELA to listOf(R.drawable.appetite_ferret_0, R.drawable.appetite_ferret_1, R.drawable.appetite_ferret_2, R.drawable.appetite_ferret_3),
    app.specimen.domain.Planet.SQUAMATA to listOf(R.drawable.appetite_lizard_0, R.drawable.appetite_lizard_1, R.drawable.appetite_lizard_2, R.drawable.appetite_lizard_3),
)

/** The dish alone, for any kind without its own scenes. */
private val APPETITE_ANY = listOf(R.drawable.appetite_any_0, R.drawable.appetite_any_1, R.drawable.appetite_any_2, R.drawable.appetite_any_3)

private fun appetiteArt(p: app.specimen.domain.Planet, v: Int): Int = (APPETITE[p] ?: APPETITE_ANY)[(3 - v).coerceIn(0, 3)]

/** Seven dots for the last seven days: green ate, amber poorly, red refused. */
@Composable
private fun Week(lab: Lab, id: Long) {
    val ctx = ctx()
    val byDay = lab.obsOf(id).filter { it.kind == Ob.APPETITE && it.day > lab.day - 7 }.groupBy { it.day }.mapValues { it.value.last().value.toInt() }
    if (byDay.isEmpty()) return
    Typed(stringResource(R.string.week_short), Modifier.padding(top = 8.dp, bottom = 8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (6 downTo 0).forEach { back ->
            val d = lab.day - back
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val v = byDay[d]
                val col = when (v) { null -> C.line; 0 -> C.stamp; 1 -> C.warn; else -> C.ok }
                Box(Modifier.size(14.dp).clip(CircleShape).background(if (v == null) Color.Transparent else col).border(1.dp, col, CircleShape))
                Text(java.time.LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, ctx.resources.configuration.locales[0]), style = T.Tiny.copy(fontSize = 10.sp), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** A big number with − and + around it, starting from the last weight; typing works too. */
@Composable
private fun WeightInput(lab: Lab, sp: app.specimen.data.Specimen, onSave: (Float) -> Unit) {
    val ctx = ctx()
    val last = lab.lastWeight(sp.id)
    val b = lab.breed(sp)
    val start = last?.second ?: if (b.wMax > 0) (b.wMin + b.wMax) / 2 else 4f
    val step = if (start >= 2f) 0.05f else if (start >= 0.3f) 0.01f else 0.001f
    var text by remember { mutableStateOf(fmtKg(start)) }
    val v = text.replace(',', '.').toFloatOrNull()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Stepper("−") { v?.let { text = fmtKg((it - step).coerceAtLeast(step)) } }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicTextField(text, { t -> text = t.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(7) }, singleLine = true,
                textStyle = T.Hero.copy(fontSize = 48.sp, textAlign = TextAlign.Center, color = C.ink), cursorBrush = SolidColor(C.teal),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.kg_unit), style = T.Small)
        }
        Stepper("+") { v?.let { text = fmtKg(it + step) } }
    }
    val norm = if (v != null) lab.inNorm(sp, v) else null
    Text(listOfNotNull(
        last?.let { stringResource(R.string.weight_last, Words.kg(ctx, it.second), Words.rel(ctx, it.first, lab.day)) },
        if (b.wMax > 0) stringResource(R.string.weight_norm, fmtKg(b.wMin), fmtKg(b.wMax)) else null,
    ).joinToString(" · "), style = T.TypedSmall.copy(color = if (norm == false) C.warn else C.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
    val w = lab.weights(sp.id)
    if (w.size > 1) WeightChart(w.takeLast(12), b.wMin, b.wMax, Modifier.padding(top = 8.dp))
    Btn(stringResource(R.string.save), { if (v != null && v > 0f) onSave(v) }, Modifier.padding(top = 14.dp).fillMaxWidth(), enabled = v != null && v > 0f)
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).border(1.dp, C.line, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = T.Title.copy(color = C.tealText))
    }
}

fun kindName(k: Int) = when (k) {
    Ob.MOOD -> R.string.ob_mood; Ob.APPETITE -> R.string.ob_appetite; Ob.STOOL -> R.string.ob_stool; Ob.WEIGHT -> R.string.ob_weight
    Ob.CONDITION -> R.string.ob_condition; Ob.SYMPTOM -> R.string.ob_symptom; else -> R.string.ob_note
}

/** A column of options, one tap files it. */
@Composable
private fun Scale(options: List<Pair<Int, String>>, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { (v, label) ->
            Text(label, style = T.Strong, modifier = Modifier.fillMaxWidth().clip(R8).background(C.paper).border(1.dp, C.line, R8).clickable { onPick(v) }.padding(horizontal = 16.dp, vertical = 13.dp))
        }
    }
}
