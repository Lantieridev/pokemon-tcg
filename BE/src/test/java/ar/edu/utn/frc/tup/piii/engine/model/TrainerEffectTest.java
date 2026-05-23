package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
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

    // -----------------------------------------------------------------------
    // Roller Skates (xy1-114)
    // -----------------------------------------------------------------------

    @Test
    void shouldDrawThreeCardsWhenRollerSkatesFlipsHeads() {
        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.rollerSkates(() -> true).apply(runtime, null); // always heads

        assertEquals(3, hand.size(), "Roller Skates heads → draw 3");
        assertEquals(DECK_SIZE - 3, deck.size());
    }

    @Test
    void shouldDrawNothingWhenRollerSkatesFlipsTails() {
        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.rollerSkates(() -> false).apply(runtime, null); // always tails

        assertEquals(0, hand.size(), "Roller Skates tails → draw nothing");
        assertEquals(DECK_SIZE, deck.size());
    }

    // -----------------------------------------------------------------------
    // Shauna (xy1-127)
    // -----------------------------------------------------------------------

    @Test
    void shouldShuffleHandIntoDeckAndDrawFiveWhenShaunaApplied() {
        final Hand hand = new Hand();
        hand.addCard(energyCard("hand-1"));
        hand.addCard(energyCard("hand-2"));
        final int originalHandSize = hand.size();

        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.shauna().apply(runtime, null);

        assertEquals(5, hand.size(), "Hand should have exactly 5 cards after Shauna");
        assertEquals(DECK_SIZE + originalHandSize - 5, deck.size(),
                "Deck should contain original cards plus previous hand minus the 5 drawn");
        assertEquals(0, discard.size(), "Shauna does not discard — it recycles hand into deck");
    }

    @Test
    void shouldEmptyHandBeforeDrawingWhenShaunaApplied() {
        final Hand hand = new Hand();
        hand.addCard(energyCard("old-1"));
        hand.addCard(energyCard("old-2"));
        hand.addCard(energyCard("old-3"));

        final Deck deck = buildDeck(10);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.shauna().apply(runtime, null);

        // Hand had 3, deck had 10 → deck becomes 13, draw 5 → deck ends at 8, hand at 5
        assertEquals(5, hand.size());
        assertEquals(8, deck.size());
    }

    // -----------------------------------------------------------------------
    // Super Potion (xy1-128)
    // -----------------------------------------------------------------------

    @Test
    void shouldHealSixtyAndDiscardOneEnergyWhenSuperPotionApplied() {
        final FakeBattlePokemonState target =
                new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        target.addDamageCounters(8); // 80 damage
        target.addAttachedEnergy(PokemonType.FIRE);
        target.addAttachedEnergy(PokemonType.FIRE);

        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.superPotion().apply(runtime, target);

        // heal 60 → 80 - 60 = 20 → 2 counters
        assertEquals(2, target.getDamageCounters(), "Super Potion should heal 60 damage (6 counters)");
        // discard 1 energy → 1 remaining
        assertEquals(1, target.getAttachedEnergies().size(), "Super Potion should discard 1 energy");
    }

    @Test
    void shouldNotApplySuperPotionWhenTargetHasNoDamage() {
        final FakeBattlePokemonState target =
                new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        // no damage counters
        target.addAttachedEnergy(PokemonType.FIRE);

        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.superPotion().apply(runtime, target);

        assertEquals(0, target.getDamageCounters(), "No healing when Pokémon is at full HP");
        assertEquals(1, target.getAttachedEnergies().size(), "No energy discarded when Pokémon is at full HP");
    }

    @Test
    void shouldBeNoOpWhenSuperPotionTargetIsNull() {
        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        // Must not throw
        TrainerEffect.superPotion().apply(runtime, null);

        assertEquals(DECK_SIZE, deck.size());
    }

    // -----------------------------------------------------------------------
    // Heal 30 factory (existing, extended coverage)
    // -----------------------------------------------------------------------

    @Test
    void shouldHealThirtyDamageFromTarget() {
        final FakeBattlePokemonState target =
                new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        target.addDamageCounters(5); // 50 damage

        final Hand hand = new Hand();
        final Deck deck = buildDeck(DECK_SIZE);
        final DiscardPile discard = new DiscardPile();
        final PlayerRuntime runtime = buildRuntime(hand, deck, discard);

        TrainerEffect.healDamage(30).apply(runtime, target);

        // heal 30 → 50 - 30 = 20 → 2 counters
        assertEquals(2, target.getDamageCounters());
    }
}
