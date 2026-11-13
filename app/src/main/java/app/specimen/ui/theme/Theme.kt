package app.specimen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.specimen.R

/**
 * A star atlas: an old celestial atlas on an observatory desk. Night is dark graphite with antique gold;
 * day is warm paper with ink. One accent only, red kept for alarms, and engravings tinted to the theme.
 * Token names are kept from the station's first look: [desk] is the page, [paper] a card, [manila] the console buttons,
 * [screen]/[phosphor] the instruments, [teal] the accent (now gold), [stamp] alarms, [star] the engravings' ink.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    val desk: Color, val deskTop: Color, val paper: Color, val paper2: Color, val line: Color,
    val ink: Color, val muted: Color,
    val teal: Color, val onTeal: Color, val tealText: Color,
    val stamp: Color, val onStamp: Color,
    val manila: Color, val onManila: Color,
    val screen: Color, val phosphor: Color, val phosphorDim: Color,
    val ok: Color, val warn: Color,
    val scrim: Color,
    val star: Color,
    val planetA: Color, val planetB: Color, val planetC: Color,
)

val Space = Palette(
    dark = true,
    desk = Color(0xFF12161C), deskTop = Color(0xFF12161C), paper = Color(0xFF181D25), paper2 = Color(0xFF1D232C), line = Color(0xFF262D37),
    ink = Color(0xFFECE6D6), muted = Color(0xFF8A8F99),
    teal = Color(0xFFC9A86A), onTeal = Color(0xFF12161C), tealText = Color(0xFFC9A86A),
    stamp = Color(0xFFD9826E), onStamp = Color(0xFF1E0E0A),
    manila = Color(0xFF181D25), onManila = Color(0xFFECE6D6),
    screen = Color(0xFF151A21), phosphor = Color(0xFFC9A86A), phosphorDim = Color(0xFF2C333D),
    ok = Color(0xFF8DBF8F), warn = Color(0xFFD9B26E),
    scrim = Color(0xCC07090C),
    star = Color(0xFFC9A86A),
    planetA = Color(0xFFC9A86A), planetB = Color(0xFFC9A86A), planetC = Color(0xFFC9A86A),
)

val Dawn = Palette(
    dark = false,
    desk = Color(0xFFF5F1E8), deskTop = Color(0xFFF5F1E8), paper = Color(0xFFFBF8F1), paper2 = Color(0xFFF0EADD), line = Color(0xFFE2D9C7),
    ink = Color(0xFF1F2430), muted = Color(0xFF7A7468),
    teal = Color(0xFF9C7A3C), onTeal = Color(0xFFFFFBF2), tealText = Color(0xFF8E6E33),
    stamp = Color(0xFFB4533E), onStamp = Color(0xFFFFFFFF),
    manila = Color(0xFFFBF8F1), onManila = Color(0xFF1F2430),
    screen = Color(0xFFFBF8F1), phosphor = Color(0xFF8E6E33), phosphorDim = Color(0xFFE2D9C7),
    ok = Color(0xFF4E7D52), warn = Color(0xFFA77B22),
    scrim = Color(0x801F2430),
    star = Color(0xFF39404D),
    planetA = Color(0xFF39404D), planetB = Color(0xFF39404D), planetC = Color(0xFF39404D),
)

/** The palette in use. Read from anywhere; switching theme recomposes. */
object C {
    var p by mutableStateOf(Space)
    val dark get() = p.dark
    val desk get() = p.desk
    val deskTop get() = p.deskTop
    val paper get() = p.paper
    val paper2 get() = p.paper2
    val line get() = p.line
    val ink get() = p.ink
    val muted get() = p.muted
    val teal get() = p.teal
    val onTeal get() = p.onTeal
    val tealText get() = p.tealText
    val stamp get() = p.stamp
    val onStamp get() = p.onStamp
    val manila get() = p.manila
    val onManila get() = p.onManila
    val screen get() = p.screen
    val phosphor get() = p.phosphor
    val phosphorDim get() = p.phosphorDim
    val ok get() = p.ok
    val warn get() = p.warn
    val scrim get() = p.scrim
    val star get() = p.star
    val planetA get() = p.planetA
    val planetB get() = p.planetB
    val planetC get() = p.planetC
    /** An opaque surface for sheets and dialogs. */
    val sheet get() = if (p.dark) Color(0xFF171C23) else Color(0xFFFBF8F1)
}

/** IBM Plex Mono (SIL OFL): the typewriter of the bureau's reports. */
val Mono = FontFamily(Font(R.font.plex_mono, FontWeight.Normal), Font(R.font.plex_mono, FontWeight.Bold))

/** Lora (SIL OFL): the atlas's serif for titles and names. */
val Display = FontFamily(Font(R.font.lora_medium, FontWeight.Medium), Font(R.font.lora_semibold, FontWeight.Bold))

/** Kept for the report: the title face. */
val Narrow = Display

/** Rubik (SIL OFL): everything else. */
val Sans = FontFamily(
    Font(R.font.rubik_regular, FontWeight.Normal),
    Font(R.font.rubik_medium, FontWeight.Medium),
    Font(R.font.rubik_medium, FontWeight.SemiBold),
    Font(R.font.rubik_bold, FontWeight.Bold),
)

object T {
    val Hero get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp, color = C.ink)
    val Title get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 32.sp, color = C.ink)
    val Heading get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 24.sp, color = C.ink)
    /** Small spaced capitals over titles and sections. */
    val Tab get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 13.sp, letterSpacing = 2.2.sp, color = C.muted)
    val Typed get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 20.sp, color = C.ink)
    val TypedSmall get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 16.sp, color = C.muted)
    val Strong get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, color = C.ink)
    val Body get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Small get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp, color = C.muted)
    val Tiny get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, color = C.muted)
    val Button get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 18.sp)
    val Number get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 30.sp, color = C.ink)
}

/** 0: night (graphite), 1: day (paper), 2: as the system. */
@Composable
fun BureauTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) { 0 -> true; 1 -> false; else -> isSystemInDarkTheme() }
    val p = if (dark) Space else Dawn
    if (C.p != p) C.p = p
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        if (C.p != p) C.p = p
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    val scheme = if (dark) darkColorScheme(primary = p.teal, onPrimary = p.onTeal, background = p.desk, surface = C.sheet, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.stamp)
    else lightColorScheme(primary = p.teal, onPrimary = p.onTeal, background = p.desk, surface = C.sheet, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.stamp)
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
