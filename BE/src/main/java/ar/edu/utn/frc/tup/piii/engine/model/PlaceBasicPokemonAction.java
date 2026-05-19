package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player placing a Basic Pokémon onto their bench
 * during the Main Phase. FR-004.
 *
 * @param cardId the identifier of the Basic Pokémon card to place
 */
public record PlaceBasicPokemonAction(String cardId) implements Action {
}
