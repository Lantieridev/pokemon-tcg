package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Sealed interface representing a distinct phase within a player's turn.
 * Pattern-matching on this type is exhaustive without a default branch. FR-001.
 */
public sealed interface TurnPhase permits DrawPhase, MainPhase, AttackPhase, BetweenTurnsPhase {

    /**
     * Returns the canonical name of this phase.
     *
     * @return phase name string
     */
    String name();
}
