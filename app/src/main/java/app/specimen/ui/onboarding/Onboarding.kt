package app.specimen.ui.onboarding

import androidx.compose.foundation.layout.height
import app.specimen.ui.components.Envoy
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.domain.Mood
import app.specimen.domain.Planet
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Dashes
import app.specimen.ui.components.Glyph
import app.specimen.ui.components.Paper
import app.specimen.ui.components.Stamp
import app.specimen.ui.components.Typed
import app.specimen.ui.components.desk
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T

/** The appointment letter: you are now a xenologist, and someone from another planet lives with you. */
@Composable
fun Onboarding() {
    val c = ctx().container
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        c.write { settings.update { it.copy(onboarded = true) } }
    }
    Box(Modifier.fillMaxSize().desk()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(18.dp)) {
            Envoy(Modifier.fillMaxWidth().height(230.dp).padding(bottom = 6.dp))
            Paper(Modifier.fillMaxWidth(), pad = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Typed(stringResource(R.string.bureau_name), Modifier.weight(1f))
                    Stamp(stringResource(R.string.top_secret_short), angle = 8f, size = 11f)
                }
                Text(stringResource(R.string.welcome_title), style = T.Hero, modifier = Modifier.padding(top = 14.dp))
                Dashes(Modifier.padding(vertical = 14.dp))
                Text(stringResource(R.string.welcome_body), style = T.Typed)
                Spacer(Modifier.size(12.dp))
                stringArrayResource(R.array.welcome_points).forEachIndexed { i, s ->
                    Row(Modifier.padding(top = 8.dp)) {
                        Text("${i + 1}.", style = T.Typed.copy(color = C.stamp), modifier = Modifier.padding(end = 10.dp))
                        Text(s, style = T.Typed.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
                    }
                }
                Text(stringResource(R.string.welcome_sign), style = T.TypedSmall, modifier = Modifier.padding(top = 18.dp))
            }
            Btn(stringResource(R.string.welcome_go), {
                if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS)
                else c.write { settings.update { it.copy(onboarded = true) } }
            }, Modifier.padding(top = 18.dp).fillMaxWidth())
            Text(stringResource(R.string.welcome_private), style = T.TypedSmall, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
