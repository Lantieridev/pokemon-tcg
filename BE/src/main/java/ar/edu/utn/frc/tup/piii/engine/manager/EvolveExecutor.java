package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

import java.util.Objects;

/**
 * Executes the evolve action for a Pokémon.
 *
 * <p>Per the Pokémon TCG XY1 ruleset, when a Pokémon evolves all special conditions
 * (status effects) on it are cleared. This class enforces that invariant by calling
 * {@link StatusEffectManager#clearAll()} after evolution is applied. FR-010.</p>
 *
 * <p>This is a pure-engine component with zero Spring dependencies.</p>
 */
public final class EvolveExecutor {

    private final StatusEffectManager statusEffectManager;

    /**
     * Constructs an EvolveExecutor.
     *
     * @param statusEffectManager the manager tracking status effects on the evolving Pokémon;
     *                            must not be null
     */
    public EvolveExecutor(final StatusEffectManager statusEffectManager) {
        this.statusEffectManager = Objects.requireNonNull(statusEffectManager, "statusEffectManager");
    }

    /**
     * Applies the evolution: clears all status effects from the target Pokémon.
     *
     * <p>Per the Pokémon TCG XY1 ruleset, evolving a Pokémon removes all special
     * conditions that were on the pre-evolved form. FR-010.</p>
     *
     * @param target the Pokémon being evolved (must not be null)
     */
    public void executeEvolve(final BattlePokemonState target) {
        Objects.requireNonNull(target, "target");
        statusEffectManager.clearAll();
    }
}
