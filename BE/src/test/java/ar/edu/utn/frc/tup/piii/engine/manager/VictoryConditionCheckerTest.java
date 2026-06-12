package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeDeckStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePrizeStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for VictoryConditionChecker. FR-016 through FR-021.
 */
class VictoryConditionCheckerTest {

    private static final int PLAYER_0 = 0;
    private static final int PLAYER_1 = 1;
    private static final int PRIZES_ZERO = 0;
    private static final int PRIZES_TWO = 2;
    private static final int BENCH_ZERO = 0;
    private static final int DECK_ZERO = 0;
    private static final int PRIZES_TO_TAKE = 1;
    private static final int MAX_HP = 100;

    private FakePrizeStateProvider prizeProvider;
    private FakeDeckStateProvider deckProvider;
    private FakeBenchStateProvider benchProvider;
    private FakeBattlefieldStateProvider battlefieldProvider;
    private List<VictoryResult> captured;
    private VictoryConditionChecker checker;

    @BeforeEach
    void setUp() {
        prizeProvider = new FakePrizeStateProvider();
        deckProvider = new FakeDeckStateProvider();
        benchProvider = new FakeBenchStateProvider();
        battlefieldProvider = new FakeBattlefieldStateProvider(null, null);
        captured = new ArrayList<>();
        checker = new VictoryConditionChecker(
                prizeProvider,
                deckProvider,
                benchProvider,
                battlefieldProvider,
                result -> captured.add(result));
        checker.setInitialPlacementComplete(true, true);
    }

    // --- 6.1 constructor null-guard ---

