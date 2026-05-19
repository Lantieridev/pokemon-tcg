package ar.edu.utn.frc.tup.piii.engine.session;

/**
 * Lifecycle states for a match session.
 * Valid transitions: WAITING → ACTIVE → FINISHED.
 */
public enum MatchSessionState {

    /** Match has been created but not yet started. */
    WAITING,

    /** Match is in progress — players are taking turns. */
    ACTIVE,

    /** Match has ended — no further state changes are allowed. */
    FINISHED
}
