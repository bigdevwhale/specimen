package app.specimen.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.container
import app.specimen.data.Specimen
import app.specimen.domain.Mood
import app.specimen.domain.Planet
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T

val R4 = RoundedCornerShape(4.dp)
val R8 = RoundedCornerShape(14.dp)
val R12 = RoundedCornerShape(22.dp)
val PILL = RoundedCornerShape(50)

/** The page: flat graphite or paper, with a few faint gold stars at night. */
fun Modifier.desk(): Modifier = this.background(C.desk).drawBehind {
    if (!C.dark) return@drawBehind
    val rnd = java.util.Random(11)
    repeat(26) {
        val x = rnd.nextFloat() * size.width; val y = rnd.nextFloat() * size.height
        drawCircle(C.star.copy(alpha = 0.08f + rnd.nextFloat() * 0.18f), (0.6f + rnd.nextFloat()) * density, Offset(x, y))
    }
}

/** A card: a flat panel with a hairline border. */
@Composable
fun Paper(modifier: Modifier = Modifier, pad: Dp = 14.dp, color: Color = C.paper, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(R12).background(color).border(1.dp, C.line, R12)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad),
        content = content,
    )
}

/** A rubber stamp: red capitals in a frame, slightly askew. */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, color: Color = C.stamp, angle: Float = -6f, size: Float = 13f) {
    Text(text.uppercase(), style = T.Tab.copy(color = color, fontSize = size.sp, letterSpacing = 1.5.sp), maxLines = 1,
        modifier = modifier.rotate(angle).border(2.dp, color.copy(alpha = .9f), R8).padding(horizontal = 9.dp, vertical = 3.dp))
}

/** Small typed capitals: a folder tab or the head of a section. */
@Composable
fun Typed(text: String, modifier: Modifier = Modifier, color: Color = C.tealText) =
    Text(text.uppercase(), style = T.Tab.copy(color = color), modifier = modifier)

