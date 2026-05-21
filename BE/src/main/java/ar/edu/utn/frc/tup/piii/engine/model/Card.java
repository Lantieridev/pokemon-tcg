package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Represents a card in a player's deck, hand, or discard pile.
 * Implemented by PokemonCard, TrainerCard, and EnergyCard.
 */
public interface Card {

    String getCardId();

    String getName();

    CardType getCardType();

    /**
     * Returns true only for Basic Pokémon cards. Used to determine Mulligan eligibility
     * without requiring instanceof checks.
     */
    default boolean isBasicPokemon() {
        return false;
    }

    /**
     * Returns true only for ACE SPEC Trainer cards. Used to enforce the 1-per-deck limit
     * without requiring instanceof checks. RF-02b.
     */
    default boolean isAceSpec() {
        return false;
    }
}
