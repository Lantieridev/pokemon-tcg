package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * The draw phase of a player's turn — the active player draws a card. FR-001.
 */
public record DrawPhase() implements TurnPhase {

    /** Canonical phase name. */
    private static final String PHASE_NAME = "DRAW";

    @Override
    public String name() {
        return PHASE_NAME;
    }
}
