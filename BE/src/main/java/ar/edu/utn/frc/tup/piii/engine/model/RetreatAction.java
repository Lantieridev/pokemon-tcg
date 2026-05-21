package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player retreating their active Pokémon. FR-004.
 *
 * @param active            the BattlePokemonState of the currently active Pokémon that will retreat
 * @param replacementIndex  zero-based bench index of the Pokémon to promote as the new active
 */
public record RetreatAction(BattlePokemonState active, int replacementIndex) implements Action {

    /**
     * Backward-compatible constructor that defaults the replacement to bench slot 0.
     *
     * @param active the Pokémon retreating
     */
    public RetreatAction(final BattlePokemonState active) {
        this(active, 0);
    }
}
