package app.specimen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.domain.Mood
import app.specimen.domain.Planet
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The bureau's pictogram of a specimen: a head drawn in strokes, ears or crest by planet, eyes and mouth by mood.
 * Used where there is no photo, on the mood picker and in the atlas.
 */
@Composable
fun Glyph(planet: Planet, mood: Mood, modifier: Modifier, color: Color) {
    Canvas(modifier) { glyph(planet, mood, color) }
}

fun DrawScope.glyph(planet: Planet, mood: Mood, color: Color) {
    val w = size.minDimension
    val cx = size.width / 2; val cy = size.height / 2 + w * 0.06f
    val r = w * 0.30f
    val sw = w * 0.055f
    val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(cx + x * r, cy + y * r)
    // Ears, crest or nothing.
    when (planet) {
        Planet.FELIS -> listOf(-1f, 1f).forEach { s ->
            drawPath(Path().apply { moveTo(p(0.55f * s, -0.82f).x, p(0.55f * s, -0.82f).y); lineTo(p(0.95f * s, -1.45f).x, p(0.95f * s, -1.45f).y); lineTo(p(0.98f * s, -0.42f).x, p(0.98f * s, -0.42f).y) }, color, style = st)
        }
        Planet.CANIS -> listOf(-1f, 1f).forEach { s ->
            drawPath(Path().apply { moveTo(p(0.6f * s, -0.8f).x, p(0.6f * s, -0.8f).y); quadraticTo(p(1.55f * s, -0.9f).x, p(1.55f * s, -0.9f).y, p(1.25f * s, 0.25f).x, p(1.25f * s, 0.25f).y) }, color, style = st)
        }
        Planet.PSITTACUS -> {
            listOf(-0.35f, 0f, 0.35f).forEach { dx -> drawLine(color, p(dx * 0.6f, -1f), p(dx * 1.4f, -1.55f), sw, StrokeCap.Round) }
        }
        Planet.GLIRES -> listOf(-1f, 1f).forEach { s ->
            drawOval(color, topLeft = p(0.35f * s - 0.22f, -1.85f), size = Size(r * 0.44f, r * 1.05f), style = st)
        }
        Planet.MUSTELA -> listOf(-1f, 1f).forEach { s -> drawCircle(color, r * 0.26f, p(0.78f * s, -0.82f), style = st) }
        Planet.SQUAMATA -> {}
    }
    // Head: round for most, a long flat oval for reptiles, a beak for birds.
    if (planet == Planet.SQUAMATA) drawOval(color, topLeft = p(-1.25f, -0.75f), size = Size(r * 2.5f, r * 1.6f), style = st)
    else drawCircle(color, r, Offset(cx, cy), style = st)
    if (planet == Planet.PSITTACUS) drawPath(Path().apply { moveTo(p(-0.22f, 0.15f).x, p(-0.22f, 0.15f).y); lineTo(p(0f, 0.6f).x, p(0f, 0.6f).y); lineTo(p(0.22f, 0.15f).x, p(0.22f, 0.15f).y) }, color, style = st)
    // Eyes.
    val ey = -0.12f; val ex = 0.42f
    when (mood) {
        Mood.CALM, Mood.AFFECTIONATE -> listOf(-ex, ex).forEach { x -> // half-closed: slow blink
            drawArc(color, 200f, 140f, false, topLeft = p(x - 0.2f, ey - 0.1f), size = Size(r * 0.4f, r * 0.3f), style = Stroke(sw * 0.85f, cap = StrokeCap.Round))
        }
        Mood.PLAYFUL, Mood.CURIOUS -> listOf(-ex, ex).forEach { x -> drawCircle(color, r * (if (mood == Mood.CURIOUS) 0.17f else 0.13f), p(x, ey)) }
        Mood.WARY -> listOf(-ex, ex).forEach { x -> drawCircle(color, r * 0.2f, p(x, ey), style = Stroke(sw * 0.8f)); drawCircle(color, r * 0.07f, p(x, ey)) }
        Mood.IRRITATED -> listOf(-1f, 1f).forEach { s -> drawLine(color, p(ex * s - 0.18f * s, ey - 0.12f), p(ex * s + 0.18f * s, ey + 0.06f), sw * 0.9f, StrokeCap.Round) }
        Mood.UNWELL -> listOf(-ex, ex).forEach { x -> drawLine(color, p(x - 0.17f, ey), p(x + 0.17f, ey), sw * 0.9f, StrokeCap.Round) }
    }
    if (planet == Planet.PSITTACUS) return
    // Mouth.
    val my = 0.38f
    when (mood) {
        Mood.AFFECTIONATE, Mood.PLAYFUL -> drawArc(color, 20f, 140f, false, topLeft = p(-0.32f, my - 0.22f), size = Size(r * 0.64f, r * 0.4f), style = Stroke(sw * 0.85f, cap = StrokeCap.Round))
        Mood.IRRITATED, Mood.UNWELL -> drawArc(color, 200f, 140f, false, topLeft = p(-0.3f, my), size = Size(r * 0.6f, r * 0.34f), style = Stroke(sw * 0.85f, cap = StrokeCap.Round))
        Mood.CURIOUS -> drawCircle(color, r * 0.09f, p(0f, my + 0.05f), style = Stroke(sw * 0.7f))
        else -> drawLine(color, p(-0.16f, my + 0.06f), p(0.16f, my + 0.06f), sw * 0.8f, StrokeCap.Round)
    }
    if (planet == Planet.FELIS || planet == Planet.MUSTELA || planet == Planet.GLIRES) listOf(-1f, 1f).forEach { s ->
        drawLine(color, p(0.45f * s, my), p(1.3f * s, my - 0.12f), sw * 0.45f, StrokeCap.Round)
        drawLine(color, p(0.45f * s, my + 0.12f), p(1.3f * s, my + 0.22f), sw * 0.45f, StrokeCap.Round)
    }
}

