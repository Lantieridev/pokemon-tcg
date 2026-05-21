package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.exception.DeckEmptyException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Mutable runtime deck for one player. Index 0 is the top card (next to be drawn).
 * Callers should use a 60-card list for standard play; the constructor only enforces non-empty.
 */
public final class Deck {

    private final List<Card> cards;

    /**
     * Constructs a deck from the given card list. The internal order is preserved.
     *
     * @param cards non-null, non-empty list of cards
     */
    public Deck(final List<Card> cards) {
        Objects.requireNonNull(cards, "cards must not be null");
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("Deck must contain at least one card");
        }
        this.cards = new ArrayList<>(cards);
    }

    /**
     * Shuffles using a default Random. For tests that need deterministic results use shuffle(Random).
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Shuffles using the supplied Random — useful for seeded, deterministic tests.
     *
     * @param rng the random source (never null)
     */
    public void shuffle(final Random rng) {
        Objects.requireNonNull(rng, "rng must not be null");
        Collections.shuffle(cards, rng);
    }

    /**
     * Removes and returns the top card (index 0).
     *
     * @throws DeckEmptyException if the deck is empty
     */
    public Card draw() {
        if (cards.isEmpty()) {
            throw new DeckEmptyException("Cannot draw from an empty deck");
        }
        return cards.remove(0);
    }

    /**
     * Removes and returns the top {@code n} cards in draw order.
     *
     * @param n number of cards to draw (must be >= 0)
     * @throws DeckEmptyException if fewer than {@code n} cards remain
     */
    public List<Card> drawMultiple(final int n) {
        if (n > cards.size()) {
            throw new DeckEmptyException(
                    "Cannot draw " + n + " cards — only " + cards.size() + " remain");
        }
        final List<Card> drawn = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            drawn.add(cards.remove(0));
        }
        return drawn;
    }

    /**
     * Returns the top card without removing it.
     *
     * @throws DeckEmptyException if the deck is empty
     */
    public Card peek() {
        if (cards.isEmpty()) {
            throw new DeckEmptyException("Cannot peek into an empty deck");
        }
        return cards.get(0);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }
}
