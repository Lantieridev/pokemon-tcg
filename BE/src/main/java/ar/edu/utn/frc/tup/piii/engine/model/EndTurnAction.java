package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player voluntarily ending their turn without declaring an attack.
 * Transitions the session from MainPhase through BetweenTurnsPhase into the next player's
 * DrawPhase. FR-002.
 */
public record EndTurnAction() implements Action {
}
