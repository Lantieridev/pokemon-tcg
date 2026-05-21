package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.exception.IllegalMatchStateTransitionException;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runtime state of a single match between two players.
 * Pure POJO — zero Spring imports. Manages the WAITING → SETUP → ACTIVE → FINISHED state machine.
 *
 * <p>Thread-safety: critical sections (state transitions, timeout management) are guarded
 * by the internal {@link ReentrantLock} exposed via {@link #getLock()}. Callers that need
 * atomic compound operations must acquire this lock directly.</p>
 */
public final class MatchSession {

    private static final int UNSET_PLAYER_INDEX = -1;

    private final String matchId;
    private final List<String> playerIds;
    private final MatchBoard board;
    private final ReentrantLock lock;
    private final List<PlayerRuntime> playerRuntimes;
    private MatchSessionState state;
    private ScheduledFuture<?> playerATimeout;
    private ScheduledFuture<?> playerBTimeout;
    private int activePlayerIndex = UNSET_PLAYER_INDEX;
    private KnockoutHandler knockoutHandler = (knocked, prizes) -> { };

    /**
     * Constructs a MatchSession in the WAITING state.
     *
     * @param matchId        unique identifier for this match (never null)
     * @param playerIds      list of player identifiers — exactly 2 (never null)
     * @param board          the match board holding both players' state (never null)
     * @param playerRuntimes live mutable state per player; null for legacy callers
     */
    public MatchSession(final String matchId,
                        final List<String> playerIds,
                        final MatchBoard board,
                        final List<PlayerRuntime> playerRuntimes) {
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.playerIds = Objects.requireNonNull(playerIds, "playerIds must not be null");
        this.board = Objects.requireNonNull(board, "board must not be null");
        this.playerRuntimes = playerRuntimes != null ? List.copyOf(playerRuntimes) : null;
        this.state = MatchSessionState.WAITING;
        this.lock = new ReentrantLock();
    }

    /**
     * Constructs a MatchSession without player runtimes (backward-compatible).
     *
     * @param matchId   unique identifier for this match (never null)
     * @param playerIds list of player identifiers — exactly 2 (never null)
     * @param board     the match board holding both players' runtime state (never null)
     */
    public MatchSession(final String matchId,
                        final List<String> playerIds,
                        final MatchBoard board) {
        this(matchId, playerIds, board, null);
    }

    /**
     * Transitions the session from WAITING to SETUP.
     *
     * @throws IllegalMatchStateTransitionException if the session is not in WAITING state
     */
    public void setup() {
        if (state != MatchSessionState.WAITING) {
            throw new IllegalMatchStateTransitionException(
                    "Cannot enter setup for a match that is in state: " + state);
        }
        state = MatchSessionState.SETUP;
    }

    /**
     * Transitions the session from SETUP to ACTIVE.
     *
     * @throws IllegalMatchStateTransitionException if the session is not in SETUP state
     */
    public void start() {
        if (state != MatchSessionState.SETUP) {
            throw new IllegalMatchStateTransitionException(
                    "Cannot start a match that is in state: " + state + " (must be in SETUP first)");
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
     * Returns the live runtime state for the specified player.
     *
     * @param playerIndex 0 or 1
     * @return the player's runtime (never null)
     * @throws IllegalStateException if player runtimes were not provided at construction
     */
    public PlayerRuntime getPlayerRuntime(final int playerIndex) {
        if (playerRuntimes == null) {
            throw new IllegalStateException("PlayerRuntime not initialized for this session");
        }
        return playerRuntimes.get(playerIndex);
    }

    /**
     * Returns the zero-based index of the player whose turn is currently being processed.
     * Returns {@code -1} if not yet set.
     *
     * @return active player index, or -1
     */
    public int getActivePlayerIndex() {
        return activePlayerIndex;
    }

    /**
     * Sets the zero-based index of the player whose action is being applied.
     * Must be called before {@link ar.edu.utn.frc.tup.piii.services.GameFacade#apply}.
     *
     * @param playerIndex 0 or 1
     */
    public void setActivePlayerIndex(final int playerIndex) {
        this.activePlayerIndex = playerIndex;
    }

    /**
     * Returns the knockout handler used by the attack pipeline. Defaults to a no-op.
     *
     * @return knockout handler (never null)
     */
    public KnockoutHandler getKnockoutHandler() {
        return knockoutHandler;
    }

    /**
     * Overrides the knockout handler (e.g. with a real VictoryConditionChecker).
     *
     * @param handler the handler to use (never null)
     */
    public void setKnockoutHandler(final KnockoutHandler handler) {
        this.knockoutHandler = Objects.requireNonNull(handler, "handler must not be null");
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
