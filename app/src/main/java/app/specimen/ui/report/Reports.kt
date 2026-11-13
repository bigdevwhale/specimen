package app.specimen.ui.report

import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.aspectRatio
import app.specimen.ui.components.PILL
import app.specimen.ui.components.R8
import app.specimen.ui.components.R12
import app.specimen.ui.components.ShotPic
import app.specimen.ui.components.PlanetArt
import app.specimen.ui.components.RankBadge
import app.specimen.ui.components.Sheet
import androidx.compose.material.icons.outlined.Palette
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.specimen.R
import app.specimen.container
import app.specimen.domain.Lab
import app.specimen.domain.Outcome
import app.specimen.domain.Reaction
import app.specimen.domain.Report
import app.specimen.domain.Trait
import app.specimen.domain.TrialKind
import app.specimen.domain.weekOf
import app.specimen.ui.BAR_SPACE
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.IconBtn
import app.specimen.ui.components.Paper
import app.specimen.ui.components.Polaroid
import app.specimen.ui.components.R4
import app.specimen.ui.components.SectionTitle
import app.specimen.ui.components.Stamp
import app.specimen.ui.components.Tag
import app.specimen.ui.components.Tone
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.components.fmtKg
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.Mono
import app.specimen.ui.theme.Narrow
import app.specimen.ui.theme.T
import app.specimen.ui.tx
import kotlinx.coroutines.launch
import java.io.File

/** A conclusion as the report types it. */
fun conclusion(ctx: android.content.Context, cn: Report.Conclusion): String = when (cn) {
    is Report.Conclusion.Up -> ctx.getString(R.string.c_up, Words.traitNoun(ctx, cn.t))
    is Report.Conclusion.Down -> ctx.getString(R.string.c_down, Words.trait(ctx, cn.t).lowercase())
    is Report.Conclusion.Skills -> ctx.resources.getQuantityString(R.plurals.c_skills, cn.count, cn.count)
    Report.Conclusion.Stable -> ctx.getString(R.string.c_stable)
    is Report.Conclusion.Deviations -> ctx.getString(R.string.c_deviations)
    Report.Conclusion.Urgent -> ctx.getString(R.string.c_urgent)
    is Report.Conclusion.Trials -> ctx.resources.getQuantityString(R.plurals.c_trials, cn.count, cn.count)
    Report.Conclusion.Quiet -> ctx.getString(R.string.c_quiet)
    is Report.Conclusion.Mostly -> ctx.getString(R.string.c_mostly, Words.mood(ctx, cn.m).lowercase())
}

/** The headline of a week: the most telling conclusion. */
private fun headline(r: Report) = r.conclusions.firstOrNull { it is Report.Conclusion.Urgent } ?: r.conclusions.firstOrNull { it is Report.Conclusion.Up }
    ?: r.conclusions.firstOrNull { it is Report.Conclusion.Skills } ?: r.conclusions.firstOrNull()

