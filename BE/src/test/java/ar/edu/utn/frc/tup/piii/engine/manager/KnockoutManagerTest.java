package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.model.AttackPhase;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for KnockoutManager — knockout detection on PhaseExited events. FR-008–FR-011.
 */
class KnockoutManagerTest {

    private static final int MAX_HP_100 = 100;
    private static final int MAX_HP_90 = 90;
    private static final int PLAYER_INDEX_0 = 0;
    private static final int DAMAGE_COUNTERS_10 = 10; // 10 * 10 = 100 >= 100 → KO
    private static final int DAMAGE_COUNTERS_9 = 9;   // 9 * 10 = 90 >= 90 → KO
    private static final int DAMAGE_COUNTERS_8 = 8;   // 8 * 10 = 80 < 90 → not KO
    private static final int DAMAGE_COUNTERS_5 = 5;   // 5 * 10 = 50 < 100 → not KO

    private List<FakeBattlePokemonState> knockedOut;
    private List<Integer> prizes;
    private KnockoutHandler handler;

    @BeforeEach
    void setUp() {
        knockedOut = new ArrayList<>();
        prizes = new ArrayList<>();
        handler = (knocked, p) -> {
            knockedOut.add((FakeBattlePokemonState) knocked);
            prizes.add(p);
        };
    }

    // --- constructor null-guard ---

    @Test
    void shouldThrowNullPointerExceptionWhenProviderIsNull() {
        assertThrows(NullPointerException.class,
                () -> new KnockoutManager(null, handler));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenHandlerIsNull() {
        FakeBattlefieldStateProvider provider =
                new FakeBattlefieldStateProvider(null, null);
        assertThrows(NullPointerException.class,
                () -> new KnockoutManager(provider, null));
    }

    // --- AttackPhase exited ---

    @Test
    void shouldDetectKoForPlayer0WhenAttackPhaseExitedAndPlayer0IsKnockedOut() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = aliveState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertEquals(1, knockedOut.size());
        assertEquals(p0, knockedOut.get(0));
        assertEquals(1, prizes.get(0));
    }

    @Test
    void shouldDetectKoForPlayer1WhenAttackPhaseExitedAndPlayer1IsKnockedOut() {
        FakeBattlePokemonState p0 = aliveState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertEquals(1, knockedOut.size());
        assertEquals(p1, knockedOut.get(0));
        assertEquals(1, prizes.get(0));
    }

    @Test
    void shouldDetectKoBothPlayersSimultaneouslyWhenAttackPhaseExitedAndBothAreKnockedOut() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertEquals(2, knockedOut.size());
        assertTrue(knockedOut.contains(p0));
        assertTrue(knockedOut.contains(p1));
    }

    @Test
    void shouldNotCallHandlerWhenAttackPhaseExitedAndNeitherPlayerIsKnockedOut() {
        FakeBattlePokemonState p0 = aliveState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = aliveState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    // --- BetweenTurnsPhase exited ---

    @Test
    void shouldDetectKoForPlayer0WhenBetweenTurnsPhaseExitedAndPlayer0IsKnockedOut() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = aliveState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new BetweenTurnsPhase()));

        assertEquals(1, knockedOut.size());
        assertEquals(p0, knockedOut.get(0));
    }

    @Test
    void shouldDetectKoForPlayer1WhenBetweenTurnsPhaseExitedAndPlayer1IsKnockedOut() {
        FakeBattlePokemonState p0 = aliveState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new BetweenTurnsPhase()));

        assertEquals(1, knockedOut.size());
        assertEquals(p1, knockedOut.get(0));
    }

    @Test
    void shouldNotCallHandlerWhenBetweenTurnsPhaseExitedAndNeitherPlayerIsKnockedOut() {
        FakeBattlePokemonState p0 = aliveState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = aliveState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new BetweenTurnsPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    // --- ignored events ---

    @Test
    void shouldIgnorePhaseEnteredEventWhenFired() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseEntered(PLAYER_INDEX_0, new AttackPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    @Test
    void shouldIgnoreTurnStartedEventWhenFired() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.TurnStarted(PLAYER_INDEX_0, new DrawPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    @Test
    void shouldIgnoreTurnEndedEventWhenFired() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.TurnEnded(PLAYER_INDEX_0, new BetweenTurnsPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    @Test
    void shouldIgnoreDrawPhaseExitedEventWhenFired() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new DrawPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    @Test
    void shouldIgnoreMainPhaseExitedEventWhenFired() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = koState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new MainPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    // --- prize calculation ---

    @Test
    void shouldPass2PrizesToHandlerWhenKnockedOutPokemonIsEx() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, true);
        FakeBattlePokemonState p1 = aliveState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertEquals(1, prizes.size());
        assertEquals(2, prizes.get(0));
    }

    @Test
    void shouldPass1PrizeToHandlerWhenKnockedOutPokemonIsNonEx() {
        FakeBattlePokemonState p0 = koState(MAX_HP_100, false);
        FakeBattlePokemonState p1 = aliveState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertEquals(1, prizes.size());
        assertEquals(1, prizes.get(0));
    }

    @Test
    void shouldNotCallHandlerWhenDamageCountersBelowKoThresholdWhenChecked() {
        FakeBattlePokemonState p0 = new FakeBattlePokemonState(MAX_HP_90, PokemonType.FIRE, null, null, false);
        p0.addDamageCounters(DAMAGE_COUNTERS_8); // 80 < 90 → not KO
        FakeBattlePokemonState p1 = aliveState(MAX_HP_100, false);
        KnockoutManager km = managerWith(p0, p1);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    // --- null active Pokémon ---

    @Test
    void shouldSkipPlayerWhenActivePokemonIsNullWhenCheckingBothPlayers() {
        FakeBattlefieldStateProvider provider = new FakeBattlefieldStateProvider(null, null);
        KnockoutManager km = new KnockoutManager(provider, handler);

        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new AttackPhase()));

        assertTrue(knockedOut.isEmpty());
    }

    // --- helpers ---

    private KnockoutManager managerWith(final FakeBattlePokemonState p0,
                                         final FakeBattlePokemonState p1) {
        FakeBattlefieldStateProvider provider = new FakeBattlefieldStateProvider(p0, p1);
        return new KnockoutManager(provider, handler);
    }

    private FakeBattlePokemonState koState(final int maxHp, final boolean ex) {
        FakeBattlePokemonState state = new FakeBattlePokemonState(maxHp, PokemonType.FIRE, null, null, ex);
        state.addDamageCounters(DAMAGE_COUNTERS_10); // 100 >= maxHp (100 or 90)
        return state;
    }

    private FakeBattlePokemonState aliveState(final int maxHp, final boolean ex) {
        FakeBattlePokemonState state = new FakeBattlePokemonState(maxHp, PokemonType.WATER, null, null, ex);
        state.addDamageCounters(DAMAGE_COUNTERS_5); // 50 < 100 → alive
        return state;
    }
}
