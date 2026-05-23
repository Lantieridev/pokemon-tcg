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
     * @param runtime     the actor's runtime (provides hand, deck, discard pile, etc.)
     * @param target      the targeted Pokémon (if applicable, otherwise null)
     */
    void apply(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime, ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState target);

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
        return (runtime, target) -> runtime.getHand().addCards(runtime.getDeck().drawMultiple(count));
    }

    /**
     * SUPPORTER effect — Professor Oak / Professor's Research:
     * discard the entire hand, then draw 7 cards from the top of the deck.
     *
     * @return a {@link TrainerEffect} implementing the "discard hand, draw 7" effect
     */
    static TrainerEffect professorOak() {
        return (runtime, target) -> {
            runtime.getDiscardPile().addAll(runtime.getHand().removeAll());
            runtime.getHand().addCards(runtime.getDeck().drawMultiple(7));
        };
    }

    /**
     * ITEM effect — heal damage from the targeted Pokémon.
     *
     * @param amount the amount of damage to heal
     * @return a {@link TrainerEffect} that heals the specified amount
     */
    static TrainerEffect healDamage(final int amount) {
        return (runtime, target) -> {
            if (target != null) {
                target.heal(amount);
            }
        };
    }

    /**
     * ITEM effect — Roller Skates (xy1-114): flip a coin; if heads, draw 3 cards.
     *
     * @param flipper the coin-flip provider (never null)
     * @return a {@link TrainerEffect} that conditionally draws 3 cards
     */
    static TrainerEffect rollerSkates(final CoinFlipper flipper) {
        return (runtime, target) -> {
            if (flipper.flip()) {
                runtime.getHand().addCards(runtime.getDeck().drawMultiple(3));
            }
        };
    }

    /**
     * SUPPORTER effect — Shauna (xy1-127): shuffle your hand into your deck, then draw 5 cards.
     *
     * @return a {@link TrainerEffect} implementing Shauna's shuffle-and-draw effect
     */
    static TrainerEffect shauna() {
        return (runtime, target) -> {
            runtime.getDeck().addCards(runtime.getHand().removeAll());
            runtime.getDeck().shuffle();
            runtime.getHand().addCards(runtime.getDeck().drawMultiple(5));
        };
    }

    /**
     * ITEM effect — Super Potion (xy1-128): heal 60 damage from the targeted Pokémon.
     * If healing is applied, discard 1 energy attached to that Pokémon.
     *
     * @return a {@link TrainerEffect} implementing Super Potion's heal-and-discard effect
     */
    static TrainerEffect superPotion() {
        return (runtime, target) -> {
            if (target != null && target.getDamageCounters() > 0) {
                target.heal(60);
                target.removeEnergies(1);
            }
        };
    }
}