/** The reports drawer: the latest week as a cover with its best photo and headline, older weeks as small covers. */
@Composable
fun ReportsScreen(lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    var skins by remember { mutableStateOf(false) }
    var who by remember { mutableStateOf(nav.current.takeIf { id -> lab.specimens.any { it.id == id } } ?: lab.specimens.firstOrNull()?.id ?: 0L) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.reports_title)) {
            IconBtn(Icons.Outlined.Palette, { skins = true }, desc = stringResource(R.string.skins))
        }
        if (lab.specimens.size > 1) Row(Modifier.padding(horizontal = 18.dp)) {
            Chips { lab.specimens.forEach { sp -> Chip(sp.name, sp.id == who) { who = sp.id } } }
        }
        val sp = lab.specimen(who)
        val weeks = sp?.let { lab.reportWeeks(it.id) }.orEmpty()
        if (sp == null || weeks.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RankBadge(0, Modifier.size(120.dp))
                Text(stringResource(R.string.reports_empty), style = T.Small, modifier = Modifier.padding(top = 12.dp))
            }
        } else {
            val top = weeks.first()
            Cover(lab.report(sp, top), lab, big = true) { nav.open(Overlay.Report(sp.id, top)) }
            if (weeks.size > 1) {
                SectionTitle(stringResource(R.string.reports_archive))
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    weeks.drop(1).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { w -> Box(Modifier.weight(1f)) { Cover(lab.report(sp, w), lab, big = false) { nav.open(Overlay.Report(sp.id, w)) } } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(BAR_SPACE))
    }
    if (skins) Sheet(stringResource(R.string.skins), { skins = false }) {
        Text(stringResource(R.string.skins_b, Words.n(lab.wallet)), style = T.Small, modifier = Modifier.padding(bottom = 12.dp))
        Lab.SKINS.forEachIndexed { i, (key, price) ->
            val owned = price == 0 || key in lab.s.skins
            val chosen = lab.s.skin == key
            val short = price - lab.wallet
            val sk = skin(key)
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(R12).border(1.dp, if (chosen) C.teal else C.line, R12).clickable {
                when {
                    owned -> c.write { settings.update { it.copy(skin = key) } }
                    short <= 0 -> c.write { settings.update { it.copy(skins = it.skins + key, spent = it.spent + price, skin = key) } }
                    else -> android.widget.Toast.makeText(ctx, ctx.getString(R.string.skins_short, Words.grants(ctx, short)), android.widget.Toast.LENGTH_LONG).show()
                }
            }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // A scrap of the report's paper with a line of its type.
                Box(Modifier.size(54.dp, 40.dp).clip(R8).background(sk.paper).border(1.dp, C.line, R8).padding(7.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.fillMaxWidth(.8f).height(3.dp).background(sk.accent))
                        Box(Modifier.fillMaxWidth().height(2.dp).background(sk.ink.copy(alpha = .6f)))
                        Box(Modifier.fillMaxWidth(.6f).height(2.dp).background(sk.ink.copy(alpha = .6f)))
                    }
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(ctx.resources.getStringArray(R.array.skins)[i], style = T.Strong)
                    Text(when {
                        chosen -> stringResource(R.string.skin_chosen)
                        owned -> stringResource(R.string.skin_choose)
                        short <= 0 -> stringResource(R.string.skin_buy, Words.grants(ctx, price))
                        else -> stringResource(R.string.skin_locked, Words.grants(ctx, price), Words.n(short))
                    }, style = T.TypedSmall.copy(color = if (chosen) C.tealText else C.muted))
                }
                if (chosen) Icon(Icons.Rounded.Check, null, tint = C.tealText)
                else if (!owned && short > 0) Icon(Icons.Outlined.Lock, null, tint = C.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** A report's cover: the week's best photo (or the home planet), its number, dates and headline. */
@Composable
private fun Cover(r: Report, lab: Lab, big: Boolean, onClick: () -> Unit) {
    val ctx = ctx()
    val photo = (r.photos.firstOrNull { it.first } ?: r.photos.lastOrNull())?.shot
    val unread = r.ready && "${r.sp.id}:${r.week}" !in lab.s.read && r.week >= weekOf(lab.day) - 7
    val shape = if (big) R12 else R8
    Column((if (big) Modifier.padding(horizontal = 18.dp) else Modifier).fillMaxWidth().clip(shape).border(1.dp, C.line, shape).background(C.paper).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (big) 1.25f else 1f).background(C.screen), contentAlignment = Alignment.Center) {
            if (photo != null) ShotPic(photo.id, Modifier.fillMaxSize(), stamp = photo.updated)
            else PlanetArt(lab.planet(r.sp), Modifier.fillMaxSize(.7f).alpha(.6f))
            if (!r.ready) Text(stringResource(R.string.stamp_draft).uppercase(), style = T.Tab.copy(color = Color.White),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(PILL).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp))
            else if (unread) Text(stringResource(R.string.stamp_new).uppercase(), style = T.Tab.copy(color = C.onTeal),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(PILL).background(C.teal).padding(horizontal = 10.dp, vertical = 4.dp))
        }
        Column(Modifier.padding(if (big) 16.dp else 10.dp)) {
            Text(stringResource(R.string.report_n, r.n), style = if (big) T.Title else T.Heading.copy(fontSize = 16.sp))
            Text(Words.week(ctx, r.week), style = T.TypedSmall)
            if (big) headline(r)?.let { Text(conclusion(ctx, it), style = T.Body.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), modifier = Modifier.padding(top = 10.dp)) }
        }
    }
}

