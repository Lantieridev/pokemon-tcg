package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for KnockoutResolutionHandler — FR-008 through FR-011.
 */
class KnockoutResolutionHandlerTest {

    private static final int HP = 60;
    private static final int ONE_PRIZE = 1;
    private static final int TWO_PRIZES = 2;

    private TurnManager turnManager;
    private PlayerRuntime attacker;
    private PlayerRuntime defender;
    private DiscardPile defenderDiscard;
    private List<Card> attackerPrizePile;

    @BeforeEach
    void setUp() {
        turnManager = new TurnManager();

        final Deck attackerDeck = buildDeck(60);
        final Deck defenderDeck = buildDeck(60);
        final Hand attackerHand = new Hand();
        final Hand defenderHand = new Hand();
        final Bench attackerBench = new Bench();
        final Bench defenderBench = new Bench();
        defenderDiscard = new DiscardPile();
        final DiscardPile attackerDiscard = new DiscardPile();
        final CoinFlipper coinFlipper = Mockito.mock(CoinFlipper.class);
        final StatusEffectManager sem = new StatusEffectManager(coinFlipper);

        final PokemonCard attackerActive = buildCharmander("atk-active");
        final PokemonCard defenderActive = buildCharmander("def-active");

        attackerPrizePile = new ArrayList<>();
        attackerPrizePile.add(buildEnergyCard("prize-1"));
        attackerPrizePile.add(buildEnergyCard("prize-2"));

        attacker = new PlayerRuntime(
                attackerDeck, attackerHand, attackerBench, attackerDiscard,
                sem, new InPlayPokemon(attackerActive), new ArrayList<>(attackerPrizePile));

        defender = new PlayerRuntime(
                defenderDeck, defenderHand, defenderBench, defenderDiscard,
                new StatusEffectManager(coinFlipper), new InPlayPokemon(defenderActive), new ArrayList<>());
    }

    @Test
    void shouldThrowWhenPlayerRuntimesIsNull() {
        assertThrows(NullPointerException.class,
                () -> new KnockoutResolutionHandler(null, turnManager, (k, p) -> { }));
    }

    @Test
    void shouldThrowWhenTurnManagerIsNull() {
        assertThrows(NullPointerException.class,
                () -> new KnockoutResolutionHandler(
                        List.of(attacker, defender), null, (k, p) -> { }));
    }

    @Test
    void shouldThrowWhenDownstreamIsNull() {
        assertThrows(NullPointerException.class,
                () -> new KnockoutResolutionHandler(
                        List.of(attacker, defender), turnManager, null));
    }

    @Test
    void shouldDiscardKnockedPokemonsBaseCardToDefendersDiscardPile() {
        // Attacker is player 0 — startTurn(0) sets activePlayerIndex = 0
        turnManager.startTurn(0);

        final PokemonCard knockedCard = buildCharmander("knocked-poke");
        final InPlayPokemon knocked = new InPlayPokemon(knockedCard);
        defender.setActivePokemon(knocked);

        final KnockoutResolutionHandler handler = new KnockoutResolutionHandler(
                List.of(attacker, defender), turnManager, (k, p) -> { });

        handler.onKnockout(knocked, ONE_PRIZE);

        assertTrue(defenderDiscard.getCards().contains(knockedCard),
                "Knocked Pokémon's card should be in defender's discard pile");
    }

    @Test
    void shouldTransferPrizeCardsToAttackersHand() {
        turnManager.startTurn(0);

        final PokemonCard knockedCard = buildCharmander("knocked-poke");
        final InPlayPokemon knocked = new InPlayPokemon(knockedCard);
        defender.setActivePokemon(knocked);

        final KnockoutResolutionHandler handler = new KnockoutResolutionHandler(
                List.of(attacker, defender), turnManager, (k, p) -> { });

        final int handSizeBefore = attacker.getHand().size();
        handler.onKnockout(knocked, ONE_PRIZE);

        assertEquals(handSizeBefore + ONE_PRIZE, attacker.getHand().size(),
                "Attacker should receive one prize card into their hand");
    }

