package app.specimen.ui.course

import app.specimen.ui.components.PILL
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import app.specimen.ui.theme.Display
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import app.specimen.ui.components.R12
import app.specimen.ui.components.PlanetArt
import androidx.compose.runtime.mutableStateOf
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.Meter
import androidx.compose.ui.draw.alpha
import app.specimen.ui.components.RankBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.domain.Lab
import app.specimen.domain.Lesson
import app.specimen.ui.BAR_SPACE
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Dashes
import app.specimen.ui.components.Kind
import app.specimen.ui.components.Label
import app.specimen.ui.components.Paper
import app.specimen.ui.components.R8
import app.specimen.ui.components.SectionTitle
import app.specimen.ui.components.Stamp
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import app.specimen.ui.tx

/** The xenology course: the rank as a medal, the next lesson, and chapters per planet that open on tap. */
@Composable
fun CourseScreen(lab: Lab, nav: Nav) {
    val ctx = ctx()
    val r = lab.rank
    var ranks by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.course_title))
        // The rank, like a medal on a certificate.
        Column(Modifier.fillMaxWidth().clip(R8).clickable { ranks = true }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RankBadge(r.index, Modifier.size(150.dp))
            Text(Words.rank(ctx, r.index), style = T.Title.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center), modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp))
            val next = r.next
            if (next != null) {
                Meter((lab.grants - r.from).toFloat() / (next - r.from), Modifier.padding(top = 12.dp).fillMaxWidth(.5f), h = 3.dp)
                Text(stringResource(R.string.next_rank, Words.rank(ctx, r.index + 1), Words.grants(ctx, next - lab.grants)), style = T.TypedSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp))
            }
        }
        // The next lesson to read.
        val course = lab.course()
        val next = course.firstOrNull { it.key !in lab.s.lessons }
        SectionTitle(stringResource(if (next != null) R.string.next_up else R.string.course_done))
        if (next != null) Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), onClick = { nav.open(Overlay.Lesson(next.key)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Typed(next.planet?.let { stringResource(R.string.course_planet, Words.planet(ctx, it)) } ?: stringResource(R.string.course_general))
                    Text(tx(next.title), style = T.Heading, modifier = Modifier.padding(top = 4.dp))
                    Text(tx(next.body), style = T.Small, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                next.planet?.let { PlanetArt(it, Modifier.padding(start = 12.dp).size(64.dp)) }
            }
            Btn(stringResource(R.string.read_lesson), { nav.open(Overlay.Lesson(next.key)) }, Modifier.padding(top = 12.dp), small = true)
        }
        SectionTitle(stringResource(R.string.course_chapters))
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Chapter(null, stringResource(R.string.course_general), lab.cat.lessonsOf(null), lab, nav)
            lab.planets.forEach { p -> Chapter(p, stringResource(R.string.course_planet, Words.planet(ctx, p)), lab.cat.lessonsOf(p), lab, nav) }
        }
        Spacer(Modifier.size(BAR_SPACE))
    }
    if (ranks) Sheet(stringResource(R.string.your_rank), { ranks = false }) {
        Lab.RANKS.forEachIndexed { i, from ->
            val reached = i <= r.index
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RankBadge(i, Modifier.padding(end = 12.dp).size(38.dp).alpha(if (reached) 1f else .3f))
                Text(Words.rank(ctx, i), style = T.Body.copy(color = if (i == r.index) C.tealText else if (reached) C.ink else C.muted), modifier = Modifier.weight(1f))
                Text(Words.n(from), style = T.TypedSmall)
            }
        }
        Text(stringResource(R.string.grants_how), style = T.Small, modifier = Modifier.padding(top = 14.dp))
        if (lab.s.refused > 0) Text(stringResource(R.string.refused_count, lab.s.refused), style = T.Small.copy(color = C.stamp), modifier = Modifier.padding(top = 8.dp))
    }
}

/** A chapter: its emblem, progress, and the lessons when opened. */
@Composable
private fun Chapter(planet: app.specimen.domain.Planet?, title: String, lessons: List<Lesson>, lab: Lab, nav: Nav) {
    if (lessons.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val done = lessons.count { it.key in lab.s.lessons }
    Column(Modifier.fillMaxWidth().clip(R12).border(1.dp, C.line, R12)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (planet != null) PlanetArt(planet, Modifier.size(52.dp)) else RankBadge(0, Modifier.size(52.dp))
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, style = T.Heading.copy(fontSize = 17.sp))
                Meter(done.toFloat() / lessons.size, Modifier.padding(top = 8.dp, end = 24.dp), h = 3.dp)
            }
            Text(stringResource(R.string.lessons_n, done, lessons.size), style = T.TypedSmall)
        }
        if (open) lessons.forEach { l ->
            val ok = l.key in lab.s.lessons
            Row(Modifier.fillMaxWidth().clickable { nav.open(Overlay.Lesson(l.key)) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("%02d".format(l.n), style = T.TypedSmall, modifier = Modifier.padding(end = 12.dp))
                Text(tx(l.title), style = T.Body.copy(fontSize = 14.5.sp, color = if (ok) C.muted else C.ink), modifier = Modifier.weight(1f))
                if (ok) Text("✓", style = T.Strong.copy(color = C.tealText))
            }
        }
    }
}

