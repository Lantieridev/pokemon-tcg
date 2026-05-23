package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Identifies the runtime effect of a Pokémon Tool card.
 * Used by the damage pipeline to apply passive modifiers without coupling
 * the pipeline to concrete card names. FR-003.
 */
public enum PokemonToolEffectId {

    /**
     * Muscle Band (xy1-121) — the attacks of the attached Pokémon do 20 more damage
     * to the opponent's Active Pokémon (applied before Weakness and Resistance).
     */
    MUSCLE_BAND,

    /**
     * Hard Charm (xy1-119) — any damage done to the attached Pokémon by an opponent's
     * attack is reduced by 20 (applied after Weakness and Resistance).
     */
    HARD_CHARM,

    /** No effect — default for tools whose runtime logic is not yet implemented. */
    NONE
}
