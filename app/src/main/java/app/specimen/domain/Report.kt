package app.specimen.domain

import app.specimen.data.Done
import app.specimen.data.Ob
import app.specimen.data.Shot
import app.specimen.data.Specimen
import app.specimen.data.Trial
import kotlin.math.abs

/**
 * A week of observation of one specimen, as the bureau files it: the field photos with their captions, the
 * behavioural profile and how it moved, vital signs, experiments, life support, conclusions and one
 * recommendation. Texts are put together by the UI from these facts.
 */
data class Report(
    val sp: Specimen,
    val week: Long,
    val n: Int,
    val ready: Boolean,
    val photos: List<Photo>,
    val traits: List<Float>,
    val shift: List<Float>,
    val weight: Float?,
    val weightBefore: Float?,
    val weightInNorm: Boolean?,
    val appetite: Int?,
    val stool: Int?,
    val moods: Map<Mood, Int>,
    val symptoms: List<Symptom>,
    val anomalies: List<Anomaly>,
    val concluded: List<Trial>,
    val running: List<Trial>,
    val done: List<Pair<Done, Tx>>,
    val overdue: List<Due>,
    val notes: List<String>,
    val entries: Int,
    val conclusions: List<Conclusion>,
    val advice: Advice?,
) {
    data class Photo(val shot: Shot, val behaviour: Behaviour?, val first: Boolean)

    val days get() = week..(week + 6)

    sealed interface Conclusion {
        data class Up(val t: Trait) : Conclusion
        data class Down(val t: Trait) : Conclusion
        data class Skills(val count: Int) : Conclusion
        data object Stable : Conclusion
        data class Deviations(val count: Int) : Conclusion
        data object Urgent : Conclusion
        data class Trials(val count: Int) : Conclusion
        data object Quiet : Conclusion
        data class Mostly(val m: Mood) : Conclusion
    }

    sealed interface Advice {
        data class Task(val lesson: Lesson) : Advice
        data class Protocol(val due: Due) : Advice
        data object Vet : Advice
    }

    companion object {
        fun build(lab: Lab, sp: Specimen, week: Long): Report {
            val days = week..(week + 6)
            val id = sp.id
            val shots = lab.shots.filter { it.specimen == id && it.day in days }.sortedWith(compareBy({ it.day }, { it.id }))
            val photos = shots.map { sh ->
                val first = lab.firstIn(sh)
                val main = first.firstOrNull() ?: sh.tagList.firstOrNull()
                Photo(sh, main?.let { lab.cat.behaviour(it) }, first.isNotEmpty())
            }
            // New skills first, then the rest, at most eight polaroids.
            val shown = (photos.filter { it.first } + photos.filter { !it.first }).take(8).sortedBy { it.shot.day }
            val ob = lab.obs.filter { it.specimen == id && it.day in days }
            val weights = lab.weights(id)
            val weight = weights.lastOrNull { it.first in days }?.second
            val before = weights.lastOrNull { it.first < week }?.second
            fun mode(kind: Int) = ob.filter { it.kind == kind }.groupingBy { it.value.toInt() }.eachCount().maxByOrNull { it.value }?.key
            val moods = ob.filter { it.kind == Ob.MOOD }.mapNotNull { Mood.entries.getOrNull(it.value.toInt()) }.groupingBy { it }.eachCount()
            val symptoms = ob.filter { it.kind == Ob.SYMPTOM }.mapNotNull { Symptom.entries.getOrNull(it.value.toInt()) }.distinct()
            val anomalies = lab.anomalies(sp, minOf(week + 6, lab.day))
            val concluded = lab.trials.filter { it.specimen == id && !it.open && it.ended in days }
            val running = lab.trials.filter { it.specimen == id && it.open && it.start <= week + 6 }
            val done = lab.done.filter { it.specimen == id && it.day in days }.mapNotNull { d ->
                lab.protocols.firstOrNull { it.id == d.protocol }?.let { p -> d to (if (p.key == "u") Tx(p.title, p.title) else lab.cat.proto(p.key)?.name ?: Tx(p.key, p.key)) }
            }
            val overdue = if (week + 6 >= lab.day) lab.dues(id).filter { it.state == Due.State.OVERDUE } else emptyList()
            val notes = ob.filter { it.kind == Ob.NOTE && it.note.isNotBlank() }.map { it.note }
            val shift = lab.shift(sp, week, week + 6)
            val entries = ob.size + shots.size + done.size + concluded.size

            val c = mutableListOf<Conclusion>()
            if (entries < 3) c += Conclusion.Quiet
            shift.withIndex().filter { it.value >= 0.25f }.maxByOrNull { it.value }?.let { c += Conclusion.Up(Trait.entries[it.index]) }
            shift.withIndex().filter { it.value <= -0.25f }.minByOrNull { it.value }?.let { c += Conclusion.Down(Trait.entries[it.index]) }
            moods.filterKeys { it != Mood.CALM }.maxByOrNull { it.value }?.takeIf { it.value >= 3 }?.let { c += Conclusion.Mostly(it.key) }
            val skills = photos.count { it.first }
            if (skills > 0) c += Conclusion.Skills(skills)
            val weekAnomalies = anomalies.filter { a -> a !is Anomaly.Sign || a.day in days }
            c += when {
                weekAnomalies.any { it.urgent } -> Conclusion.Urgent
                weekAnomalies.isNotEmpty() || symptoms.isNotEmpty() -> Conclusion.Deviations(maxOf(weekAnomalies.size, symptoms.size))
                else -> Conclusion.Stable
            }
            if (concluded.isNotEmpty()) c += Conclusion.Trials(concluded.size)

            val planet = lab.planet(sp)
            val advice: Advice? = when {
                weekAnomalies.any { it.urgent } -> Advice.Vet
                else -> lab.dues(id).firstOrNull { it.state == Due.State.OVERDUE || it.state == Due.State.TODAY || it.state == Due.State.SOON }?.let { Advice.Protocol(it) }
                    ?: lab.nextTask(planet)?.let { Advice.Task(it) }
            }

            return Report(
                sp = sp, week = week, n = lab.reportNumber(id, week), ready = lab.ready(week),
                photos = shown, traits = lab.traits(sp, week + 6), shift = shift,
                weight = weight, weightBefore = before, weightInNorm = weight?.let { lab.inNorm(sp, it) },
                appetite = mode(Ob.APPETITE), stool = mode(Ob.STOOL), moods = moods, symptoms = symptoms,
                anomalies = weekAnomalies, concluded = concluded, running = running, done = done, overdue = overdue,
                notes = notes, entries = entries, conclusions = c, advice = advice,
            )
        }

        /** Whether two weights differ enough to call it a change (2 %). */
        fun changed(a: Float, b: Float) = b > 0f && abs(a - b) / b >= 0.02f
    }
}
