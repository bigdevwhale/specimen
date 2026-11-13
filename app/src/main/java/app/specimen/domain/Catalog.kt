package app.specimen.domain

import android.content.Context
import androidx.compose.ui.graphics.Color

/*
 * The bureau's reference books, read once from assets: home planets and their subspecies (breeds), known health
 * risks, life-support protocols, the behaviour atlas and the xenology course. Every text comes in English and
 * Russian; [ru] picks one when shown.
 */

/** The six home worlds. [key] matches the TSV files. */
enum class Planet(val key: String, val color: Color) {
    FELIS("felis", Color(0xFFD08A3E)),
    CANIS("canis", Color(0xFF8C6A4F)),
    PSITTACUS("psittacus", Color(0xFF4FA36B)),
    GLIRES("glires", Color(0xFFC7A27A)),
    MUSTELA("mustela", Color(0xFF9A7B9E)),
    SQUAMATA("squamata", Color(0xFF6E9A4A));

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: FELIS
    }
}

/** A text in both languages. */
data class Tx(val en: String, val ru: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en
}

/** The eight measured traits, in the order the radar draws them. */
enum class Trait(val key: String) {
    SOCIAL("social"), CURIOUS("curious"), TERRITORY("territory"), VOICE("voice"),
    ENERGY("energy"), APPETITE("appetite"), INDEPENDENCE("independence"), CHAOS("chaos");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

data class Breed(
    val key: String,
    val planet: Planet,
    val name: Tx,
    /** 1–10 per [Trait], in its order. */
    val traits: List<Int>,
    val wMin: Float,
    val wMax: Float,
    val life: String,
    val care: List<String>,
    val risks: List<String>,
    val lore: Tx,
) {
    val local get() = key.endsWith("_local")
}

data class Risk(val key: String, val name: Tx, val watch: Tx)

data class ProtoDef(
    val key: String,
    val planets: Set<String>,
    val default: Boolean,
    val every: Int,
    val name: Tx,
    val why: Tx,
) {
    fun forPlanet(p: Planet) = "*" in planets || p.key in planets
}

data class Behaviour(
    val key: String,
    val planet: Planet,
    val trait: Trait?,
    val delta: Int,
    /** 1 common, 2 uncommon, 3 rare. */
    val rarity: Int,
    val name: Tx,
    val caption: Tx,
    val meaning: Tx,
    val hint: Tx,
    /** Its number in the planet's atlas, from 1. */
    val n: Int,
)

data class Lesson(
    val key: String,
    /** null for the general course everyone takes. */
    val planet: Planet?,
    val n: Int,
    val title: Tx,
    val body: Tx,
    val task: Tx,
    val question: Tx,
    val answersEn: List<String>,
    val answersRu: List<String>,
    val ok: Int,
) {
    fun answers(ru: Boolean) = if (ru) answersRu else answersEn
}

class Catalog(val breeds: List<Breed>, val risks: List<Risk>, val protocols: List<ProtoDef>, val behaviours: List<Behaviour>, val lessons: List<Lesson>) {
    private val breedBy = breeds.associateBy { it.key }
    private val riskBy = risks.associateBy { it.key }
    private val protoBy = protocols.associateBy { it.key }
    private val behaviourBy = behaviours.associateBy { it.key }

    fun breed(key: String): Breed = breedBy[key] ?: breedBy["felis_local"] ?: breeds.first()
    fun breedOrNull(key: String) = breedBy[key]
    fun risk(key: String) = riskBy[key]
    fun proto(key: String) = protoBy[key]
    fun behaviour(key: String) = behaviourBy[key]
    fun breedsOf(p: Planet) = breeds.filter { it.planet == p }.sortedWith(compareBy({ !it.local }, { it.name.en }))
    fun behavioursOf(p: Planet) = behaviours.filter { it.planet == p }
    fun lessonsOf(p: Planet?) = lessons.filter { it.planet == p }.sortedBy { it.n }

