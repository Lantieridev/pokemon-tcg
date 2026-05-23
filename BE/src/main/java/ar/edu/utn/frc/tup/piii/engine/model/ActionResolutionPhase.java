package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * A phase that interrupts the normal turn flow to resolve an interactive selection.
 */
public record ActionResolutionPhase() implements TurnPhase {
    @Override
    public String name() {
        return "ActionResolutionPhase";
    }
}
