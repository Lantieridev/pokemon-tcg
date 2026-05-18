package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player playing a Trainer card. FR-004.
 *
 * @param trainerType the category of the Trainer card being played
 */
public record PlayTrainerAction(TrainerType trainerType) implements Action {
}
