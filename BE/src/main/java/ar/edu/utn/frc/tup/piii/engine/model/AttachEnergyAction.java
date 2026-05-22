package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player attaching an energy card to a Pokémon. FR-004.
 *
 * @param target the pokemon to attach the energy to
 * @param energyType the type of energy being attached
 */
public record AttachEnergyAction(BattlePokemonState target, PokemonType energyType) implements Action {
}
