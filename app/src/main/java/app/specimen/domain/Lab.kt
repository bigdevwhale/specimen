package app.specimen.domain

import app.specimen.data.Done
import app.specimen.data.Ob
import app.specimen.data.Obs
import app.specimen.data.Protocol
import app.specimen.data.Settings
import app.specimen.data.Shot
import app.specimen.data.Specimen
import app.specimen.data.Trial
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

fun today(): Long = LocalDate.now().toEpochDay()

/** Monday of the week [day] falls in. */
fun weekOf(day: Long): Long = day - (LocalDate.ofEpochDay(day).dayOfWeek.value - 1)

/** A protocol with its state today. */
data class Due(val p: Protocol, val name: Tx, val day: Long?, val state: State) {
    enum class State { OVERDUE, TODAY, SOON, LATER, UNKNOWN, ONCE }
}

/** Something in the log that a vet should hear about. */
sealed interface Anomaly {
    val urgent: Boolean
    data class Sign(val s: Symptom, val day: Long, override val urgent: Boolean) : Anomaly
    data class Vomits(val count: Int) : Anomaly { override val urgent get() = count >= 3 }
    data class NotEating(val days: Int, override val urgent: Boolean) : Anomaly
    data class Weight(val percent: Int) : Anomaly { override val urgent get() = false }
}

/** A food tried, with how it went. */
data class Food(val name: String, val kind: TrialKind, val outcome: Outcome?, val tries: Int, val last: Long, val bad: Boolean)

data class Rank(val index: Int, val from: Int, val next: Int?)

/**
 * Everything the station knows, computed from the log: each specimen's traits as observed, the atlas,
 * which protocols are due, anomalies, grants and the researcher's rank.
 */
