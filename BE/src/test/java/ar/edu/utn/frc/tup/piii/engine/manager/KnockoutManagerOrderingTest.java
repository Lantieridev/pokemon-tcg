package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for FR-012: the ordering invariant between status-damage application
 * and PhaseExited(BetweenTurnsPhase) event firing.
 *
 * <p>StatusEffectManager.processBetweenTurns() MUST apply status damage BEFORE
 * TurnManager fires PhaseExited(BetweenTurnsPhase). These tests verify that
 * KnockoutManager correctly detects (or ignores) a KO based on the damage counter
 * state at the moment the event arrives.</p>
 */
class KnockoutManagerOrderingTest {

    private static final int MAX_HP_90 = 90;
    private static final int COUNTERS_8 = 8;  // 80 damage < 90 HP → alive
    private static final int COUNTERS_1 = 1;  // +1 → 9 counters = 90 damage >= 90 HP → KO
    private static final int PLAYER_INDEX_0 = 0;

    private List<FakeBattlePokemonState> knockedOut;
    private KnockoutHandler handler;

    @BeforeEach
    void setUp() {
        knockedOut = new ArrayList<>();
        handler = (knocked, prizes) -> knockedOut.add((FakeBattlePokemonState) knocked);
    }

    @Test
    void shouldDetectKoWhenStatusDamageIsAppliedBeforePhaseExitedEventIsFired() {
        // Pokémon at 8 counters (80 damage < 90 HP — not yet KO)
        FakeBattlePokemonState pokemon = new FakeBattlePokemonState(
                MAX_HP_90, PokemonType.FIRE, null, null, false);
        pokemon.addDamageCounters(COUNTERS_8);

        FakeBattlefieldStateProvider provider =
                new FakeBattlefieldStateProvider(pokemon, null);
        KnockoutManager km = new KnockoutManager(provider, handler);

        // Simulate: status effect applies damage FIRST (as StatusEffectManager would)
        pokemon.addDamageCounters(COUNTERS_1); // now 9 counters = 90 damage >= 90 HP → KO

        // THEN the event fires
        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new BetweenTurnsPhase()));

        assertEquals(1, knockedOut.size(), "Handler must be called: KO was detected after damage applied");
    }

    @Test
    void shouldNotDetectKoWhenPhaseExitedEventFiresBeforeStatusDamageIsApplied() {
        // Pokémon at 8 counters (80 damage < 90 HP — not yet KO)
        FakeBattlePokemonState pokemon = new FakeBattlePokemonState(
                MAX_HP_90, PokemonType.FIRE, null, null, false);
        pokemon.addDamageCounters(COUNTERS_8);

        FakeBattlefieldStateProvider provider =
                new FakeBattlefieldStateProvider(pokemon, null);
        KnockoutManager km = new KnockoutManager(provider, handler);

        // Event fires FIRST — damage not yet applied → 80 < 90 → no KO
        km.on(new PhaseEvent.PhaseExited(PLAYER_INDEX_0, new BetweenTurnsPhase()));

        // THEN status damage is applied (too late — event already fired)
        pokemon.addDamageCounters(COUNTERS_1); // 90 damage, but handler already did not fire

        assertTrue(knockedOut.isEmpty(), "Handler must NOT be called: event fired before damage was applied");
    }
}
