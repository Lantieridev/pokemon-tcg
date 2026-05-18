package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.TurnPhase;

/**
 * Sealed event hierarchy fired by TurnManager as the turn progresses through its phases.
 * All four variants are nested records for sealed-permits coupling. FR-012.
 */
public sealed interface PhaseEvent
        permits PhaseEvent.PhaseEntered, PhaseEvent.PhaseExited,
                PhaseEvent.TurnStarted, PhaseEvent.TurnEnded {

    /**
     * Returns the zero-based index of the player whose turn this event belongs to.
     *
     * @return player index
     */
    int playerIndex();

    /**
     * Returns the TurnPhase associated with this event.
     *
     * @return current phase
     */
    TurnPhase phase();

    /**
     * Fired immediately after the TurnManager transitions into a new phase.
     *
     * @param playerIndex zero-based index of the active player
     * @param phase       the phase just entered
     */
    record PhaseEntered(int playerIndex, TurnPhase phase) implements PhaseEvent {
    }

    /**
     * Fired immediately before the TurnManager transitions out of the current phase.
     *
     * @param playerIndex zero-based index of the active player
     * @param phase       the phase being exited
     */
    record PhaseExited(int playerIndex, TurnPhase phase) implements PhaseEvent {
    }

    /**
     * Fired at the beginning of a player's full turn (before the first phase is entered).
     *
     * @param playerIndex zero-based index of the player whose turn is starting
     * @param phase       the first phase of the new turn (always DrawPhase)
     */
    record TurnStarted(int playerIndex, TurnPhase phase) implements PhaseEvent {
    }

    /**
     * Fired at the end of a player's full turn (after the last phase is exited).
     *
     * @param playerIndex zero-based index of the player whose turn is ending
     * @param phase       the last phase of the ending turn (always BetweenTurnsPhase)
     */
    record TurnEnded(int playerIndex, TurnPhase phase) implements PhaseEvent {
    }
}
