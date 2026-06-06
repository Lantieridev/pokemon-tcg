package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Represents the evolution stage of a Pokémon card. Used to validate
 * evolution chains: BASIC evolves only to STAGE_1, STAGE_1 only to STAGE_2.
 */
public enum EvolutionStage {
    BASIC, STAGE_1, STAGE_2, MEGA
}
