package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.exception.IllegalMatchStateTransitionException;

import java.util.List;
import java.util.Objects;

/**
 * Runtime state of a single match between two players.
 * Pure POJO — zero Spring imports. Manages the WAITING → ACTIVE → FINISHED state machine.
 */
public final class MatchSession {

    private final String matchId;
    private final List<String> playerIds;
    private final MatchBoard board;
    private MatchSessionState state;

    /**
     * Constructs a MatchSession in the WAITING state.
     *
     * @param matchId   unique identifier for this match (never null)
     * @param playerIds list of player identifiers (never null)
     * @param board     the match board holding both players' runtime state (never null)
     */
    public MatchSession(final String matchId,
                        final List<String> playerIds,
                        final MatchBoard board) {
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.playerIds = Objects.requireNonNull(playerIds, "playerIds must not be null");
        this.board = Objects.requireNonNull(board, "board must not be null");
        this.state = MatchSessionState.WAITING;
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
     * @return player IDs (never null)
     */
    public List<String> getPlayerIds() {
        return List.copyOf(playerIds);
    }

    /**
     * Returns the match board for this session.
     *
     * @return board (never null)
     */
    public MatchBoard getBoard() {
        return board;
    }
}