enum class Kind { TEAL, STAMP, SOFT, GHOST }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.TEAL, small: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    val (bg, fg) = when (kind) {
        Kind.TEAL -> C.teal to C.onTeal
        Kind.STAMP -> C.stamp to C.onStamp
        Kind.SOFT -> Color.Transparent to C.ink
        Kind.GHOST -> Color.Transparent to C.ink
    }
    Row(
        modifier.clip(PILL).background(if (enabled) bg else C.paper2)
            .then(if (kind == Kind.GHOST || kind == Kind.SOFT) Modifier.border(1.dp, C.ink.copy(alpha = .22f), PILL) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (small) 12.dp else 18.dp, vertical = if (small) 8.dp else 13.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val col = if (enabled) fg else C.muted
        if (icon != null) Icon(icon, null, tint = col, modifier = Modifier.padding(end = 6.dp).size(if (small) 16.dp else 18.dp))
        Text(text, style = T.Button.copy(color = col, fontSize = if (small) 12.5.sp else 14.5.sp), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, desc: String? = null) {
    Box(modifier.size(44.dp).clip(PILL).background(C.paper).border(1.dp, C.line, PILL).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = C.ink, modifier = Modifier.size(21.dp))
    }
}

/** A page's title row: back, a typed line above the title, actions on the right. */
@Composable
fun TopBar(title: String, over: String? = null, onBack: (() -> Unit)? = null, right: @Composable (RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) { IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, onBack); Box(Modifier.size(12.dp)) }
        Column(Modifier.weight(1f)) {
            if (over != null) Typed(over, Modifier.padding(bottom = 2.dp))
            Text(title, style = T.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (right != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { right() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, note: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 9.dp), verticalAlignment = Alignment.Bottom) {
        Typed(text, Modifier.weight(1f))
        if (note != null) Text(note, style = T.TypedSmall)
        if (action != null) Text(action, style = T.Small.copy(color = C.tealText, fontWeight = FontWeight.Bold),
            modifier = Modifier.clip(R4).clickable { onAction?.invoke() }.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}

/** A selectable label. */
@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, color: Color = C.teal, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Text(text, style = T.Small.copy(color = if (on) (if (color == C.teal) C.tealText else color) else C.ink, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        modifier = modifier.clip(PILL).background(if (on) color.copy(alpha = .14f) else Color.Transparent).border(1.dp, if (on) color else C.line, PILL)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { content() }
}

enum class Tone { ALARM, WARN, PLAIN, GOOD }

/** A small label: «просрочено», «сегодня». */
@Composable
fun Tag(text: String, tone: Tone = Tone.PLAIN, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        Tone.ALARM -> C.stamp to C.onStamp
        Tone.WARN -> C.warn.copy(alpha = .2f) to (if (C.dark) C.warn else Color(0xFF8A5A0A))
        Tone.GOOD -> C.ok.copy(alpha = .16f) to C.ok
        Tone.PLAIN -> C.paper2 to C.muted
    }
    Text(text, style = T.Tiny.copy(color = fg, fontWeight = FontWeight.Bold), maxLines = 1,
        modifier = modifier.clip(PILL).background(bg).padding(horizontal = 9.dp, vertical = 3.dp))
}

/** A field shot from the station's files. */
@Composable
fun ShotPic(id: Long, modifier: Modifier = Modifier, big: Boolean = false, stamp: Long = 0) {
    val c = LocalContext.current.container
    val key = "$id:$big:$stamp"
    val state = remember(key) { mutableStateOf<ImageBitmap?>(c.photos.cached(id, big, stamp)?.asImageBitmap()) }
    LaunchedEffect(key) { if (state.value == null) state.value = c.photos.load(id, big, stamp)?.asImageBitmap() }
    Box(modifier.background(C.paper2), contentAlignment = Alignment.Center) {
        state.value?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** A specimen's portrait: its chosen shot, or the bureau's pictogram of its kind. */
@Composable
fun Portrait(sp: Specimen, size: Dp = 56.dp, mood: Mood = Mood.CALM, glow: Boolean = false) {
    val planet = Planet.of(sp.planet)
    // A thin gold ring with a gap, like a porthole drawn on a plate.
    val gap = if (glow) 6.dp else (size.value * 0.04f).coerceIn(1.5f, 4f).dp
    Box(Modifier.size(size).border(1.dp, C.teal.copy(alpha = if (glow) .7f else .5f), androidx.compose.foundation.shape.CircleShape).padding(gap)
        .clip(androidx.compose.foundation.shape.CircleShape).background(C.screen), contentAlignment = Alignment.Center) {
        if (sp.portrait != 0L) ShotPic(sp.portrait, Modifier.fillMaxSize())
        else Bust(planet, Modifier.size(size * 0.86f).padding(top = size * 0.08f))
    }
}

/** A polaroid: the shot on a white frame with a typed caption under it. */
@Composable
fun Polaroid(id: Long, caption: String?, modifier: Modifier = Modifier, angle: Float = 0f, onClick: (() -> Unit)? = null) {
    Column(modifier.rotate(angle).shadow(2.dp, R4).background(Color(0xFFFDFBF6)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 9.dp)) {
        ShotPic(id, Modifier.fillMaxWidth().height(130.dp))
        if (caption != null) Text(caption, style = T.TypedSmall.copy(color = Color(0xFF3A3A3A), fontSize = 10.5.sp, lineHeight = 13.sp),
            maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun Rule(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(C.line))

/** A dashed rule, as typed with hyphens. */
@Composable
fun Dashes(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).drawBehind {
    val dash = 5.dp.toPx(); var x = 0f
    while (x < size.width) { drawLine(C.muted.copy(alpha = .5f), Offset(x, 0f), Offset(minOf(x + dash, size.width), 0f), 1.5f); x += dash * 2 }
})

@Composable
fun Gap(h: Dp) = Box(Modifier.height(h))

/** A text field with its label above. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String?, modifier: Modifier = Modifier, hint: String? = null, number: Boolean = false, lines: Int = 1) {
    Column(modifier.fillMaxWidth()) {
        if (label != null) Label(label, top = 0.dp)
        BasicTextField(
            value, onChange, textStyle = T.Body.copy(color = C.ink), singleLine = lines == 1, minLines = lines, cursorBrush = SolidColor(C.teal),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().clip(R8).background(C.paper).border(1.dp, C.line, R8).padding(horizontal = 14.dp, vertical = 13.dp)) {
                    if (value.isEmpty() && hint != null) Text(hint, style = T.Body.copy(color = C.muted))
                    inner()
                }
            },
        )
    }
}

/** A dimmed layer; tapping outside or Back closes it. */
@Composable
fun Scrim(onDismiss: (() -> Unit)?, bottom: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    if (onDismiss != null) BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(C.scrim).clickable(remember { MutableInteractionSource() }, null) { onDismiss?.invoke() }.imePadding()
            .padding(if (bottom) 0.dp else 18.dp),
        contentAlignment = if (bottom) Alignment.BottomCenter else Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 560.dp).clickable(remember { MutableInteractionSource() }, null) {}) { content() }
    }
}

/** A bottom sheet with a title. */
@Composable
fun Sheet(title: String?, onDismiss: () -> Unit, over: String? = null, content: @Composable ColumnScope.() -> Unit) {
    // A sheet opened from a tab is drawn by the tabs' host, above the tab bar.
    val above = LocalAbove.current
    if (above != null) {
        val latest by rememberUpdatedState<@Composable () -> Unit> { SheetBody(title, over, onDismiss, content) }
        val entry = remember { @Composable { latest() } }
        DisposableEffect(above) { above.add(entry); onDispose { above.remove(entry) } }
        return
    }
    SheetBody(title, over, onDismiss, content)
}

/** Where the tab screens' sheets go to be drawn over the tab bar; null on pages, which already cover it. */
val LocalAbove = staticCompositionLocalOf<MutableList<@Composable () -> Unit>?> { null }

@Composable
private fun SheetBody(title: String?, over: String?, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scrim(onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.sheet)
            .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 18.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(42.dp, 4.dp).clip(R4).background(C.ink.copy(alpha = .2f)))
            if (over != null) Typed(over, Modifier.padding(bottom = 2.dp))
            if (title != null) Text(title, style = T.Title.copy(fontSize = 23.sp), modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

/** A label above a group in a form. */
@Composable
fun Label(text: String, top: Dp = 16.dp) = Typed(text, Modifier.padding(top = top, bottom = 7.dp, start = 2.dp))

/** A row in a menu sheet: an icon on teal steel, a title and a line under it. */
@Composable
fun MenuRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(R12).background(C.paper).border(1.dp, C.line, R12).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(PILL).border(1.dp, C.line, PILL), contentAlignment = Alignment.Center) { Icon(icon, null, tint = C.tealText, modifier = Modifier.size(21.dp)) }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = T.Strong.copy(fontSize = 16.sp))
            Text(sub, style = T.Small)
        }
    }
}

/** A bar of a gauge on the phosphor screen. */
@Composable
fun Meter(progress: Float, modifier: Modifier = Modifier, color: Color = C.teal, track: Color = C.line, h: Dp = 8.dp) {
    Box(modifier.fillMaxWidth().height(h).clip(PILL).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(h).clip(PILL).background(color))
    }
}

/** A key – value line in a typed file. */
@Composable
fun KV(k: String, v: String, modifier: Modifier = Modifier, vColor: Color = C.ink) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(k, style = T.TypedSmall, modifier = Modifier.weight(0.42f))
        Text(v, style = T.Typed.copy(color = vColor, fontSize = 13.sp), modifier = Modifier.weight(0.58f))
    }
}
