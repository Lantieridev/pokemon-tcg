package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Sealed interface representing a player action during the main phase of a turn.
 * All permits are records; a switch over Action must be exhaustive.
 * FR-004.
 */
public sealed interface Action
        permits EvolveAction, RetreatAction, PlayTrainerAction, AttachEnergyAction, DeclareAttackAction,
                PlaceBasicPokemonAction, UseAbilityAction, EndTurnAction, PromoteActiveAction {
}
