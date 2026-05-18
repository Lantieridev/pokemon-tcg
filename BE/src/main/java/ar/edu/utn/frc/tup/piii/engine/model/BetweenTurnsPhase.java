package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * The between-turns phase — status effects resolve and the turn transitions to the next player.
 * FR-001.
 */
public record BetweenTurnsPhase() implements TurnPhase {

    /** Canonical phase name. */
    private static final String PHASE_NAME = "BETWEEN_TURNS";

    @Override
    public String name() {
        return PHASE_NAME;
    }
}