/** Colours a report is typed in. */
private data class Skin(val paper: Color, val ink: Color, val muted: Color, val accent: Color, val rule: Color)

private fun skin(key: String) = when (key) {
    "blueprint" -> Skin(Color(0xFF1F4E8C), Color(0xFFF2F6FF), Color(0xFFB9CBEA), Color(0xFFFFFFFF), Color(0x55FFFFFF))
    "secret" -> Skin(Color(0xFFF1E7CF), Color(0xFF1C1B19), Color(0xFF6A604D), Color(0xFFB0201A), Color(0x33000000))
    "journal" -> Skin(Color(0xFFFFFFFF), Color(0xFF111111), Color(0xFF6E6E6E), Color(0xFF2F6F6A), Color(0x22000000))
    else -> Skin(Color(0xFFFAF6EC), Color(0xFF24262A), Color(0xFF746D60), Color(0xFFB8432F), Color(0x26000000))
}

/** One weekly report, typed out as the bureau files it; shared as a picture. */
@Composable
fun ReportPage(specimen: Long, week: Long, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val sp = lab.specimen(specimen) ?: run { nav.close(); return }
    val r = remember(lab, specimen, week) { lab.report(sp, week) }
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    LaunchedEffect(week) { if (r.ready && "${sp.id}:$week" !in lab.s.read) c.settings.update { it.copy(read = it.read + "${sp.id}:$week") } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.report_n, r.n), over = Words.week(ctx, week), onBack = { nav.close() }) {
            IconBtn(Icons.Outlined.Share, {
                if (sharing) return@IconBtn
                sharing = true
                scope.launch {
                    runCatching {
                        val bmp = layer.toImageBitmap().asAndroidBitmap()
                        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
                        val f = File(dir, "report-${sp.num}-${r.n}.png")
                        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), ctx.getString(R.string.share_report)))
                    }
                    sharing = false
                }
            }, desc = stringResource(R.string.share_report))
        }
        Box(Modifier.padding(horizontal = 14.dp).drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }) {
            Document(r, lab)
        }
        Spacer(Modifier.size(28.dp))
    }
}

