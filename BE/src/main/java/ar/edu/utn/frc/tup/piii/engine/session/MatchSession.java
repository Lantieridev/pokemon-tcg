package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.exception.IllegalMatchStateTransitionException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runtime state of a single match between two players.
 * Pure POJO — zero Spring imports. Manages the WAITING → ACTIVE → FINISHED state machine.
 *
 * <p>Thread-safety: critical sections (state transitions, timeout management) are guarded
 * by the internal {@link ReentrantLock} exposed via {@link #getLock()}. Callers that need
 * atomic compound operations must acquire this lock directly.</p>
 */
public final class MatchSession {

    private final String matchId;
    private final List<String> playerIds;
    private final MatchBoard board;
    private final ReentrantLock lock;
    private MatchSessionState state;
    private ScheduledFuture<?> playerATimeout;
    private ScheduledFuture<?> playerBTimeout;

    /**
     * Constructs a MatchSession in the WAITING state.
     *
     * @param matchId   unique identifier for this match (never null)
     * @param playerIds list of player identifiers — exactly 2 (never null)
     * @param board     the match board holding both players' runtime state (never null)
     */
    public MatchSession(final String matchId,
                        final List<String> playerIds,
                        final MatchBoard board) {
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.playerIds = Objects.requireNonNull(playerIds, "playerIds must not be null");
        this.board = Objects.requireNonNull(board, "board must not be null");
        this.state = MatchSessionState.WAITING;
        this.lock = new ReentrantLock();
    }

    /**
     * Transitions the session from WAITING to ACTIVE.
     *
     * @throws IllegalMatchStateTransitionException if the session is not in WAITING state
     */
    public void start() {
        if (state != MatchSessionState.WAITING) {
            throw new IllegalMatchStateTransitionException(
                    "Cannot start a match that is in state: " + state);
        }
        state = MatchSessionState.ACTIVE;
    }

    /**
     * Transitions the session from ACTIVE to FINISHED.
     *
     * @throws IllegalMatchStateTransitionException if the session is not in ACTIVE state
     */
    public void finish() {
        if (state != MatchSessionState.ACTIVE) {
            throw new IllegalMatchStateTransitionException(
                    "Cannot finish a match that is in state: " + state);
        }
        state = MatchSessionState.FINISHED;
    }

    /**
     * Returns the current state of this match session.
     *
     * @return current state (never null)
     */
    public MatchSessionState getState() {
        return state;
    }

    /**
     * Returns the unique identifier for this match.
     *
     * @return matchId (never null)
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * Returns the player identifiers for this match.
     *
     * @return player IDs (never null, defensive copy)
     */
    public List<String> getPlayerIds() {
        return List.copyOf(playerIds);
    }

    /**
     * Returns the player identifier for player at index 0.
     *
     * @return player A's ID (never null)
     */
    public String getPlayerIdA() {
        return playerIds.get(0);
    }

    /**
     * Returns the player identifier for player at index 1.
     *
     * @return player B's ID (never null)
     */
    public String getPlayerIdB() {
        return playerIds.get(1);
    }

    /**
     * Returns the match board for this session.
     *
     * @return board (never null)
     */
    public MatchBoard getBoard() {
        return board;
    }

    /**
     * Returns the session's reentrant lock for callers that need to execute
     * compound operations atomically.
     *
     * @return the reentrant lock (never null)
     */
    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Stores a pending abandonment timeout future for the given player.
     * The caller is responsible for lock discipline.
     *
     * @param playerId the player whose disconnect timer is being set (never null)
     * @param future   the scheduled task to cancel on reconnect (never null)
     */
    public void setDisconnectTimeout(final String playerId, final ScheduledFuture<?> future) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(future, "future must not be null");
        if (playerId.equals(getPlayerIdA())) {
            playerATimeout = future;
        } else {
            playerBTimeout = future;
        }
    }

    /**
     * Returns the pending abandonment future for the given player, or null if none.
     *
     * @param playerId the player ID to query (never null)
     * @return the scheduled future, or null
     */
    public ScheduledFuture<?> getTimeoutFuture(final String playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.equals(getPlayerIdA())) {
            return playerATimeout;
        }
        return playerBTimeout;
    }

    /**
     * Clears the stored timeout future for the given player (sets it to null).
     * Must be called inside the session lock.
     *
     * @param playerId the player whose future to clear (never null)
     */
    public void clearTimeoutFuture(final String playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.equals(getPlayerIdA())) {
            playerATimeout = null;
        } else {
            playerBTimeout = null;
        }
    }

    /**
     * Atomically cancels the abandonment timeout for the given player inside the session lock.
     * Safe to call even if no timeout is pending (no-op in that case).
     *
     * @param playerId the reconnecting player's ID (never null)
     */
    public void cancelDisconnectTimeout(final String playerId) {
        lock.lock();
        try {
            final ScheduledFuture<?> future = getTimeoutFuture(playerId);
            if (future != null) {
                future.cancel(false);
                clearTimeoutFuture(playerId);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the zero-based index of the given player within this session.
     *
     * @param playerId the player to find (never null)
     * @return 0 or 1
     * @throws IllegalArgumentException if playerId is not a participant of this session
     */
    public int indexOf(final String playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        final int index = playerIds.indexOf(playerId);
        if (index < 0) {
            throw new IllegalArgumentException("Player '" + playerId + "' is not part of match " + matchId);
        }
        return index;
    }
}
