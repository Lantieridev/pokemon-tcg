package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player evolving a Pokémon in play. FR-004.
 *
 * @param target the BattlePokemonState being evolved
 */
public record EvolveAction(BattlePokemonState target) implements Action {
}