    @Test
    void shouldThrowNullPointerExceptionWhenPrizeProviderIsNull() {
        assertThrows(NullPointerException.class, () ->
                new VictoryConditionChecker(null, deckProvider, benchProvider,
                        battlefieldProvider, result -> captured.add(result)));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenDeckProviderIsNull() {
        assertThrows(NullPointerException.class, () ->
                new VictoryConditionChecker(prizeProvider, null, benchProvider,
                        battlefieldProvider, result -> captured.add(result)));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBenchProviderIsNull() {
        assertThrows(NullPointerException.class, () ->
                new VictoryConditionChecker(prizeProvider, deckProvider, null,
                        battlefieldProvider, result -> captured.add(result)));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBattlefieldProviderIsNull() {
        assertThrows(NullPointerException.class, () ->
                new VictoryConditionChecker(prizeProvider, deckProvider, benchProvider,
                        null, result -> captured.add(result)));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenVictoryHandlerIsNull() {
        assertThrows(NullPointerException.class, () ->
                new VictoryConditionChecker(prizeProvider, deckProvider, benchProvider,
                        battlefieldProvider, null));
    }

    @Test
    void shouldImplementBothKnockoutHandlerAndPhaseListenerWhenConstructed() {
        assertInstanceOf(KnockoutHandler.class, checker);
        assertInstanceOf(PhaseListener.class, checker);
    }

    // --- 6.2 TurnStarted tracking ---

    @Test
    void shouldSetActivePlayerIndexToZeroWhenTurnStartedIsReceivedForPlayerZero() {
        // Send TurnStarted for player 0; prize check triggers prize victory for player 0
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
        assertEquals(PLAYER_0, ((VictoryResult.PrizeVictory) captured.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldUpdateActivePlayerIndexWhenSubsequentTurnStartedIsReceived() {
        // First turn for player 0, then turn for player 1
        prizeProvider.set(PLAYER_0, PRIZES_TWO); // player 0 no prize victory
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));
        // Now switch to player 1
        prizeProvider.set(PLAYER_1, PRIZES_ZERO); // player 1 takes last prize → wins
        checker.on(new PhaseEvent.TurnStarted(PLAYER_1, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
        assertEquals(PLAYER_1, ((VictoryResult.PrizeVictory) captured.get(0)).winnerPlayerIndex());
    }

    // --- 6.3 Prize victory ---

    @Test
    void shouldFirePrizeVictoryWhenLastPrizeIsTakenOnKnockout() {
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
        assertEquals(PLAYER_0, ((VictoryResult.PrizeVictory) captured.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldNotInvokeVictoryHandlerWhenPrizesRemainAfterKnockout() {
        prizeProvider.set(PLAYER_0, PRIZES_TWO);
        benchProvider.set(PLAYER_1, PRIZES_TWO); // bench not empty either
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertTrue(captured.isEmpty());
    }

    // --- 6.4 Bench-out ---

    @Test
    void shouldFireBenchOutVictoryWhenDefenderBenchIsEmptyAfterKnockout() {
        prizeProvider.set(PLAYER_0, PRIZES_TWO); // no prize victory
        benchProvider.set(PLAYER_1, BENCH_ZERO); // defender has no bench
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.BenchOutVictory.class, captured.get(0));
        assertEquals(PLAYER_0, ((VictoryResult.BenchOutVictory) captured.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldFirePrizeVictoryAndNotBenchOutWhenBothConditionsAreTrueSimultaneously() {
        prizeProvider.set(PLAYER_0, PRIZES_ZERO); // prize victory condition met
        benchProvider.set(PLAYER_1, BENCH_ZERO);  // bench-out condition also met
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size()); // only one victory fires
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
    }

    // --- 6.5 Deck-out ---

    @Test
    void shouldFireDeckOutVictoryWhenDrawingPlayerHasEmptyDeck() {
        deckProvider.set(PLAYER_1, DECK_ZERO);

        checker.on(new PhaseEvent.PhaseEntered(PLAYER_1, new DrawPhase()));

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.DeckOutVictory.class, captured.get(0));
        assertEquals(PLAYER_0, ((VictoryResult.DeckOutVictory) captured.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldNotInvokeVictoryHandlerWhenDeckIsNotEmpty() {
        // deckProvider default is 60 for any player
        checker.on(new PhaseEvent.PhaseEntered(PLAYER_1, new DrawPhase()));

        assertTrue(captured.isEmpty());
    }

    // --- 6.6 Latch ---

    @Test
    void shouldNotFireVictoryAgainWhenLatchIsAlreadySetAfterPrizeVictory() {
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        benchProvider.set(PLAYER_1, BENCH_ZERO);
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE); // fires PrizeVictory, latch set

        // Now bench-out condition — latch should prevent second firing
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size()); // still only one
    }

    @Test
    void shouldNotInvokeVictoryHandlerWithNegativeOneIndexWhenNoTurnStartedReceived() {
        // activePlayerIndex = -1 (no TurnStarted sent), guard must prevent KO processing
        // even when prize condition would otherwise fire for player 0
        prizeProvider.set(PLAYER_0, PRIZES_ZERO); // player 0 has 0 prizes remaining

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE); // no TurnStarted sent

        assertTrue(captured.isEmpty()); // guard on -1 prevents invocation
    }

    // --- 7.1 Integration ordering invariant ---

    @Test
    void shouldCheckPrizeBeforeBenchOutWhenBothConditionsMeetInSameKoEvent() {
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);  // prize victory condition
        benchProvider.set(PLAYER_1, BENCH_ZERO);   // bench-out condition
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
    }

    // --- 7.2 Deck-out before TurnStarted ---

    @Test
    void shouldFireDeckOutBeforeTurnStartedIsReceivedWhenDeckIsEmpty() {
        // Deck-out is independent of activePlayerIndex — fires even without TurnStarted
        deckProvider.set(PLAYER_0, DECK_ZERO);

        checker.on(new PhaseEvent.PhaseEntered(PLAYER_0, new DrawPhase()));

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.DeckOutVictory.class, captured.get(0));
        assertEquals(PLAYER_1, ((VictoryResult.DeckOutVictory) captured.get(0)).winnerPlayerIndex());
    }

    // --- 6.7 Bilateral bench-out (Bug-5) ---

    @Test
    void shouldEmitSuddenDeathWhenBothPlayersHaveEmptyBenchAfterSimultaneousKO() {
        // No prize victory for attacker; both benches are empty
        prizeProvider.set(PLAYER_0, PRIZES_TWO);
        prizeProvider.set(PLAYER_1, PRIZES_TWO);
        benchProvider.set(PLAYER_0, BENCH_ZERO);
        benchProvider.set(PLAYER_1, BENCH_ZERO);
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        final FakeBattlePokemonState knocked =
                new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.SuddenDeath.class, captured.get(0));
    }

    @Test
    void shouldDetectBenchOutVictoryForOpponentWhenAttackerDiesFromPoisonWithEmptyBench() {
        // Player 1 is the "attacker" (active player) — but Player 0 (the defender/owner
        // of the knocked-out Pokémon) has an empty bench; Player 1 wins
        prizeProvider.set(PLAYER_1, PRIZES_TWO); // no prize victory
        benchProvider.set(PLAYER_0, BENCH_ZERO); // KO'd player has no bench left
        benchProvider.set(PLAYER_1, PRIZES_TWO); // attacker has bench (not a SuddenDeath)
        checker.on(new PhaseEvent.TurnStarted(PLAYER_1, new DrawPhase()));

        final FakeBattlePokemonState knocked =
                new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.BenchOutVictory.class, captured.get(0));
        // Player 1 (the attacker) wins because Player 0's bench is empty
        assertEquals(PLAYER_1, ((VictoryResult.BenchOutVictory) captured.get(0)).winnerPlayerIndex());
    }

    // --- 6.8 Defender prize victory (BLOCKER-1) ---

    @Test
    void shouldFirePrizeVictoryForDefenderWhenDefenderReachesZeroPrizesAlone() {
        // attacker (player 0) still has prizes; defender (player 1) has 0 prizes remaining
        prizeProvider.set(PLAYER_0, PRIZES_TWO);
        prizeProvider.set(PLAYER_1, PRIZES_ZERO);
        benchProvider.set(PLAYER_0, PRIZES_TWO);  // bench non-empty — no SuddenDeath via bench
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        final FakeBattlePokemonState knocked =
                new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
        assertEquals(PLAYER_1, ((VictoryResult.PrizeVictory) captured.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldNotFireSuddenDeathWhenOnlyDefenderReachesZeroPrizes() {
        // Only defender has 0 prizes — must NOT be SuddenDeath
        prizeProvider.set(PLAYER_0, PRIZES_TWO);
        prizeProvider.set(PLAYER_1, PRIZES_ZERO);
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        final FakeBattlePokemonState knocked =
                new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        org.junit.jupiter.api.Assertions.assertFalse(
                captured.get(0) instanceof VictoryResult.SuddenDeath,
                "Should be PrizeVictory for defender, not SuddenDeath");
    }

    // --- ignored events ---

    @Test
    void shouldIgnoreMainPhaseEnteredEventWhenFired() {
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));
        checker.on(new PhaseEvent.PhaseEntered(PLAYER_0, new MainPhase()));

        assertTrue(captured.isEmpty());
    }

    @Test
    void shouldNotBenchOutAttackerIfAttackerHasNoBenchButActivePokemonIsAlive() {
        prizeProvider.set(PLAYER_0, PRIZES_TWO);
        prizeProvider.set(PLAYER_1, PRIZES_TWO);

        benchProvider.set(PLAYER_0, BENCH_ZERO);
        benchProvider.set(PLAYER_1, PRIZES_TWO);

        FakeBattlePokemonState active0 = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState active1 = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        FakeBattlefieldStateProvider localBattlefieldProvider = new FakeBattlefieldStateProvider(active0, active1);

        VictoryConditionChecker localChecker = new VictoryConditionChecker(
                prizeProvider,
                deckProvider,
                benchProvider,
                localBattlefieldProvider,
                result -> captured.add(result));

        localChecker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        localChecker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertTrue(captured.isEmpty());
    }

    @Test
    void shouldFireVictoryWhenCheckFieldVictoryIsCalledAndActiveAndBenchAreBothEmpty() {
        FakeBattlefieldStateProvider localBattlefieldProvider = new FakeBattlefieldStateProvider(null,
                new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false));
        benchProvider.set(PLAYER_0, BENCH_ZERO);
        benchProvider.set(PLAYER_1, PRIZES_TWO);
        
        VictoryConditionChecker localChecker = new VictoryConditionChecker(
                prizeProvider,
                deckProvider,
                benchProvider,
                localBattlefieldProvider,
                result -> captured.add(result));
        localChecker.setInitialPlacementComplete(true, true);
        
        localChecker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));
        localChecker.checkFieldVictory();
        
        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.BenchOutVictory.class, captured.get(0));
        assertEquals(PLAYER_1, ((VictoryResult.BenchOutVictory) captured.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldNotFireVictoryWhenCheckFieldVictoryIsCalledDuringSetupPhase() {
        FakeBattlefieldStateProvider localBattlefieldProvider = new FakeBattlefieldStateProvider(null,
                new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false));
        benchProvider.set(PLAYER_0, BENCH_ZERO);
        benchProvider.set(PLAYER_1, PRIZES_TWO);
        
        VictoryConditionChecker localChecker = new VictoryConditionChecker(
                prizeProvider,
                deckProvider,
                benchProvider,
                localBattlefieldProvider,
                result -> captured.add(result));
        localChecker.setInitialPlacementComplete(false, false);
        
        localChecker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));
        localChecker.checkFieldVictory();
        
        assertTrue(captured.isEmpty());
    }

    @Test
    void shouldEmitSuddenDeathWhenBothPlayersTakeLastPrizeMutually() {
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        prizeProvider.set(PLAYER_1, PRIZES_ZERO);
        benchProvider.set(PLAYER_0, PRIZES_TWO);
        benchProvider.set(PLAYER_1, PRIZES_TWO);
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.SuddenDeath.class, captured.get(0));
    }

    @Test
    void shouldEmitPrizeVictoryForPlayerWithMoreConditionsMet() {
        // Player 0 takes last prize (meets Prize Victory condition)
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        // Player 1 still has prizes remaining (meets 0 prize conditions)
        prizeProvider.set(PLAYER_1, PRIZES_TWO);

        // BOTH players end up with no Pokémon on the board (both meet the other player's bench-out condition)
        // Player 0 gets: 1 (prizes) + 1 (bench-out player 1) = 2 conditions
        // Player 1 gets: 0 (prizes) + 1 (bench-out player 0) = 1 condition
        // Therefore, Player 0 should be declared the winner directly, without a Sudden Death tiebreaker.
        benchProvider.set(PLAYER_0, BENCH_ZERO);
        benchProvider.set(PLAYER_1, BENCH_ZERO);
        
        FakeBattlefieldStateProvider localBattlefieldProvider = new FakeBattlefieldStateProvider(null, null);
        VictoryConditionChecker localChecker = new VictoryConditionChecker(
                prizeProvider,
                deckProvider,
                benchProvider,
                localBattlefieldProvider,
                result -> captured.add(result));
        localChecker.setInitialPlacementComplete(true, true);
        localChecker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        localChecker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
        assertEquals(PLAYER_0, ((VictoryResult.PrizeVictory) captured.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldEmitSuddenDeathWhenBothPlayersMeetEqualNumberOfConditions() {
        // BOTH players take last prize (1 condition each)
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        prizeProvider.set(PLAYER_1, PRIZES_ZERO);
        
        // BOTH players end up with empty benches (1 condition each)
        // Total conditions met: Player 0 = 2, Player 1 = 2
        // Equal conditions met -> Sudden Death tiebreaker.
        benchProvider.set(PLAYER_0, BENCH_ZERO);
        benchProvider.set(PLAYER_1, BENCH_ZERO);

        FakeBattlefieldStateProvider localBattlefieldProvider = new FakeBattlefieldStateProvider(null, null);
        VictoryConditionChecker localChecker = new VictoryConditionChecker(
                prizeProvider,
                deckProvider,
                benchProvider,
                localBattlefieldProvider,
                result -> captured.add(result));
        localChecker.setInitialPlacementComplete(true, true);
        localChecker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        localChecker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.SuddenDeath.class, captured.get(0));
    }

    @Test
    void shouldDeferVictoryEvaluationUntilAllPendingKnockoutsAreProcessed() {
        // Player 0 took last prize card
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        prizeProvider.set(PLAYER_1, PRIZES_TWO);
        benchProvider.set(PLAYER_0, PRIZES_TWO);
        benchProvider.set(PLAYER_1, PRIZES_TWO);

        // Put active pokemon for both players. Player 1 active has lethal damage (100 damage)
        FakeBattlePokemonState p0Active = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState p1Active = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        p1Active.addDamageCounters(10); // 10 counters * 10 = 100 damage (lethal)
        
        FakeBattlefieldStateProvider localBattlefieldProvider = new FakeBattlefieldStateProvider(p0Active, p1Active);
        
        VictoryConditionChecker localChecker = new VictoryConditionChecker(
                prizeProvider,
                deckProvider,
                benchProvider,
                localBattlefieldProvider,
                result -> captured.add(result));
        localChecker.setInitialPlacementComplete(true, true);
        localChecker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        // We process KO for some other pokemon (e.g. a benched pokemon), while active pokemon has pending lethal damage
        FakeBattlePokemonState someBenched = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        localChecker.onKnockout(someBenched, PRIZES_TO_TAKE);

        // Since p1Active still has pending lethal damage, victory check should be deferred
        assertTrue(captured.isEmpty());

        // Now remove the damage from p1Active (as if it was processed and removed)
        p1Active.setDamageCounters(0);
        localChecker.onKnockout(p1Active, PRIZES_TO_TAKE);

        // Now that there are no pending knockouts, victory should fire
        assertEquals(1, captured.size());
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
        assertEquals(PLAYER_0, ((VictoryResult.PrizeVictory) captured.get(0)).winnerPlayerIndex());
    }
}
