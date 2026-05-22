package ar.edu.utn.frc.tup.piii.dtos;

/**
 * Request body for creating a new match.
 *
 * @param playerAId identifier of player A (never null)
 * @param playerBId identifier of player B (never null)
 * @param deckAId   ID of player A's saved deck (resolved via CardResolutionService)
 * @param deckBId   ID of player B's saved deck (resolved via CardResolutionService)
 */
public record CreateMatchRequestDTO(
        String playerAId,
        String playerBId,
        Long deckAId,
        Long deckBId) {
}
