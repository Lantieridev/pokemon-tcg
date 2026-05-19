package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Holds runtime state for a single player in a match.
 * Pure POJO — no Spring imports.
 */
public final class PlayerState {

    private final BattlePokemonState activePokemon;
    private final List<BattlePokemonState> bench;
    private final List<String> hand;
    private final List<Attack> activeAttacks;
    private final int deckSize;
    private final int prizeCount;
    private final Map<BattlePokemonState, Integer> turnsInPlay;

    /**
     * Constructs a PlayerState with all fields including hand and active attacks.
     *
     * @param activePokemon the active Pokémon slot (may be null between KO and replacement)
     * @param bench         non-null list of benched Pokémon
     * @param hand          non-null list of card IDs in this player's hand
     * @param activeAttacks non-null list of attacks available to the active Pokémon
     * @param deckSize      number of cards remaining in this player's deck
     * @param prizeCount    number of prize cards remaining
     * @param turnsInPlay   map of Pokémon to how many full turns they have been in play
     */
    public PlayerState(final BattlePokemonState activePokemon,
                       final List<BattlePokemonState> bench,
                       final List<String> hand,
                       final List<Attack> activeAttacks,
                       final int deckSize,
                       final int prizeCount,
                       final Map<BattlePokemonState, Integer> turnsInPlay) {
        this.activePokemon = activePokemon;
        this.bench = Objects.requireNonNull(bench, "bench must not be null");
        this.hand = Objects.requireNonNull(hand, "hand must not be null");
        this.activeAttacks = Objects.requireNonNull(activeAttacks, "activeAttacks must not be null");
        this.deckSize = deckSize;
        this.prizeCount = prizeCount;
        this.turnsInPlay = Objects.requireNonNull(turnsInPlay, "turnsInPlay must not be null");
    }

    /**
     * Constructs a PlayerState with hand but no active attacks (backward-compatible).
     *
     * @param activePokemon the active Pokémon slot (may be null between KO and replacement)
     * @param bench         non-null list of benched Pokémon
     * @param hand          non-null list of card IDs in this player's hand
     * @param deckSize      number of cards remaining in this player's deck
     * @param prizeCount    number of prize cards remaining
     * @param turnsInPlay   map of Pokémon to how many full turns they have been in play
     */
    public PlayerState(final BattlePokemonState activePokemon,
                       final List<BattlePokemonState> bench,
                       final List<String> hand,
                       final int deckSize,
                       final int prizeCount,
                       final Map<BattlePokemonState, Integer> turnsInPlay) {
        this(activePokemon, bench, hand, List.of(), deckSize, prizeCount, turnsInPlay);
    }

    /**
     * Constructs a PlayerState without a hand or attacks (backward-compatible constructor).
     * Hand defaults to an empty list; activeAttacks defaults to an empty list.
     *
     * @param activePokemon the active Pokémon slot (may be null between KO and replacement)
     * @param bench         non-null list of benched Pokémon
     * @param deckSize      number of cards remaining in this player's deck
     * @param prizeCount    number of prize cards remaining
     * @param turnsInPlay   map of Pokémon to how many full turns they have been in play
     */
    public PlayerState(final BattlePokemonState activePokemon,
                       final List<BattlePokemonState> bench,
                       final int deckSize,
                       final int prizeCount,
                       final Map<BattlePokemonState, Integer> turnsInPlay) {
        this(activePokemon, bench, List.of(), List.of(), deckSize, prizeCount, turnsInPlay);
    }

    /**
     * Returns the active Pokémon for this player, or null if the active slot is empty.
     *
     * @return active Pokémon state, or null
     */
    public BattlePokemonState getActivePokemon() {
        return activePokemon;
    }

    /**
     * Returns an unmodifiable view of the bench.
     *
     * @return bench list (never null)
     */
    public List<BattlePokemonState> getBench() {
        return Collections.unmodifiableList(bench);
    }

    /**
     * Returns the number of cards remaining in this player's deck.
     *
     * @return deck size (&gt;= 0)
     */
    public int getDeckSize() {
        return deckSize;
    }

    /**
     * Returns the number of prize cards still face-down for this player.
     *
     * @return prize count (&gt;= 0)
     */
    public int getPrizeCount() {
        return prizeCount;
    }

    /**
     * Returns an unmodifiable view of this player's hand (list of card IDs).
     *
     * @return hand card IDs (never null; may be empty)
     */
    public List<String> getHand() {
        return Collections.unmodifiableList(hand);
    }

    /**
     * Returns an unmodifiable list of attacks available to the active Pokémon.
     *
     * @return attacks (never null; may be empty)
     */
    public List<Attack> getActiveAttacks() {
        return Collections.unmodifiableList(activeAttacks);
    }

    /**
     * Returns the number of full turns the given Pokémon has been in play.
     * Returns 0 if the Pokémon is not in the map.
     *
     * @param pokemon the Pokémon to query (never null)
     * @return turns in play (&gt;= 0)
     */
    public int getTurnsInPlay(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        return turnsInPlay.getOrDefault(pokemon, 0);
    }
}
