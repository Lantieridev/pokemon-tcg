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
        if (effectText != null) {
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
            if (effectText.startsWith("coin_flips_multiplier:")) {
                String[] parts = effectText.split(":");
                int coins = Integer.parseInt(parts[1]);
                int dmgPerHead = Integer.parseInt(parts[2]);
                int heads = 0;
                for (int i = 0; i < coins; i++) {
                    if (ctx.getCoinFlipper().flip()) {
                        heads++;
                    }
                }
                final int totalDamage = heads * dmgPerHead;
                ctx.addAttackerModifier(dmg -> totalDamage);
            } else if (effectText.startsWith("coin_flips_until_tails:")) {
                String[] parts = effectText.split(":");
                int dmgPerHead = Integer.parseInt(parts[1]);
                int heads = 0;
                while (ctx.getCoinFlipper().flip()) {
                    heads++;
                }
                final int totalDamage = heads * dmgPerHead;
                ctx.addAttackerModifier(dmg -> totalDamage);
            } else if (effectText.startsWith("coin_flips_per_energy:")) {
                String[] parts = effectText.split(":");
                String energyTypeStr = parts[1];
                int dmgPerHead = Integer.parseInt(parts[2]);
                ar.edu.utn.frc.tup.piii.engine.model.PokemonType targetType = ar.edu.utn.frc.tup.piii.engine.model.PokemonType.valueOf(energyTypeStr.toUpperCase());
                long matchingEnergies = ctx.getAttacker().getAttachedEnergyCards().stream()
                        .filter(ec -> ec.getEnergyType() == targetType || ec.isProvidesAllTypes())
                        .mapToLong(ec -> ec.getEnergyCount())
                        .sum();
                int heads = 0;
                for (int i = 0; i < matchingEnergies; i++) {
                    if (ctx.getCoinFlipper().flip()) {
                        heads++;
                    }
                }
                final int totalDamage = heads * dmgPerHead;
                ctx.addAttackerModifier(dmg -> totalDamage);
            } else if (effectText.startsWith("coin_flips_per_damage_counter:")) {
                String[] parts = effectText.split(":");
                int dmgPerHead = Integer.parseInt(parts[1]);
                int counters = ctx.getAttacker().getDamageCounters();
                int heads = 0;
                for (int i = 0; i < counters; i++) {
                    if (ctx.getCoinFlipper().flip()) {
                        heads++;
                    }
                }
                final int totalDamage = heads * dmgPerHead;
                ctx.addAttackerModifier(dmg -> totalDamage);
            }
        }

        // Check for SAFEGUARD ability on the defender
        boolean hasSafeguard = ctx.getDefender() != null && ctx.getDefender().getAbilities().stream()
                .anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SAFEGUARD);
        
        if (hasSafeguard && ctx.getAttacker().isEx()) {
            ctx.setAttackBlocked(true);
        }

        // Check for next-turn damage prevention effect (e.g. from Scrunch / Acurruque)
        if (ctx.getDefenderStatusManager() != null && ctx.getDefenderStatusManager().isDamagePreventedNextTurn()) {
            ctx.addDefenderModifier(dmg -> 0);
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
