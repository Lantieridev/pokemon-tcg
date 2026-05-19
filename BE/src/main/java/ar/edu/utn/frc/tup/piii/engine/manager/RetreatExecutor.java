package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;

import java.util.Objects;

/**
 * Executes the retreat action for the active Pokémon.
 *
 * <p>Per the Pokémon TCG XY1 ruleset, when a Pokémon retreats all special conditions
 * (status effects) on it are cleared. This class enforces that invariant by calling
 * {@link StatusEffectManager#clearAll()} after a retreat is applied. FR-011.</p>
 *
 * <p>This is a pure-engine component with zero Spring dependencies. It is intended to
 * be composed into the game session layer (e.g., GameFacade) once that module exists.</p>
 */
public final class RetreatExecutor {

    private final StatusEffectManager statusEffectManager;

    /**
     * Constructs a RetreatExecutor.
     *
     * @param statusEffectManager the manager tracking status effects on the retreating Pokémon;
     *                            must not be null
     */
    public RetreatExecutor(final StatusEffectManager statusEffectManager) {
        this.statusEffectManager = Objects.requireNonNull(statusEffectManager, "statusEffectManager");
    }

    /**
     * Applies the retreat: clears all status effects from the active Pokémon.
     *
     * <p>Callers are responsible for ensuring the retreat action has already been validated
     * (e.g., via {@link RuleValidator}) before invoking this method.</p>
     *
     * @param action the retreat action being executed (must not be null)
     */
    public void executeRetreat(final RetreatAction action) {
        Objects.requireNonNull(action, "action");
        statusEffectManager.clearAll();
    }
}
