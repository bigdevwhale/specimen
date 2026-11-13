package app.specimen.domain

/**
 * The bureau's ethics committee reads every experiment before it starts. It turns down pranks that frighten the
 * specimen, punishment and hunger, and anything poisonous to its planet – with the reason, stamped «REFUSED».
 * Matching is by word beginnings, so «огурцом» is caught by «огур»; a stem ending in «=» must be the whole word.
 */
object Ethics {
    enum class Why { SCARE, PUNISH, HUNGER, TOXIC, HUMAN_MEDS }

    data class Refusal(val why: Why, val word: String)

    private class Rule(val why: Why, val planets: Set<Planet>?, val stems: List<String>)

    private val all: Set<Planet>? = null
    private val furry = setOf(Planet.FELIS, Planet.CANIS, Planet.MUSTELA)

    private val rules = listOf(
        Rule(Why.SCARE, all, listOf("огур", "cucumber", "напуг", "испуг", "пугат", "пугн", "scare", "prank", "пранк", "startle", "spook", "розыгрыш")),
        Rule(Why.PUNISH, all, listOf("наказ", "punish", "ударит", "ударю", "шлёп", "шлеп", "smack", "spank", "hit=", "пульверизатор", "брызгалк", "spray=", "water pistol")),
        Rule(Why.HUNGER, all, listOf("голод", "starv", "не кормит", "без еды", "без воды", "no food", "no water", "withhold")),
        Rule(Why.TOXIC, all, listOf("шоколад", "chocolate", "cocoa", "какао", "алкогол", "alcohol", "пиво", "beer", "вино=", "wine", "водк", "vodka",
            "кофе", "coffee", "caffeine", "кофеин", "энергетик", "ксилит", "xylitol", "лук=", "луком", "onion", "чеснок", "чесноч", "garlic")),
        Rule(Why.TOXIC, setOf(Planet.CANIS, Planet.FELIS, Planet.MUSTELA), listOf("виноград", "grape", "изюм", "raisin", "макадами", "macadamia")),
        Rule(Why.TOXIC, setOf(Planet.FELIS), listOf("лили", "lily", "lilies", "перметрин", "permethrin", "собачьи капли", "dog flea")),
        Rule(Why.TOXIC, setOf(Planet.PSITTACUS, Planet.GLIRES, Planet.SQUAMATA), listOf("авокадо", "avocado")),
        Rule(Why.TOXIC, setOf(Planet.PSITTACUS), listOf("тефлон", "teflon", "солён", "солен", "salty", "salted")),
        Rule(Why.TOXIC, setOf(Planet.SQUAMATA), listOf("светлячк", "firefl", "lightning bug")),
        Rule(Why.TOXIC, setOf(Planet.CANIS), listOf("варёные кости", "вареные кости", "cooked bone", "трубчат")),
        Rule(Why.HUMAN_MEDS, furry, listOf("парацетамол", "paracetamol", "acetaminophen", "tylenol", "ибупрофен", "ibuprofen", "нурофен", "nurofen", "аспирин", "aspirin", "анальгин", "диклофенак", "diclofenac")),
    )

    fun check(planet: Planet, text: String): Refusal? {
        val low = text.lowercase().replace('ё', 'е').let { " " + it.replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex(" +"), " ") + " " }
        for (r in rules) {
            if (r.planets != null && planet !in r.planets) continue
            for (stem in r.stems) {
                val exact = stem.endsWith("=")
                val s = stem.removeSuffix("=").lowercase().replace('ё', 'е')
                val hit = if (exact) " $s " in low else " $s" in low
                if (hit) return Refusal(r.why, s)
            }
        }
        return null
    }
}