    /** The protocols a new specimen of [b] starts with: its planet's defaults and what its breed needs. */
    fun startProtocols(b: Breed): List<ProtoDef> =
        (protocols.filter { it.default && it.forPlanet(b.planet) } + b.care.mapNotNull { protoBy[it] }).distinctBy { it.key }

    companion object {
        @Volatile private var loaded: Catalog? = null

        fun get(ctx: Context): Catalog = loaded ?: synchronized(this) {
            loaded ?: read(ctx.applicationContext).also { loaded = it }
        }

        private fun rows(ctx: Context, file: String): List<Map<String, String>> = runCatching {
            ctx.assets.open(file).bufferedReader(Charsets.UTF_8).use { r ->
                val lines = r.readLines().filter { it.isNotBlank() }
                val head = lines.first().split('\t').map { it.trim() }
                lines.drop(1).map { line ->
                    val cells = line.split('\t')
                    head.mapIndexed { i, h -> h to (cells.getOrNull(i)?.trim() ?: "") }.toMap()
                }
            }
        }.getOrDefault(emptyList())

        private fun list(s: String?) = s.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        private fun tx(r: Map<String, String>, k: String) = Tx(r["${k}_en"].orEmpty(), r["${k}_ru"].orEmpty().ifEmpty { r["${k}_en"].orEmpty() })

        private fun read(ctx: Context): Catalog {
            val breeds = rows(ctx, "breeds.tsv").map { r ->
                Breed(
                    key = r["key"].orEmpty(), planet = Planet.of(r["planet"].orEmpty()),
                    name = Tx(r["en"].orEmpty(), r["ru"].orEmpty()),
                    traits = Trait.entries.map { (r[it.key]?.toIntOrNull() ?: 5).coerceIn(1, 10) },
                    wMin = r["wmin"]?.toFloatOrNull() ?: 0f, wMax = r["wmax"]?.toFloatOrNull() ?: 0f,
                    life = r["life"].orEmpty(), care = list(r["care"]), risks = list(r["risks"]),
                    lore = tx(r, "lore"),
                )
            }
            val risks = rows(ctx, "risks.tsv").map { r -> Risk(r["key"].orEmpty(), Tx(r["en"].orEmpty(), r["ru"].orEmpty()), tx(r, "watch")) }
            val protos = rows(ctx, "protocols.tsv").map { r ->
                ProtoDef(r["key"].orEmpty(), list(r["planets"]).toSet(), r["default"] == "1", r["every"]?.toIntOrNull() ?: 0,
                    Tx(r["en"].orEmpty(), r["ru"].orEmpty()), tx(r, "why"))
            }
            val counter = mutableMapOf<Planet, Int>()
            val behaviours = rows(ctx, "behaviours.tsv").map { r ->
                val p = Planet.of(r["planet"].orEmpty())
                val n = (counter[p] ?: 0) + 1
                counter[p] = n
                Behaviour(r["key"].orEmpty(), p, Trait.of(r["trait"].orEmpty()), r["delta"]?.toIntOrNull() ?: 0, r["rarity"]?.toIntOrNull() ?: 1,
                    Tx(r["en"].orEmpty(), r["ru"].orEmpty()), tx(r, "caption"), tx(r, "meaning"), tx(r, "hint"), n)
            }
            val lessons = rows(ctx, "lessons.tsv").map { r ->
                Lesson(r["key"].orEmpty(), r["planet"].takeIf { it != "*" }?.let { Planet.of(it) }, r["n"]?.toIntOrNull() ?: 0,
                    tx(r, "title"), tx(r, "body"), tx(r, "task"), tx(r, "q"),
                    r["a_en"].orEmpty().split('|').map { it.trim() }, r["a_ru"].orEmpty().split('|').map { it.trim() },
                    r["ok"]?.toIntOrNull() ?: 0)
            }
            return Catalog(breeds, risks, protos, behaviours, lessons)
        }
    }
}
