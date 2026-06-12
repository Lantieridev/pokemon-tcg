package ar.edu.utn.frc.tup.piii.dtos;

/**
 * Request body for all lobby join operations (public queue and private rooms).
 *
 * @param deckId the deck the player wants to use for the match
 */
public record LobbyJoinRequestDTO(Long deckId, Boolean isRanked) {
}
