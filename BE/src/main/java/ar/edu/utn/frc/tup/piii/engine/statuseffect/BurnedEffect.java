package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

/**
 * Strategy for the QUEMADO (burned) status condition.
 * Flips a coin between turns; on TAILS adds two damage counters. FR-007.
 */
public class BurnedEffect implements StatusEffect {

    private static final int BURN_DAMAGE_COUNTERS = 2;

    @Override
    public StatusEffectType getType() {
        return StatusEffectType.QUEMADO;
    }

    @Override
    public boolean isRotationSlot() {
        return false;
    }

    @Override
    public boolean blocksAttack() {
        return false;
    }

    @Override
    public boolean blocksRetreat() {
        return false;
    }

    @Override
    public boolean processBetweenTurns(final ActivePokemonState state, final CoinFlipper flipper) {
        if (!flipper.flip()) {
            state.addDamageCounters(BURN_DAMAGE_COUNTERS);
        }
        return false;
    }
}
