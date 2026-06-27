package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.model.DamageContext;
import ar.edu.utn.frc.tup.piii.engine.model.DamageResult;

import java.util.Objects;

/**
 * Step 3 — delegates to {@link DamageCalculator} with the attacker, defender, attack,
 * and any modifier lists accumulated so far, then stores the result in the context
 * for use by {@link DamageApplicationStep}.
 */
public final class DamageCalculationStep implements AttackPipelineStep {

    private final DamageCalculator calculator;

    /**
     * @param calculator the stateless damage calculator (never null)
     */
    public DamageCalculationStep(final DamageCalculator calculator) {
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        final DamageContext dmgCtx = new DamageContext(
                ctx.getAttacker(),
                ctx.getDefender(),
                ctx.getAttack(),
                ctx.getAttackerModifiers(),
                ctx.getDefenderModifiers());
        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard activeStadium = ctx.getActiveStadium();
        final boolean suppressResistance = (activeStadium != null && "xy2-91".equals(activeStadium.getCardId()))
                || "ignore_resistance".equals(ctx.getEffectText());
        
        final DamageResult result = calculator.calculate(dmgCtx, ctx.isWeaknessSuppressed(), suppressResistance);
        
        final int rawModifiedDamage = PassiveAbilityRegistry.modifyIncomingDamage(result.finalDamage(), ctx);
        final DamageResult finalResult = rawModifiedDamage == result.finalDamage() ? result : new DamageResult(rawModifiedDamage, rawModifiedDamage / 10);
        ctx.setDamageResult(finalResult);
        next.run();
    }
}
