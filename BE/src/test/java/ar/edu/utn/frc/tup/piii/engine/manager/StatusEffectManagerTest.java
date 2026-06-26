package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidStatusEffectException;
import ar.edu.utn.frc.tup.piii.engine.exception.PokemonAsleepException;
import ar.edu.utn.frc.tup.piii.engine.exception.PokemonParalyzedException;
import ar.edu.utn.frc.tup.piii.engine.model.AttackModifierResult;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for StatusEffectManager — all sub-batches in one file. FR-011 through FR-017.
 */
class StatusEffectManagerTest {

    private CoinFlipper coinFlipper;
    private StatusEffectManager manager;
    private FakeActivePokemonState fakeState;

    @BeforeEach
    void setUp() {
        coinFlipper = Mockito.mock(CoinFlipper.class);
        manager = new StatusEffectManager(coinFlipper);
        fakeState = new FakeActivePokemonState();
    }

    // -------------------------------------------------------------------------
    // Sub-batch 5.1 — apply / hasEffect / activeEffects / clearAll / remove
    // -------------------------------------------------------------------------

    @Test
    void shouldApplySingleEffectSuccessfully() {
        manager.apply(StatusEffectType.DORMIDO);
        assertTrue(manager.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldNotApplyAsleepWhenProtectedBySweetVeilWithFairyEnergy() {
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime mockRuntime = Mockito.mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        when(mockRuntime.hasAbility(ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SWEET_VEIL)).thenReturn(true);
        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState mockActive = Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState.class);
        ar.edu.utn.frc.tup.piii.engine.model.EnergyCard fairyEnergy = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("xy1-fairy", "Fairy Energy", ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FAIRY, true);
        when(mockActive.getAttachedEnergyCards()).thenReturn(java.util.List.of(fairyEnergy));
        when(mockRuntime.getActivePokemon()).thenReturn(mockActive);
        manager.setPlayerRuntime(mockRuntime);

        manager.apply(StatusEffectType.DORMIDO);
        assertFalse(manager.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldNotApplyOtherEffectsWhenProtectedBySweetVeilWithFairyEnergy() {
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime mockRuntime = Mockito.mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        when(mockRuntime.hasAbility(ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SWEET_VEIL)).thenReturn(true);
        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState mockActive = Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState.class);
        ar.edu.utn.frc.tup.piii.engine.model.EnergyCard fairyEnergy = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("xy1-fairy", "Fairy Energy", ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FAIRY, true);
        when(mockActive.getAttachedEnergyCards()).thenReturn(java.util.List.of(fairyEnergy));
        when(mockRuntime.getActivePokemon()).thenReturn(mockActive);
        manager.setPlayerRuntime(mockRuntime);

        manager.apply(StatusEffectType.ENVENENADO);
        assertFalse(manager.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldApplyEffectsWhenSweetVeilIsActiveButNoFairyEnergy() {
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime mockRuntime = Mockito.mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        when(mockRuntime.hasAbility(ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SWEET_VEIL)).thenReturn(true);
        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState mockActive = Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState.class);
        when(mockActive.getAttachedEnergyCards()).thenReturn(java.util.List.of());
        when(mockRuntime.getActivePokemon()).thenReturn(mockActive);
        manager.setPlayerRuntime(mockRuntime);

        manager.apply(StatusEffectType.ENVENENADO);
        assertTrue(manager.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldRemoveExistingRotationSlotEffectWhenApplyingNewRotationSlotEffect() {
        manager.apply(StatusEffectType.DORMIDO);
        manager.apply(StatusEffectType.PARALIZADO);
        assertTrue(manager.has(StatusEffectType.PARALIZADO));
        assertFalse(manager.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldRemoveConfusedWhenApplyingAsleep() {
        manager.apply(StatusEffectType.CONFUNDIDO);
        manager.apply(StatusEffectType.DORMIDO);
        assertTrue(manager.has(StatusEffectType.DORMIDO));
        assertFalse(manager.has(StatusEffectType.CONFUNDIDO));
    }

    @Test
    void shouldKeepBothMarkersWhenApplyingBurnedOnTopOfPoisoned() {
        manager.apply(StatusEffectType.ENVENENADO);
        manager.apply(StatusEffectType.QUEMADO);
        assertTrue(manager.has(StatusEffectType.ENVENENADO));
        assertTrue(manager.has(StatusEffectType.QUEMADO));
    }

    @Test
    void shouldKeepBothMarkersWhenApplyingPoisonedOnTopOfBurned() {
        manager.apply(StatusEffectType.QUEMADO);
        manager.apply(StatusEffectType.ENVENENADO);
        assertTrue(manager.has(StatusEffectType.QUEMADO));
        assertTrue(manager.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldKeepRotationSlotEffectWhenApplyingMarkerEffect() {
        manager.apply(StatusEffectType.DORMIDO);
        manager.apply(StatusEffectType.ENVENENADO);
        assertTrue(manager.has(StatusEffectType.DORMIDO));
        assertTrue(manager.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldBeIdempotentWhenReapplyingBurned() {
        manager.apply(StatusEffectType.QUEMADO);
        manager.apply(StatusEffectType.QUEMADO);
        assertTrue(manager.has(StatusEffectType.QUEMADO));
        assertEquals(1, manager.activeEffects().size());
    }

    @Test
    void shouldThrowInvalidStatusEffectExceptionWhenApplyingNull() {
        assertThrows(InvalidStatusEffectException.class, () -> manager.apply(null));
    }

    @Test
    void shouldRemoveSpecificEffectWhenPresent() {
        manager.apply(StatusEffectType.DORMIDO);
        manager.remove(StatusEffectType.DORMIDO);
        assertFalse(manager.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldBeNoOpWhenRemovingAbsentEffect() {
        assertDoesNotThrow(() -> manager.remove(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldClearAllEffectsWhenClearAllIsCalled() {
        manager.apply(StatusEffectType.DORMIDO);
        manager.apply(StatusEffectType.ENVENENADO);
        manager.apply(StatusEffectType.QUEMADO);
        manager.clearAll();
        assertTrue(manager.activeEffects().isEmpty());
    }

    @Test
    void shouldReturnEmptySetWhenNoEffectsActive() {
        assertTrue(manager.activeEffects().isEmpty());
    }

    @Test
    void shouldReturnCorrectSetWhenMultipleEffectsActive() {
        manager.apply(StatusEffectType.DORMIDO);
        manager.apply(StatusEffectType.ENVENENADO);
        assertTrue(manager.activeEffects().contains(StatusEffectType.DORMIDO));
        assertTrue(manager.activeEffects().contains(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldReturnImmutableSetFromActiveEffects() {
        manager.apply(StatusEffectType.DORMIDO);
        assertThrows(UnsupportedOperationException.class,
                () -> manager.activeEffects().add(StatusEffectType.QUEMADO));
    }

    @Test
    void shouldAcceptNewEffectsAfterClearAll() {
        manager.apply(StatusEffectType.DORMIDO);
        manager.clearAll();
        manager.apply(StatusEffectType.QUEMADO);
        assertTrue(manager.has(StatusEffectType.QUEMADO));
    }

    @Test
    void shouldHoldOnlyLastAppliedRotationSlotEffectAfterSequentialApplication() {
        manager.apply(StatusEffectType.DORMIDO);
        manager.apply(StatusEffectType.CONFUNDIDO);
        manager.apply(StatusEffectType.PARALIZADO);
        assertTrue(manager.has(StatusEffectType.PARALIZADO));
        assertFalse(manager.has(StatusEffectType.DORMIDO));
        assertFalse(manager.has(StatusEffectType.CONFUNDIDO));
    }

    // -------------------------------------------------------------------------
    // Sub-batch 5.2 — canAttack / canRetreat
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowAttackWhenNoEffectsAreActive() {
        assertTrue(manager.canAttack());
    }

    @Test
    void shouldAllowRetreatWhenNoEffectsAreActive() {
        assertTrue(manager.canRetreat());
    }

    @Test
    void shouldBlockAttackWhenDormidoIsActive() {
        manager.apply(StatusEffectType.DORMIDO);
        assertFalse(manager.canAttack());
    }

    @Test
    void shouldBlockRetreatWhenDormidoIsActive() {
        manager.apply(StatusEffectType.DORMIDO);
        assertFalse(manager.canRetreat());
    }

    @Test
    void shouldBlockAttackWhenParalizadoIsActive() {
        manager.apply(StatusEffectType.PARALIZADO);
        assertFalse(manager.canAttack());
    }

    @Test
    void shouldAllowAttackWhenOnlyBurnedAndPoisonedAreActive() {
        manager.apply(StatusEffectType.QUEMADO);
        manager.apply(StatusEffectType.ENVENENADO);
        assertTrue(manager.canAttack());
    }

    @Test
    void shouldAllowRetreatWhenOnlyConfusedIsActive() {
        manager.apply(StatusEffectType.CONFUNDIDO);
        assertTrue(manager.canRetreat());
    }

    // -------------------------------------------------------------------------
    // Sub-batch 5.3 — onAttackAttempt / applyConfusionCheck
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowPokemonAsleepExceptionWhenAttackingWhileAsleep() {
        manager.apply(StatusEffectType.DORMIDO);
        assertThrows(PokemonAsleepException.class, () -> manager.onAttackAttempt(fakeState));
    }

    @Test
    void shouldThrowPokemonParalyzedExceptionWhenAttackingWhileParalyzed() {
        manager.apply(StatusEffectType.PARALIZADO);
        assertThrows(PokemonParalyzedException.class, () -> manager.onAttackAttempt(fakeState));
    }

    @Test
    void shouldReturnProceedWhenConfusedCoinIsHeads() {
        when(coinFlipper.flip()).thenReturn(true);
        manager.apply(StatusEffectType.CONFUNDIDO);
        AttackModifierResult result = manager.onAttackAttempt(fakeState);
        assertInstanceOf(AttackModifierResult.Proceed.class, result);
    }

    @Test
    void shouldReturnConfusionFailedWithThreeCountersWhenConfusedCoinIsTails() {
        when(coinFlipper.flip()).thenReturn(false);
        manager.apply(StatusEffectType.CONFUNDIDO);
        AttackModifierResult result = manager.onAttackAttempt(fakeState);
        assertInstanceOf(AttackModifierResult.ConfusionFailed.class, result);
        assertEquals(3, ((AttackModifierResult.ConfusionFailed) result).selfDamageCounters());
    }

    @Test
    void shouldReturnProceedWithoutFlippingWhenNoEffectsActive() {
        AttackModifierResult result = manager.onAttackAttempt(fakeState);
        assertInstanceOf(AttackModifierResult.Proceed.class, result);
        verify(coinFlipper, never()).flip();
    }

    @Test
    void shouldReturnProceedWhenOnlyMarkerEffectsAreActive() {
        manager.apply(StatusEffectType.QUEMADO);
        manager.apply(StatusEffectType.ENVENENADO);
        AttackModifierResult result = manager.onAttackAttempt(fakeState);
        assertInstanceOf(AttackModifierResult.Proceed.class, result);
    }

    @Test
    void shouldFlipCoinExactlyOncePerAttackAttemptWhenConfused() {
        when(coinFlipper.flip()).thenReturn(true);
        manager.apply(StatusEffectType.CONFUNDIDO);
        manager.onAttackAttempt(fakeState);
        verify(coinFlipper, times(1)).flip();
    }

    // -------------------------------------------------------------------------
    // Sub-batch 5.4 — processBetweenTurns
    // -------------------------------------------------------------------------

    @Test
    void shouldProcessPoisonBeforeBurnInBetweenTurnsOrder() {
        // ENVENENADO: always +1 counter; QUEMADO: heads = no damage
        when(coinFlipper.flip()).thenReturn(true);
        manager.apply(StatusEffectType.ENVENENADO);
        manager.apply(StatusEffectType.QUEMADO);
        manager.processBetweenTurns(fakeState);
        // Poison adds 1, Burn (heads) adds 0 → total = 1
        assertEquals(1, fakeState.getDamageCounters());
    }

    @Test
    void shouldRemoveParalysisAfterBetweenTurnsProcessing() {
        manager.apply(StatusEffectType.PARALIZADO);
        manager.processBetweenTurns(fakeState);
        assertFalse(manager.has(StatusEffectType.PARALIZADO));
    }

    @Test
    void shouldRetainPoisonAfterBetweenTurnsProcessing() {
        manager.apply(StatusEffectType.ENVENENADO);
        manager.processBetweenTurns(fakeState);
        assertTrue(manager.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldRemoveDormidoAfterBetweenTurnsWhenCoinIsHeads() {
        when(coinFlipper.flip()).thenReturn(true);
        manager.apply(StatusEffectType.DORMIDO);
        manager.processBetweenTurns(fakeState);
        assertFalse(manager.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldRetainDormidoAfterBetweenTurnsWhenCoinIsTails() {
        when(coinFlipper.flip()).thenReturn(false);
        manager.apply(StatusEffectType.DORMIDO);
        manager.processBetweenTurns(fakeState);
        assertTrue(manager.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldNotProcessAbsentEffectsInBetweenTurns() {
        manager.apply(StatusEffectType.ENVENENADO);
        manager.processBetweenTurns(fakeState);
        // QUEMADO and DORMIDO absent → coinFlipper.flip() should never be called
        verify(coinFlipper, never()).flip();
    }

    @Test
    void shouldBeNoOpWhenProcessingBetweenTurnsWithNoActiveEffects() {
        assertDoesNotThrow(() -> manager.processBetweenTurns(fakeState));
        assertEquals(0, fakeState.getDamageCounters());
    }

    @Test
    void shouldRemoveParalysisAtomicallyAfterBetweenTurnsIteration() {
        manager.apply(StatusEffectType.PARALIZADO);
        // Must not throw ConcurrentModificationException
        assertDoesNotThrow(() -> manager.processBetweenTurns(fakeState));
        assertFalse(manager.has(StatusEffectType.PARALIZADO));
    }

    @Test
    void shouldTrackAndClearDamagePreventedNextTurnFlag() {
        assertFalse(manager.isDamagePreventedNextTurn());
        manager.setDamagePreventedNextTurn(true);
        assertTrue(manager.isDamagePreventedNextTurn());
        manager.clearAll();
        assertFalse(manager.isDamagePreventedNextTurn());
    }

    @Test
    void shouldRemovePrecisionBajaAfterBetweenTurnsProcessing() {
        manager.apply(StatusEffectType.PRECISION_BAJA);
        assertTrue(manager.has(StatusEffectType.PRECISION_BAJA));
        manager.processBetweenTurns(fakeState);
        assertFalse(manager.has(StatusEffectType.PRECISION_BAJA));
    }

    @Test
    void shouldReturnProceedWhenPrecisionBajaCoinIsHeads() {
        when(coinFlipper.flip()).thenReturn(true);
        manager.apply(StatusEffectType.PRECISION_BAJA);
        AttackModifierResult result = manager.onAttackAttempt(fakeState);
        assertInstanceOf(AttackModifierResult.Proceed.class, result);
    }

    @Test
    void shouldReturnSmokescreenFailedWhenPrecisionBajaCoinIsTails() {
        when(coinFlipper.flip()).thenReturn(false);
        manager.apply(StatusEffectType.PRECISION_BAJA);
        AttackModifierResult result = manager.onAttackAttempt(fakeState);
        assertInstanceOf(AttackModifierResult.SmokescreenFailed.class, result);
    }

    @Test
    void shouldTrackAndClearSelfDisabledNextTurnFlags() {
        assertFalse(manager.isSelfDisabledNextTurn());
        assertFalse(manager.isSelfDisabledNextTurnSetThisTurn());

        manager.setSelfDisabledNextTurn(true);
        manager.setSelfDisabledNextTurnSetThisTurn(true);

        assertTrue(manager.isSelfDisabledNextTurn());
        assertTrue(manager.isSelfDisabledNextTurnSetThisTurn());

        manager.clearAll();

        assertFalse(manager.isSelfDisabledNextTurn());
        assertFalse(manager.isSelfDisabledNextTurnSetThisTurn());
    }

    @Test
    void shouldTrackAndClearNewCorrectionsFlags() {
        assertFalse(manager.isRetreatBlockedNextTurn());
        assertFalse(manager.isRetreatBlockedNextTurnSetThisTurn());
        assertFalse(manager.isDrawStepBlocked());
        assertFalse(manager.isExcitingShakeActiveNextTurn());
        assertFalse(manager.isExcitingShakeActiveNextTurnSetThisTurn());
        assertFalse(manager.isStrongGustUsedLastTurn());
        assertFalse(manager.isStrongGustUsedLastTurnSetThisTurn());

        manager.setRetreatBlockedNextTurn(true);
        manager.setRetreatBlockedNextTurnSetThisTurn(true);
        manager.setDrawStepBlocked(true);
        manager.setExcitingShakeActiveNextTurn(true);
        manager.setExcitingShakeActiveNextTurnSetThisTurn(true);
        manager.setStrongGustUsedLastTurn(true);
        manager.setStrongGustUsedLastTurnSetThisTurn(true);

        assertTrue(manager.isRetreatBlockedNextTurn());
        assertTrue(manager.isRetreatBlockedNextTurnSetThisTurn());
        assertTrue(manager.isDrawStepBlocked());
        assertTrue(manager.isExcitingShakeActiveNextTurn());
        assertTrue(manager.isExcitingShakeActiveNextTurnSetThisTurn());
        assertTrue(manager.isStrongGustUsedLastTurn());
        assertTrue(manager.isStrongGustUsedLastTurnSetThisTurn());

        manager.clearAll();

        assertFalse(manager.isRetreatBlockedNextTurn());
        assertFalse(manager.isRetreatBlockedNextTurnSetThisTurn());
        assertFalse(manager.isDrawStepBlocked());
        assertFalse(manager.isExcitingShakeActiveNextTurn());
        assertFalse(manager.isExcitingShakeActiveNextTurnSetThisTurn());
        assertFalse(manager.isStrongGustUsedLastTurn());
        assertFalse(manager.isStrongGustUsedLastTurnSetThisTurn());
    }
}
