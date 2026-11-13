package app.specimen.ui.log

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import app.specimen.ui.components.PILL
import app.specimen.ui.components.R12
import androidx.compose.ui.res.painterResource
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import app.specimen.R
import app.specimen.container
import app.specimen.data.Shot
import app.specimen.domain.Behaviour
import app.specimen.domain.Lab
import app.specimen.domain.Planet
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.Field
import app.specimen.ui.components.Gap
import app.specimen.ui.components.IconBtn
import app.specimen.ui.components.Kind
import app.specimen.ui.components.Label
import app.specimen.ui.components.Paper
import app.specimen.ui.components.R8
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.ShotPic
import app.specimen.ui.components.Stamp
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import app.specimen.ui.tx
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** A new field shot: the photo, the behaviours it shows, a note. */
@Composable
fun ShotEdit(o: Overlay.ShotEdit, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val sp = lab.specimen(o.specimen) ?: run { nav.close(); return }
    val planet = lab.planet(sp)
    var photo by remember { mutableStateOf<Uri?>(null) }
    var shot by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var day by remember { mutableLongStateOf(lab.day) }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val tags = remember { mutableStateListOf<String>() }

    LaunchedEffect(photo) {
        photo?.let { u ->
            preview = c.photos.read(u, 1000)?.asImageBitmap()
            // A picture from the gallery keeps the day it was taken.
            takenOn(ctx, u)?.let { d -> if (d in (lab.day - 3650)..lab.day) day = d }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) photo = shot }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> if (u != null) photo = u }
    fun shoot() {
        val f = File(c.photos.inbox, "shot_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        shot = uri
        runCatching { camera.launch(uri) }.onFailure { Toast.makeText(ctx, R.string.no_camera, Toast.LENGTH_LONG).show() }
    }
    fun pick() = runCatching { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    // Straight to the camera or the gallery, unless the page is about to explain itself first.
    LaunchedEffect(Unit) { if (lab.s.tipsOff || "SHOT" in lab.s.tips) { if (o.camera) shoot() else pick() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.shot_title), over = sp.name, onBack = { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            val p = preview
            if (p == null) {
                // No photo yet: two engraved choices.
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Triple(R.drawable.menu_1, R.string.photo_camera, ::shoot), Triple(R.drawable.menu_2, R.string.photo_gallery, { pick(); Unit })).forEach { (img, l, go) ->
                        Column(Modifier.weight(1f).clip(R12).border(1.dp, C.line, R12).clickable { go() }.padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(img), null, Modifier.size(84.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(C.star))
                            Text(stringResource(l), style = T.Heading.copy(fontSize = 16.sp), modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(R12)) {
                    Image(p, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    // Retake and the day, laid on the photo.
                    Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        val dayLabel = when (day) { lab.day -> stringResource(R.string.today); lab.day - 1 -> stringResource(R.string.yesterday); else -> Words.dateFull(ctx, day) }
                        Text(dayLabel, style = T.Small.copy(color = Color.White), modifier = Modifier.clip(PILL).border(1.dp, Color(0x88FFFFFF), PILL)
                            .clickable { day = if (day == lab.day) lab.day - 1 else lab.day }.padding(horizontal = 12.dp, vertical = 6.dp))
                        Spacer(Modifier.weight(1f))
                        IconBtn(Icons.Outlined.PhotoCamera, ::shoot, desc = stringResource(R.string.reshoot))
                        Spacer(Modifier.size(8.dp))
                        IconBtn(Icons.Outlined.PhotoLibrary, { pick() }, desc = stringResource(R.string.another_photo))
                    }
                }
            }
            Text(stringResource(R.string.shot_tags), style = T.Heading, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            TagPicker(lab, planet, tags)
            // What the weekly report will say under this photo.
            tags.firstOrNull()?.let { lab.cat.behaviour(it) }?.let { b ->
                val first = b.key !in lab.shots.filter { it.specimen == sp.id }.flatMap { it.tagList }
                Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(R12).background(C.paper).border(1.dp, C.line, R12).padding(14.dp)) {
                    Typed(stringResource(if (first) R.string.in_report_new else R.string.in_report))
                    Text("«" + tx(b.caption) + "»", style = T.Body.copy(fontFamily = app.specimen.ui.theme.Display, fontSize = 16.sp), modifier = Modifier.padding(top = 6.dp))
                }
            }
            var noting by remember { mutableStateOf(false) }
            if (noting || note.isNotEmpty()) { Gap(14.dp); Field(note, { note = it }, null, hint = stringResource(R.string.shot_note_hint), lines = 2) }
            else Text(stringResource(R.string.add_note), style = T.Small.copy(color = C.tealText), modifier = Modifier.padding(top = 14.dp).clip(R8).clickable { noting = true }.padding(4.dp))
            Btn(stringResource(R.string.file_shot), {
                val u = photo ?: return@Btn
                saving = true
                c.write {
                    val id = addShot(sp.id, u, tags.toList(), note, day)
                    if (id > 0) nav.replace(Overlay.Shot(id)) else { saving = false; Toast.makeText(ctx, R.string.photo_failed, Toast.LENGTH_LONG).show() }
                }
            }, Modifier.padding(top = 18.dp, bottom = 24.dp).fillMaxWidth(), enabled = photo != null && !saving)
        }
    }
}

/** The day the picture was taken, from its EXIF. */
private fun takenOn(ctx: android.content.Context, u: Uri): Long? = runCatching {
    ctx.contentResolver.openInputStream(u)?.use { s ->
        val t = ExifInterface(s).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: return@use null
        LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")).toLocalDate().toEpochDay()
    }
}.getOrNull()

/** The planet's behaviours to tag a shot with; ones the atlas lacks are starred, a filter narrows the long list. */
@Composable
fun TagPicker(lab: Lab, planet: Planet, tags: SnapshotStateList<String>) {
    val ctx = ctx()
    var q by remember { mutableStateOf("") }
    val every = lab.cat.behavioursOf(planet)
    // Ones seen in this colony first: they are what the specimen tends to do.
    val seen = lab.shots.flatMap { it.tagList }.groupingBy { it }.eachCount()
    val list = every.filter { q.isBlank() || tx(it.name).contains(q.trim(), ignoreCase = true) }
        .sortedWith(compareBy<Behaviour>({ it.key !in tags }, { -(seen[it.key] ?: 0) }, { it.n }))
    var full by remember { mutableStateOf(false) }
    Field(q, { q = it }, null, hint = stringResource(R.string.tag_filter))
    Gap(10.dp)
    val shown = if (full || q.isNotBlank()) list else list.take(12)
    Chips {
        if (shown.size < list.size) Chip(stringResource(R.string.all_n, list.size), false, color = C.ink) { full = true }
        shown.forEach { b ->
            val new = b.key !in lab.atlas
            Chip((if (new) "★ " else "") + b.name.of(ctx.resources.configuration.locales[0].language == "ru"), b.key in tags) {
                if (b.key in tags) tags.remove(b.key) else tags.add(b.key)
            }
        }
    }
    if (list.any { it.key !in lab.atlas }) Text(stringResource(R.string.tag_star), style = T.TypedSmall, modifier = Modifier.padding(top = 10.dp))
}

/** A filed shot: the photo, what it shows and means, and the stamp if it brought a new skill. */
@Composable
fun ShotPage(id: Long, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val shot = lab.shots.firstOrNull { it.id == id } ?: run { nav.close(); return }
    val sp = lab.specimen(shot.specimen) ?: run { nav.close(); return }
    val first = lab.firstIn(shot)
    var editing by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(sp.name, over = "${Words.num(ctx, sp)} · ${Words.dateFull(ctx, shot.day)}", onBack = { nav.close() }) {
            IconBtn(Icons.Outlined.Delete, { confirm = true }, desc = stringResource(R.string.delete))
        }
        Box(Modifier.padding(horizontal = 18.dp)) {
            ShotPic(shot.id, Modifier.fillMaxWidth().heightIn(min = 240.dp).aspectRatio(0.9f).clip(R8), big = true, stamp = shot.updated)
            if (first.isNotEmpty()) Stamp(stringResource(R.string.stamp_new_skill), Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 22.dp), angle = -6f, size = 14f, fill = C.sheet.copy(alpha = .9f))
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shot.tagList.mapNotNull { lab.cat.behaviour(it) }.forEach { b ->
                Paper(Modifier.fillMaxWidth(), onClick = { nav.open(Overlay.Behaviour(b.key)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Typed(stringResource(R.string.atlas_n, b.n), Modifier.weight(1f), C.stamp)
                        if (b.key in first) Text(stringResource(R.string.first_time), style = T.TypedSmall.copy(color = C.stamp))
                    }
                    Text(tx(b.name), style = T.Heading, modifier = Modifier.padding(top = 2.dp))
                    Text(tx(b.caption), style = T.Typed, modifier = Modifier.padding(top = 6.dp))
                    Text(tx(b.meaning), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (shot.tagList.isEmpty()) Text(stringResource(R.string.shot_no_tags), style = T.Typed.copy(color = C.muted))
            if (shot.note.isNotBlank()) Paper(Modifier.fillMaxWidth()) { Typed(stringResource(R.string.shot_note)); Text(shot.note, style = T.Typed, modifier = Modifier.padding(top = 4.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(stringResource(R.string.edit_tags), { editing = true }, Modifier.weight(1f), kind = Kind.SOFT, small = true)
                if (sp.portrait != shot.id) Btn(stringResource(R.string.make_portrait), { c.write { save(sp.copy(portrait = shot.id)) } }, Modifier.weight(1f), kind = Kind.SOFT, small = true)
            }
            Spacer(Modifier.size(20.dp))
        }
    }
    if (editing) {
        val tags = remember { mutableStateListOf<String>().apply { addAll(shot.tagList) } }
        Sheet(stringResource(R.string.edit_tags), { editing = false }) {
            TagPicker(lab, lab.planet(sp), tags)
            Btn(stringResource(R.string.save), { c.write { save(shot.copy(tags = tags.joinToString(","))) }; editing = false }, Modifier.padding(top = 16.dp).fillMaxWidth())
        }
    }
    if (confirm) Sheet(stringResource(R.string.delete_shot), { confirm = false }) {
        Text(stringResource(R.string.delete_shot_b), style = T.Body)
        Btn(stringResource(R.string.delete), { c.write { drop(shot) }; nav.close() }, Modifier.padding(top = 16.dp).fillMaxWidth(), kind = Kind.STAMP)
    }
}

/** For the atlas and the dossier: a shot as a small square. */
@Composable
fun ShotTile(s: Shot, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ShotPic(s.id, modifier.aspectRatio(1f).clip(R8).clickable(onClick = onClick), stamp = s.updated)
}
