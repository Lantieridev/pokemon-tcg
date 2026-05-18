package ar.edu.utn.frc.tup.piii.engine.listener;

/**
 * Observer callback invoked by TurnManager each time a PhaseEvent is fired.
 * Declared as a functional interface so callers may supply lambda expressions. FR-011.
 */
@FunctionalInterface
public interface PhaseListener {

    /**
     * Called when a phase event occurs.
     *
     * @param event the event fired by the TurnManager
     */
    void on(PhaseEvent event);
}