@Composable
private fun Document(r: Report, lab: Lab) {
    val ctx = ctx()
    val s = skin(lab.s.skin)
    val sp = r.sp
    val typed = TextStyle(fontFamily = Mono, fontSize = 12.5.sp, lineHeight = 18.sp, color = s.ink)
    val small = typed.copy(fontSize = 10.5.sp, lineHeight = 14.sp, color = s.muted)
    val head = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp, color = s.accent)
    var section = 0
    @Composable fun Section(title: String) {
        section++
        Text("$section. ${title.uppercase()}", style = head, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(s.rule))
        Spacer(Modifier.size(6.dp))
    }
    @Composable fun Line(k: String, v: String, color: Color = s.ink) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(k, style = small, modifier = Modifier.width(128.dp))
            Text(v, style = typed.copy(color = color), modifier = Modifier.weight(1f))
        }
    }
    Column(Modifier.fillMaxWidth().background(s.paper).border(1.dp, s.rule, R4).padding(horizontal = 18.dp, vertical = 20.dp)) {
        if (lab.s.skin == "secret") Text(stringResource(R.string.top_secret), style = head.copy(fontSize = 13.sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().border(2.dp, s.accent, R4).padding(vertical = 4.dp))
        Row(Modifier.padding(top = if (lab.s.skin == "secret") 12.dp else 0.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (lab.s.skin == "journal") R.string.journal_name else R.string.bureau_name).uppercase(), style = small.copy(letterSpacing = 1.sp))
                Text(stringResource(R.string.report_n, r.n).uppercase(), style = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = if (lab.s.skin == "journal") 26.sp else 21.sp, color = s.ink), modifier = Modifier.padding(top = 4.dp))
            }
            if (!r.ready) Stamp(stringResource(R.string.stamp_draft), color = s.muted, angle = 8f, size = 12f)
        }
        Spacer(Modifier.size(8.dp))
        Line(stringResource(R.string.r_object), "${sp.name} (${Words.num(ctx, sp)})")
        Line(stringResource(R.string.r_kind), Words.planet(ctx, lab.planet(sp)) + ", " + tx(lab.breed(sp).name).lowercase())
        Line(stringResource(R.string.r_period), Words.week(ctx, r.week))
        Line(stringResource(R.string.r_entries), r.entries.toString())

        if (r.photos.isNotEmpty()) {
            Section(stringResource(R.string.r_photos))
            r.photos.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { i, p ->
                        val cap = when {
                            p.behaviour != null && p.first -> stringResource(R.string.r_first) + " " + tx(p.behaviour.caption).replaceFirstChar { it.lowercase() }
                            p.behaviour != null -> tx(p.behaviour.caption)
                            p.shot.note.isNotBlank() -> p.shot.note
                            else -> stringResource(R.string.r_under_watch)
                        }
                        Box(Modifier.weight(1f)) {
                            Polaroid(p.shot.id, Words.date(ctx, p.shot.day) + ". " + cap, Modifier.fillMaxWidth(), angle = if ((row + i) % 2 == 0) -1.6f else 1.4f)
                            if (p.first) Stamp(stringResource(R.string.stamp_new_skill), Modifier.align(Alignment.TopEnd).padding(top = 98.dp, end = 4.dp).background(Color(0xCCFDFBF6)), color = s.accent.takeIf { lab.s.skin != "blueprint" } ?: Color(0xFFB8432F), angle = -9f, size = 10f)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Section(stringResource(R.string.r_profile))
        Trait.entries.forEach { t ->
            val v = r.traits[t.ordinal]; val d = r.shift[t.ordinal]
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Words.trait(ctx, t), style = small, modifier = Modifier.width(128.dp))
                Box(Modifier.weight(1f).height(7.dp).clip(R4).background(s.rule)) { Box(Modifier.fillMaxWidth(v / 10f).height(7.dp).clip(R4).background(s.ink.copy(alpha = .75f))) }
                Text(String.format(java.util.Locale.getDefault(), "%.1f", v), style = typed.copy(fontSize = 11.5.sp), modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                Text(when { d >= 0.05f -> "↑"; d <= -0.05f -> "↓"; else -> " " }, style = typed.copy(color = if (d >= 0.05f) Color(0xFF3F8A5A) else s.accent), modifier = Modifier.width(16.dp), textAlign = TextAlign.End)
            }
        }

        Section(stringResource(R.string.r_vitals))
        r.weight?.let { w ->
            val trend = r.weightBefore?.let { b -> if (Report.changed(w, b)) (if (w > b) " ↑" else " ↓") + " " + stringResource(R.string.from_kg, fmtKg(b)) else " " + stringResource(R.string.stable) } ?: ""
            val norm = r.weightInNorm?.let { if (it) " · " + stringResource(R.string.in_norm) else " · " + stringResource(R.string.out_of_norm) } ?: ""
            Line(stringResource(R.string.r_weight), Words.kg(ctx, w) + trend + norm)
        } ?: Line(stringResource(R.string.r_weight), stringResource(R.string.not_measured), s.muted)
        Line(stringResource(R.string.r_appetite), r.appetite?.let { Words.appetite(ctx, it) } ?: stringResource(R.string.not_recorded), if (r.appetite == null) s.muted else s.ink)
        r.stool?.let { Line(stringResource(R.string.r_stool), Words.stool(ctx, it)) }
        if (r.moods.isNotEmpty()) Line(stringResource(R.string.r_moods), r.moods.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${Words.mood(ctx, it.key).lowercase()} ×${it.value}" })
        val signs = (r.symptoms.map { Words.symptom(ctx, it) } + r.anomalies.filter { it !is app.specimen.domain.Anomaly.Sign }.map { Words.anomaly(ctx, it) }).distinct()
        Line(stringResource(R.string.r_signs), if (signs.isEmpty()) stringResource(R.string.none_found) else signs.joinToString(", ").lowercase().replaceFirstChar { it.uppercase() }, if (signs.isEmpty()) s.ink else s.accent)

        if (r.concluded.isNotEmpty() || r.running.isNotEmpty()) {
            Section(stringResource(R.string.r_trials))
            r.concluded.forEach { t ->
                val o = Outcome.entries.getOrNull(t.outcome) ?: return@forEach
                val confirmed = t.guess == t.outcome || (Outcome.entries.getOrNull(t.guess)?.good == true && o.good)
                val rs = t.reactionList.mapNotNull { Reaction.of(it) }.filter { it.bad }
                Text("– ${t.subject}: ${Words.outcome(ctx, o).lowercase()}." + (if (rs.isNotEmpty()) " " + rs.joinToString(", ") { Words.reaction(ctx, it).lowercase() }.replaceFirstChar { it.uppercase() } + "." else "") +
                    " " + stringResource(if (confirmed) R.string.hyp_confirmed else R.string.hyp_refuted), style = typed, modifier = Modifier.padding(vertical = 2.dp))
            }
            r.running.forEach { t ->
                val k = TrialKind.entries.getOrNull(t.kind) ?: TrialKind.OTHER
                Text("– ${t.subject} (${Words.kind(ctx, k).lowercase()}): " + stringResource(R.string.r_running, ((minOf(lab.day, r.week + 6) - t.start).toInt() + 1).coerceAtLeast(1), t.days), style = typed.copy(color = s.muted), modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        if (r.done.isNotEmpty() || r.overdue.isNotEmpty()) {
            Section(stringResource(R.string.r_support))
            r.done.forEach { (d, name) -> Text("– ${Words.date(ctx, d.day)}: ${tx(name).lowercase()}" + if (d.late) " (" + stringResource(R.string.late) + ")" else "", style = typed, modifier = Modifier.padding(vertical = 2.dp)) }
            r.overdue.forEach { d -> Text("– " + stringResource(R.string.r_overdue, tx(d.name).lowercase()), style = typed.copy(color = s.accent), modifier = Modifier.padding(vertical = 2.dp)) }
        }

        if (r.notes.isNotEmpty()) {
            Section(stringResource(R.string.r_notes))
            r.notes.takeLast(5).forEach { Text("«$it»", style = typed, modifier = Modifier.padding(vertical = 2.dp)) }
        }

        Section(stringResource(R.string.r_conclusions))
        r.conclusions.forEachIndexed { i, cn ->
            val text = conclusion(ctx, cn)
            Text("${i + 1}. $text", style = typed.copy(color = if (cn is Report.Conclusion.Urgent) s.accent else s.ink), modifier = Modifier.padding(vertical = 2.dp))
        }

        r.advice?.let { a ->
            Section(stringResource(R.string.r_advice))
            Text(when (a) {
                is Report.Advice.Task -> tx(a.lesson.task)
                is Report.Advice.Protocol -> stringResource(R.string.advice_protocol, tx(a.due.name).lowercase(), a.due.day?.let { Words.dateFull(ctx, it) } ?: "")
                Report.Advice.Vet -> stringResource(R.string.advice_vet)
            }, style = typed)
        }

        Spacer(Modifier.size(22.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.r_signed), style = small)
                Text(Words.rank(ctx, lab.rank.index) + (lab.s.researcher.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""), style = typed)
                Text(Words.dateFull(ctx, minOf(r.week + 6, lab.day)), style = small)
            }
            if (r.ready) Stamp(stringResource(if (r.conclusions.any { it is Report.Conclusion.Urgent }) R.string.stamp_attention else R.string.stamp_approved),
                color = s.accent.takeIf { lab.s.skin != "journal" } ?: Color(0xFFB8432F), angle = -12f, size = 15f)
        }
    }
}
