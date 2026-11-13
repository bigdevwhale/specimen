package app.specimen.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.specimen.R
import app.specimen.container
import app.specimen.data.Trial
import app.specimen.domain.Ethics
import app.specimen.domain.Lab
import app.specimen.domain.Outcome
import app.specimen.domain.Reaction
import app.specimen.domain.TrialKind
import app.specimen.ui.Nav
import app.specimen.ui.Overlay
import app.specimen.ui.Words
import app.specimen.ui.components.Btn
import app.specimen.ui.components.Chip
import app.specimen.ui.components.Chips
import app.specimen.ui.components.Dashes
import app.specimen.ui.components.Field
import app.specimen.ui.components.IconBtn
import app.specimen.ui.components.KV
import app.specimen.ui.components.Kind
import app.specimen.ui.components.Label
import app.specimen.ui.components.Meter
import app.specimen.ui.components.Paper
import app.specimen.ui.components.R8
import app.specimen.ui.components.Sheet
import app.specimen.ui.components.Stamp
import app.specimen.ui.components.TopBar
import app.specimen.ui.components.Typed
import app.specimen.ui.ctx
import app.specimen.ui.theme.C
import app.specimen.ui.theme.T

/** An application for an experiment: what, what is expected, for how long. The ethics committee reads it first. */
@Composable
fun TrialEdit(o: Overlay.TrialEdit, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val sp = lab.specimen(o.specimen) ?: run { nav.close(); return }
    val planet = lab.planet(sp)
    var kind by remember { mutableStateOf(TrialKind.entries.getOrNull(o.kind)) }
    var subject by remember { mutableStateOf("") }
    var guess by remember { mutableIntStateOf(Outcome.ACCEPTED.ordinal) }
    var days by remember { mutableIntStateOf(kind?.days ?: 3) }
    var note by remember { mutableStateOf("") }
    var refusal by remember { mutableStateOf<Ethics.Refusal?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.trial_new), over = "${Words.num(ctx, sp)} · ${sp.name}", onBack = { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            Label(stringResource(R.string.trial_kind), top = 4.dp)
            var more by remember { mutableStateOf(false) }
            val common = listOf(TrialKind.FOOD, TrialKind.TREAT, TrialKind.MED, TrialKind.VACCINE, TrialKind.VET, TrialKind.TOY)
            Chips {
                (if (more) TrialKind.entries else common + listOfNotNull(kind?.takeIf { it !in common })).forEach { k -> Chip(Words.kind(ctx, k), kind == k) { kind = k; days = k.days } }
                if (!more) Chip(stringResource(R.string.more_kinds), false, color = C.ink) { more = true }
            }
            val k = kind
            if (k != null) {
                Gap()
                Field(subject, { subject = it }, stringResource(R.string.trial_subject), hint = Words.kindHint(ctx, k))
                // A food tried before: what happened then.
                if (k.food && subject.isNotBlank()) lab.foods(sp.id).firstOrNull { it.name.equals(subject.trim(), ignoreCase = true) }?.let { f ->
                    Text(stringResource(R.string.trial_before, f.outcome?.let { Words.outcome(ctx, it).lowercase() } ?: stringResource(R.string.trial_running_short), Words.dateFull(ctx, f.last)),
                        style = T.TypedSmall.copy(color = if (f.bad) C.stamp else C.muted), modifier = Modifier.padding(top = 6.dp))
                }
                Label(stringResource(R.string.trial_guess))
                Chips {
                    listOf(Outcome.ACCEPTED to R.string.guess_like, Outcome.INDIFFERENT to R.string.guess_meh, Outcome.REFUSED to R.string.guess_dislike).forEach { (out, l) ->
                        Chip(stringResource(l), guess == out.ordinal, color = C.ink) { guess = out.ordinal }
                    }
                }
                Label(stringResource(R.string.trial_days))
                Chips { (listOf(1, 3, 7, 14) + listOf(days).filter { it !in listOf(1, 3, 7, 14) }).sorted().forEach { d -> Chip(Words.days(ctx, d), days == d) { days = d } } }
                var noting by remember { mutableStateOf(false) }
                if (noting) { Gap(); Field(note, { note = it }, stringResource(R.string.trial_note), hint = stringResource(R.string.trial_note_hint), lines = 2) }
                else Text(stringResource(R.string.add_note), style = T.Small.copy(color = C.tealText), modifier = Modifier.padding(top = 14.dp).clickable { noting = true }.padding(vertical = 4.dp))
                Btn(stringResource(R.string.trial_submit), {
                    val r = Ethics.check(planet, "$subject $note")
                    if (r != null) { refusal = r; c.write { settings.update { it.copy(refused = it.refused + 1) } } }
                    else c.write {
                        val id = start(Trial(specimen = sp.id, kind = k.ordinal, subject = subject.trim(), guess = guess, start = lab.day, days = days, note = note.trim()))
                        nav.replace(Overlay.Trial(id))
                    }
                }, Modifier.padding(top = 14.dp, bottom = 24.dp).fillMaxWidth(), enabled = subject.isNotBlank())
            }
        }
    }
    refusal?.let { r ->
        Sheet(null, { refusal = null }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Typed(stringResource(R.string.ethics_committee), Modifier.weight(1f))
                Stamp(stringResource(R.string.stamp_refused), angle = -10f, size = 17f)
            }
            Text(stringResource(R.string.ethics_title), style = T.Title.copy(fontSize = 22.sp), modifier = Modifier.padding(top = 12.dp))
            Dashes(Modifier.padding(vertical = 10.dp))
            Text(stringResource(when (r.why) {
                Ethics.Why.SCARE -> R.string.ethics_scare
                Ethics.Why.PUNISH -> R.string.ethics_punish
                Ethics.Why.HUNGER -> R.string.ethics_hunger
                Ethics.Why.TOXIC -> R.string.ethics_toxic
                Ethics.Why.HUMAN_MEDS -> R.string.ethics_meds
            }, r.word, Words.planet(ctx, planet)), style = T.Typed)
            Btn(stringResource(R.string.ethics_ok), { refusal = null }, Modifier.padding(top = 18.dp).fillMaxWidth())
        }
    }
}

