package ar.edu.utn.frc.tup.piii.dtos;

import java.util.List;

/**
 * Full game state response sent to one specific player over WebSocket.
 * The {@code self} field contains the full hand; the {@code opponent} field
 * contains only the hand SIZE (war-fog security rule).
 *
 * @param matchId           unique identifier of the match
 * @param version           monotonically increasing state version
 * @param activePlayerIndex index (0 or 1) of the player whose turn it currently is
 * @param currentPhase      human-readable current phase name
 * @param self              full view of the receiving player's state
 * @param opponent          restricted view of the opponent (no card IDs exposed)
 */
    public record GameStateResponseDTO(
        String matchId,
        long version,
        int turnNumber,
        int activePlayerIndex,
        String currentPhase,
        PendingSelectionRequestDTO pendingSelectionRequest,
        PlayerView self,
        OpponentView opponent,
        String activeStadiumCardId,
        String winnerId,
        String victoryReason,
        String mvpCardId,
        Integer mvpCardDamage) {

    public GameStateResponseDTO(
        String matchId,
        long version,
        int turnNumber,
        int activePlayerIndex,
        String currentPhase,
        PendingSelectionRequestDTO pendingSelectionRequest,
        PlayerView self,
        OpponentView opponent,
        String activeStadiumCardId) {
        this(matchId, version, turnNumber, activePlayerIndex, currentPhase, pendingSelectionRequest, self, opponent, activeStadiumCardId, null, null, null, null);
    }

    /**
     * Full view of the receiving player's state, including the complete hand.
     *
     * @param playerId   the player's identifier
     * @param active     the active Pokémon DTO, or null if the slot is empty
     * @param bench      list of benched Pokémon DTOs
     * @param hand       list of card IDs in this player's hand (full, only for self)
     * @param deckSize   number of cards remaining in the deck
     * @param prizeCount number of prize cards still face-down
     */
    public record PlayerView(
            String playerId,
            BattlePokemonDTO active,
            List<BattlePokemonDTO> bench,
            List<String> hand,
            int deckSize,
            int prizeCount) {
    }

    /**
     * Restricted view of the opponent's state — war-fog enforced.
     * The hand is intentionally reduced to a count to prevent card spying.
     *
     * @param playerId   the opponent's identifier
     * @param active     the active Pokémon DTO, or null if the slot is empty
     * @param bench      list of benched Pokémon DTOs
     * @param handSize   number of cards in the opponent's hand (count only — NO card IDs)
     * @param deckSize   number of cards remaining in the opponent's deck
     * @param prizeCount number of prize cards still face-down for the opponent
     */
    public record OpponentView(
            String playerId,
            BattlePokemonDTO active,
            List<BattlePokemonDTO> bench,
            int handSize,
            int deckSize,
            int prizeCount) {
    }
}
