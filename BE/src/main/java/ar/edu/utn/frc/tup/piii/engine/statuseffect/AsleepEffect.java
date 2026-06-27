package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

/**
 * Strategy for the DORMIDO (asleep) status condition.
 * Blocks attack and retreat. Flips a coin between turns:
 * HEADS = wakes up (effect removed), TAILS = stays asleep. FR-008.
 */
public class AsleepEffect implements StatusEffect {

    @Override
    public StatusEffectType getType() {
        return StatusEffectType.DORMIDO;
    }

    @Override
    public boolean isRotationSlot() {
        return true;
    }

    @Override
    public boolean blocksAttack() {
        return true;
    }

    @Override
    public boolean blocksRetreat() {
        return true;
    }

    @Override
    public boolean processBetweenTurns(final ActivePokemonState state, final CoinFlipper flipper) {
        if (state instanceof ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState bps) {
            boolean hasStirAndSnooze = bps.getAbilities().stream()
                    .anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.STIR_AND_SNOOZE);
            if (hasStirAndSnooze) {
                boolean first = flipper.flip();
                boolean second = flipper.flip();
                return first && second;
            }
        }
        return flipper.flip();
    }
}
