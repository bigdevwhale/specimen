package app.specimen.ui.settings

import androidx.compose.material.icons.outlined.Devices
import app.specimen.ui.Words
import app.specimen.ui.components.RankBadge
import app.specimen.ui.components.R8
import app.specimen.ui.components.R12
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import app.specimen.notify.Reminders
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.specimen.BuildConfig
import app.specimen.R
import app.specimen.container
import app.specimen.data.Archive
import app.specimen.domain.Lab
import app.specimen.ui.Nav
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.Field
import app.specimen.ui.components.Kind
import app.specimen.ui.components.Paper
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.ctx
import app.specimen.ui.openUrl
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "specimen"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
}

/** A titled group of rows on one card, with an optional note under it. */
@Composable
private fun Group(title: String, note: String? = null, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Typed(title, Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp), C.muted)
    Column(Modifier.fillMaxWidth().clip(R12).border(1.dp, C.line, R12)) { content() }
    if (note != null) Text(note, style = T.TypedSmall, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
}

/** A settings row: icon, label, the current value and a chevron. */
@Composable
private fun SetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = C.tealText, modifier = Modifier.size(21.dp))
        Text(title, style = T.Body, modifier = Modifier.padding(start = 14.dp).weight(1f))
        if (value != null) Text(value, style = T.Small, modifier = Modifier.padding(end = 4.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
    }
}

/** One choice in a picker sheet. */
@Composable
private fun Pick(label: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(R8).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.Body.copy(color = if (on) C.tealText else C.ink), modifier = Modifier.weight(1f))
        if (on) Icon(Icons.Rounded.Check, null, tint = C.tealText)
    }
}

@Composable
private fun Section(title: String, hint: String? = null) {
    Typed(title, Modifier.padding(top = 18.dp, bottom = 6.dp), C.ink.copy(alpha = .75f))
    if (hint != null) Text(hint, style = T.Small, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Link(label: Int, sub: Int, go: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = go).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Strong); Text(stringResource(sub), style = T.Small) }
        Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
    }
}

