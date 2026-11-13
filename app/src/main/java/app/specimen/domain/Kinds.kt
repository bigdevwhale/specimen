package app.specimen.domain

/*
 * The fixed vocabularies of the station log. The names are stored in the database (by ordinal or name),
 * so new entries go to the end.
 */

/** A state read from the tail, ears, pose and voice. [trait] is what logging it nudges. */
enum class Mood(val trait: Trait?, val delta: Int) {
    CALM(null, 0),
    AFFECTIONATE(Trait.SOCIAL, 1),
    PLAYFUL(Trait.ENERGY, 1),
    CURIOUS(Trait.CURIOUS, 1),
    WARY(Trait.SOCIAL, -1),
    IRRITATED(Trait.TERRITORY, 1),
    UNWELL(null, 0),
}

/**
 * Something wrong. [urgent] means «to the vet today»; [urgentFor] makes it urgent only on some planets
 * (a cat or a rabbit that stops eating is an emergency, a dog skipping a meal is not); [planets] limits where it is offered.
 */
enum class Symptom(val urgent: Boolean, val urgentFor: Set<Planet> = emptySet(), val planets: Set<Planet>? = null) {
    VOMIT(false),
    NO_FOOD(false, setOf(Planet.FELIS, Planet.GLIRES, Planet.PSITTACUS)),
    LETHARGY(false),
    DIARRHEA(false),
    BLOOD(true),
    NO_PEE(true),
    BREATHING(true),
    SEIZURE(true),
    LIMP(false),
    EYES(false),
    ITCH(false),
    SNEEZE(false),
    THIRST(false),
    BLOAT(true, planets = setOf(Planet.CANIS, Planet.GLIRES)),
    FLUFFED(true, planets = setOf(Planet.PSITTACUS)),
    NO_DROPPINGS(true, planets = setOf(Planet.GLIRES, Planet.PSITTACUS)),
    STUCK_SHED(false, planets = setOf(Planet.SQUAMATA));

    fun urgentOn(p: Planet) = urgent || p in urgentFor
    fun offeredOn(p: Planet) = planets == null || p in planets
}

/** What an experiment is about, with how many days it usually takes to judge. */
enum class TrialKind(val days: Int, val food: Boolean = false) {
    FOOD(7, true),
    TREAT(3, true),
    MED(5),
    VACCINE(2),
    VET(1),
    MOVE(14),
    GUEST(1),
    NEWPET(21),
    TOY(3),
    GROOM(1),
    TRIP(2),
    NOISE(1),
    OTHER(3),
}

/** How it went, from best to worst. */
enum class Outcome {
    DELIGHT, ACCEPTED, INDIFFERENT, WARY, REFUSED, BAD;

    val good get() = this == DELIGHT || this == ACCEPTED
}

/** Things noticed during an experiment. */
enum class Reaction(val bad: Boolean = false) {
    EAGER, EXPLORED, PURR, SLEEPY, HIDING, VOCAL, NO_APPETITE(true), VOMIT(true), LOOSE(true), ITCH(true), SWELLING(true), AGGRESSION(true);

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name }
    }
}
