package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.DamageContext;
import ar.edu.utn.frc.tup.piii.engine.model.DamageModifier;
import ar.edu.utn.frc.tup.piii.engine.model.DamageResult;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

import java.util.List;
import java.util.Objects;

/**
 * Stateless service that applies the §3 damage formula to a {@link DamageContext}
 * and returns a {@link DamageResult}. FR-007, FR-015.
 *
 * <p>Step order (IMMUTABLE LAW — do not reorder):
 * <ol>
 *   <li>Base damage from {@code ctx.attack().baseDamage()}</li>
 *   <li>Fold {@code attackerModifiers} in list order</li>
 *   <li>Weakness: ×2 if types match and weakness is not null</li>
 *   <li>Resistance: −20 if types match and resistance is not null</li>
 *   <li>Fold {@code defenderModifiers} in list order</li>
 *   <li>Floor to 0; convert to counters via integer division by 10</li>
 * </ol>
 * </p>
 */
public final class DamageCalculator {

    private static final int WEAKNESS_MULTIPLIER = 2;
    private static final int RESISTANCE_REDUCTION = 20;
    private static final int DAMAGE_PER_COUNTER = 10;
    private static final int MINIMUM_DAMAGE = 0;

    /**
     * Calculates the damage and counter count for the given context.
     *
     * @param ctx the damage context (must not be null)
     * @return the final damage result
     * @throws NullPointerException if {@code ctx} is null
     */
    public DamageResult calculate(final DamageContext ctx) {
        return calculate(ctx, false);
    }

    /**
     * Calculates the damage and counter count for the given context, with optional
     * weakness suppression for Stadium effects such as Shadow Circle (xy1-126).
     *
     * <p>When {@code suppressWeakness} is {@code true}, step 3 of the §3 formula is skipped:
     * the defender's weakness type is not applied even if it matches the attacker's type.</p>
     *
     * @param ctx              the damage context (must not be null)
     * @param suppressWeakness {@code true} to skip the Weakness ×2 step
     * @return the final damage result
     * @throws NullPointerException if {@code ctx} is null
     */
    public DamageResult calculate(final DamageContext ctx, final boolean suppressWeakness) {
        Objects.requireNonNull(ctx, "DamageContext must not be null");

        int dmg = ctx.attack().baseDamage();
        dmg = applyAttackerModifiers(dmg, ctx.attackerModifiers());
        if (!suppressWeakness) {
            dmg = applyWeakness(dmg, ctx);
        }
        dmg = applyResistance(dmg, ctx);
        dmg = applyDefenderModifiers(dmg, ctx.defenderModifiers());
        int floored = Math.max(dmg, MINIMUM_DAMAGE);
        return new DamageResult(floored, floored / DAMAGE_PER_COUNTER);
    }

    private int applyAttackerModifiers(final int dmg, final List<DamageModifier> mods) {
        int result = dmg;
        for (DamageModifier mod : mods) {
            result = mod.apply(result);
        }
        return result;
    }

    private int applyWeakness(final int dmg, final DamageContext ctx) {
        PokemonType weaknessType = ctx.defender().getWeaknessType();
        if (weaknessType != null && weaknessType == ctx.attacker().getPokemonType()) {
            return dmg * WEAKNESS_MULTIPLIER;
        }
        return dmg;
    }

    private int applyResistance(final int dmg, final DamageContext ctx) {
        PokemonType resistanceType = ctx.defender().getResistanceType();
        if (resistanceType != null && resistanceType == ctx.attacker().getPokemonType()) {
            return dmg - RESISTANCE_REDUCTION;
        }
        return dmg;
    }

    private int applyDefenderModifiers(final int dmg, final List<DamageModifier> mods) {
        int result = dmg;
        for (DamageModifier mod : mods) {
            result = mod.apply(result);
        }
        return result;
    }
}
