package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BloqueBTest {

    private AttackEffectResolver resolver;
    private StatusEffectManager attackerSM;
    private StatusEffectManager defenderSM;
    private BattlePokemonState attacker;
    private BattlePokemonState defender;

    @BeforeEach
    void setUp() {
        resolver = new AttackEffectResolver();
        attackerSM = new StatusEffectManager(() -> true);
        defenderSM = new StatusEffectManager(() -> true);
        attacker = mock(BattlePokemonState.class);
        defender = mock(BattlePokemonState.class);
    }

    @Test
    void shouldResolveHardenAndSwitchOpponentType() {
        assertEquals(AttackEffectType.PREVENT_DAMAGE_60_OR_LESS, resolver.resolveType("prevent_damage_60_or_less"));
        assertEquals(AttackEffectType.FORCE_SWITCH_OPPONENT, resolver.resolveType("force_switch_opponent"));
        assertEquals(AttackEffectType.BENCH_DAMAGE_ONE, resolver.resolveType("bench_damage_one:20"));
    }

    @Test
    void shouldExecuteForceSwitchOpponentOnly() {
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final PlayerRuntime defenderRuntime = mock(PlayerRuntime.class);

        final Bench defenderBench = mock(Bench.class);
        final BattlePokemonState activeDefender = mock(BattlePokemonState.class);
        final BattlePokemonState benchDefender = mock(BattlePokemonState.class);
        final StatusEffectManager defenderSM = mock(StatusEffectManager.class);

        when(defenderRuntime.getBench()).thenReturn(defenderBench);
        when(defenderBench.getAll()).thenReturn(List.of(benchDefender));
        when(defenderRuntime.getActivePokemon()).thenReturn(activeDefender);
        when(defenderBench.promote(0)).thenReturn(benchDefender);
        when(defenderRuntime.getStatusEffectManager()).thenReturn(defenderSM);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender,
                new Attack("Push Down", 20, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> true)
                .attackerRuntime(attackerRuntime)
                .defenderRuntime(defenderRuntime)
                .effectText("force_switch_opponent")
                .build();

        resolver.apply(ctx);

        // Verifies defender switches, but attacker does not switch (attackerRuntime.getBench() is never called)
        verify(defenderRuntime, atLeastOnce()).getBench();
        verify(attackerRuntime, never()).getBench();
    }

    @Test
    void shouldExecuteBenchDamageOneDirectlyIfOnlyOneBenched() {
        final PlayerRuntime defenderRuntime = mock(PlayerRuntime.class);
        final Bench defenderBench = mock(Bench.class);
        final BattlePokemonState benched = mock(BattlePokemonState.class);

        when(defenderRuntime.getBench()).thenReturn(defenderBench);
        when(defenderBench.getAll()).thenReturn(List.of(benched));
        when(benched.getCardId()).thenReturn("benched-001");
        when(benched.getMaxHp()).thenReturn(60);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender,
                new Attack("Overrun", 20, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> true)
                .defenderRuntime(defenderRuntime)
                .effectText("bench_damage_one:20")
                .build();

        resolver.apply(ctx);

        // 20 damage = 2 counters applied directly because size is 1
        verify(benched, times(1)).addDamageCounters(2);
    }
}
