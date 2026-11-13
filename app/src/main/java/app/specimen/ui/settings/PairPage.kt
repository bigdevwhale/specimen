package app.specimen.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.specimen.R
import app.specimen.container
import app.specimen.domain.Lab
import app.specimen.sync.Invite
import app.specimen.sync.LinkService
import app.specimen.ui.Nav
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Kind
import app.specimen.ui.components.R12
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Two phones keeping one station: a family where both look after the pet. One shows a QR, the other scans it; from
 * then on they find each other on the home Wi-Fi and swap every entry and photo, sealed, with no server in between.
 */
@Composable
fun PairPage(lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val s = lab.s
    val near by c.link.near.collectAsState()
    var invite by remember { mutableStateOf<Invite?>(null) }
    var bad by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    // While this page is open the phone listens for the other one, even before pairing is done.
    DisposableEffect(Unit) { c.link.start("pair"); onDispose { c.link.stop("pair") } }
    val scan = rememberLauncherForActivityResult(ScanContract()) { r ->
        val i = r.contents?.let(Invite::decode)
        if (r.contents != null && i == null) bad = true
        if (i != null) { bad = false; c.write { pair(i) } }
    }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.pair_title), onBack = { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            when {
                s.paired -> {
                    // Who and how the link is.
                    Row(Modifier.fillMaxWidth().clip(R12).border(1.dp, C.line, R12).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (near.together) C.ok else C.muted))
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(s.partnerName.ifBlank { stringResource(R.string.pair_partner) }, style = T.Heading)
                            Text(stringResource(if (near.together) R.string.pair_together else R.string.pair_apart), style = T.Small)
                            if (!near.together && s.met > 0) Text(stringResource(R.string.pair_met, Words.rel(ctx, s.met / 86_400_000L, lab.day)), style = T.TypedSmall)
                            if (near.together && near.received > 0) Text(stringResource(R.string.pair_received, near.received), style = T.TypedSmall.copy(color = C.ok))
                        }
                    }
                    Text(stringResource(R.string.pair_how), style = T.Small, modifier = Modifier.padding(top = 12.dp))
                    // In the background, at home.
                    Row(Modifier.padding(top = 22.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.pair_hum), style = T.Strong)
                            Text(stringResource(R.string.pair_hum_b), style = T.Small)
                        }
                        Switch(s.hum, { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS)
                            c.write { settings.update { it.copy(hum = on) }; LinkService.sync(app, on) }
                        }, colors = SwitchDefaults.colors(checkedTrackColor = C.teal, checkedThumbColor = C.onTeal))
                    }
                    if (s.hum) KeepAwake()
                    Btn(stringResource(R.string.pair_unpair), { confirm = true }, Modifier.padding(top = 28.dp).fillMaxWidth(), kind = Kind.GHOST)
                }
                s.waiting && invite != null -> {
                    // Showing the QR, waiting for the other phone to scan it.
                    Text(stringResource(R.string.pair_show_b), style = T.Body)
                    Box(Modifier.padding(top = 18.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val img = remember(invite) { qr(invite!!.encode(), 720).asImageBitmap() }
                        Image(img, null, Modifier.size(260.dp).clip(R12).background(Color.White).padding(12.dp))
                    }
                    Text(stringResource(R.string.pair_waiting), style = T.Small.copy(color = C.tealText), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
                    Btn(stringResource(R.string.cancel), { invite = null; c.write { unpair() } }, Modifier.padding(top = 18.dp).fillMaxWidth(), kind = Kind.GHOST)
                }
                else -> {
                    Text(stringResource(R.string.pair_intro), style = T.Body)
                    Text(stringResource(R.string.pair_intro_b), style = T.Small, modifier = Modifier.padding(top = 10.dp))
                    Btn(stringResource(R.string.pair_show), { c.write { invite = offer() } }, Modifier.padding(top = 22.dp).fillMaxWidth(), icon = Icons.Outlined.QrCode2)
                    Btn(stringResource(R.string.pair_scan), {
                        runCatching { scan.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false).setOrientationLocked(true).setPrompt("")) }
                    }, Modifier.padding(top = 10.dp).fillMaxWidth(), kind = Kind.SOFT, icon = Icons.Outlined.QrCodeScanner)
                    if (bad) Text(stringResource(R.string.pair_bad), style = T.Small.copy(color = C.stamp), modifier = Modifier.padding(top = 10.dp))
                    Typed(stringResource(R.string.pair_private_t), Modifier.padding(top = 28.dp, bottom = 6.dp))
                    Text(stringResource(R.string.pair_private), style = T.Small)
                }
            }
            Spacer(Modifier.size(28.dp))
        }
    }
    if (confirm) Sheet(stringResource(R.string.pair_unpair), { confirm = false }) {
        Text(stringResource(R.string.pair_unpair_b), style = T.Body)
        Btn(stringResource(R.string.pair_unpair), { confirm = false; c.write { unpair() } }, Modifier.padding(top = 16.dp).fillMaxWidth(), kind = Kind.STAMP)
    }
}

/** Phones that put apps to sleep would silence the link; one tap asks Android to leave it awake. Shown only while it isn't. */
@SuppressLint("BatteryLife")
@Composable
private fun KeepAwake() {
    val ctx = ctx()
    val power = remember { ctx.getSystemService(PowerManager::class.java) }
    if (power.isIgnoringBatteryOptimizations(ctx.packageName)) return
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(R12).border(1.dp, C.warn.copy(alpha = .5f), R12).padding(14.dp)) {
        Text(stringResource(R.string.pair_awake), style = T.Small)
        Btn(stringResource(R.string.pair_awake_go), {
            runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) }
        }, Modifier.padding(top = 10.dp), kind = Kind.SOFT, small = true)
    }
}

fun qr(text: String, size: Int): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"))
    val px = IntArray(size * size) { i -> if (m.get(i % size, i / size)) 0xFF12161C.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}
