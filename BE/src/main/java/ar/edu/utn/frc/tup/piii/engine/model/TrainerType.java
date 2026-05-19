package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Classifies the three categories of Trainer cards in the Pokémon TCG XY1 ruleset.
 * FR-003.
 */
public enum TrainerType {

    /** Item cards; may be played any number of times per turn. */
    ITEM,

    /** Supporter cards; at most one may be played per turn. */
    SUPPORTER,

    /** Stadium cards; at most one may be played per turn. */
    STADIUM,

    /** Pokémon Tool cards; attach to a Pokémon — only one tool per Pokémon allowed. */
    POKEMON_TOOL
}
