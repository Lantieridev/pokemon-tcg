package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.SetupStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Orchestrates the XY1 Setup Phase for a two-player match:
 * <ol>
 *   <li>Shuffle decks and deal initial 7-card hands.</li>
 *   <li>Mulligan loop — any player without a Basic Pokémon shuffles their hand back and
 *       redraws until both have at least one Basic.</li>
 *   <li>Both players place an Active Pokémon and optionally up to 5 Bench Pokémon.</li>
 *   <li>Each opponent may draw bonus cards equal to the number of Mulligans the other
 *       player declared (drawn after placement, per XY1 §1.3).</li>
 *   <li>Each player sets aside 6 Prize cards from the top of their deck.</li>
 *   <li>A coin flip determines who takes the first turn.</li>
 * </ol>
 *
 * <p>Pure POJO — zero Spring imports. The {@code shuffler} dependency is injected via
 * the two-argument constructor to allow deterministic tests.</p>
 */
public final class SetupManager {

    private static final int INITIAL_HAND_SIZE = 7;
    private final int prizeCount;

    private final CoinFlipper coinFlipper;
    private final Consumer<Deck> shuffler;

    /**
     * Production constructor — uses the deck's built-in default shuffle and 6 prizes.
     *
     * @param coinFlipper decides who goes first (never null)
     */
    public SetupManager(final CoinFlipper coinFlipper) {
        this(coinFlipper, Deck::shuffle, 6);
    }

    /**
     * Production constructor with custom prize count (e.g. for Sudden Death).
     *
     * @param coinFlipper decides who goes first (never null)
     * @param prizeCount  number of prize cards to deal
     */
    public SetupManager(final CoinFlipper coinFlipper, final int prizeCount) {
        this(coinFlipper, Deck::shuffle, prizeCount);
    }

    /**
     * Test-friendly constructor that accepts an injectable shuffler and prize count.
     *
     * @param coinFlipper decides who goes first (never null)
     * @param shuffler    called whenever a deck needs to be shuffled (never null)
     * @param prizeCount  number of prize cards to deal
     */
    SetupManager(final CoinFlipper coinFlipper, final Consumer<Deck> shuffler, final int prizeCount) {
        this.coinFlipper = Objects.requireNonNull(coinFlipper, "coinFlipper must not be null");
        this.shuffler = Objects.requireNonNull(shuffler, "shuffler must not be null");
        this.prizeCount = prizeCount;
    }

    /**
     * Executes the full Setup Phase, mutating both slots in-place.
     *
     * @param slot0     player 0's mutable setup state (never null)
     * @param strategy0 player 0's decision callbacks (never null)
     * @param slot1     player 1's mutable setup state (never null)
     * @param strategy1 player 1's decision callbacks (never null)
     * @return immutable result carrying first-player index and mulligan counts
     */
    public SetupResult execute(
            final PlayerSetupSlot slot0, final SetupStrategy strategy0,
            final PlayerSetupSlot slot1, final SetupStrategy strategy1) {

        Objects.requireNonNull(slot0, "slot0 must not be null");
        Objects.requireNonNull(strategy0, "strategy0 must not be null");
        Objects.requireNonNull(slot1, "slot1 must not be null");
        Objects.requireNonNull(strategy1, "strategy1 must not be null");

        // Step 1 — shuffle and deal initial hands
        shuffler.accept(slot0.getDeck());
        shuffler.accept(slot1.getDeck());
        slot0.getHand().addCards(slot0.getDeck().drawMultiple(INITIAL_HAND_SIZE));
        slot1.getHand().addCards(slot1.getDeck().drawMultiple(INITIAL_HAND_SIZE));

        // Step 2 — Mulligan loop
        final List<List<Card>> mulligansP0 = new ArrayList<>();
        final List<List<Card>> mulligansP1 = new ArrayList<>();

        while (!hasBasicPokemon(slot0.getHand()) || !hasBasicPokemon(slot1.getHand())) {
            if (!hasBasicPokemon(slot0.getHand())) {
                mulligansP0.add(new ArrayList<>(slot0.getHand().getCards()));
                doMulligan(slot0);
            }
            if (!hasBasicPokemon(slot1.getHand())) {
                mulligansP1.add(new ArrayList<>(slot1.getHand().getCards()));
                doMulligan(slot1);
            }
        }

        // Step 3 — Place Active and Bench
        placeActive(slot0, strategy0);
        placeBench(slot0, strategy0);
        placeActive(slot1, strategy1);
        placeBench(slot1, strategy1);

        // Step 4 — Mandatory bonus draws
        if (mulligansP0.size() > mulligansP1.size()) {
            final int difference = mulligansP0.size() - mulligansP1.size();
            slot1.getHand().addCards(slot1.getDeck().drawMultiple(difference));
        } else if (mulligansP1.size() > mulligansP0.size()) {
            final int difference = mulligansP1.size() - mulligansP0.size();
            slot0.getHand().addCards(slot0.getDeck().drawMultiple(difference));
        }

        // Step 5 — set aside Prize cards
        slot0.addPrizes(slot0.getDeck().drawMultiple(prizeCount));
        slot1.addPrizes(slot1.getDeck().drawMultiple(prizeCount));

        // Step 6 — coin flip for first player
        final int firstPlayerIndex = coinFlipper.flip() ? 0 : 1;

        return new SetupResult(firstPlayerIndex, mulligansP0, mulligansP1);
    }

    private boolean hasBasicPokemon(final ar.edu.utn.frc.tup.piii.engine.model.Hand hand) {
        return hand.getCards().stream()
                .anyMatch(card -> card instanceof PokemonCard pokemon
                        && pokemon.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void doMulligan(final PlayerSetupSlot slot) {
        final List<Card> returned = slot.getHand().removeAll();
        slot.getDeck().addCards(returned);
        shuffler.accept(slot.getDeck());
        slot.getHand().addCards(slot.getDeck().drawMultiple(INITIAL_HAND_SIZE));
    }

    private void placeActive(final PlayerSetupSlot slot, final SetupStrategy strategy) {
        final String activeId = strategy.chooseActiveCardId(slot.getHand().getCards());
        final Card card = slot.getHand().removeCard(activeId);
        slot.setActivePokemon(new InPlayPokemon((PokemonCard) card));
    }

    private void placeBench(final PlayerSetupSlot slot, final SetupStrategy strategy) {
        final List<String> benchIds = strategy.chooseBenchCardIds(slot.getHand().getCards());
        for (final String id : benchIds) {
            final Card card = slot.getHand().removeCard(id);
            slot.getBench().place(new InPlayPokemon((PokemonCard) card));
        }
    }
}
