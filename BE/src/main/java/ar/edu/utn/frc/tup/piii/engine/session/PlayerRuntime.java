package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mutable runtime state for a single player during an active match.
 * Aggregates deck, hand, bench, discard pile, status effects, and active Pokémon.
 * Pure POJO — zero Spring imports. FR-004 through FR-014.
 */
public final class PlayerRuntime {

    private final Deck deck;
    private final Hand hand;
    private final Bench bench;
    private final DiscardPile discardPile;
    private final StatusEffectManager statusEffectManager;
    private BattlePokemonState activePokemon;
    private final List<Card> prizePile;

    /**
     * Full constructor including prize pile.
     *
     * @param deck                 the player's deck (never null)
     * @param hand                 the player's hand (never null)
     * @param bench                the player's bench (never null)
     * @param discardPile          the player's discard pile (never null)
     * @param statusEffectManager  tracks the active Pokémon's status conditions (never null)
     * @param activePokemon        the Pokémon currently in the Active slot (never null)
     * @param prizePile            the face-down prize cards set aside at setup (never null)
     */
    public PlayerRuntime(final Deck deck,
                         final Hand hand,
                         final Bench bench,
                         final DiscardPile discardPile,
                         final StatusEffectManager statusEffectManager,
                         final BattlePokemonState activePokemon,
                         final List<Card> prizePile) {
        this.deck = Objects.requireNonNull(deck, "deck must not be null");
        this.hand = Objects.requireNonNull(hand, "hand must not be null");
        this.bench = Objects.requireNonNull(bench, "bench must not be null");
        this.discardPile = Objects.requireNonNull(discardPile, "discardPile must not be null");
        this.statusEffectManager = Objects.requireNonNull(statusEffectManager, "statusEffectManager must not be null");
        this.activePokemon = Objects.requireNonNull(activePokemon, "activePokemon must not be null");
        this.prizePile = new ArrayList<>(Objects.requireNonNull(prizePile, "prizePile must not be null"));
    }

    /**
     * Backward-compatible constructor without prize pile (defaults to empty).
     *
     * @param deck                the player's deck (never null)
     * @param hand                the player's hand (never null)
     * @param bench               the player's bench (never null)
     * @param discardPile         the player's discard pile (never null)
     * @param statusEffectManager tracks the active Pokémon's status conditions (never null)
     * @param activePokemon       the Pokémon currently in the Active slot (never null)
     */
    public PlayerRuntime(final Deck deck,
                         final Hand hand,
                         final Bench bench,
                         final DiscardPile discardPile,
                         final StatusEffectManager statusEffectManager,
                         final BattlePokemonState activePokemon) {
        this(deck, hand, bench, discardPile, statusEffectManager, activePokemon, List.of());
    }

    public Deck getDeck() {
        return deck;
    }

    public Hand getHand() {
        return hand;
    }

    public Bench getBench() {
        return bench;
    }

    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public StatusEffectManager getStatusEffectManager() {
        return statusEffectManager;
    }

    public BattlePokemonState getActivePokemon() {
        return activePokemon;
    }

    public void setActivePokemon(final BattlePokemonState pokemon) {
        this.activePokemon = Objects.requireNonNull(pokemon, "pokemon must not be null");
    }

    public void clearActivePokemon() {
        this.activePokemon = null;
    }

    /**
     * Returns an unmodifiable view of the prize pile.
     *
     * @return prize cards (never null; may be empty once all prizes are taken)
     */
    public List<Card> getPrizePile() {
        return Collections.unmodifiableList(prizePile);
    }

    /**
     * Returns the number of prize cards remaining for this player.
     *
     * @return remaining prize count (&gt;= 0)
     */
    public int getPrizeCount() {
        return prizePile.size();
    }

    /**
     * Takes {@code count} prize cards from the top of this player's prize pile
     * and adds them to the player's hand. No-op for any count exceeding the pile size.
     *
     * @param count number of prizes to take (&gt;= 0)
     */
    public void takePrizes(final int count) {
        final int toTake = Math.min(count, prizePile.size());
        for (int i = 0; i < toTake; i++) {
            hand.addCard(prizePile.remove(0));
        }
    }

    /**
     * Clears all remaining prize cards (e.g. for Sudden Death reset).
     *
     * @return the removed prize cards
     */
    public List<Card> clearPrizes() {
        final List<Card> removed = new ArrayList<>(prizePile);
        prizePile.clear();
        return removed;
    }

    /**
     * Adds the given prize cards to the prize pile (e.g. for Sudden Death reset).
     *
     * @param prizes the prize cards to add (never null)
     */
    public void addPrizes(final List<Card> prizes) {
        prizePile.addAll(Objects.requireNonNull(prizes, "prizes must not be null"));
    }
}