    @Test
    void shouldTransferTwoPrizesForExPokemon() {
        turnManager.startTurn(0);

        final PokemonCard knockedCard = buildCharmander("knocked-ex");
        final InPlayPokemon knocked = new InPlayPokemon(knockedCard);
        defender.setActivePokemon(knocked);

        final KnockoutResolutionHandler handler = new KnockoutResolutionHandler(
                List.of(attacker, defender), turnManager, (k, p) -> { });

        final int handSizeBefore = attacker.getHand().size();
        handler.onKnockout(knocked, TWO_PRIZES);

        assertEquals(handSizeBefore + TWO_PRIZES, attacker.getHand().size(),
                "Attacker should receive two prize cards for an EX knock-out");
    }

    @Test
    void shouldNotifyDownstreamHandlerAfterKnockout() {
        turnManager.startTurn(0);

        final PokemonCard knockedCard = buildCharmander("knocked-poke");
        final InPlayPokemon knocked = new InPlayPokemon(knockedCard);
        defender.setActivePokemon(knocked);

        final AtomicBoolean downstreamCalled = new AtomicBoolean(false);
        final KnockoutHandler downstream = (k, p) -> downstreamCalled.set(true);

        final KnockoutResolutionHandler handler = new KnockoutResolutionHandler(
                List.of(attacker, defender), turnManager, downstream);

        handler.onKnockout(knocked, ONE_PRIZE);

        assertTrue(downstreamCalled.get(), "Downstream handler must be called after knockout resolution");
    }

    @Test
    void shouldResolveKnockoutWhenActivePokemonReferenceDiffersFromKnockedButUuidMatches() {
        // Regression test: onKnockout() used to find the owner via
        // `playerRuntimes.get(i).getActivePokemon() == knocked` (reference equality).
        // InPlayPokemon.equals() is UUID-based specifically so the same Pokemon is still
        // recognized after a JSON deserialization round-trip (new object, same UUID) - the
        // reference check silently failed that case for an ACTIVE (non-benched) Pokemon,
        // where the bench-side `.contains()` fallback doesn't apply either, so the whole
        // knockout (discard, prizes, downstream notification) was skipped with no error.
        turnManager.startTurn(0);

        final PokemonCard knockedCard = buildCharmander("knocked-poke");
        final InPlayPokemon activeOnBoard = new InPlayPokemon(knockedCard);
        defender.setActivePokemon(activeOnBoard);

        final InPlayPokemon knockedFromEvent = new InPlayPokemon(knockedCard);
        knockedFromEvent.setUuid(activeOnBoard.getUuid());

        final AtomicBoolean downstreamCalled = new AtomicBoolean(false);
        final KnockoutHandler downstream = (k, p) -> downstreamCalled.set(true);
        final KnockoutResolutionHandler handler = new KnockoutResolutionHandler(
                List.of(attacker, defender), turnManager, downstream);

        final int handSizeBefore = attacker.getHand().size();
        handler.onKnockout(knockedFromEvent, ONE_PRIZE);

        assertTrue(defenderDiscard.getCards().contains(knockedCard),
                "Knocked Pokémon's card should be in defender's discard pile");
        assertEquals(handSizeBefore + ONE_PRIZE, attacker.getHand().size(),
                "Attacker should receive the prize card even when the knocked reference differs");
        assertTrue(downstreamCalled.get(), "Downstream handler must still be called");
    }

    @Test
    void shouldRemoveBenchedKnockedPokemonFromDefendersBench() {
        turnManager.startTurn(0);

        final PokemonCard benchedCard = buildCharmander("benched-poke");
        final InPlayPokemon benched = new InPlayPokemon(benchedCard);
        defender.getBench().place(benched);

        assertEquals(1, defender.getBench().size());

        final KnockoutResolutionHandler handler = new KnockoutResolutionHandler(
                List.of(attacker, defender), turnManager, (k, p) -> { });

        handler.onKnockout(benched, ONE_PRIZE);

        assertEquals(0, defender.getBench().size(),
                "Knocked benched Pokémon should be removed from the bench");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Deck buildDeck(final int size) {
        final List<Card> cards = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cards.add(buildEnergyCard("energy-" + i));
        }
        return new Deck(cards);
    }

    private static EnergyCard buildEnergyCard(final String id) {
        return new EnergyCard(id, "Fire Energy", PokemonType.FIRE, true);
    }

    private static PokemonCard buildCharmander(final String id) {
        return new PokemonCard.Builder(id, "Charmander", HP, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
    }
}
