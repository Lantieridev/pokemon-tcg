package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Strategy for a Trainer card's runtime effect. Implementations receive the acting player's
 * mutable zone references so they can draw, discard, or otherwise mutate game state.
 *
 * <p>The engine resolves trainer effects AFTER the card has been removed from the hand and placed
 * in the discard pile (standard XY1 rule). Effects should not re-discard the source card.</p>
 *
 * <p>Implementations must be pure (no Spring / no I/O) to satisfy the engine isolation rule.</p>
 */
@FunctionalInterface
public interface TrainerEffect {

    /**
     * Applies this trainer's effect.
     *
     * @param hand        the actor's hand (may be drawn into or discarded from)
     * @param deck        the actor's deck (draw source)
     * @param discardPile the actor's discard pile (discard target)
     */
    void apply(Hand hand, Deck deck, DiscardPile discardPile);

    // -------------------------------------------------------------------------
    // Built-in effect factories (static helpers for common XY1 Trainer effects)
    // -------------------------------------------------------------------------

    /**
     * ITEM effect — draw {@code count} cards from the top of the deck.
     * If fewer than {@code count} cards remain the deck may throw; callers are responsible
     * for victory-condition checking (deck-out) before or after applying this effect.
     *
     * @param count number of cards to draw (must be &gt; 0)
     * @return a {@link TrainerEffect} that draws exactly {@code count} cards
     */
    static TrainerEffect drawCards(final int count) {
        return (hand, deck, discardPile) -> hand.addCards(deck.drawMultiple(count));
    }

    /**
     * SUPPORTER effect — Professor Oak / Professor's Research:
     * discard the entire hand, then draw 7 cards from the top of the deck.
     *
     * @return a {@link TrainerEffect} implementing the "discard hand, draw 7" effect
     */
    static TrainerEffect professorOak() {
        return (hand, deck, discardPile) -> {
            discardPile.addAll(hand.removeAll());
            hand.addCards(deck.drawMultiple(7));
        };
    }
}
