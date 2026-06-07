package ar.edu.utn.frc.tup.piii.engine.pipeline;

/**
 * Step 2 — resolves effects that modify damage BEFORE calculation.
 *
 * <p>Currently handles {@code coin_flip_extra:N}: flips the coin once; on heads,
 * registers a {@code +N} attacker modifier that {@link DamageCalculationStep} will fold in.</p>
 */
public final class PreDamageEffectsStep implements AttackPipelineStep {

    private static final String COIN_FLIP_EXTRA_PREFIX = "coin_flip_extra:";

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        final String effectText = ctx.getEffectText();
        if (effectText.startsWith(COIN_FLIP_EXTRA_PREFIX)) {
            final int extraDamage = parseAmount(effectText, COIN_FLIP_EXTRA_PREFIX.length());
            if (ctx.getCoinFlipper().flip()) {
                ctx.addAttackerModifier(dmg -> dmg + extraDamage);
            }
        }
        if ("coin_flip_fail".equals(effectText)) {
            if (!ctx.getCoinFlipper().flip()) {
                ctx.setAttackBlocked(true);
            }
        }

        // Check for SAFEGUARD ability on the defender
        boolean hasSafeguard = ctx.getDefender().getAbilities().stream()
                .anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SAFEGUARD);
        
        if (hasSafeguard && ctx.getAttacker().isEx()) {
            ctx.setAttackBlocked(true);
        }

        next.run();
    }

    private int parseAmount(final String text, final int fromIndex) {
        try {
            return Integer.parseInt(text.substring(fromIndex));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