@Composable private fun Gap() = Spacer(Modifier.size(14.dp))

/** An experiment: its application, how far along it is, and the conclusion. */
@Composable
fun TrialPage(id: Long, lab: Lab, nav: Nav) {
    val ctx = ctx()
    val c = ctx.container
    val t = lab.trials.firstOrNull { it.id == id } ?: run { nav.close(); return }
    val sp = lab.specimen(t.specimen) ?: run { nav.close(); return }
    val kind = TrialKind.entries.getOrNull(t.kind) ?: TrialKind.OTHER
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(t.subject, over = "${Words.kind(ctx, kind)} · ${sp.name}", onBack = { nav.close() }) {
            IconBtn(Icons.Outlined.Delete, { confirm = true }, desc = stringResource(R.string.delete))
        }
        Paper(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
            Typed(stringResource(R.string.trial_file, t.id))
            Dashes(Modifier.padding(vertical = 8.dp))
            KV(stringResource(R.string.trial_k_specimen), "${sp.name} (${Words.num(ctx, sp)})")
            KV(stringResource(R.string.trial_k_start), Words.dateFull(ctx, t.start))
            KV(stringResource(R.string.trial_k_days), Words.days(ctx, t.days))
            Outcome.entries.getOrNull(t.guess)?.let { KV(stringResource(R.string.trial_k_guess), Words.outcome(ctx, it)) }
            if (t.note.isNotBlank() && t.open) KV(stringResource(R.string.trial_k_note), t.note)
            if (t.open) {
                val day = (lab.day - t.start).toInt() + 1
                Meter(day.toFloat() / t.days, Modifier.padding(top = 10.dp), h = 5.dp)
                Text(if (day > t.days) stringResource(R.string.trial_ripe) else stringResource(R.string.day_of, day, t.days), style = T.TypedSmall, modifier = Modifier.padding(top = 5.dp))
            }
        }
        if (t.open) Conclude(t, lab) else Result(t)
        Spacer(Modifier.size(24.dp))
    }
    if (confirm) Sheet(stringResource(R.string.delete_trial), { confirm = false }) {
        Btn(stringResource(R.string.delete), { c.write { drop(t) }; nav.close() }, Modifier.fillMaxWidth(), kind = Kind.STAMP)
    }
}

