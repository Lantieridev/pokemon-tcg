package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.services.GameFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BloqueDTest {

    private AttackEffectResolver resolver;
    private FakeBattlePokemonState attacker;
    private FakeBattlePokemonState defender;
    private StatusEffectManager attackerSM;
    private StatusEffectManager defenderSM;
    private KnockoutHandler knockoutHandler;
    private AttackPipeline pipeline;

    @BeforeEach
    void setUp() {
        resolver = new AttackEffectResolver();
        attacker = new FakeBattlePokemonState(100, PokemonType.COLORLESS, null, null, false);
        defender = new FakeBattlePokemonState(100, PokemonType.COLORLESS, null, null, false);
        attackerSM = new StatusEffectManager(() -> true);
        defenderSM = new StatusEffectManager(() -> true);
        knockoutHandler = mock(KnockoutHandler.class);
        pipeline = new AttackPipeline(List.of(
                new ValidationStep(),
                new PreDamageEffectsStep(),
                new DamageCalculationStep(new DamageCalculator()),
                new DamageApplicationStep(),
                new PostDamageEffectsStep(resolver)
        ));
    }

    @Test
    void shouldResolveBloqueDTypes() {
        assertEquals(AttackEffectType.PLACE_COUNTERS_OPPONENT, resolver.resolveType("place_counters_opponent:1"));
        assertEquals(AttackEffectType.PLACE_COUNTERS_DISTRIBUTED, resolver.resolveType("place_counters_distributed:4"));
        assertEquals(AttackEffectType.MOVE_OPPONENT_COUNTERS, resolver.resolveType("move_opponent_counters"));
        assertEquals(AttackEffectType.DISCARD_OPPONENT_HAND_TO_LIMIT, resolver.resolveType("discard_opponent_hand_to_limit:4"));
        assertEquals(AttackEffectType.PLACE_OPPONENT_BASIC_FROM_DISCARD, resolver.resolveType("place_opponent_basic_from_discard"));
        assertEquals(AttackEffectType.DISCARD_TRAINER_FROM_OPPONENT_HAND, resolver.resolveType("discard_trainer_from_opponent_hand"));
        assertEquals(AttackEffectType.SHUFFLE_POKEMON_FROM_DISCARD, resolver.resolveType("shuffle_pokemon_from_discard:3"));
    }

    @Test
    void shouldPlaceCountersOpponentDirectly() {
        final Attack attack = new Attack("Sneaky Placement", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("place_counters_opponent:2")
                .build();

        pipeline.execute(ctx);

        // Places 2 damage counters directly
        assertEquals(2, defender.getDamageCounters());
    }

    @Test
    void shouldDiscardOpponentHandToLimit() {
        final PlayerRuntime defenderRuntime = mock(PlayerRuntime.class);
        final Hand hand = new Hand();
        final DiscardPile discard = new DiscardPile();

        Card c1 = mock(Card.class); when(c1.getCardId()).thenReturn("c1");
        Card c2 = mock(Card.class); when(c2.getCardId()).thenReturn("c2");
        Card c3 = mock(Card.class); when(c3.getCardId()).thenReturn("c3");
        Card c4 = mock(Card.class); when(c4.getCardId()).thenReturn("c4");
        Card c5 = mock(Card.class); when(c5.getCardId()).thenReturn("c5");
        Card c6 = mock(Card.class); when(c6.getCardId()).thenReturn("c6");

        hand.addCard(c1);
        hand.addCard(c2);
        hand.addCard(c3);
        hand.addCard(c4);
        hand.addCard(c5);
        hand.addCard(c6);

        when(defenderRuntime.getHand()).thenReturn(hand);
        when(defenderRuntime.getDiscardPile()).thenReturn(discard);

        final Attack attack = new Attack("Chip Off", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .defenderRuntime(defenderRuntime)
                .effectText("discard_opponent_hand_to_limit:4")
                .build();

        pipeline.execute(ctx);

        // 6 cards initially, limit 4 -> should discard 2 cards
        assertEquals(4, hand.size());
        assertEquals(2, discard.getCards().size());
    }

    @Test
    void shouldPlaceOpponentBasicFromDiscard() {
        final PlayerRuntime defenderRuntime = mock(PlayerRuntime.class);
        final DiscardPile discard = new DiscardPile();
        final Bench bench = new Bench();

        PokemonCard basic = mock(PokemonCard.class);
        when(basic.getCardId()).thenReturn("basic-1");
        when(basic.getEvolutionStage()).thenReturn(EvolutionStage.BASIC);
        discard.add(basic);

        when(defenderRuntime.getDiscardPile()).thenReturn(discard);
        when(defenderRuntime.getBench()).thenReturn(bench);

        final Attack attack = new Attack("Revival", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .defenderRuntime(defenderRuntime)
                .effectText("place_opponent_basic_from_discard")
                .build();

        pipeline.execute(ctx);

        // Bench should have 1 pokemon, discard should be empty
        assertEquals(1, bench.getAll().size());
        assertEquals(0, discard.getCards().size());
        assertEquals("basic-1", bench.getAll().get(0).getBaseCard().getCardId());
    }

    @Test
    void shouldRequestSelectionWhenMultipleOpponentBasicInDiscard() {
        final PlayerRuntime defenderRuntime = mock(PlayerRuntime.class);
        final DiscardPile discard = new DiscardPile();
        final Bench bench = new Bench();
        final MatchSession mockSession = mock(MatchSession.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);

        PokemonCard basic1 = mock(PokemonCard.class);
        when(basic1.getCardId()).thenReturn("basic-1");
        when(basic1.getEvolutionStage()).thenReturn(EvolutionStage.BASIC);
        PokemonCard basic2 = mock(PokemonCard.class);
        when(basic2.getCardId()).thenReturn("basic-2");
        when(basic2.getEvolutionStage()).thenReturn(EvolutionStage.BASIC);

        discard.add(basic1);
        discard.add(basic2);

        when(defenderRuntime.getDiscardPile()).thenReturn(discard);
        when(defenderRuntime.getBench()).thenReturn(bench);
        when(mockSession.getTurnManager()).thenReturn(turnManager);

        final Attack attack = new Attack("Revival", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .defenderRuntime(defenderRuntime)
                .effectText("place_opponent_basic_from_discard")
                .matchSession(mockSession)
                .build();

        pipeline.execute(ctx);

        verify(mockSession).setPendingSelectionRequest(argThat(req -> 
            req.sourceEffect() == TrainerEffectId.REVIVAL &&
            req.maxSelections() == 1 &&
            req.source() == SelectionSource.DISCARD_PILE
        ));
        verify(turnManager).interruptMainPhase();
    }

    @Test
    void shouldShufflePokemonFromDiscardAutomatically() {
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final DiscardPile discard = new DiscardPile();
        final Deck deck = mock(Deck.class);

        PokemonCard p1 = mock(PokemonCard.class);
        when(p1.getCardId()).thenReturn("p1");
        EnergyCard e1 = mock(EnergyCard.class);
        when(e1.getCardId()).thenReturn("e1");
        PokemonCard p2 = mock(PokemonCard.class);
        when(p2.getCardId()).thenReturn("p2");

        discard.add(p1);
        discard.add(e1);
        discard.add(p2);

        when(attackerRuntime.getDiscardPile()).thenReturn(discard);
        when(attackerRuntime.getDeck()).thenReturn(deck);

        final Attack attack = new Attack("Rescue", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .attackerRuntime(attackerRuntime)
                .effectText("shuffle_pokemon_from_discard:3")
                .build();

        pipeline.execute(ctx);

        // Discard should have only e1 remaining, since both Pokemon cards (p1, p2) should have been moved.
        assertEquals(1, discard.getCards().size());
        assertTrue(discard.getCards().contains(e1));

        // The mock deck should have received the two pokemon cards and shuffled
        verify(deck).addCards(argThat(list -> list.contains(p1) && list.contains(p2) && list.size() == 2));
        verify(deck).shuffle();
    }

    @Test
    void testStompOff() {
        final PlayerRuntime opponentRuntime = mock(PlayerRuntime.class);
        final DiscardPile discard = new DiscardPile();
        final Deck deck = mock(Deck.class);

        Card topCard = mock(Card.class);
        when(topCard.getCardId()).thenReturn("top-card");
        when(deck.isEmpty()).thenReturn(false);
        when(deck.draw()).thenReturn(topCard);

        when(opponentRuntime.getDiscardPile()).thenReturn(discard);
        when(opponentRuntime.getDeck()).thenReturn(deck);

        final Attack attack = new Attack("Stomp Off", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .defenderRuntime(opponentRuntime)
                .effectText("discard_opponent_deck:1")
                .build();

        pipeline.execute(ctx);

        verify(deck, times(1)).draw();
        assertEquals(1, discard.getCards().size());
        assertTrue(discard.getCards().contains(topCard));
    }

    @Test
    void testFangSnipeWithTrainers() {
        final PlayerRuntime opponentRuntime = mock(PlayerRuntime.class);
        final Hand hand = mock(Hand.class);
        final MatchSession mockSession = mock(MatchSession.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);
        
        TrainerCard tc = mock(TrainerCard.class);
        when(opponentRuntime.getHand()).thenReturn(hand);
        when(hand.getCards()).thenReturn(List.of(tc));

        final Attack attack = new Attack("Fang Snipe", 40, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .defenderRuntime(opponentRuntime)
                .effectText("discard_trainer_from_opponent_hand")
                .matchSession(mockSession)
                .build();

        when(mockSession.getTurnManager()).thenReturn(turnManager);

        pipeline.execute(ctx);

        verify(mockSession).setPendingSelectionRequest(argThat(req -> 
            req.sourceEffect() == TrainerEffectId.FANG_SNIPE &&
            req.maxSelections() == 1
        ));
        verify(turnManager).interruptMainPhase();
    }

    @Test
    void testFangSnipeWithoutTrainers() {
        final PlayerRuntime opponentRuntime = mock(PlayerRuntime.class);
        final Hand hand = mock(Hand.class);
        final MatchSession mockSession = mock(MatchSession.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);
        
        Card pc = mock(PokemonCard.class);
        when(opponentRuntime.getHand()).thenReturn(hand);
        when(hand.getCards()).thenReturn(List.of(pc));

        final Attack attack = new Attack("Fang Snipe", 40, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .defenderRuntime(opponentRuntime)
                .effectText("discard_trainer_from_opponent_hand")
                .matchSession(mockSession)
                .build();

        when(mockSession.getTurnManager()).thenReturn(turnManager);

        pipeline.execute(ctx);

        verify(mockSession).setPendingSelectionRequest(argThat(req -> 
            req.sourceEffect() == TrainerEffectId.FANG_SNIPE &&
            req.maxSelections() == 0
        ));
        verify(turnManager).interruptMainPhase();
    }

    @Test
    void testCursedDropSelectionSource() {
        final MatchSession mockSession = mock(MatchSession.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);
        when(mockSession.getTurnManager()).thenReturn(turnManager);

        final Attack attack = new Attack("Cursed Drop", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("place_counters_distributed:4")
                .matchSession(mockSession)
                .build();

        pipeline.execute(ctx);

        verify(mockSession).setPendingSelectionRequest(argThat(req -> 
            req.sourceEffect() == TrainerEffectId.CURSED_DROP &&
            req.maxSelections() == 4 &&
            req.source() == SelectionSource.OPPONENT_FIELD
        ));
        verify(turnManager).interruptMainPhase();
    }
}

