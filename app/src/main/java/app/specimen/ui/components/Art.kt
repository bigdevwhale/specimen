package app.specimen.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import app.specimen.ui.theme.C
import androidx.compose.ui.res.painterResource
import app.specimen.R
import app.specimen.domain.Planet

/*
 * The Codex-drawn engravings, kept as white line masks and tinted to the theme's ink (tools/gen_art.py → tools/cut.py): the six home planets, the rank badges and the
 * envoy in his bubble helmet.
 */

fun planetArt(p: Planet): Int = when (p) {
    Planet.FELIS -> R.drawable.planet_felis
    Planet.CANIS -> R.drawable.planet_canis
    Planet.PSITTACUS -> R.drawable.planet_psittacus
    Planet.GLIRES -> R.drawable.planet_glires
    Planet.MUSTELA -> R.drawable.planet_mustela
    Planet.SQUAMATA -> R.drawable.planet_squamata
}

private val ranks = listOf(R.drawable.rank_0, R.drawable.rank_1, R.drawable.rank_2, R.drawable.rank_3, R.drawable.rank_4, R.drawable.rank_5, R.drawable.rank_6, R.drawable.rank_7)

@Composable
fun PlanetArt(p: Planet, modifier: Modifier) = Image(painterResource(planetArt(p)), null, modifier, contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(C.star))

@Composable
fun RankBadge(i: Int, modifier: Modifier) = Image(painterResource(ranks[i.coerceIn(0, ranks.lastIndex)]), null, modifier, contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(C.star))

@Composable
fun Envoy(modifier: Modifier) = Image(painterResource(R.drawable.hero), null, modifier, contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(C.star))

fun bustArt(p: Planet): Int = when (p) {
    Planet.FELIS -> R.drawable.bust_felis
    Planet.CANIS -> R.drawable.bust_canis
    Planet.PSITTACUS -> R.drawable.bust_psittacus
    Planet.GLIRES -> R.drawable.bust_glires
    Planet.MUSTELA -> R.drawable.bust_mustela
    Planet.SQUAMATA -> R.drawable.bust_squamata
}

/** An engraved portrait of the kind, for a specimen without a photo. */
@Composable
fun Bust(p: Planet, modifier: Modifier, alpha: Float = 1f) =
    Image(painterResource(bustArt(p)), null, modifier, contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(C.star.copy(alpha = alpha)))
