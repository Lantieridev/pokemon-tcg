package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player evolving a Pokémon in play. FR-004.
 *
 * @param target    the BattlePokemonState already in play that will be evolved
 * @param evolutionCardId the cardId of the new Pokémon card (from hand) being placed on top of target
 */
public record EvolveAction(BattlePokemonState target, PokemonCard evolution) implements Action {
}
