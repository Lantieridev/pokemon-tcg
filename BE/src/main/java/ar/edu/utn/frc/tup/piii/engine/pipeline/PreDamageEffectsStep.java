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
            if (effectText.startsWith("damage_all_opponents:")) {
                final int amount = parseAmount(effectText, "damage_all_opponents:".length());
                ctx.addAttackerModifier(dmg -> amount);
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
            } else if (effectText.startsWith("coin_flips_until_tails_extra:")) {
                String[] parts = effectText.split(":");
                int dmgPerHead = Integer.parseInt(parts[1]);
                int heads = 0;
                while (ctx.getCoinFlipper().flip()) {
                    heads++;
                }
                final int totalDamage = heads * dmgPerHead;
                ctx.addAttackerModifier(dmg -> dmg + totalDamage);
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
            } else if (effectText.startsWith("powerful_friends:")) {
                final int extraDamage = parseAmount(effectText, "powerful_friends:".length());
                boolean hasStage2OnBench = false;
                if (ctx.getAttackerRuntime() != null) {
                    hasStage2OnBench = ctx.getAttackerRuntime().getBench().getAll().stream()
                            .anyMatch(p -> p.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.STAGE_2);
                }
                if (hasStage2OnBench) {
                    ctx.addAttackerModifier(dmg -> dmg + extraDamage);
                }
            } else if (effectText.startsWith("damage_per_energy_type:")) {
                String[] parts = effectText.split(":");
                String energyTypeStr = parts[1];
                int dmgPerEnergy = Integer.parseInt(parts[2]);
                ar.edu.utn.frc.tup.piii.engine.model.PokemonType targetType = ar.edu.utn.frc.tup.piii.engine.model.PokemonType.valueOf(energyTypeStr.toUpperCase());
                long matchingEnergies = ctx.getAttacker().getAttachedEnergyCards().stream()
                        .filter(ec -> ec.getEnergyType() == targetType || ec.isProvidesAllTypes())
                        .mapToLong(ec -> ec.getEnergyCount())
                        .sum();
                final int totalDamage = (int) (matchingEnergies * dmgPerEnergy);
                ctx.addAttackerModifier(dmg -> dmg + totalDamage);
            } else if (effectText.startsWith("damage_if_target_damaged:")) {
                final int extraDamage = parseAmount(effectText, "damage_if_target_damaged:".length());
                if (ctx.getDefender() != null && ctx.getDefender().getDamageCounters() > 0) {
                    ctx.addAttackerModifier(dmg -> dmg + extraDamage);
                }
            } else if (effectText.startsWith("damage_minus_per_counter:")) {
                final int minusDamagePerCounter = parseAmount(effectText, "damage_minus_per_counter:".length());
                final int counters = ctx.getAttacker().getDamageCounters();
                ctx.addAttackerModifier(dmg -> Math.max(0, dmg - (counters * minusDamagePerCounter)));
            } else if (effectText.startsWith("revenge_damage:")) {
                final int extraDamage = parseAmount(effectText, "revenge_damage:".length());
                if (ctx.getAttackerRuntime() != null && ctx.getAttackerRuntime().isKnockedOutLastTurn()) {
                    ctx.addAttackerModifier(dmg -> dmg + extraDamage);
                }
            } else if (effectText.startsWith("damage_per_opponent_prize:")) {
                final int dmgPerPrize = parseAmount(effectText, "damage_per_opponent_prize:".length());
                if (ctx.getDefenderRuntime() != null) {
                    final int prizesTaken = Math.max(0, ctx.getDefenderRuntime().getStartingPrizeCount() - ctx.getDefenderRuntime().getPrizeCount());
                    final int totalDamage = prizesTaken * dmgPerPrize;
                    ctx.addAttackerModifier(dmg -> totalDamage);
                }
            } else if ("discard_opponent_tool".equals(effectText)) {
                if (ctx.getDefender() != null && ctx.getDefender().hasToolAttached()) {
                    ctx.getDefender().getAttachedTool().ifPresent(tool -> {
                        if (ctx.getDefenderRuntime() != null) {
                            ctx.getDefenderRuntime().getDiscardPile().add(tool);
                        }
                        ctx.getDefender().detachTool();
                    });
                }
            } else if (effectText.startsWith("discard_hand_energy_multiply_damage:fighting:")) {
                String[] parts = effectText.split(":");
                int dmgPerEnergy = Integer.parseInt(parts[2]);
                if (ctx.isRockRushResolved()) {
                    final int totalDamage = dmgPerEnergy * ctx.getRockRushDiscardCount();
                    ctx.addAttackerModifier(dmg -> totalDamage);
                } else {
                    final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = ctx.getAttackerRuntime();
                    int fightingEnergiesInHand = 0;
                    if (runtime != null) {
                        fightingEnergiesInHand = (int) runtime.getHand().getCards().stream()
                                .filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && (ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIGHTING || ec.isProvidesAllTypes()))
                                .count();
                    }
                    if (fightingEnergiesInHand > 0) {
                        ctx.getMatchSession().setPendingSelectionRequest(
                                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ROCK_RUSH,
                                        null,
                                        fightingEnergiesInHand,
                                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                                )
                        );
                        if (ctx.getMatchSession().getTurnManager() != null) {
                            ctx.getMatchSession().getTurnManager().interruptMainPhase();
                        }
                        ctx.addDefenderModifier(dmg -> 0);
                    } else {
                        ctx.addAttackerModifier(dmg -> 0);
                    }
                }
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

        if (ctx.getDefenderStatusManager() != null && ctx.getDefenderStatusManager().isDamagePreventedIf60OrLessNextTurn()) {
            ctx.addDefenderModifier(dmg -> dmg <= 60 ? 0 : dmg);
        }

        if (ctx.getDefenderStatusManager() != null && ctx.getDefenderStatusManager().isDamageReducedBy20NextTurn()) {
            ctx.addDefenderModifier(dmg -> Math.max(0, dmg - 20));
        }

        if (ctx.isScorchingFangDiscarded()) {
            ctx.addAttackerModifier(dmg -> dmg + 30);
        }

        if ("deranged_dance".equals(effectText)) {
            final int playerBench = ctx.getAttackerRuntime() != null ? ctx.getAttackerRuntime().getBench().getAll().size() : 0;
            final int opponentBench = ctx.getDefenderBench().size();
            final int totalBenched = playerBench + opponentBench;
            ctx.addAttackerModifier(dmg -> 20 * totalBenched);
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
