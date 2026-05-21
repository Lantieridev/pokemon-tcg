package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.exception.CardNotInHandException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mutable runtime hand for one player. Supports card addition, targeted removal by ID,
 * and Mulligan detection via hasBasicPokemon().
 */
public final class Hand {

    private final List<Card> cards = new ArrayList<>();

    public void addCard(final Card card) {
        Objects.requireNonNull(card, "card must not be null");
        cards.add(card);
    }

    public void addCards(final List<Card> toAdd) {
        Objects.requireNonNull(toAdd, "cards list must not be null");
        cards.addAll(toAdd);
    }

    /**
     * Removes and returns the card with the given ID.
     *
     * @param cardId the ID to look up
     * @throws CardNotInHandException if no card with that ID is in the hand
     */
    public Card removeCard(final String cardId) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getCardId().equals(cardId)) {
                return cards.remove(i);
            }
        }
        throw new CardNotInHandException(cardId);
    }

    /**
     * Returns true if the hand contains at least one Basic Pokémon.
     * Used to determine whether a Mulligan is required during setup.
     */
    public boolean hasBasicPokemon() {
        for (final Card card : cards) {
            if (card.isBasicPokemon()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes and returns all cards, leaving the hand empty.
     * Used during a Mulligan to return the hand to the deck before reshuffling.
     *
     * @return all cards that were in the hand (never null; may be empty)
     */
    public List<Card> removeAll() {
        final List<Card> all = new ArrayList<>(cards);
        cards.clear();
        return all;
    }

    /** Returns a defensive copy of the hand. */
    public List<Card> getCards() {
        return Collections.unmodifiableList(new ArrayList<>(cards));
    }

    public int size() {
        return cards.size();
    }
}
