package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * The attack phase of a player's turn — the active player may declare an attack. FR-001.
 */
public record AttackPhase() implements TurnPhase {

    /** Canonical phase name. */
    private static final String PHASE_NAME = "ATTACK";

    @Override
    public String name() {
        return PHASE_NAME;
    }
}
