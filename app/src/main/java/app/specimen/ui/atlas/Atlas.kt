package app.specimen.ui.atlas

import app.specimen.ui.components.PlanetArt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.domain.Behaviour
import app.specimen.domain.Lab
import app.specimen.domain.Mood
import app.specimen.domain.Planet
import app.specimen.ui.BAR_SPACE
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.Glyph
import app.specimen.ui.components.Bust
import app.specimen.ui.components.Meter
import app.specimen.ui.components.Paper
import app.specimen.ui.components.R8
import app.specimen.ui.components.SectionTitle
import app.specimen.ui.components.ShotPic
import app.specimen.ui.components.Stamp
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.ctx
import app.specimen.ui.log.ShotTile
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import app.specimen.ui.tx

/** The behaviour atlas: every pose and habit of the planet, each opened by a photo of your own specimen. */
@Composable
fun AtlasScreen(lab: Lab, nav: Nav) {
    val ctx = ctx()
    val planets = lab.planets
    var planet by remember { mutableStateOf(lab.specimen(nav.current)?.let { lab.planet(it) } ?: planets.first()) }
    if (planet !in planets) planet = planets.first()
    val list = lab.cat.behavioursOf(planet)
    val found = list.count { it.key in lab.atlas }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.atlas_title), over = stringResource(R.string.atlas_over, Words.planet(ctx, planet)))
        if (planets.size > 1) Row(Modifier.padding(horizontal = 18.dp)) { Chips { planets.forEach { p -> Chip(Words.planet(ctx, p), p == planet) { planet = p } } } }
        Paper(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp).fillMaxWidth(), color = C.screen) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlanetArt(planet, Modifier.padding(end = 12.dp).size(64.dp))
                Text("$found", style = T.Number.copy(color = C.phosphor, fontSize = 30.sp))
                Text(" / ${list.size}", style = T.Typed.copy(color = C.phosphor.copy(alpha = .7f)), modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.atlas_rare, list.count { it.rarity == 3 && it.key in lab.atlas }, list.count { it.rarity == 3 }), style = T.TypedSmall.copy(color = C.phosphor.copy(alpha = .7f)))
            }
            Meter(if (list.isEmpty()) 0f else found.toFloat() / list.size, Modifier.padding(top = 8.dp), color = C.phosphor, track = C.phosphorDim, h = 5.dp)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { b -> Cell(b, lab, planet, Modifier.weight(1f)) { nav.open(Overlay.Behaviour(b.key)) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.size(BAR_SPACE))
    }
}

@Composable
private fun Cell(b: Behaviour, lab: Lab, planet: Planet, modifier: Modifier, onClick: () -> Unit) {
    val shot = lab.atlas[b.key]
    Column(modifier.clip(R8).background(C.paper).border(1.dp, if (b.rarity == 3) C.warn.copy(alpha = .6f) else C.line, R8).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(if (shot == null) C.paper2 else C.screen), contentAlignment = Alignment.Center) {
            if (shot != null) {
                ShotPic(shot.id, Modifier.fillMaxSize(), stamp = shot.updated)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)), startY = 120f)))
            } else Bust(planet, Modifier.fillMaxSize(.62f), alpha = .18f)
            Text("№${b.n}", style = T.TypedSmall.copy(color = if (shot != null) Color.White else C.muted, fontSize = 10.sp), modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
            Text("•".repeat(b.rarity), style = T.Strong.copy(color = if (shot != null) Color.White else C.warn, fontSize = 12.sp), modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 6.dp))
        }
        Text(tx(b.name), style = T.Tiny.copy(color = if (shot != null) C.ink else C.muted, fontSize = 11.sp, lineHeight = 13.sp), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp))
    }
}

/** One behaviour: what it means, where to look for it, and every photo of it. */
@Composable
fun BehaviourPage(key: String, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val b = lab.cat.behaviour(key) ?: run { nav.close(); return }
    val first = lab.atlas[key]
    val shots = lab.shotsWith(key)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(tx(b.name), over = stringResource(R.string.atlas_entry, Words.planet(ctx, b.planet), b.n), onBack = { nav.close() })
        Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Typed(stringResource(R.string.rarity) + ": " + ctx.resources.getStringArray(R.array.rarity)[(b.rarity - 1).coerceIn(0, 2)], Modifier.weight(1f))
                if (first != null) Stamp(stringResource(R.string.stamp_recorded), color = C.ok, angle = 5f, size = 11f)
            }
            b.trait?.let { t ->
                Text((if (b.delta > 0) "↑ " else "↓ ") + Words.trait(ctx, t), style = T.TypedSmall.copy(color = C.tealText), modifier = Modifier.padding(top = 6.dp))
            }
            Text(tx(b.meaning), style = T.Body, modifier = Modifier.padding(top = 10.dp))
            Text(stringResource(R.string.where_to_look) + " " + tx(b.hint), style = T.Typed.copy(color = C.muted, fontSize = 12.5.sp), modifier = Modifier.padding(top = 10.dp))
            if (first != null) {
                val sp = lab.specimen(first.specimen)
                Text(stringResource(R.string.first_recorded, sp?.name ?: "—", Words.dateFull(ctx, first.day)), style = T.TypedSmall, modifier = Modifier.padding(top = 10.dp))
                Text("«" + tx(b.caption) + "»", style = T.Typed.copy(fontSize = 12.5.sp), modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (shots.isNotEmpty()) {
            SectionTitle(stringResource(R.string.field_shots), note = shots.size.toString())
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shots.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { s -> ShotTile(s, Modifier.weight(1f)) { nav.open(Overlay.Shot(s.id)) } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        val who = lab.here.filter { lab.planet(it) == b.planet }
        if (who.isNotEmpty()) Btn(stringResource(if (first == null) R.string.catch_it else R.string.shoot_again), {
            nav.open(Overlay.ShotEdit(who.firstOrNull { it.id == nav.current }?.id ?: who.first().id, camera = true))
        }, Modifier.padding(18.dp).fillMaxWidth())
        Spacer(Modifier.size(20.dp))
    }
}
