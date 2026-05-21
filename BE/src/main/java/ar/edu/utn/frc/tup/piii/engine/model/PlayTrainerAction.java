package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player playing a Trainer card. FR-004.
 *
 * <p>For {@link TrainerType#POKEMON_TOOL} the {@code target} field identifies
 * which Pokémon the tool is being attached to; it must not be {@code null} in
 * that case. For all other trainer types {@code target} is {@code null}.</p>
 *
 * @param trainerType the category of the Trainer card being played
 * @param target      the Pokémon receiving the tool (non-null for POKEMON_TOOL, null otherwise)
 * @param cardId      the ID of the trainer card being played from hand (may be null for legacy callers)
 */
public record PlayTrainerAction(TrainerType trainerType, BattlePokemonState target, String cardId) implements Action {

    /**
     * Convenience constructor for Trainer cards that do not target a specific Pokémon.
     *
     * @param trainerType the category of the Trainer card being played
     * @param target      the Pokémon receiving the tool (non-null for POKEMON_TOOL, null otherwise)
     */
    public PlayTrainerAction(final TrainerType trainerType, final BattlePokemonState target) {
        this(trainerType, target, null);
    }

    /**
     * Convenience constructor for Trainer cards with no target and no card ID.
     *
     * @param trainerType the category of the Trainer card being played
     */
    public PlayTrainerAction(final TrainerType trainerType) {
        this(trainerType, null, null);
    }
}
