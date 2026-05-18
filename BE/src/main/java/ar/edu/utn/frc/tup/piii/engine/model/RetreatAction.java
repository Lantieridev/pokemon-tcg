package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player retreating their active Pokémon. FR-004.
 *
 * @param active the BattlePokemonState of the currently active Pokémon that will retreat
 */
public record RetreatAction(BattlePokemonState active) implements Action {
}
