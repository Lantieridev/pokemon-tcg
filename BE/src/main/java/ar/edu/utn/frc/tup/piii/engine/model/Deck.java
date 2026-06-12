package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.exception.DeckEmptyException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

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
        final int limit = Math.min(n, cards.size());
        final List<Card> drawn = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
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

    /**
     * Appends cards to the bottom of the deck.
     * Used during a Mulligan to return a player's hand before reshuffling.
     *
     * @param toReturn non-null list of cards to append
     */
    public void addCards(final List<Card> toReturn) {
        Objects.requireNonNull(toReturn, "cards list must not be null");
        cards.addAll(toReturn);
    }

    /**
     * Inserts a card at the top of the deck (index 0).
     * Used by Max Revive (xy1-120): "Put a Pokémon from your discard pile on top of your deck."
     *
     * @param card the card to place on top (never null)
     */
    public void addToTop(final Card card) {
        Objects.requireNonNull(card, "card must not be null");
        cards.add(0, card);
    }

    /**
     * Searches the deck for cards matching the predicate, removes up to {@code maxCount} of them,
     * and returns them. Does NOT shuffle — caller is responsible for shuffling afterward.
     * Used by Professor's Letter (search for basic energies) and Evosoda (search for evolution).
     *
     * @param predicate condition to match (never null)
     * @param maxCount  maximum number of cards to remove (must be &gt;= 1)
     * @return list of removed matching cards (may be empty, never null)
     */
    public List<Card> searchAndRemove(final Predicate<Card> predicate, final int maxCount) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        final List<Card> found = new ArrayList<>();
        final Iterator<Card> it = cards.iterator();
        while (it.hasNext() && found.size() < maxCount) {
            final Card c = it.next();
            if (predicate.test(c)) {
                found.add(c);
                it.remove();
            }
        }
        return found;
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }

    /**
     * Returns an unmodifiable view of the cards currently in the deck.
     */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
