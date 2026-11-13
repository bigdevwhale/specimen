package app.specimen.data

import android.graphics.BitmapFactory
import app.specimen.AppContainer
import app.specimen.domain.Mood
import app.specimen.domain.Outcome
import app.specimen.domain.Symptom
import app.specimen.domain.TrialKind
import app.specimen.domain.today
import kotlin.random.Random

/**
 * A lived-in station for the demo build (`-Pdemo`) and the screen renders: a British cat five weeks into
 * observation and a young corgi, with moods, weights, experiments, protocols and field shots. The shots come
 * from assets/demo (only packed into demo and render builds); without them the station simply has no photos.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.dao.specimensNow().isNotEmpty()) return
        val d = today()
        val ru = c.app.resources.configuration.locales[0].language == "ru"
        val rnd = Random(7)
        val cat = c.catalog

        val musya = c.addSpecimen(
            Specimen(num = 0, name = if (ru) "Муся" else "Mousse", planet = "felis", breed = "british_shorthair", sex = 2, born = d - 365 * 3 - 40, arrived = d - 365 * 2 - 300, neutered = true),
            cat.startProtocols(cat.breed("british_shorthair")),
        )
        val bublik = c.addSpecimen(
            Specimen(num = 0, name = if (ru) "Бублик" else "Bagel", planet = "canis", breed = "corgi_pembroke", sex = 1, born = d - 540, arrived = d - 480),
            cat.startProtocols(cat.breed("corgi_pembroke")),
        )

        // Protocols: most done on time, a couple late, one unknown.
        val lasts = mapOf("vaccine_cat" to 300, "vaccine_dog" to 200, "rabies" to 330, "deworm" to 70, "fleas_ticks" to 33, "claws" to 12, "brush_weekly" to 3,
            "teeth" to 9, "ears" to 20, "weigh" to 6, "litter" to 0, "litter_change" to 8, "vet_annual" to 290, "paw_check" to 1, "undercoat" to 10)
        c.dao.protocolsNow().forEach { p ->
            val ago = lasts[p.key] ?: return@forEach
            if (p.specimen == bublik && p.key == "teeth") return@forEach
            val day = d - ago
            c.dao.save(p.copy(last = day))
            c.dao.add(Done(protocol = p.id, specimen = p.specimen, day = day))
            if (p.every in 1..14) c.dao.add(Done(protocol = p.id, specimen = p.specimen, day = day - p.every))
        }

        // Five weeks of the log.
        for (back in 34 downTo 0) {
            val day = d - back
            if (rnd.nextInt(10) < 7) c.dao.add(Obs(specimen = musya, day = day, at = 9 * 60 + rnd.nextInt(600), kind = Ob.MOOD,
                value = listOf(Mood.CALM, Mood.AFFECTIONATE, Mood.AFFECTIONATE, Mood.CURIOUS, Mood.PLAYFUL, Mood.CALM)[rnd.nextInt(6)].ordinal.toFloat()))
            if (rnd.nextInt(10) < 6) c.dao.add(Obs(specimen = bublik, day = day, at = 8 * 60 + rnd.nextInt(700), kind = Ob.MOOD,
                value = listOf(Mood.PLAYFUL, Mood.PLAYFUL, Mood.AFFECTIONATE, Mood.CURIOUS)[rnd.nextInt(4)].ordinal.toFloat()))
            if (rnd.nextInt(10) < 5) c.dao.add(Obs(specimen = musya, day = day, at = 19 * 60, kind = Ob.APPETITE, value = if (back in 12..13) 1f else 2f + rnd.nextInt(2)))
            if (back % 7 == 3) {
                c.dao.add(Obs(specimen = musya, day = day, at = 20 * 60, kind = Ob.WEIGHT, value = 4.6f - (34 - back) * 0.008f + rnd.nextFloat() * 0.05f))
                c.dao.add(Obs(specimen = bublik, day = day, at = 20 * 60, kind = Ob.WEIGHT, value = 11.2f + (34 - back) * 0.02f))
            }
        }
        c.dao.add(Obs(specimen = musya, day = d - 12, at = 8 * 60, kind = Ob.SYMPTOM, value = Symptom.VOMIT.ordinal.toFloat()))
        c.dao.add(Obs(specimen = musya, day = d - 20, kind = Ob.CONDITION, value = 6f))
        c.dao.add(Obs(specimen = musya, day = d - 2, at = 22 * 60, kind = Ob.NOTE,
            note = if (ru) "Сидела у окна полчаса и «стрекотала» на голубей." else "Sat at the window for half an hour chattering at the pigeons."))
        c.dao.add(Obs(specimen = bublik, day = d - 1, at = 21 * 60, kind = Ob.NOTE,
            note = if (ru) "Принёс тапок и положил в миску. Возможно, обмен." else "Brought a slipper and put it in the bowl. Possibly a trade."))

        // Experiments.
        fun trial(sp: Long, k: TrialKind, ru1: String, en1: String, start: Int, days: Int, out: Outcome?, guess: Outcome = Outcome.ACCEPTED, reactions: String = "") =
            Trial(specimen = sp, kind = k.ordinal, subject = if (ru) ru1 else en1, guess = guess.ordinal, start = d - start, days = days,
                outcome = out?.ordinal ?: -1, reactions = reactions, ended = if (out != null) d - start + days else 0)
        c.dao.add(trial(musya, TrialKind.FOOD, "Влажный корм с индейкой", "Wet food with turkey", 30, 7, Outcome.DELIGHT, reactions = "EAGER,PURR"))
        c.dao.add(trial(musya, TrialKind.FOOD, "Сухой корм «для стерилизованных»", "Dry food for neutered cats", 16, 5, Outcome.REFUSED, reactions = "NO_APPETITE"))
        c.dao.add(trial(musya, TrialKind.VET, "Плановый осмотр", "Routine checkup", 9, 1, Outcome.WARY, guess = Outcome.WARY, reactions = "HIDING,VOCAL"))
        c.dao.add(trial(musya, TrialKind.TOY, "Удочка с пёрышками", "Feather wand", 2, 3, null, guess = Outcome.DELIGHT))
        c.dao.add(trial(bublik, TrialKind.MED, "Таблетка от глистов", "Deworming tablet", 70, 2, Outcome.ACCEPTED, reactions = "SLEEPY"))
        c.dao.add(trial(bublik, TrialKind.NOISE, "Салют во дворе", "Fireworks outside", 6, 1, Outcome.WARY, guess = Outcome.INDIFFERENT, reactions = "HIDING"))
        c.dao.add(trial(bublik, TrialKind.TREAT, "Сушёное лёгкое", "Dried lung treats", 4, 3, null, guess = Outcome.DELIGHT))

        // Field shots from the demo pictures, tagged.
        val shots = listOf(
            Triple(musya, "cat1", listOf("cat_loaf") to 27), Triple(musya, "cat2", listOf("cat_belly_up") to 20),
            Triple(musya, "cat3", listOf("cat_window_watch", "cat_chatter") to 13), Triple(musya, "cat4", listOf("cat_box_sleep") to 6),
            Triple(musya, "cat5", listOf("cat_slow_blink") to 4), Triple(musya, "cat6", listOf("cat_sunbath", "cat_stretch") to 2),
            Triple(musya, "cat7", listOf("cat_kneading") to 1),
            Triple(bublik, "dog1", listOf("dog_play_bow") to 18), Triple(bublik, "dog2", listOf("dog_head_tilt") to 5), Triple(bublik, "dog3", listOf("dog_sploot") to 3),
        )
        val known = cat.behaviours.map { it.key }.toSet()
        for ((sp, file, tagsAgo) in shots) {
            val bmp = runCatching { c.app.assets.open("demo/$file.jpg").use { BitmapFactory.decodeStream(it) } }.getOrNull() ?: continue
            val (tags, ago) = tagsAgo
            val id = c.dao.add(Shot(specimen = sp, day = d - ago, tags = tags.filter { it in known }.joinToString(",")))
            c.photos.keep(id, bmp)
        }
        c.dao.specimensNow().forEach { s -> c.dao.shotsNow().lastOrNull { it.specimen == s.id }?.let { c.dao.save(s.copy(portrait = it.id)) } }

        c.settings.update { it.copy(onboarded = true, researcher = if (ru) "М. Котова" else "M. Hartley", lessons = setOf("gen_observation", "cat_body", "cat_ears_eyes"), tasks = setOf("cat_body")) }
    }
}
