package app.specimen.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.specimen.R
import app.specimen.data.Specimen
import app.specimen.domain.Anomaly
import app.specimen.domain.Mood
import app.specimen.domain.Outcome
import app.specimen.domain.Planet
import app.specimen.domain.Reaction
import app.specimen.domain.Symptom
import app.specimen.domain.Trait
import app.specimen.domain.TrialKind
import app.specimen.domain.Tx
import app.specimen.ru
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ready-made phrases; work from a [Context] so the reminders and the PDF can use them too. */
object Words {
    private fun arr(ctx: Context, id: Int, i: Int) = ctx.resources.getStringArray(id).let { it[i.coerceIn(0, it.lastIndex)] }

    fun planet(ctx: Context, p: Planet) = arr(ctx, R.array.planets, p.ordinal)
    fun planetKind(ctx: Context, p: Planet) = arr(ctx, R.array.planet_kinds, p.ordinal)
    fun planetLore(ctx: Context, p: Planet) = arr(ctx, R.array.planet_lore, p.ordinal)
    fun mood(ctx: Context, m: Mood) = arr(ctx, R.array.moods, m.ordinal)
    /** How the mood shows on this planet: «хвост трубой», «перья прижаты». */
    fun moodSign(ctx: Context, m: Mood, p: Planet) = arr(ctx, when (p) {
        Planet.FELIS -> R.array.mood_signs_felis; Planet.CANIS -> R.array.mood_signs_canis; Planet.PSITTACUS -> R.array.mood_signs_psittacus
        Planet.GLIRES -> R.array.mood_signs_glires; Planet.MUSTELA -> R.array.mood_signs_mustela; Planet.SQUAMATA -> R.array.mood_signs_squamata
    }, m.ordinal)
    fun symptom(ctx: Context, s: Symptom) = arr(ctx, R.array.symptoms, s.ordinal)
    fun kind(ctx: Context, k: TrialKind) = arr(ctx, R.array.trial_kinds, k.ordinal)
    fun kindHint(ctx: Context, k: TrialKind) = arr(ctx, R.array.trial_hints, k.ordinal)
    fun outcome(ctx: Context, o: Outcome) = arr(ctx, R.array.outcomes, o.ordinal)
    fun reaction(ctx: Context, r: Reaction) = arr(ctx, R.array.reactions, r.ordinal)
    fun trait(ctx: Context, t: Trait) = arr(ctx, R.array.traits, t.ordinal)
    fun traitShort(ctx: Context, t: Trait) = arr(ctx, R.array.traits_short, t.ordinal)
    /** «дружелюбие» as in «существо демонстрирует дружелюбие». */
    fun traitNoun(ctx: Context, t: Trait) = arr(ctx, R.array.trait_nouns, t.ordinal)
    fun rank(ctx: Context, i: Int) = arr(ctx, R.array.ranks, i)
    fun appetite(ctx: Context, v: Int) = arr(ctx, R.array.appetite, v)
    fun stool(ctx: Context, v: Int) = arr(ctx, R.array.stool, v)
    fun sex(ctx: Context, v: Int) = arr(ctx, R.array.sexes, v)

    fun anomaly(ctx: Context, a: Anomaly): String = when (a) {
        is Anomaly.Sign -> symptom(ctx, a.s)
        is Anomaly.Vomits -> ctx.getString(R.string.anomaly_vomits, a.count)
        is Anomaly.NotEating -> ctx.getString(R.string.anomaly_not_eating, days(ctx, a.days))
        is Anomaly.Weight -> ctx.getString(if (a.percent > 0) R.string.anomaly_weight_up else R.string.anomaly_weight_down, kotlin.math.abs(a.percent))
    }

    fun days(ctx: Context, n: Int) = ctx.resources.getQuantityString(R.plurals.days, n, n)
    fun weeks(ctx: Context, n: Int) = ctx.resources.getQuantityString(R.plurals.weeks, n, n)
    fun grants(ctx: Context, n: Int) = ctx.resources.getQuantityString(R.plurals.grants, n, n)

    /** «3 года 2 месяца», «7 месяцев», «2 недели». */
    fun age(ctx: Context, born: Long, today: Long): String {
        val a = LocalDate.ofEpochDay(born); val b = LocalDate.ofEpochDay(today)
        val p = java.time.Period.between(a, b)
        val r = ctx.resources
        return when {
            p.years > 0 && p.months > 0 && p.years < 5 -> r.getQuantityString(R.plurals.years, p.years, p.years) + " " + r.getQuantityString(R.plurals.months, p.months, p.months)
            p.years > 0 -> r.getQuantityString(R.plurals.years, p.years, p.years)
            p.months > 0 -> r.getQuantityString(R.plurals.months, p.months, p.months)
            else -> weeks(ctx, ((today - born) / 7).toInt().coerceAtLeast(1))
        }
    }

    /** «сегодня», «вчера», «через 3 дня», «5 дней назад». */
    fun rel(ctx: Context, day: Long, today: Long): String {
        val d = (day - today).toInt()
        return when {
            d == 0 -> ctx.getString(R.string.today)
            d == -1 -> ctx.getString(R.string.yesterday)
            d == 1 -> ctx.getString(R.string.tomorrow)
            d > 1 -> ctx.getString(R.string.in_x, days(ctx, d))
            else -> ctx.getString(R.string.x_ago, days(ctx, -d))
        }
    }

    private fun loc(ctx: Context) = if (ctx.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH

    /** «12 нояб.» / «12 Nov». */
    fun date(ctx: Context, day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern(if (ctx.ru) "d MMM" else "d MMM", loc(ctx))).trimEnd('.')

    /** «12.11.2026». */
    fun dateFull(ctx: Context, day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern(if (ctx.ru) "dd.MM.yyyy" else "d MMM yyyy", loc(ctx)))

    /** «9–15 нояб. 2026». */
    fun week(ctx: Context, monday: Long): String {
        val a = LocalDate.ofEpochDay(monday); val b = a.plusDays(6)
        val m = DateTimeFormatter.ofPattern("MMM", loc(ctx))
        return if (a.month == b.month) "${a.dayOfMonth}–${b.dayOfMonth} ${b.format(m).trimEnd('.')} ${b.year}"
        else "${a.dayOfMonth} ${a.format(m).trimEnd('.')} – ${b.dayOfMonth} ${b.format(m).trimEnd('.')} ${b.year}"
    }

    /** «ОБР-007» / «SPC-007». */
    fun num(ctx: Context, sp: Specimen) = ctx.getString(R.string.specimen_num, sp.num)

    fun tx(ctx: Context, t: Tx) = t.of(ctx.ru)

    fun kg(ctx: Context, v: Float) = ctx.getString(R.string.kg, app.specimen.ui.components.fmtKg(v))

    /** 12 500 with a thin gap. */
    fun n(x: Int): String = "%,d".format(Locale.ENGLISH, x).replace(',', ' ')
}

@Composable fun tx(t: Tx) = t.of(LocalContext.current.ru)
@Composable fun rel(day: Long, today: Long) = Words.rel(LocalContext.current, day, today)
@Composable fun date(day: Long) = Words.date(LocalContext.current, day)
@Composable fun ctx(): Context = LocalContext.current
