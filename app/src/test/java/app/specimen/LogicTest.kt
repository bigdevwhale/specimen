package app.specimen

import app.specimen.data.Ob
import app.specimen.data.Obs
import app.specimen.data.Protocol
import app.specimen.data.Settings
import app.specimen.data.Shot
import app.specimen.data.Specimen
import app.specimen.domain.Anomaly
import app.specimen.domain.Behaviour
import app.specimen.domain.Breed
import app.specimen.domain.Catalog
import app.specimen.domain.Due
import app.specimen.domain.Ethics
import app.specimen.domain.Lab
import app.specimen.domain.Planet
import app.specimen.domain.Report
import app.specimen.domain.Symptom
import app.specimen.domain.Trait
import app.specimen.domain.Tx
import app.specimen.domain.weekOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    private val tx = Tx("x", "x")
    private val cat = Catalog(
        breeds = listOf(Breed("felis_local", Planet.FELIS, tx, List(8) { 5 }, 3.5f, 5.5f, "12-16", emptyList(), emptyList(), tx)),
        risks = emptyList(), protocols = emptyList(),
        behaviours = listOf(Behaviour("cat_belly_up", Planet.FELIS, Trait.SOCIAL, 1, 1, tx, tx, tx, tx, 1), Behaviour("cat_loaf", Planet.FELIS, null, 0, 1, tx, tx, tx, tx, 2)),
        lessons = emptyList(),
    )
    private val day = LocalDate.of(2026, 11, 13).toEpochDay()
    private val sp = Specimen(id = 1, num = 1, name = "Муся", planet = "felis", breed = "felis_local", arrived = day - 100)

    private fun lab(obs: List<Obs> = emptyList(), shots: List<Shot> = emptyList(), protocols: List<Protocol> = emptyList()) =
        Lab(Settings(), cat, listOf(sp), obs, emptyList(), protocols, emptyList(), shots, day)

    @Test fun ethicsCatchesPranksAndPoison() {
        assertEquals(Ethics.Why.SCARE, Ethics.check(Planet.FELIS, "Положить огурец за спину")?.why)
        assertEquals(Ethics.Why.TOXIC, Ethics.check(Planet.FELIS, "Кусочек шоколада")?.why)
        assertEquals(Ethics.Why.TOXIC, Ethics.check(Planet.CANIS, "grapes as a treat")?.why)
        assertEquals(Ethics.Why.HUMAN_MEDS, Ethics.check(Planet.CANIS, "дать Нурофен от боли")?.why)
        assertEquals(Ethics.Why.TOXIC, Ethics.check(Planet.FELIS, "Букет лилий на столе")?.why)
        assertNull(Ethics.check(Planet.FELIS, "Влажный корм с индейкой"))
        assertNull(Ethics.check(Planet.FELIS, "Луковица? нет: лукошко с мячиками"))
        assertNull(Ethics.check(Planet.PSITTACUS, "Виноград без косточек"))
    }

    @Test fun mondayStartsTheWeek() {
        val fri = LocalDate.of(2026, 11, 13).toEpochDay()
        assertEquals(LocalDate.of(2026, 11, 9).toEpochDay(), weekOf(fri))
        assertEquals(LocalDate.of(2026, 11, 9).toEpochDay(), weekOf(LocalDate.of(2026, 11, 9).toEpochDay()))
    }

    @Test fun shotsAndMoodsMoveTraits() {
        val shots = (1..10).map { Shot(id = it.toLong(), specimen = 1, day = day - it, tags = "cat_belly_up") }
        val l = lab(shots = shots)
        assertEquals(5f + 10 * Lab.STEP, l.traits(sp)[Trait.SOCIAL.ordinal], 0.001f)
        assertEquals(5f, l.traits(sp)[Trait.CHAOS.ordinal], 0.001f)
        assertTrue(l.atlas.containsKey("cat_belly_up"))
        assertEquals(listOf("cat_belly_up"), l.firstIn(shots.last()))
        assertTrue(l.firstIn(shots.first()).isEmpty())
    }

    @Test fun anomaliesFromTheLog() {
        val urgent = lab(obs = listOf(Obs(specimen = 1, day = day, kind = Ob.SYMPTOM, value = Symptom.NO_PEE.ordinal.toFloat())))
        assertTrue(urgent.anomalies(sp).first().urgent)
        val vomits = lab(obs = (0..2).map { Obs(specimen = 1, day = day - it, kind = Ob.SYMPTOM, value = Symptom.VOMIT.ordinal.toFloat()) })
        assertTrue(vomits.anomalies(sp).any { it is Anomaly.Vomits && it.urgent })
        val notEating = lab(obs = (0..1).map { Obs(specimen = 1, day = day - it, kind = Ob.APPETITE, value = 0f) })
        assertTrue(notEating.anomalies(sp).any { it is Anomaly.NotEating && it.urgent })
        val weight = lab(obs = listOf(Obs(specimen = 1, day = day - 30, kind = Ob.WEIGHT, value = 5f), Obs(specimen = 1, day = day, kind = Ob.WEIGHT, value = 4.4f)))
        assertTrue(weight.anomalies(sp).any { it is Anomaly.Weight && it.percent == -12 })
        assertTrue(lab().anomalies(sp).isEmpty())
    }

    @Test fun protocolsDue() {
        val l = lab(protocols = listOf(
            Protocol(id = 1, specimen = 1, key = "u", title = "A", every = 30, last = day - 40),
            Protocol(id = 2, specimen = 1, key = "u", title = "B", every = 30, last = day - 2),
            Protocol(id = 3, specimen = 1, key = "u", title = "C", every = 30),
        ))
        val d = l.dues(1)
        assertEquals(Due.State.OVERDUE, d.first().state)
        assertEquals(Due.State.UNKNOWN, d.first { it.p.id == 3L }.state)
        assertEquals(50, l.integrity(1))
    }

    @Test fun reportConclusions() {
        val week = weekOf(day)
        val shots = (0..4).map { Shot(id = it + 1L, specimen = 1, day = week + it, tags = "cat_belly_up") }
        val obs = listOf(Obs(specimen = 1, day = week, kind = Ob.WEIGHT, value = 4.2f))
        val r = lab(obs = obs, shots = shots).report(sp, week)
        assertTrue(r.conclusions.any { it is Report.Conclusion.Up && it.t == Trait.SOCIAL })
        assertTrue(r.conclusions.any { it == Report.Conclusion.Stable })
        assertEquals(1, r.photos.count { it.first })
        assertNotNull(r.weight)
    }
}