@Composable
private fun Conclude(t: Trial, lab: Lab) {
    val ctx = ctx()
    val c = ctx.container
    var outcome by remember { mutableIntStateOf(-1) }
    val reactions = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf(t.note) }
    Column(Modifier.padding(horizontal = 18.dp)) {
        Label(stringResource(R.string.trial_outcome), top = 22.dp)
        Chips { Outcome.entries.forEach { o -> Chip(Words.outcome(ctx, o), outcome == o.ordinal, color = if (o == Outcome.BAD) C.stamp else C.teal) { outcome = o.ordinal } } }
        Label(stringResource(R.string.trial_reactions))
        Chips {
            val k = TrialKind.entries.getOrNull(t.kind) ?: TrialKind.OTHER
            val fit = when {
                k.food -> listOf(Reaction.EAGER, Reaction.EXPLORED, Reaction.NO_APPETITE, Reaction.VOMIT, Reaction.LOOSE, Reaction.ITCH)
                k == TrialKind.MED || k == TrialKind.VACCINE -> listOf(Reaction.SLEEPY, Reaction.NO_APPETITE, Reaction.VOMIT, Reaction.LOOSE, Reaction.ITCH, Reaction.SWELLING)
                k in setOf(TrialKind.VET, TrialKind.MOVE, TrialKind.GUEST, TrialKind.NEWPET, TrialKind.NOISE) -> listOf(Reaction.EXPLORED, Reaction.PURR, Reaction.HIDING, Reaction.VOCAL, Reaction.AGGRESSION, Reaction.NO_APPETITE)
                else -> listOf(Reaction.EAGER, Reaction.EXPLORED, Reaction.PURR, Reaction.HIDING, Reaction.VOCAL, Reaction.AGGRESSION)
            }
            fit.forEach { r ->
                Chip(Words.reaction(ctx, r), r.name in reactions, color = if (r.bad) C.stamp else C.teal) { if (r.name in reactions) reactions.remove(r.name) else reactions.add(r.name) }
            }
        }
        Spacer(Modifier.size(14.dp))
        Field(note, { note = it }, stringResource(R.string.trial_note), lines = 2)
        if (reactions.any { Reaction.of(it)?.bad == true } && TrialKind.entries.getOrNull(t.kind) in setOf(TrialKind.MED, TrialKind.VACCINE))
            Text(stringResource(R.string.trial_side_effects), style = T.Typed.copy(color = C.stamp), modifier = Modifier.padding(top = 10.dp))
        Btn(stringResource(R.string.trial_conclude), { c.write { conclude(t, outcome, reactions.toList(), note) } },
            Modifier.padding(top = 16.dp).fillMaxWidth(), enabled = outcome >= 0)
        if (lab.day - t.start < t.days) Text(stringResource(R.string.trial_early), style = T.TypedSmall, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Result(t: Trial) {
    val ctx = ctx()
    val out = Outcome.entries.getOrNull(t.outcome) ?: return
    val confirmed = t.guess == t.outcome || (Outcome.entries.getOrNull(t.guess)?.good == true && out.good)
    Paper(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Typed(stringResource(R.string.trial_conclusion))
                Text(Words.outcome(ctx, out), style = T.Heading.copy(color = if (out == Outcome.BAD) C.stamp else C.ink), modifier = Modifier.padding(top = 2.dp))
                Text(Words.dateFull(ctx, t.ended), style = T.TypedSmall)
            }
            Stamp(stringResource(if (confirmed) R.string.stamp_confirmed else R.string.stamp_refuted), color = if (confirmed) C.ok else C.stamp, angle = 7f)
        }
        val rs = t.reactionList.mapNotNull { Reaction.of(it) }
        if (rs.isNotEmpty()) Text(rs.joinToString(", ") { Words.reaction(ctx, it).lowercase() }.replaceFirstChar { it.uppercase() }, style = T.Typed, modifier = Modifier.padding(top = 10.dp))
        if (t.note.isNotBlank()) Text(t.note, style = T.Small, modifier = Modifier.padding(top = 8.dp))
    }
}