/** The eight traits as a radar on the phosphor screen; [base] (the breed's figures) is drawn faint under the observed shape. */
@Composable
fun Radar(values: List<Float>, base: List<Int>?, labels: List<String>, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val label = T.TypedSmall.copy(color = C.phosphor, fontSize = 9.5.sp)
    val phosphor = C.phosphor; val dim = C.phosphorDim
    Canvas(modifier) {
        val n = values.size
        val cx = size.width / 2; val cy = size.height / 2
        val r = min(size.width, size.height) / 2 - 30.dp.toPx()
        fun at(i: Int, v: Float): Offset { val a = -PI / 2 + 2 * PI * i / n; return Offset(cx + (cos(a) * r * v / 10f).toFloat(), cy + (sin(a) * r * v / 10f).toFloat()) }
        for (ring in listOf(2.5f, 5f, 7.5f, 10f)) {
            val path = Path(); for (i in 0 until n) { val o = at(i, ring); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }; path.close()
            drawPath(path, dim, style = Stroke(1.dp.toPx()))
        }
        for (i in 0 until n) drawLine(dim, Offset(cx, cy), at(i, 10f), 1.dp.toPx())
        if (base != null) {
            val path = Path(); base.forEachIndexed { i, v -> val o = at(i, v.toFloat()); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }; path.close()
            drawPath(path, phosphor.copy(alpha = .35f), style = Stroke(1.2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
        }
        val path = Path(); values.forEachIndexed { i, v -> val o = at(i, v); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }; path.close()
        drawPath(path, phosphor.copy(alpha = .18f), style = Fill)
        drawPath(path, phosphor, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        values.forEachIndexed { i, v -> drawCircle(phosphor, 2.6.dp.toPx(), at(i, v)) }
        labels.forEachIndexed { i, l -> drawLabel(tm, l, at(i, 11.9f), label) }
    }
}

private fun DrawScope.drawLabel(tm: TextMeasurer, text: String, at: Offset, style: androidx.compose.ui.text.TextStyle) {
    val m = tm.measure(text, style)
    val x = (at.x - m.size.width / 2).coerceIn(0f, size.width - m.size.width)
    val y = (at.y - m.size.height / 2).coerceIn(0f, size.height - m.size.height)
    drawText(m, topLeft = Offset(x, y))
}

/** Weight over time on graph paper; [lo]..[hi] is the breed's usual range, shaded. */
@Composable
fun WeightChart(points: List<Pair<Long, Float>>, lo: Float, hi: Float, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val style = T.TypedSmall.copy(fontSize = 9.5.sp)
    val ink = C.teal; val band = C.ok.copy(alpha = .12f); val grid = C.line
    Box(modifier.fillMaxWidth().height(150.dp)) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            if (points.isEmpty()) return@Canvas
            val pad = 34.dp.toPx()
            val minV = min(points.minOf { it.second }, if (lo > 0) lo else Float.MAX_VALUE) * 0.95f
            val maxV = max(points.maxOf { it.second }, hi) * 1.05f
            val d0 = points.first().first; val d1 = max(points.last().first, d0 + 1)
            fun x(d: Long) = pad + (size.width - pad - 8.dp.toPx()) * (d - d0) / (d1 - d0).toFloat()
            fun y(v: Float) = size.height - 14.dp.toPx() - (size.height - 24.dp.toPx()) * (v - minV) / (maxV - minV).coerceAtLeast(0.0001f)
            if (hi > 0f) drawRoundRect(band, Offset(pad, y(hi)), Size(size.width - pad, y(lo) - y(hi)), CornerRadius(4f))
            for (k in 0..3) {
                val v = minV + (maxV - minV) * k / 3
                drawLine(grid, Offset(pad, y(v)), Offset(size.width, y(v)), 1f)
                drawLabel(tm, fmtKg(v), Offset(pad / 2, y(v)), style)
            }
            val path = Path()
            points.forEachIndexed { i, (d, v) -> if (i == 0) path.moveTo(x(d), y(v)) else path.lineTo(x(d), y(v)) }
            drawPath(path, ink, style = Stroke(2.2.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
            points.forEach { (d, v) -> drawCircle(ink, 3.dp.toPx(), Offset(x(d), y(v))) }
        }
    }
}

/** «4,2» or «0,035» – as many decimals as a small animal needs. */
fun fmtKg(v: Float): String = when {
    v >= 10f -> String.format(java.util.Locale.getDefault(), "%.1f", v)
    v >= 1f -> String.format(java.util.Locale.getDefault(), "%.2f", v).trimEnd('0').trimEnd(',', '.')
    else -> String.format(java.util.Locale.getDefault(), "%.3f", v).trimEnd('0').trimEnd(',', '.')
}
