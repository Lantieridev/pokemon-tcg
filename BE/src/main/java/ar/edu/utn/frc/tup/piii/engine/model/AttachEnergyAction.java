package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player attaching an energy card to a Pokémon. FR-004.
 *
 * @param energyType the type of energy being attached
 */
public record AttachEnergyAction(PokemonType energyType) implements Action {
}
