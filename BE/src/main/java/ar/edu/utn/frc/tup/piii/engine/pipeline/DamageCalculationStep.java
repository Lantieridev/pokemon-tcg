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
        final DamageResult result = calculator.calculate(dmgCtx, ctx.isWeaknessSuppressed());
        
        final int rawModifiedDamage = PassiveAbilityRegistry.modifyIncomingDamage(result.finalDamage(), ctx);
        final DamageResult finalResult = rawModifiedDamage == result.finalDamage() ? result : new DamageResult(rawModifiedDamage, rawModifiedDamage / 10);
        ctx.setDamageResult(finalResult);
        next.run();
    }
}
