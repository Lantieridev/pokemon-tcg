package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;

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

    public PlayerRuntime(final Deck deck,
                         final Hand hand,
                         final Bench bench,
                         final DiscardPile discardPile,
                         final StatusEffectManager statusEffectManager,
                         final BattlePokemonState activePokemon) {
        this.deck = Objects.requireNonNull(deck, "deck must not be null");
        this.hand = Objects.requireNonNull(hand, "hand must not be null");
        this.bench = Objects.requireNonNull(bench, "bench must not be null");
        this.discardPile = Objects.requireNonNull(discardPile, "discardPile must not be null");
        this.statusEffectManager = Objects.requireNonNull(statusEffectManager, "statusEffectManager must not be null");
        this.activePokemon = Objects.requireNonNull(activePokemon, "activePokemon must not be null");
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
}