@Composable
fun SettingsPage(lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val s = lab.s
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(s.researcher) }
    var restoring by remember { mutableStateOf<android.net.Uri?>(null) }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val stamp = LocalDate.now().toString()
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { u ->
        if (u != null) scope.launch {
            val ok = runCatching { ctx.contentResolver.openOutputStream(u)!!.use { Archive.export(c, it) } }.isSuccess
            Toast.makeText(ctx, if (ok) R.string.saved else R.string.failed_file, Toast.LENGTH_LONG).show()
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) restoring = u }

    var sheet by remember { mutableStateOf("") }
    val themeName = stringResource(listOf(R.string.theme_paper, R.string.theme_night, R.string.theme_system)[s.theme.coerceIn(0, 2)])
    val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().take(2)
    val langName = stringResource(when (cur) { "ru" -> R.string.lang_ru; "en" -> R.string.lang_en; else -> R.string.lang_system })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.settings), onBack = { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            // The researcher: the medal, the name under the reports, the rank.
            Row(Modifier.fillMaxWidth().clip(R12).clickable { sheet = "name" }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RankBadge(lab.rank.index, Modifier.size(76.dp))
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(s.researcher.ifBlank { stringResource(R.string.set_researcher_none) }, style = T.Title.copy(fontSize = 24.sp, color = if (s.researcher.isBlank()) C.muted else C.ink))
                    Text(Words.rank(ctx, lab.rank.index), style = T.Small)
                }
                Icon(Icons.Outlined.Edit, null, tint = C.muted, modifier = Modifier.size(20.dp))
            }
            Group(stringResource(R.string.group_app)) {
                SetRow(Icons.Outlined.DarkMode, stringResource(R.string.set_theme), themeName) { sheet = "theme" }
                SetRow(Icons.Outlined.Language, stringResource(R.string.set_lang), langName) { sheet = "lang" }
                SetRow(Icons.Outlined.Notifications, stringResource(R.string.set_remind), if (s.remind) "%d:00".format(s.hour) else stringResource(R.string.off_short)) { sheet = "remind" }
                SetRow(Icons.Outlined.Info, stringResource(R.string.tips_again), null) { c.write { settings.update { it.copy(tips = emptySet(), tipsOff = false) } }; Toast.makeText(ctx, R.string.tips_back, Toast.LENGTH_SHORT).show() }
            }
            Group(stringResource(R.string.group_family)) {
                SetRow(Icons.Outlined.Devices, stringResource(R.string.pair_title),
                    if (s.paired) s.partnerName.ifBlank { stringResource(R.string.pair_on) } else stringResource(R.string.pair_off)) { nav.open(app.specimen.ui.Overlay.Pair) }
            }
            val gone = lab.specimens.filter { !it.here }
            if (gone.isNotEmpty()) Group(stringResource(R.string.missions_over)) {
                gone.forEach { g ->
                    Row(Modifier.fillMaxWidth().clickable { nav.open(app.specimen.ui.Overlay.Dossier(g.id)) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        app.specimen.ui.components.Portrait(g, 34.dp)
                        Text(g.name, style = T.Body, modifier = Modifier.padding(start = 14.dp).weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
                    }
                }
            }
            Group(stringResource(R.string.data), stringResource(R.string.privacy_text)) {
                SetRow(Icons.Outlined.SaveAlt, stringResource(R.string.backup), null) { backup.launch("specimen-$stamp.zip") }
                SetRow(Icons.Outlined.Restore, stringResource(R.string.restore), null) { restore.launch(arrayOf("application/zip", "application/octet-stream")) }
            }
            Group(stringResource(R.string.community), stringResource(R.string.community_text)) {
                SetRow(Icons.Outlined.BugReport, stringResource(R.string.community_bug), null) { openUrl(ctx, Community.BUG) }
                SetRow(Icons.Outlined.Lightbulb, stringResource(R.string.community_idea), null) { openUrl(ctx, Community.IDEA) }
                SetRow(Icons.Outlined.NotificationsActive, stringResource(R.string.community_follow), null) { openUrl(ctx, Community.FOLLOW) }
                SetRow(Icons.Outlined.Forum, stringResource(R.string.community_chat), null) { openUrl(ctx, Community.CHAT) }
                SetRow(Icons.Outlined.FavoriteBorder, stringResource(R.string.community_support), null) { openUrl(ctx, Community.SUPPORT) }
            }
            Column(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Small)
                Text(stringResource(R.string.vet_disclaimer), style = T.TypedSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Text(stringResource(R.string.font_credits), style = T.TypedSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Text(stringResource(R.string.source), style = T.Small.copy(color = C.tealText), modifier = Modifier.padding(top = 8.dp).clip(R8).clickable { openUrl(ctx, "https://github.com/bigdevwhale/specimen") }.padding(6.dp))
            }
            Spacer(Modifier.size(28.dp))
        }
    }

    when (sheet) {
        "name" -> Sheet(stringResource(R.string.set_researcher), { sheet = "" }) {
            Text(stringResource(R.string.set_researcher_b), style = T.Small, modifier = Modifier.padding(bottom = 12.dp))
            Field(name, { name = it }, null, hint = stringResource(R.string.set_researcher_hint))
            Btn(stringResource(R.string.save), { val v = name.trim(); c.write { settings.update { it.copy(researcher = v) } }; sheet = "" }, Modifier.padding(top = 14.dp).fillMaxWidth())
        }
        "theme" -> Sheet(stringResource(R.string.set_theme), { sheet = "" }) {
            listOf(R.string.theme_paper, R.string.theme_night, R.string.theme_system).forEachIndexed { i, l -> Pick(stringResource(l), s.theme == i) { c.write { settings.update { it.copy(theme = i) } }; sheet = "" } }
        }
        "lang" -> Sheet(stringResource(R.string.set_lang), { sheet = "" }) {
            for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                Pick(stringResource(l), cur == tag) { sheet = ""; AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }
        }
        "remind" -> Sheet(stringResource(R.string.set_remind), { sheet = "" }) {
            Text(stringResource(R.string.set_remind_b), style = T.Small, modifier = Modifier.padding(bottom = 12.dp))
            Pick(stringResource(R.string.off_short), !s.remind) { c.write { settings.update { it.copy(remind = false) } }; sheet = "" }
            listOf(8, 9, 10, 12, 18, 20).forEach { h ->
                Pick("%d:00".format(h), s.remind && s.hour == h) {
                    if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS)
                    c.write { settings.update { it.copy(remind = true, hour = h) }; Reminders.schedule(app) }; sheet = ""
                }
            }
        }
    }

    restoring?.let { u ->
        Sheet(stringResource(R.string.restore_t), { restoring = null }) {
            Text(stringResource(R.string.restore_b), style = T.Body)
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(stringResource(R.string.cancel), { restoring = null }, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.restore), {
                    restoring = null
                    scope.launch {
                        val ok = runCatching { ctx.contentResolver.openInputStream(u)!!.use { Archive.import(c, it) } }.getOrDefault(false)
                        if (ok) Archive.restart(ctx) else Toast.makeText(ctx, R.string.failed_file, Toast.LENGTH_LONG).show()
                    }
                }, Modifier.weight(1f), Kind.STAMP)
            }
        }
    }
}
