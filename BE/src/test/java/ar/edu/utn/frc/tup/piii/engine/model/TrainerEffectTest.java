package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the built-in TrainerEffect factory methods.
 */
class TrainerEffectTest {

    private static final int DECK_SIZE = 20;
    private static final int DRAW_TWO = 2;
    private static final int PROFESSOR_OAK_DRAW = 7;

    private static EnergyCard energyCard(final String id) {
        return new EnergyCard(id, "Fire Energy", PokemonType.FIRE, true);
    }

    private static Deck buildDeck(final int size) {
        final List<Card> cards = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cards.add(energyCard("e" + i));
        }
        return new Deck(cards);
    }

    private static PlayerRuntime buildRuntime(final Hand hand, final Deck deck, final DiscardPile discard) {
        // Provide a dummy ActivePokemonState. Since we only test TrainerEffect logic, 
        // passing null for the active pokemon might crash PlayerRuntime constructor.
        // Let's use a mocked or anonymous class if we need to. Or wait, PlayerRuntime allows null activePokemon? 
        // No, it doesn't. 
        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState dummyPokemon = new InPlayPokemon(
            new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 100, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.COLORLESS).build()
        );
        return new PlayerRuntime(
                deck,
                hand,
                new Bench(),
                discard,
                new StatusEffectManager(new RandomCoinFlipper()),
                dummyPokemon
        );
    }

    @Test
    void shouldDrawTwoCardsFromDeckWhenDrawCardsEffectApplied() {
        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.drawCards(DRAW_TWO).apply(runtime, null);

        assertEquals(DRAW_TWO, hand.size(), "Hand should contain the drawn cards");
        assertEquals(DECK_SIZE - DRAW_TWO, deck.size(), "Deck should have two fewer cards");
    }

    @Test
    void shouldDiscardEntireHandAndDrawSevenWhenProfessorOakEffectApplied() {
        final Hand hand = new Hand();
        hand.addCard(energyCard("hand-1"));
        hand.addCard(energyCard("hand-2"));
        hand.addCard(energyCard("hand-3"));
        final int originalHandSize = hand.size();

        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.professorOak().apply(runtime, null);

        assertEquals(PROFESSOR_OAK_DRAW, hand.size(),
                "Hand should contain exactly 7 cards after Professor Oak");
        assertEquals(originalHandSize, discard.size(),
                "Previously held cards should be in the discard pile");
        assertEquals(DECK_SIZE - PROFESSOR_OAK_DRAW, deck.size(),
                "Deck should have 7 fewer cards");
    }

    @Test
    void shouldLeavePreviousHandCardsInDiscardAfterProfessorOak() {
        final Hand hand = new Hand();
        final EnergyCard discardCandidate = energyCard("old-card");
        hand.addCard(discardCandidate);

        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.professorOak().apply(runtime, null);

        assertTrue(discard.getCards().contains(discardCandidate),
                "Previously held card must be in discard after Professor Oak");
    }

    @Test
    void shouldKeepDeckSizeConsistentAfterDrawCards() {
        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.drawCards(1).apply(runtime, null);

        assertEquals(1, hand.size());
        assertEquals(DECK_SIZE - 1, deck.size());
        assertEquals(0, discard.size());
    }

    @Test
    void shouldAllowTrainerCardToBeBuiltWithEffect() {
        final TrainerEffect effect = TrainerEffect.drawCards(DRAW_TWO);
        final TrainerCard card = new TrainerCard.Builder("t-001", "Poké Ball", TrainerType.ITEM)
                .effect(effect)
                .build();

        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        card.getEffect().apply(runtime, null);

        assertEquals(DRAW_TWO, hand.size());
    }

    @Test
    void shouldReturnNullEffectWhenBuilderHasNoEffect() {
        final TrainerCard card = new TrainerCard.Builder("t-002", "Nothing", TrainerType.SUPPORTER)
                .build();

        assertEquals(null, card.getEffect(),
                "TrainerCard with no effect set should return null from getEffect()");
    }
}
