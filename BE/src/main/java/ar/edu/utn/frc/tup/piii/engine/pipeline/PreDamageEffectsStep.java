package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.SelectionSource;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Step 2 — resolves effects that modify damage BEFORE calculation.
 *
 * <p>Each attack effect is stored as a {@code prefix:params} (or bare-keyword)
 * string on the card data; {@link #EFFECT_HANDLERS} dispatches to the handler
 * whose key matches {@code effectText}, keeping each effect's logic in its own
 * small method instead of one giant if/else-if chain.</p>
 */
public final class PreDamageEffectsStep implements AttackPipelineStep {

    private static final String COIN_FLIP_EXTRA_PREFIX = "coin_flip_extra:";
    private static final String DISCARD_HAND_ENERGY_FIGHTING_PREFIX = "discard_hand_energy_multiply_damage:fighting:";
    private static final String DERANGED_DANCE = "deranged_dance";

    @FunctionalInterface
    private interface EffectHandler {
        void handle(AttackContext ctx, String effectText);
    }

    private static final Map<String, EffectHandler> EFFECT_HANDLERS = buildEffectHandlers();

    private static Map<String, EffectHandler> buildEffectHandlers() {
        final Map<String, EffectHandler> handlers = new LinkedHashMap<>();
        handlers.put(COIN_FLIP_EXTRA_PREFIX, PreDamageEffectsStep::handleCoinFlipExtra);
        handlers.put("damage_all_opponents:", PreDamageEffectsStep::handleDamageAllOpponents);
        handlers.put("coin_flip_fail", (ctx, text) -> handleCoinFlipFail(ctx));
        handlers.put("coin_flips_multiplier:", PreDamageEffectsStep::handleCoinFlipsMultiplier);
        handlers.put("strong_gust", (ctx, text) -> handleStrongGust(ctx));
        handlers.put("coin_flips_until_tails_extra:", PreDamageEffectsStep::handleCoinFlipsUntilTailsExtra);
        handlers.put("coin_flips_until_tails:", PreDamageEffectsStep::handleCoinFlipsUntilTails);
        handlers.put("coin_flips_per_energy:", PreDamageEffectsStep::handleCoinFlipsPerEnergy);
        handlers.put("coin_flips_per_damage_counter:", PreDamageEffectsStep::handleCoinFlipsPerDamageCounter);
        handlers.put("powerful_friends:", PreDamageEffectsStep::handlePowerfulFriends);
        handlers.put("damage_per_opponent_all_energy:", PreDamageEffectsStep::handleDamagePerOpponentAllEnergy);
        handlers.put("damage_times_self_counters:", PreDamageEffectsStep::handleDamageTimesSelfCounters);
        handlers.put("damage_per_retreat_cost:", PreDamageEffectsStep::handleDamagePerRetreatCost);
        handlers.put("damage_per_energy_type:", PreDamageEffectsStep::handleDamagePerEnergyType);
        handlers.put("damage_if_target_damaged:", PreDamageEffectsStep::handleDamageIfTargetDamaged);
        handlers.put("damage_minus_per_counter:", PreDamageEffectsStep::handleDamageMinusPerCounter);
        handlers.put("revenge_damage:", PreDamageEffectsStep::handleRevengeDamage);
        handlers.put("damage_per_opponent_prize:", PreDamageEffectsStep::handleDamagePerOpponentPrize);
        handlers.put("discard_opponent_tool", (ctx, text) -> handleDiscardOpponentTool(ctx));
        handlers.put(DISCARD_HAND_ENERGY_FIGHTING_PREFIX, PreDamageEffectsStep::handleDiscardHandEnergyMultiplyDamage);
        return handlers;
    }

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        final String effectText = ctx.getEffectText();
        if (effectText != null) {
            dispatch(ctx, effectText);
        }

        applySafeguardBlock(ctx);
        applyDefenderStatusModifiers(ctx);
        applyScorchingFangModifier(ctx);
        applyDerangedDance(ctx, effectText);

        next.run();
    }

    private static void applySafeguardBlock(final AttackContext ctx) {
        final boolean hasSafeguard = ctx.getDefender() != null && ctx.getDefender().getAbilities().stream()
                .anyMatch(a -> a.effectId() == AbilityEffectId.SAFEGUARD);
        if (hasSafeguard && ctx.getAttacker().isEx()) {
            ctx.setAttackBlocked(true);
        }
    }

    // Next-turn damage prevention effects, e.g. from Scrunch / Acurruque
    private static void applyDefenderStatusModifiers(final AttackContext ctx) {
        if (ctx.getDefenderStatusManager() == null) {
            return;
        }
        if (ctx.getDefenderStatusManager().isDamagePreventedNextTurn()) {
            ctx.addDefenderModifier(dmg -> 0);
        }
        if (ctx.getDefenderStatusManager().isDamagePreventedIf60OrLessNextTurn()) {
            ctx.addDefenderModifier(dmg -> dmg <= 60 ? 0 : dmg);
        }
        if (ctx.getDefenderStatusManager().isDamageReducedBy20NextTurn()) {
            ctx.addDefenderModifier(dmg -> Math.max(0, dmg - 20));
        }
    }

    private static void applyScorchingFangModifier(final AttackContext ctx) {
        if (ctx.isScorchingFangDiscarded()) {
            ctx.addAttackerModifier(dmg -> dmg + 30);
        }
    }

    // Not in EFFECT_HANDLERS: unlike the other ~20 effects, this one has always run as an
    // unconditional post-check alongside safeguard/status/scorching-fang above, not as a
    // dispatched case (pre-dates this class's Strategy-map conversion).
    private static void applyDerangedDance(final AttackContext ctx, final String effectText) {
        if (!DERANGED_DANCE.equals(effectText)) {
            return;
        }
        final int playerBench = ctx.getAttackerRuntime() != null ? ctx.getAttackerRuntime().getBench().getAll().size() : 0;
        final int opponentBench = ctx.getDefenderBench().size();
        final int totalBenched = playerBench + opponentBench;
        ctx.addAttackerModifier(dmg -> 20 * totalBenched);
    }

    private static void dispatch(final AttackContext ctx, final String effectText) {
        for (final Map.Entry<String, EffectHandler> entry : EFFECT_HANDLERS.entrySet()) {
            final String key = entry.getKey();
            final boolean matches = key.endsWith(":") ? effectText.startsWith(key) : effectText.equals(key);
            if (matches) {
                entry.getValue().handle(ctx, effectText);
                return;
            }
        }
    }

    private static void handleCoinFlipExtra(final AttackContext ctx, final String effectText) {
        final int extraDamage = parseAmount(effectText, COIN_FLIP_EXTRA_PREFIX.length());
        if (ctx.getCoinFlipper().flip()) {
            ctx.addAttackerModifier(dmg -> dmg + extraDamage);
        }
    }

    private static void handleDamageAllOpponents(final AttackContext ctx, final String effectText) {
        final int amount = parseAmount(effectText, "damage_all_opponents:".length());
        ctx.addAttackerModifier(dmg -> amount);
    }

    private static void handleCoinFlipFail(final AttackContext ctx) {
        if (!ctx.getCoinFlipper().flip()) {
            ctx.setAttackBlocked(true);
        }
    }

    private static void handleCoinFlipsMultiplier(final AttackContext ctx, final String effectText) {
        final String[] parts = effectText.split(":");
        int coins = Integer.parseInt(parts[1]);
        final int dmgPerHead = Integer.parseInt(parts[2]);
        if (ctx.getAttackerStatusManager() != null && ctx.getAttackerStatusManager().isExcitingShakeActiveNextTurn() && coins == 2) {
            coins = 6;
        }
        final int totalDamage = flipHeads(ctx, coins) * dmgPerHead;
        ctx.addAttackerModifier(dmg -> totalDamage);
    }

    private static void handleStrongGust(final AttackContext ctx) {
        if (ctx.getAttackerStatusManager() != null && ctx.getAttackerStatusManager().isStrongGustUsedLastTurn()) {
            ctx.addAttackerModifier(dmg -> dmg + 60);
        }
    }

    private static void handleCoinFlipsUntilTails(final AttackContext ctx, final String effectText) {
        final int dmgPerHead = Integer.parseInt(effectText.split(":")[1]);
        final int totalDamage = flipUntilTails(ctx) * dmgPerHead;
        ctx.addAttackerModifier(dmg -> totalDamage);
    }

    private static void handleCoinFlipsUntilTailsExtra(final AttackContext ctx, final String effectText) {
        final int dmgPerHead = Integer.parseInt(effectText.split(":")[1]);
        final int totalDamage = flipUntilTails(ctx) * dmgPerHead;
        ctx.addAttackerModifier(dmg -> dmg + totalDamage);
    }

    private static void handleCoinFlipsPerEnergy(final AttackContext ctx, final String effectText) {
        final String[] parts = effectText.split(":");
        final PokemonType targetType = PokemonType.valueOf(parts[1].toUpperCase(Locale.ROOT));
        final int dmgPerHead = Integer.parseInt(parts[2]);
        final long matchingEnergies = matchingAttachedEnergy(ctx.getAttacker().getAttachedEnergyCards(), targetType);
        final int totalDamage = flipHeads(ctx, (int) matchingEnergies) * dmgPerHead;
        ctx.addAttackerModifier(dmg -> totalDamage);
    }

    private static void handleCoinFlipsPerDamageCounter(final AttackContext ctx, final String effectText) {
        final int dmgPerHead = Integer.parseInt(effectText.split(":")[1]);
        final int counters = ctx.getAttacker().getDamageCounters();
        final int totalDamage = flipHeads(ctx, counters) * dmgPerHead;
        ctx.addAttackerModifier(dmg -> totalDamage);
    }

    private static void handlePowerfulFriends(final AttackContext ctx, final String effectText) {
        final int extraDamage = parseAmount(effectText, "powerful_friends:".length());
        boolean hasStage2OnBench = false;
        if (ctx.getAttackerRuntime() != null) {
            hasStage2OnBench = ctx.getAttackerRuntime().getBench().getAll().stream()
                    .anyMatch(p -> p.getEvolutionStage() == EvolutionStage.STAGE_2);
        }
        if (hasStage2OnBench) {
            ctx.addAttackerModifier(dmg -> dmg + extraDamage);
        }
    }

    private static void handleDamagePerOpponentAllEnergy(final AttackContext ctx, final String effectText) {
        final int dmgPerEnergy = parseAmount(effectText, "damage_per_opponent_all_energy:".length());
        long totalEnergies = 0;
        if (ctx.getDefender() != null) {
            totalEnergies = ctx.getDefender().getAttachedEnergyCards().stream()
                    .mapToLong(EnergyCard::getEnergyCount).sum();
        }
        final long finalTotal = totalEnergies;
        ctx.addAttackerModifier(dmg -> dmg + (int) (finalTotal * dmgPerEnergy));
    }

    private static void handleDamageTimesSelfCounters(final AttackContext ctx, final String effectText) {
        final int dmgPer = parseAmount(effectText, "damage_times_self_counters:".length());
        final int c = ctx.getAttacker().getDamageCounters();
        ctx.addAttackerModifier(dmg -> c * dmgPer);
    }

    private static void handleDamagePerRetreatCost(final AttackContext ctx, final String effectText) {
        final int dmgPerColorless = parseAmount(effectText, "damage_per_retreat_cost:".length());
        int retreatCost = 0;
        if (ctx.getDefender() != null) {
            retreatCost = ctx.getDefender().getRetreatCost();
        }
        final int finalRetreat = retreatCost;
        ctx.addAttackerModifier(dmg -> dmg + (finalRetreat * dmgPerColorless));
    }

    private static void handleDamagePerEnergyType(final AttackContext ctx, final String effectText) {
        final String[] parts = effectText.split(":");
        final PokemonType targetType = PokemonType.valueOf(parts[1].toUpperCase(Locale.ROOT));
        final int dmgPerEnergy = Integer.parseInt(parts[2]);
        final long matchingEnergies = matchingAttachedEnergy(ctx.getAttacker().getAttachedEnergyCards(), targetType);
        final int totalDamage = (int) (matchingEnergies * dmgPerEnergy);
        ctx.addAttackerModifier(dmg -> dmg + totalDamage);
    }

    private static void handleDamageIfTargetDamaged(final AttackContext ctx, final String effectText) {
        final int extraDamage = parseAmount(effectText, "damage_if_target_damaged:".length());
        if (ctx.getDefender() != null && ctx.getDefender().getDamageCounters() > 0) {
            ctx.addAttackerModifier(dmg -> dmg + extraDamage);
        }
    }

    private static void handleDamageMinusPerCounter(final AttackContext ctx, final String effectText) {
        final int minusDamagePerCounter = parseAmount(effectText, "damage_minus_per_counter:".length());
        final int counters = ctx.getAttacker().getDamageCounters();
        ctx.addAttackerModifier(dmg -> Math.max(0, dmg - (counters * minusDamagePerCounter)));
    }

    private static void handleRevengeDamage(final AttackContext ctx, final String effectText) {
        final int extraDamage = parseAmount(effectText, "revenge_damage:".length());
        if (ctx.getAttackerRuntime() != null && ctx.getAttackerRuntime().isKnockedOutLastTurn()) {
            ctx.addAttackerModifier(dmg -> dmg + extraDamage);
        }
    }

    private static void handleDamagePerOpponentPrize(final AttackContext ctx, final String effectText) {
        final int dmgPerPrize = parseAmount(effectText, "damage_per_opponent_prize:".length());
        if (ctx.getDefenderRuntime() != null) {
            final int prizesTaken = Math.max(0, ctx.getDefenderRuntime().getStartingPrizeCount() - ctx.getDefenderRuntime().getPrizeCount());
            final int totalDamage = prizesTaken * dmgPerPrize;
            ctx.addAttackerModifier(dmg -> totalDamage);
        }
    }

    private static void handleDiscardOpponentTool(final AttackContext ctx) {
        if (ctx.getDefender() != null && ctx.getDefender().hasToolAttached()) {
            ctx.getDefender().getAttachedTool().ifPresent(tool -> {
                if (ctx.getDefenderRuntime() != null) {
                    ctx.getDefenderRuntime().getDiscardPile().add(tool);
                }
                ctx.getDefender().detachTool();
            });
        }
    }

    private static void handleDiscardHandEnergyMultiplyDamage(final AttackContext ctx, final String effectText) {
        final int dmgPerEnergy = Integer.parseInt(effectText.split(":")[2]);
        if (ctx.isRockRushResolved()) {
            final int totalDamage = dmgPerEnergy * ctx.getRockRushDiscardCount();
            ctx.addAttackerModifier(dmg -> totalDamage);
            return;
        }

        final PlayerRuntime runtime = ctx.getAttackerRuntime();
        final int fightingEnergiesInHand = countFightingEnergiesInHand(runtime);
        if (fightingEnergiesInHand <= 0) {
            ctx.addAttackerModifier(dmg -> 0);
            return;
        }

        ctx.getMatchSession().setPendingSelectionRequest(
                new PendingSelectionRequest(
                        TrainerEffectId.ROCK_RUSH,
                        null,
                        fightingEnergiesInHand,
                        SelectionSource.HAND
                )
        );
        if (ctx.getMatchSession().getTurnManager() != null) {
            ctx.getMatchSession().getTurnManager().interruptMainPhase();
        }
        ctx.addDefenderModifier(dmg -> 0);
    }

    private static int countFightingEnergiesInHand(final PlayerRuntime runtime) {
        if (runtime == null) {
            return 0;
        }
        return (int) runtime.getHand().getCards().stream()
                .filter(c -> c instanceof EnergyCard ec && (ec.getEnergyType() == PokemonType.FIGHTING || ec.isProvidesAllTypes()))
                .count();
    }

    private static long matchingAttachedEnergy(final List<EnergyCard> attachedEnergyCards, final PokemonType targetType) {
        return attachedEnergyCards.stream()
                .filter(ec -> ec.getEnergyType() == targetType || ec.isProvidesAllTypes())
                .mapToLong(EnergyCard::getEnergyCount)
                .sum();
    }

    private static int flipHeads(final AttackContext ctx, final int flips) {
        int heads = 0;
        for (int i = 0; i < flips; i++) {
            if (ctx.getCoinFlipper().flip()) {
                heads++;
            }
        }
        return heads;
    }

    private static int flipUntilTails(final AttackContext ctx) {
        int heads = 0;
        while (ctx.getCoinFlipper().flip()) {
            heads++;
        }
        return heads;
    }

    private static int parseAmount(final String text, final int fromIndex) {
        try {
            return Integer.parseInt(text.substring(fromIndex));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
