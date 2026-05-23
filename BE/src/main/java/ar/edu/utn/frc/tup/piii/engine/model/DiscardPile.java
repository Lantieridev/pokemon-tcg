package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Append-only discard pile for one player.
 * Used when Pokémon are KO'd (Pokémon card + all attached energies are discarded together).
 */
public final class DiscardPile {

    private final List<Card> cards = new ArrayList<>();

    public void add(final Card card) {
        Objects.requireNonNull(card, "card must not be null");
        cards.add(card);
    }

    public void addAll(final List<Card> toDiscard) {
        Objects.requireNonNull(toDiscard, "cards list must not be null");
        cards.addAll(toDiscard);
    }

    /**
     * Removes the given card from the discard pile.
     * Used by Max Revive (xy1-120) to retrieve a Pokémon card from the discard.
     *
     * @param card the card to remove (never null)
     * @return {@code true} if the card was found and removed
     */
    public boolean remove(final Card card) {
        Objects.requireNonNull(card, "card must not be null");
        return cards.remove(card);
    }

    /** Returns an unmodifiable view of the discard pile in insertion order. */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /**
     * Removes all cards from the discard pile.
     *
     * @return the removed cards
     */
    public List<Card> removeAll() {
        final List<Card> removed = new ArrayList<>(cards);
        cards.clear();
        return removed;
    }

    public int size() {
        return cards.size();
    }
}
