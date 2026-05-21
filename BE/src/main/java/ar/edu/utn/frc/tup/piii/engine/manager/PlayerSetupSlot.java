package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates all per-player mutable state needed during the Setup Phase.
 * Passed into {@link SetupManager#execute} and mutated in-place.
 */
public final class PlayerSetupSlot {

    private final Deck deck;
    private final Hand hand;
    private final Bench bench;
    private final List<Card> prizes = new ArrayList<>();
    private BattlePokemonState activePokemon;

    /**
     * @param deck  the player's shuffled 60-card deck (never null)
     * @param hand  empty hand that will be filled during setup (never null)
     * @param bench empty bench that will be populated during setup (never null)
     */
    public PlayerSetupSlot(final Deck deck, final Hand hand, final Bench bench) {
        this.deck = Objects.requireNonNull(deck, "deck must not be null");
        this.hand = Objects.requireNonNull(hand, "hand must not be null");
        this.bench = Objects.requireNonNull(bench, "bench must not be null");
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

    /** Returns an unmodifiable view of the prize pile (populated after setup). */
    public List<Card> getPrizes() {
        return Collections.unmodifiableList(prizes);
    }

    /** Returns the Active Pokémon placed during setup, or null if not yet placed. */
    public BattlePokemonState getActivePokemon() {
        return activePokemon;
    }

    /** Called by {@link SetupManager} once the Active card has been placed. */
    void setActivePokemon(final BattlePokemonState pokemon) {
        this.activePokemon = pokemon;
    }

    /** Called by {@link SetupManager} to fill the prize pile from the top of the deck. */
    void addPrizes(final List<Card> cards) {
        prizes.addAll(cards);
    }
}