/** A lesson laid out like a page of the atlas: engraving, title, the text with a drop cap, a field task and one question. */
@Composable
fun LessonPage(key: String, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val l = lab.cat.lessons.firstOrNull { it.key == key } ?: run { nav.close(); return }
    val ru = ctx.resources.configuration.locales[0].language == "ru"
    var picked by remember(key) { mutableIntStateOf(-1) }
    val passed = key in lab.s.lessons
    val task = key in lab.s.tasks
    val chapter = lab.cat.lessonsOf(l.planet)
    val chapterName = l.planet?.let { stringResource(R.string.course_planet, Words.planet(ctx, it)) } ?: stringResource(R.string.course_general)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar("", over = chapterName, onBack = { nav.close() })
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (l.planet != null) PlanetArt(l.planet, Modifier.size(128.dp)) else RankBadge(0, Modifier.size(128.dp))
            Text(stringResource(R.string.lesson_n, l.n, chapter.size).uppercase(), style = T.Tab, modifier = Modifier.padding(top = 14.dp))
            Text(tx(l.title), style = T.Title.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(top = 6.dp))
            // Where this lesson stands in its chapter.
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chapter.forEach { x ->
                    val on = x.key == key; val ok = x.key in lab.s.lessons
                    Box(Modifier.size(if (on) 18.dp else 6.dp, 6.dp).clip(PILL).background(if (on) C.teal else if (ok) C.teal.copy(alpha = .5f) else C.line))
                }
            }
        }
        // The text, with a drop cap.
        val body = tx(l.body)
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = Display, fontSize = 46.sp, color = C.tealText)) { append(body.take(1)) }
            append(body.drop(1))
        }, style = T.Body.copy(fontFamily = Display, fontSize = 17.sp, lineHeight = 27.sp), modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp))

        // The field task, done with the reader's own animal.
        Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 26.dp).fillMaxWidth().clip(R12).border(1.dp, if (task) C.ok.copy(alpha = .6f) else C.line, R12).padding(14.dp)) {
            androidx.compose.foundation.Image(painterResource(R.drawable.menu_0), null, Modifier.size(56.dp), colorFilter = ColorFilter.tint(C.star))
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Typed(stringResource(R.string.field_task))
                Text(tx(l.task), style = T.Body.copy(fontSize = 14.5.sp), modifier = Modifier.padding(top = 4.dp))
                if (task) Text("✓ " + stringResource(R.string.stamp_done), style = T.Small.copy(color = C.ok), modifier = Modifier.padding(top = 8.dp))
                else Btn(stringResource(R.string.task_done, Lab.G_TASK), { c.write { settings.update { it.copy(tasks = it.tasks + key) } } }, Modifier.padding(top = 10.dp), kind = Kind.SOFT, small = true)
            }
        }

        // One question.
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 26.dp)) {
            Typed(stringResource(R.string.quiz))
            Text(tx(l.question), style = T.Heading, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
            val shown = passed || picked >= 0
            l.answers(ru).forEachIndexed { i, ans ->
                val right = i == l.ok
                val col = when { shown && right -> C.ok; picked == i -> C.stamp; else -> C.line }
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(R12).border(1.dp, col, R12).clickable(enabled = !shown) {
                    picked = i
                    if (right) c.write { settings.update { it.copy(lessons = it.lessons + key) } }
                }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).border(1.dp, if (col == C.line) C.muted else col, CircleShape), contentAlignment = Alignment.Center) {
                        Text(if (shown && right) "✓" else ctx.resources.getStringArray(R.array.letters)[i], style = T.Small.copy(color = if (col == C.line) C.muted else col))
                    }
                    Text(ans, style = T.Body.copy(fontSize = 14.5.sp), modifier = Modifier.padding(start = 12.dp))
                }
            }
            if (picked >= 0 && picked != l.ok) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.quiz_wrong), style = T.Small.copy(color = C.stamp), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.quiz_retry), style = T.Small.copy(color = C.tealText), modifier = Modifier.clip(R8).clickable { picked = -1 }.padding(6.dp))
            }
            if (passed || picked == l.ok) Text(stringResource(R.string.quiz_right, Lab.G_LESSON), style = T.Small.copy(color = C.ok))
        }

        val next = chapter.firstOrNull { it.n > l.n }
        if (next != null) Btn(stringResource(R.string.next_lesson_go, tx(next.title)), { nav.replace(Overlay.Lesson(next.key)) },
            Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp).fillMaxWidth())
        Spacer(Modifier.size(28.dp))
    }
}