class Lab(
    val s: Settings,
    val cat: Catalog,
    val specimens: List<Specimen>,
    val obs: List<Obs>,
    val trials: List<Trial>,
    val protocols: List<Protocol>,
    val done: List<Done>,
    val shots: List<Shot>,
    val day: Long = today(),
) {
    val here = specimens.filter { it.here }
    val planets: List<Planet> get() = specimens.map { Planet.of(it.planet) }.distinct().ifEmpty { listOf(Planet.FELIS) }

    fun specimen(id: Long) = specimens.firstOrNull { it.id == id }
    fun breed(sp: Specimen) = cat.breed(sp.breed)
    fun planet(sp: Specimen) = Planet.of(sp.planet)
    fun obsOf(id: Long) = obs.filter { it.specimen == id }
    fun shotsOf(id: Long) = shots.filter { it.specimen == id }
    fun trialsOf(id: Long) = trials.filter { it.specimen == id }
    fun protocolsOf(id: Long) = protocols.filter { it.specimen == id && !it.off }

    // ── traits ──

    /** Each behaviour tag and logged mood is a signal pushing one trait up or down. */
    private fun signals(id: Long, from: Long = Long.MIN_VALUE, to: Long = Long.MAX_VALUE): FloatArray {
        val out = FloatArray(Trait.entries.size)
        shots.filter { it.specimen == id && it.day in from..to }.forEach { sh ->
            sh.tagList.forEach { k -> cat.behaviour(k)?.let { b -> b.trait?.let { out[it.ordinal] += b.delta.toFloat() } } }
        }
        obs.filter { it.specimen == id && it.kind == Ob.MOOD && it.day in from..to }.forEach { o ->
            Mood.entries.getOrNull(o.value.toInt())?.let { m -> m.trait?.let { out[it.ordinal] += m.delta * 0.5f } }
        }
        return out
    }

    private fun push(sig: Float) = (sig * STEP).coerceIn(-MAX_SHIFT, MAX_SHIFT)

    /** 1–10 per trait: the breed's figure moved by what was observed. */
    fun traits(sp: Specimen, upTo: Long = day): List<Float> {
        val base = breed(sp).traits
        val sig = signals(sp.id, to = upTo)
        return Trait.entries.map { (base[it.ordinal] + push(sig[it.ordinal])).coerceIn(1f, 10f) }
    }

    /** How each trait moved between the end of the day before [from] and [to]. */
    fun shift(sp: Specimen, from: Long, to: Long): List<Float> {
        val a = traits(sp, from - 1)
        val b = traits(sp, to)
        return Trait.entries.map { b[it.ordinal] - a[it.ordinal] }
    }

    // ── the atlas ──

    /** Behaviour key → the first shot that showed it, across the whole colony. */
    val atlas: Map<String, Shot> by lazy {
        val m = mutableMapOf<String, Shot>()
        shots.sortedWith(compareBy({ it.day }, { it.id })).forEach { sh -> sh.tagList.forEach { if (it !in m) m[it] = sh } }
        m
    }

    /** Behaviours this specimen showed for the first time in [shot]. */
    fun firstIn(shot: Shot): List<String> {
        val before = shots.filter { it.specimen == shot.specimen && (it.day < shot.day || (it.day == shot.day && it.id < shot.id)) }.flatMap { it.tagList }.toSet()
        return shot.tagList.filter { it !in before }
    }

    fun shotsWith(tag: String) = shots.filter { tag in it.tagList }

    // ── life support ──

    fun dues(id: Long): List<Due> = protocolsOf(id).map { p ->
        val name = if (p.key == "u") Tx(p.title, p.title) else cat.proto(p.key)?.name ?: Tx(p.key, p.key)
        val d = p.due
        val st = when {
            p.every <= 0 -> Due.State.ONCE
            d == null -> Due.State.UNKNOWN
            d < day -> Due.State.OVERDUE
            d == day -> Due.State.TODAY
            d <= day + 7 -> Due.State.SOON
            else -> Due.State.LATER
        }
        Due(p, name, d, st)
    }.sortedWith(compareBy({ it.state.ordinal }, { it.day ?: Long.MAX_VALUE }))

    val allDues get() = here.flatMap { dues(it.id) }

    /** Share of known recurring protocols that are not overdue, 0–100; null with nothing to judge. */
    fun integrity(id: Long? = null): Int? {
        val list = (if (id == null) allDues else dues(id)).filter { it.state in setOf(Due.State.OVERDUE, Due.State.TODAY, Due.State.SOON, Due.State.LATER) }
        if (list.isEmpty()) return null
        return (100f * list.count { it.state != Due.State.OVERDUE } / list.size).roundToInt()
    }

    // ── health ──

    fun weights(id: Long) = obs.filter { it.specimen == id && it.kind == Ob.WEIGHT && it.value > 0f }.map { it.day to it.value }

    fun lastWeight(id: Long) = weights(id).lastOrNull()

    /** Within the breed's usual adult range (null if the breed gives none). */
    fun inNorm(sp: Specimen, kg: Float): Boolean? {
        val b = breed(sp)
        if (b.wMax <= 0f) return null
        return kg >= b.wMin * 0.9f && kg <= b.wMax * 1.1f
    }

    fun anomalies(sp: Specimen, upTo: Long = day): List<Anomaly> {
        val p = planet(sp)
        val mine = obs.filter { it.specimen == sp.id && it.day <= upTo }
        val out = mutableListOf<Anomaly>()
        mine.filter { it.kind == Ob.SYMPTOM && it.day >= upTo - 2 }.mapNotNull { o -> Symptom.entries.getOrNull(o.value.toInt())?.let { it to o.day } }
            .distinctBy { it.first }.filter { it.first != Symptom.VOMIT }
            .forEach { (sy, d) -> out += Anomaly.Sign(sy, d, sy.urgentOn(p)) }
        val vomits = mine.count { it.kind == Ob.SYMPTOM && it.value.toInt() == Symptom.VOMIT.ordinal && it.day > upTo - 7 }
        if (vomits >= 2) out += Anomaly.Vomits(vomits)
        // Two days running with the bowl refused.
        val refused = mine.filter { it.kind == Ob.APPETITE && it.value.toInt() == 0 && it.day >= upTo - 3 }.map { it.day }.toSet()
        val run = (0..3).takeWhile { (upTo - it) in refused }.size
        if (run >= 2 && out.none { it is Anomaly.Sign && it.s == Symptom.NO_FOOD }) out += Anomaly.NotEating(run, p in Symptom.NO_FOOD.urgentFor)
        // More than a tenth of the body weight gone or gained in a month.
        val w = weights(sp.id).filter { it.first <= upTo }
        val last = w.lastOrNull()
        val month = w.lastOrNull { it.first <= (last?.first ?: 0) - 21 && it.first >= (last?.first ?: 0) - 45 }
        if (last != null && month != null && month.second > 0f) {
            val pc = ((last.second - month.second) / month.second * 100).roundToInt()
            if (abs(pc) >= 10) out += Anomaly.Weight(pc)
        }
        return out.sortedBy { if (it.urgent) 0 else 1 }
    }

    // ── food ──

    fun foods(id: Long): List<Food> = trials.filter { it.specimen == id && TrialKind.entries.getOrNull(it.kind)?.food == true }
        .groupBy { it.subject.trim().lowercase() }
        .map { (_, list) ->
            val l = list.maxBy { it.start }
            val outcome = list.filter { !it.open }.maxByOrNull { it.start }?.let { Outcome.entries.getOrNull(it.outcome) }
            Food(l.subject.trim(), TrialKind.entries[l.kind], outcome, list.size, l.start,
                list.any { t -> t.outcome == Outcome.BAD.ordinal || t.reactionList.any { Reaction.of(it)?.bad == true } })
        }
        .sortedWith(compareBy({ it.outcome?.ordinal ?: 9 }, { -it.last }))

    // ── grants and rank ──

    val grants: Int by lazy {
        var g = 0
        obs.groupBy { it.specimen to it.day }.values.forEach { g += minOf(it.size, 5) * G_OBS }
        shots.groupBy { it.specimen to it.day }.values.forEach { g += minOf(it.size, 6) * G_SHOT }
        g += atlas.size * G_DISCOVERY
        g += trials.count { !it.open } * G_TRIAL
        done.forEach { g += if (it.late) G_LATE else G_DONE }
        g += s.lessons.size * G_LESSON + s.tasks.size * G_TASK
        // A week with at least five entries is a report worth filing.
        val weeks = (obs.map { it.specimen to weekOf(it.day) } + shots.map { it.specimen to weekOf(it.day) } + done.map { it.specimen to weekOf(it.day) })
            .groupingBy { it }.eachCount()
        g += weeks.count { (k, n) -> n >= 5 && k.second < weekOf(day) } * G_REPORT
        g
    }

    val wallet get() = grants - s.spent

    val rank: Rank get() {
        val i = RANKS.indexOfLast { grants >= it }
        return Rank(i, RANKS[i], RANKS.getOrNull(i + 1))
    }

    /** Days in a row, up to today, with anything written in the log. */
    val streak: Int get() {
        val days = (obs.map { it.day } + shots.map { it.day } + done.map { it.day } + trials.map { it.start }).toSet()
        var d = if (day in days) day else day - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    // ── reports ──

    /** The weeks this specimen has anything in, newest first. */
    fun reportWeeks(id: Long): List<Long> {
        val days = obs.filter { it.specimen == id }.map { it.day } + shots.filter { it.specimen == id }.map { it.day } +
            trials.filter { it.specimen == id }.flatMap { listOf(it.start, it.ended) }.filter { it > 0 }
        return days.map { weekOf(it) }.distinct().sortedDescending()
    }

    /** The report's serial number: weeks since the first entry about this specimen. */
    fun reportNumber(id: Long, week: Long): Int {
        val first = reportWeeks(id).minOrNull() ?: week
        return ((week - first) / 7).toInt() + 1
    }

    /** A report for the current week is a draft until Sunday evening. */
    fun ready(week: Long) = week + 6 < day || (week + 6 == day && java.time.LocalTime.now().hour >= 18)

    fun report(sp: Specimen, week: Long): Report = Report.build(this, sp, week)

    /** Lessons for the planets on the station, the general ones first. */
    fun course(): List<Lesson> = cat.lessonsOf(null) + planets.flatMap { cat.lessonsOf(it) }

    /** The next lesson whose field task is still open. */
    fun nextTask(planet: Planet): Lesson? = (cat.lessonsOf(planet) + cat.lessonsOf(null)).firstOrNull { it.key !in s.tasks }

    companion object {
        const val STEP = 0.15f
        const val MAX_SHIFT = 4f

        const val G_OBS = 1
        const val G_SHOT = 3
        const val G_DISCOVERY = 10
        const val G_TRIAL = 5
        const val G_DONE = 4
        const val G_LATE = 2
        const val G_LESSON = 8
        const val G_TASK = 6
        const val G_REPORT = 10

        /** Grants needed for each rank, from laboratory assistant to academician. */
        val RANKS = listOf(0, 60, 180, 400, 800, 1500, 2600, 4000)

        /** Report designs and their price in grants. */
        val SKINS = listOf("typed" to 0, "blueprint" to 150, "secret" to 300, "journal" to 600)
    }
}
