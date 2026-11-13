package app.specimen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.data.Settings
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T

/** Screens that explain themselves on the first visit: a typed memo from the bureau. */
enum class TipOf(val text: Int) {
    STATION(R.array.tip_station), ATLAS(R.array.tip_atlas), REPORTS(R.array.tip_reports), COURSE(R.array.tip_course),
    DOSSIER(R.array.tip_dossier), TRIAL(R.array.tip_trial), SHOT(R.array.tip_shot),
}

@Composable
fun Tip(tip: TipOf, s: Settings) {
    val c = LocalContext.current.container
    var gone by remember { mutableStateOf(false) }
    if (gone || s.tipsOff || tip.name in s.tips) return
    val lines = stringArrayResource(tip.text)
    fun close(all: Boolean) {
        gone = true
        c.write { settings.update { it.copy(tips = it.tips + tip.name, tipsOff = it.tipsOff || all) } }
    }
    Scrim({ close(false) }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(C.paper)
            .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Typed(stringResource(R.string.memo), Modifier.weight(1f))
                Stamp(stringResource(R.string.stamp_read), size = 11f)
            }
            Text(lines[0], style = T.Title.copy(fontSize = 24.sp), modifier = Modifier.padding(top = 8.dp))
            Dashes(Modifier.padding(vertical = 10.dp))
            lines.drop(1).forEachIndexed { k, line ->
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                    Text("${k + 1}.", style = T.Typed.copy(color = C.stamp), modifier = Modifier.padding(end = 10.dp))
                    Text(line, style = T.Typed.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
                }
            }
            Btn(stringResource(R.string.tip_ok), { close(false) }, Modifier.padding(top = 20.dp).fillMaxWidth())
            Text(stringResource(R.string.tip_off), style = T.Small, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { close(true) }.padding(vertical = 12.dp))
        }
    }
}
