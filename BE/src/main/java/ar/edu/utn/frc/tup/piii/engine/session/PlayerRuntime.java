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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final MatchStatisticsTracker statisticsTracker = new MatchStatisticsTracker();
    private boolean knockedOutLastTurn = false;
    private int startingPrizeCount = 6;

    /**
     * Tracks how many full turns each Pokémon has been in play.
     * Keyed by Pokémon identity (object reference). A value of 0 means the Pokémon
     * entered play this turn and cannot yet evolve. Incremented by TurnInPlayTracker
     * at the end of each full turn. FR-010.
     */
    private final Map<BattlePokemonState, Integer> turnsInPlay = new HashMap<>();

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
        this.activePokemon = activePokemon;
        this.prizePile = new ArrayList<>(Objects.requireNonNull(prizePile, "prizePile must not be null"));
        this.startingPrizeCount = this.prizePile.size();
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

    /**
     * Reconstructive constructor including prize pile and turns-in-play tracking.
     *
     * @param deck                 the player's deck (never null)
     * @param hand                 the player's hand (never null)
     * @param bench                the player's bench (never null)
     * @param discardPile          the player's discard pile (never null)
     * @param statusEffectManager  tracks the active Pokémon's status conditions (never null)
     * @param activePokemon        the Pokémon currently in the Active slot (never null)
     * @param prizePile            the face-down prize cards set aside at setup (never null)
     * @param turnsInPlay          turns-in-play tracking map (never null)
     */
    public PlayerRuntime(final Deck deck,
                         final Hand hand,
                         final Bench bench,
                         final DiscardPile discardPile,
                         final StatusEffectManager statusEffectManager,
                         final BattlePokemonState activePokemon,
                         final List<Card> prizePile,
                         final Map<BattlePokemonState, Integer> turnsInPlay) {
        this(deck, hand, bench, discardPile, statusEffectManager, activePokemon, prizePile);
        if (turnsInPlay != null) {
            this.turnsInPlay.putAll(turnsInPlay);
        }
        try {
            java.lang.reflect.Field field = StatusEffectManager.class.getDeclaredField("playerRuntime");
            field.setAccessible(true);
            field.set(statusEffectManager, this);
        } catch (Exception e) {
            // ignore
        }
    }


    public Deck getDeck() {
        return deck;
    }

    public Hand getHand() {
        return hand;
    }

    public Bench getBench() {
        refreshPokemonOwners();
        return bench;
    }

    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public StatusEffectManager getStatusEffectManager() {
        return statusEffectManager;
    }

    public BattlePokemonState getActivePokemon() {
        refreshPokemonOwners();
        return activePokemon;
    }

    public void setActivePokemon(final BattlePokemonState pokemon) {
        this.activePokemon = pokemon;
        if (pokemon != null) {
            pokemon.setOwner(this);
        }
    }

    public void clearActivePokemon() {
        this.activePokemon = null;
        this.statusEffectManager.clearAll();
    }

    public void refreshPokemonOwners() {
        if (activePokemon != null) {
            activePokemon.setOwner(this);
        }
        if (bench != null) {
            for (BattlePokemonState benched : bench.getAll()) {
                if (benched != null) {
                    benched.setOwner(this);
                }
            }
        }
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
        this.startingPrizeCount = 0;
        return removed;
    }

    /**
     * Adds the given prize cards to the prize pile (e.g. for Sudden Death reset).
     *
     * @param prizes the prize cards to add (never null)
     */
    public void addPrizes(final List<Card> prizes) {
        prizePile.addAll(Objects.requireNonNull(prizes, "prizes must not be null"));
        this.startingPrizeCount = prizePile.size();
    }

    // -------------------------------------------------------------------------
    // turnsInPlay tracking (FR-010 — evolution restriction)
    // -------------------------------------------------------------------------

    /**
     * Records that a Pokémon has just entered play (active slot or bench).
     * Sets its turns-in-play counter to 0, preventing immediate evolution.
     *
     * @param pokemon the Pokémon that entered play (never null)
     */
    public void recordPokemonEntered(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        turnsInPlay.put(pokemon, 0);
    }

    /**
     * Increments the turns-in-play counter for every Pokémon currently tracked.
     * Called by {@code TurnInPlayTracker} at the end of this player's full turn.
     */
    public void incrementAllTurnsInPlay() {
        turnsInPlay.replaceAll((pokemon, turns) -> turns + 1);
    }

    /**
     * Resets the used abilities tracker for all Pokémon in play.
     * Called at the end of this player's full turn.
     */
    public void resetAllAbilitiesUsedThisTurn() {
        if (activePokemon != null) {
            activePokemon.resetAbilitiesUsedThisTurn();
        }
        for (BattlePokemonState benched : bench.getAll()) {
            benched.resetAbilitiesUsedThisTurn();
        }
    }

    /**
     * Returns the number of full turns the given Pokémon has been in play.
     * Returns 0 if the Pokémon entered play this turn or is not tracked.
     *
     * @param pokemon the Pokémon to look up (never null)
     * @return turns in play (&gt;= 0)
     */
    public int getTurnsInPlay(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        return turnsInPlay.getOrDefault(pokemon, 0);
    }

    public java.util.Set<BattlePokemonState> getTurnsInPlayKeys() {
        return turnsInPlay.keySet();
    }

    /**
     * Returns true if this player has the given Pokémon registered in their
     * turns-in-play map (i.e. the Pokémon is or was in play for this player).
     *
     * @param pokemon the Pokémon to check (never null)
     * @return true if tracked
     */
    public boolean hasPokemonInPlay(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        return turnsInPlay.containsKey(pokemon);
    }

    /**
     * Removes a Pokémon from the turns-in-play map (e.g. after KO or discard).
     *
     * @param pokemon the Pokémon to remove (never null)
     */
    public void removePokemonFromPlay(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        turnsInPlay.remove(pokemon);
    }

    /**
     * Checks if any Pokémon currently in play (Active or Bench) has the specified ability.
     *
     * @param abilityId the ability ID to check
     * @return true if the ability is present on any of this player's in-play Pokémon
     */
    public boolean hasAbility(final ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId abilityId) {
        if (activePokemon != null && activePokemon.getAbilities().stream().anyMatch(a -> a.effectId() == abilityId)) {
            return true;
        }
        for (BattlePokemonState benched : bench.getAll()) {
            if (benched.getAbilities().stream().anyMatch(a -> a.effectId() == abilityId)) {
                return true;
            }
        }
        return false;
    }

    public MatchStatisticsTracker getStatisticsTracker() {
        return statisticsTracker;
    }

    public boolean isKnockedOutLastTurn() {
        return knockedOutLastTurn;
    }

    public void setKnockedOutLastTurn(final boolean knockedOutLastTurn) {
        this.knockedOutLastTurn = knockedOutLastTurn;
    }

    public int getStartingPrizeCount() {
        return startingPrizeCount;
    }

    public void setStartingPrizeCount(final int startingPrizeCount) {
        this.startingPrizeCount = startingPrizeCount;
    }
}
