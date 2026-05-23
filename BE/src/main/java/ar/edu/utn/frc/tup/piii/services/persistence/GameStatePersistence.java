package ar.edu.utn.frc.tup.piii.services.persistence;

/**
 * Port for persisting game state after each successful action.
 * Implementations may write to a database, a message bus, or a no-op sink.
 */
public interface GameStatePersistence {

    /**
     * Persists the given game state snapshot.
     * Called after every successful action, before broadcasting to clients.
     *
     * @param snapshot the snapshot to persist (never null)
     */
    void save(GameStateSnapshot snapshot);

    /**
     * Persists the match session itself.
     *
     * @param session the match session to save
     */
    default void saveMatch(ar.edu.utn.frc.tup.piii.engine.session.MatchSession session) {}

    /**
     * Appends an action log entry.
     *
     * @param matchId the match identifier
     * @param turnNumber the turn number
     * @param playerId the player identifier who took the action
     * @param actionType the action type identifier
     * @param result the outcome description
     */
    default void logAction(String matchId, int turnNumber, String playerId, String actionType, String result) {}

    /**
     * Declares the winner of a match.
     *
     * @param matchId        the match identifier (never null)
     * @param winnerUsername the username of the winning player (never null)
     * @deprecated Use the unified saveMatch flow with setWinnerId on MatchSession instead.
     */
    @Deprecated
    default void declareWinner(String matchId, String winnerUsername) {}
}

