package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.exception.FirstTurnAttackException;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidPhaseTransitionException;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidTurnPhaseException;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener;
import ar.edu.utn.frc.tup.piii.engine.model.AttackPhase;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.TurnPhase;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the turn lifecycle for a two-player Pokémon TCG game session.
 *
 * <p>A turn progresses through: DrawPhase → MainPhase → (AttackPhase | BetweenTurnsPhase),
 * then into BetweenTurnsPhase → next player's DrawPhase. Each transition fires PhaseEvents
 * to all registered PhaseListeners. FR-002 through FR-014.
 */
public final class TurnManager {

    private static final int UNSTARTED_PLAYER_INDEX = -1;
    private static final int PLAYER_COUNT = 2;
    private static final int FIRST_PLAYER_INDEX = 0;

    private TurnPhase currentPhase;
    private TurnPhase previousPhase;
    private int activePlayerIndex = UNSTARTED_PLAYER_INDEX;
    private int startingPlayerIndex = FIRST_PLAYER_INDEX;
    private final boolean[] firstTurnCompleted = new boolean[PLAYER_COUNT];
    private final int[] turnCounts = new int[PLAYER_COUNT];
    private final List<PhaseListener> listeners = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Observer API
    // -------------------------------------------------------------------------

    /**
     * Registers a listener to receive PhaseEvents fired by this manager.
     *
     * @param listener the listener to add; must not be null
     * @throws IllegalArgumentException if listener is null
     */
    public void registerListener(final PhaseListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     * No-op if the listener is not currently registered.
     *
     * @param listener the listener to remove
     */
    public void unregisterListener(final PhaseListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the starting player index used for the first-turn attack restriction.
     * Call this before {@link #startTurn(int)} to configure which player cannot
     * attack on their very first turn (as per XY1 coin-flip rules).
     *
     * @param playerIndex 0 or 1
     */
    public void setStartingPlayer(final int playerIndex) {
        this.startingPlayerIndex = playerIndex;
    }

    /**
     * Resets all turn-tracking state so that a new game (e.g. Sudden Death) can begin.
     * Clears the current phase, resets the active player index, and marks both players
     * as not yet having completed their first turn. Must be called before
     * {@link #startTurn(int)} when restarting a match from FINISHED state.
     */
    public void reset() {
        this.currentPhase = null;
        this.activePlayerIndex = UNSTARTED_PLAYER_INDEX;
        this.firstTurnCompleted[0] = false;
        this.firstTurnCompleted[1] = false;
        this.turnCounts[0] = 0;
        this.turnCounts[1] = 0;
    }

    /**
     * Returns the index of the player who goes first.
     * Used by DrawPhaseExecutor to skip the starting player's first draw.
     *
     * @return starting player index (0 or 1)
     */
    public int getStartingPlayerIndex() {
        return startingPlayerIndex;
    }

    // -------------------------------------------------------------------------
    // State getters
    // -------------------------------------------------------------------------

    /**
     * Returns the current turn phase, or {@code null} before the first turn starts.
     *
     * @return current phase
     */
    public TurnPhase currentPhase() {
        return currentPhase;
    }

    /**
     * Returns the zero-based index of the active player,
     * or {@code -1} before the first turn has been started.
     *
     * @return active player index
     */
    public int activePlayerIndex() {
        return activePlayerIndex;
    }

    /**
     * Returns whether the specified player is still in their first turn
     * (i.e., has not yet completed a full turn cycle).
     *
     * @param playerIndex the zero-based player index
     * @return {@code true} while this is still the player's first turn
     */
    public boolean isFirstTurnOfPlayer(final int playerIndex) {
        return !firstTurnCompleted[playerIndex];
    }

    /**
     * Returns the total number of turns started by the given player.
     *
     * @param playerIndex the zero-based player index
     * @return the number of turns
     */
    public int getTurnCount(final int playerIndex) {
        if (playerIndex < 0 || playerIndex >= PLAYER_COUNT) {
            throw new IllegalArgumentException("Invalid player index: " + playerIndex);
        }
        return turnCounts[playerIndex];
    }

    // -------------------------------------------------------------------------
    // Turn transitions
    // -------------------------------------------------------------------------

    /**
     * Begins a new turn for the specified player.
     * Sets the phase to DrawPhase and fires TurnStarted then PhaseEntered.
     *
     * @param playerIndex the zero-based index of the player whose turn is starting
     * @throws InvalidPhaseTransitionException if a turn is already in progress
     * @throws InvalidTurnPhaseException       if playerIndex is out of range [0, PLAYER_COUNT)
     */
    public void startTurn(final int playerIndex) {
        if (currentPhase != null) {
            throw new InvalidPhaseTransitionException(
                    "Cannot start a new turn while a turn is already in progress (phase: "
                            + currentPhase.name() + ")");
        }
        if (playerIndex < 0 || playerIndex >= PLAYER_COUNT) {
            throw new InvalidTurnPhaseException(
                    "Invalid player index: " + playerIndex + ". Must be in [0, " + PLAYER_COUNT + ")");
        }
        activePlayerIndex = playerIndex;
        turnCounts[playerIndex]++;
        currentPhase = new DrawPhase();
        fire(new PhaseEvent.TurnStarted(activePlayerIndex, currentPhase));
        fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
    }

    /**
     * Transitions from DrawPhase to MainPhase.
     * Fires PhaseExited (Draw) then PhaseEntered (Main).
     *
     * @throws InvalidPhaseTransitionException if not currently in DrawPhase or no turn in progress
     */
    public void endDraw() {
        if (currentPhase == null) {
            throw new InvalidPhaseTransitionException("No turn in progress — call startTurn() first");
        }
        switch (currentPhase) {
            case DrawPhase d -> {
                fire(new PhaseEvent.PhaseExited(activePlayerIndex, currentPhase));
                currentPhase = new MainPhase();
                fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
            }
            case MainPhase m -> throw new InvalidPhaseTransitionException(
                    "endDraw() called during MainPhase");
            case AttackPhase a -> throw new InvalidPhaseTransitionException(
                    "endDraw() called during AttackPhase");
            case BetweenTurnsPhase b -> throw new InvalidPhaseTransitionException(
                    "endDraw() called during BetweenTurnsPhase");
            case ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase a -> throw new InvalidPhaseTransitionException(
                    "endDraw() called during ActionResolutionPhase");
        }
    }

    /**
     * Returns the current phase cast to MainPhase.
     *
     * @return the current MainPhase instance
     * @throws InvalidTurnPhaseException if the current phase is not MainPhase or no turn in progress
     */
    public MainPhase requireMainPhase() {
        if (currentPhase == null) {
            throw new InvalidTurnPhaseException("No turn in progress — call startTurn() first");
        }
        if (!(currentPhase instanceof MainPhase main)) {
            throw new InvalidTurnPhaseException(
                    "Expected MainPhase but was: " + phaseName(currentPhase));
        }
        return main;
    }

    /**
     * Interrupts the MainPhase to resolve an interactive action (like selecting cards from the deck).
     *
     * @throws InvalidPhaseTransitionException if not currently in MainPhase
     */
    public void interruptMainPhase() {
        if (currentPhase == null) {
            throw new InvalidPhaseTransitionException("No turn in progress — call startTurn() first");
        }
        if (!(currentPhase instanceof MainPhase)) {
            throw new InvalidPhaseTransitionException("Can only interrupt during MainPhase");
        }
        fire(new PhaseEvent.PhaseExited(activePlayerIndex, currentPhase));
        previousPhase = currentPhase;
        currentPhase = new ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase();
        fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
    }

    /**
     * Resumes the MainPhase after resolving an interactive action.
     *
     * @throws InvalidPhaseTransitionException if not currently in ActionResolutionPhase
     */
    public void resumeMainPhase() {
        if (currentPhase == null) {
            throw new InvalidPhaseTransitionException("No turn in progress");
        }
        if (!(currentPhase instanceof ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase)) {
            throw new InvalidPhaseTransitionException("Can only resume from ActionResolutionPhase");
        }
        fire(new PhaseEvent.PhaseExited(activePlayerIndex, currentPhase));
        currentPhase = previousPhase;
        previousPhase = null;
        fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
    }

    /**
     * Transitions from MainPhase to AttackPhase.
     * Fires PhaseExited (Main) then PhaseEntered (Attack).
     *
     * @throws InvalidPhaseTransitionException if not currently in MainPhase or no turn in progress
     * @throws FirstTurnAttackException        if player 0 attempts to attack on their first turn
     */
    public void declareAttack() {
        if (currentPhase == null) {
            throw new InvalidPhaseTransitionException("No turn in progress — call startTurn() first");
        }
        switch (currentPhase) {
            case MainPhase m -> {
                if (activePlayerIndex == startingPlayerIndex
                        && !firstTurnCompleted[startingPlayerIndex]) {
                    throw new FirstTurnAttackException(
                            "Player " + startingPlayerIndex + " cannot attack on their first turn");
                }
                fire(new PhaseEvent.PhaseExited(activePlayerIndex, currentPhase));
                currentPhase = new AttackPhase();
                fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
            }
            case DrawPhase d -> throw new InvalidPhaseTransitionException(
                    "declareAttack() called during DrawPhase");
            case AttackPhase a -> throw new InvalidPhaseTransitionException(
                    "declareAttack() called during AttackPhase");
            case BetweenTurnsPhase b -> throw new InvalidPhaseTransitionException(
                    "declareAttack() called during BetweenTurnsPhase");
            case ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase a -> throw new InvalidPhaseTransitionException(
                    "declareAttack() called during ActionResolutionPhase");
        }
    }

    /**
     * Transitions from MainPhase to BetweenTurnsPhase (pass without attacking).
     * Fires PhaseExited (Main) then PhaseEntered (BetweenTurns).
     *
     * @throws InvalidPhaseTransitionException if not currently in MainPhase or no turn in progress
     */
    public void passTurn() {
        if (currentPhase == null) {
            throw new InvalidPhaseTransitionException("No turn in progress — call startTurn() first");
        }
        switch (currentPhase) {
            case MainPhase m -> {
                fire(new PhaseEvent.PhaseExited(activePlayerIndex, currentPhase));
                currentPhase = new BetweenTurnsPhase();
                fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
            }
            case DrawPhase d -> throw new InvalidPhaseTransitionException(
                    "passTurn() called during DrawPhase");
            case AttackPhase a -> throw new InvalidPhaseTransitionException(
                    "passTurn() called during AttackPhase");
            case BetweenTurnsPhase b -> throw new InvalidPhaseTransitionException(
                    "passTurn() called during BetweenTurnsPhase");
            case ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase a -> throw new InvalidPhaseTransitionException(
                    "passTurn() called during ActionResolutionPhase");
        }
    }

    /**
     * Transitions from AttackPhase to BetweenTurnsPhase.
     * Fires PhaseExited (Attack) then PhaseEntered (BetweenTurns).
     *
     * @throws InvalidPhaseTransitionException if not currently in AttackPhase or no turn in progress
     */
    public void endAttack() {
        if (currentPhase == null) {
            throw new InvalidPhaseTransitionException("No turn in progress — call startTurn() first");
        }
        switch (currentPhase) {
            case AttackPhase a -> {
                fire(new PhaseEvent.PhaseExited(activePlayerIndex, currentPhase));
                currentPhase = new BetweenTurnsPhase();
                fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
            }
            case DrawPhase d -> throw new InvalidPhaseTransitionException(
                    "endAttack() called during DrawPhase");
            case MainPhase m -> throw new InvalidPhaseTransitionException(
                    "endAttack() called during MainPhase");
            case BetweenTurnsPhase b -> throw new InvalidPhaseTransitionException(
                    "endAttack() called during BetweenTurnsPhase");
            case ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase a -> throw new InvalidPhaseTransitionException(
                    "endAttack() called during ActionResolutionPhase");
        }
    }

    /**
     * Ends the between-turns phase, flips the active player, and starts the next turn.
     *
     * <p>Event order: PhaseExited → TurnEnded (ending player) → TurnStarted (next player)
     * → PhaseEntered (next player's DrawPhase).
     *
     * @throws InvalidPhaseTransitionException if not currently in BetweenTurnsPhase or no turn in progress
     */
    public void endBetweenTurns() {
        if (currentPhase == null) {
            throw new InvalidPhaseTransitionException("No turn in progress — call startTurn() first");
        }
        switch (currentPhase) {
            case BetweenTurnsPhase b -> {
                int endingPlayer = activePlayerIndex;
                TurnPhase endingPhase = currentPhase;

                fire(new PhaseEvent.PhaseExited(endingPlayer, endingPhase));
                fire(new PhaseEvent.TurnEnded(endingPlayer, endingPhase));

                firstTurnCompleted[endingPlayer] = true;
                activePlayerIndex = PLAYER_COUNT - 1 - endingPlayer;
                turnCounts[activePlayerIndex]++;
                currentPhase = new DrawPhase();

                fire(new PhaseEvent.TurnStarted(activePlayerIndex, currentPhase));
                fire(new PhaseEvent.PhaseEntered(activePlayerIndex, currentPhase));
            }
            case DrawPhase d -> throw new InvalidPhaseTransitionException(
                    "endBetweenTurns() called during DrawPhase");
            case MainPhase m -> throw new InvalidPhaseTransitionException(
                    "endBetweenTurns() called during MainPhase");
            case AttackPhase a -> throw new InvalidPhaseTransitionException(
                    "endBetweenTurns() called during AttackPhase");
            case ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase a -> throw new InvalidPhaseTransitionException(
                    "endBetweenTurns() called during ActionResolutionPhase");
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Delivers the event to a snapshot copy of the listener list.
     * Using a snapshot prevents ConcurrentModificationException when a listener
     * adds or removes listeners during delivery. Exceptions propagate to the caller.
     *
     * @param event the event to deliver
     */
    private void fire(final PhaseEvent event) {
        List.copyOf(listeners).forEach(l -> l.on(event));
    }

    /**
     * Returns a human-readable name for the given phase, handling {@code null}.
     *
     * @param phase the phase (may be null)
     * @return phase name or "null"
     */
    private String phaseName(final TurnPhase phase) {
        return phase == null ? "null" : phase.name();
    }
}
