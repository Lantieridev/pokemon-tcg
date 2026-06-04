package ar.edu.utn.frc.tup.piii.dtos;

/**
 * Response returned by all lobby endpoints.
 *
 * <ul>
 *   <li>{@code status} — {@code "WAITING"} while searching/waiting, {@code "MATCH_READY"} once paired.</li>
 *   <li>{@code matchId} — populated only when {@code status == "MATCH_READY"}.</li>
 *   <li>{@code roomCode} — populated only when a private room is created ({@code status == "WAITING"}).</li>
 *   <li>{@code opponentId} — populated when a match is created; the opponent's username.</li>
 * </ul>
 */
public record LobbyResponseDTO(
        String status,
        String matchId,
        String roomCode,
        String opponentId
) {
    /** Convenience factory for a "still waiting" response with a room code. */
    public static LobbyResponseDTO waiting(final String roomCode) {
        return new LobbyResponseDTO("WAITING", null, roomCode, null);
    }

    /** Convenience factory for a "match is ready" response. */
    public static LobbyResponseDTO matchReady(final String matchId, final String opponentId) {
        return new LobbyResponseDTO("MATCH_READY", matchId, null, opponentId);
    }

    /** Convenience factory for "joined the queue, still waiting". */
    public static LobbyResponseDTO queued() {
        return new LobbyResponseDTO("WAITING", null, null, null);
    }
}
