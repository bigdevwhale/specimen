package app.specimen.ui.dossier

import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.asImageBitmap
import app.specimen.ui.components.PlanetArt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableLongStateOf
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
import app.specimen.data.Specimen
import app.specimen.domain.Lab
import app.specimen.domain.Mood
import app.specimen.domain.Planet
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.Field
import app.specimen.ui.components.Glyph
import app.specimen.ui.components.Kind
import app.specimen.ui.components.Label
import app.specimen.ui.components.R8
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.TopBar
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import app.specimen.ui.tx
import java.time.Instant
import java.time.ZoneOffset

/** Registering a specimen: home planet, subspecies, name and papers. Also used to edit one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecimenEdit(id: Long, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val old = lab.specimen(id)
    var planet by remember { mutableStateOf(old?.let { Planet.of(it.planet) }) }
    var breed by remember { mutableStateOf(old?.breed ?: "") }
    var name by remember { mutableStateOf(old?.name ?: "") }
    var sex by remember { mutableIntStateOf(old?.sex ?: 0) }
    var neutered by remember { mutableStateOf(old?.neutered ?: false) }
    var born by remember { mutableLongStateOf(old?.born ?: 0L) }
    var arrived by remember { mutableLongStateOf(old?.arrived ?: lab.day) }
    var q by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(0) } // 1 born, 2 arrived
    var confirm by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var shotUri by remember { mutableStateOf<android.net.Uri?>(null) }
    // A new specimen keeps the photo until it is filed; an existing one gets it at once.
    fun took(u: android.net.Uri) {
        if (old != null) c.write { setAvatar(old.id, u) }
        else { pendingUri = u; c.write { pending = photos.read(u, 600)?.asImageBitmap() } }
    }
    val camera = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { ok -> if (ok) shotUri?.let(::took) }
    val gallery = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { u -> if (u != null) took(u) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(if (old == null) R.string.register_title else R.string.edit_specimen), over = stringResource(R.string.form_over), onBack = { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            // The photo: tap the porthole.
            Box(Modifier.fillMaxWidth().padding(bottom = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(132.dp).clip(CircleShape).border(1.dp, C.teal.copy(alpha = .6f), CircleShape).padding(6.dp).clip(CircleShape).background(C.screen)
                    .clickable { choosing = true }, contentAlignment = Alignment.Center) {
                    val pv = pending
                    when {
                        pv != null -> androidx.compose.foundation.Image(pv, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        old != null && old.portrait != 0L -> app.specimen.ui.components.ShotPic(old.portrait, Modifier.fillMaxSize(), stamp = if (old.portrait < 0) old.updated else 0)
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.AddAPhoto, null, tint = C.tealText, modifier = Modifier.size(30.dp))
                            Text(stringResource(R.string.avatar_add), style = T.Tiny.copy(color = C.tealText), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            Label(stringResource(R.string.f_planet), top = 4.dp)
            Planet.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        val on = planet == p
                        Column(Modifier.weight(1f).clip(R8).background(if (on) C.screen else C.paper).border(1.dp, if (on) C.phosphor else C.line, R8)
                            .clickable { if (planet != p) { planet = p; breed = p.key + "_local"; q = "" } }.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            PlanetArt(p, Modifier.size(58.dp))
                            Text(Words.planet(ctx, p), style = T.Strong.copy(fontSize = 13.5.sp, color = if (on) C.phosphor else C.ink))
                            Text(Words.planetKind(ctx, p), style = T.Tiny.copy(color = if (on) C.phosphor.copy(alpha = .7f) else C.muted, fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            val p = planet
            if (p != null) {
                Text(Words.planetLore(ctx, p), style = T.TypedSmall, modifier = Modifier.padding(bottom = 4.dp))
                Label(stringResource(R.string.f_breed))
                val all = lab.cat.breedsOf(p)
                if (all.size > 8) { Field(q, { q = it }, null, hint = stringResource(R.string.breed_search)); Spacer(Modifier.size(8.dp)) }
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()).clip(R8).background(C.paper).border(1.dp, C.line, R8)) {
                    all.filter { q.isBlank() || tx(it.name).contains(q.trim(), true) || it.name.en.contains(q.trim(), true) || it.key == breed }.forEach { b ->
                        val on = b.key == breed
                        Row(Modifier.fillMaxWidth().background(if (on) C.teal.copy(alpha = .14f) else C.paper).clickable { breed = b.key }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tx(b.name), style = T.Strong.copy(fontSize = 14.5.sp, color = if (on) C.tealText else C.ink))
                                if (on && tx(b.lore).isNotBlank()) Text(tx(b.lore), style = T.TypedSmall, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (on) Text("✓", style = T.Strong.copy(color = C.tealText))
                        }
                    }
                }
                Spacer(Modifier.size(16.dp))
                Field(name, { name = it }, stringResource(R.string.f_name), hint = stringResource(R.string.f_name_hint))
                Label(stringResource(R.string.f_sex))
                Chips {
                    listOf(1, 2, 0).forEach { v -> Chip(Words.sex(ctx, v), sex == v) { sex = v } }
                    Chip(stringResource(R.string.neutered), neutered, color = C.ink) { neutered = !neutered }
                }
                Label(stringResource(R.string.f_born))
                Chips {
                    Chip(if (born > 0) Words.dateFull(ctx, born) else stringResource(R.string.pick_date), born > 0) { picking = 1 }
                    if (born > 0) Chip(stringResource(R.string.unknown), false) { born = 0 }
                }
                Label(stringResource(R.string.f_arrived))
                Chip(Words.dateFull(ctx, arrived), true) { picking = 2 }
                if (old == null) {
                    val protos = lab.cat.breedOrNull(breed)?.let { lab.cat.startProtocols(it) }.orEmpty()
                    if (protos.isNotEmpty()) {
                        Label(stringResource(R.string.f_protocols))
                        val ru = ctx.resources.configuration.locales[0].language == "ru"
                        Text(protos.joinToString(", ") { it.name.of(ru).lowercase() }.replaceFirstChar { it.uppercase() }, style = T.Typed)
                        Text(stringResource(R.string.f_protocols_b), style = T.TypedSmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Btn(stringResource(if (old == null) R.string.register_go else R.string.save), {
                    val b = lab.cat.breedOrNull(breed) ?: return@Btn
                    c.write {
                        if (old == null) {
                            val newId = addSpecimen(Specimen(num = 0, name = name.trim(), planet = p.key, breed = b.key, sex = sex, born = born, arrived = arrived, neutered = neutered), catalog.startProtocols(b))
                            pendingUri?.let { setAvatar(newId, it) }
                            nav.current = newId
                            nav.replace(Overlay.Dossier(newId))
                        } else {
                            save(old.copy(name = name.trim(), planet = p.key, breed = b.key, sex = sex, born = born, arrived = arrived, neutered = neutered))
                            nav.close()
                        }
                    }
                }, Modifier.padding(top = 22.dp).fillMaxWidth(), enabled = name.isNotBlank() && lab.cat.breedOrNull(breed) != null)
                if (old != null) Btn(stringResource(R.string.delete_specimen), { confirm = true }, Modifier.padding(top = 10.dp).fillMaxWidth(), kind = Kind.GHOST)
            }
            Spacer(Modifier.size(28.dp))
        }
    }
    if (picking != 0) {
        val initial = (if (picking == 1) born.takeIf { it > 0 } ?: (lab.day - 365) else arrived) * 86_400_000L
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(onDismissRequest = { picking = 0 }, confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { ms ->
                    val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay().coerceAtMost(lab.day)
                    if (picking == 1) born = d else arrived = d
                }
                picking = 0
            }) { Text(stringResource(R.string.ok)) }
        }, dismissButton = { TextButton({ picking = 0 }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state) }
    }
    if (choosing) Sheet(stringResource(R.string.avatar_title), { choosing = false }) {
        Btn(stringResource(R.string.photo_camera), {
            choosing = false
            val f = java.io.File(c.photos.inbox, "avatar_${System.currentTimeMillis()}.jpg")
            val u = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
            shotUri = u
            runCatching { camera.launch(u) }.onFailure { android.widget.Toast.makeText(ctx, R.string.no_camera, android.widget.Toast.LENGTH_LONG).show() }
        }, Modifier.fillMaxWidth(), icon = androidx.compose.material.icons.Icons.Outlined.PhotoCamera)
        Btn(stringResource(R.string.photo_gallery), {
            choosing = false
            runCatching { gallery.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }, Modifier.padding(top = 8.dp).fillMaxWidth(), kind = Kind.SOFT, icon = androidx.compose.material.icons.Icons.Outlined.PhotoLibrary)
        if ((old != null && old.portrait < 0) || pending != null) Btn(stringResource(R.string.avatar_remove), {
            choosing = false
            if (old != null) c.write { dropAvatar(old.id) } else { pending = null; pendingUri = null }
        }, Modifier.padding(top = 8.dp).fillMaxWidth(), kind = Kind.GHOST)
    }
    if (confirm && old != null) Sheet(stringResource(R.string.delete_specimen), { confirm = false }) {
        Text(stringResource(R.string.delete_specimen_b, old.name), style = T.Typed)
        Btn(stringResource(R.string.delete), { c.write { delete(old) }; nav.home(app.specimen.ui.Tab.STATION) }, Modifier.padding(top = 16.dp).fillMaxWidth(), kind = Kind.STAMP)
    }
}
