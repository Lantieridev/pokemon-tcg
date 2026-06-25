package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import java.util.List;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Parses an attack's effect-text descriptor and dispatches to the appropriate handler
 * using a pre-built {@code Map<AttackEffectType, BiConsumer>} — no switch, no instanceof.
 *
 * <p>Effect-text format: {@code "keyword"} or {@code "keyword:amount"}, e.g.:
 * <ul>
 *   <li>{@code ""} / {@code "none"} — no secondary effect</li>
 *   <li>{@code "poison"} — apply ENVENENADO to the defender</li>
 *   <li>{@code "burn"} — apply QUEMADO to the defender</li>
 *   <li>{@code "paralysis"} — apply PARALIZADO to the defender</li>
 *   <li>{@code "sleep"} — apply DORMIDO to the defender</li>
 *   <li>{@code "confusion"} — apply CONFUNDIDO to the defender</li>
 *   <li>{@code "heal:30"} — heal the attacker 30 HP (3 counters)</li>
 *   <li>{@code "self_damage:10"} — place 1 counter on the attacker</li>
 *   <li>{@code "discard_energy:1"} — discard 1 attached energy from the attacker</li>
 *   <li>{@code "coin_flip_extra:20"} — no-op here; handled earlier by PreDamageEffectsStep</li>
 * </ul>
 * </p>
 */
public final class AttackEffectResolver {

    private static final int DAMAGE_PER_COUNTER = 10;

    private static final Map<String, AttackEffectType> TEXT_TO_TYPE;

    static {
        final Map<String, AttackEffectType> m = new HashMap<>();
        m.put("poison",           AttackEffectType.APPLY_POISON);
        m.put("burn",             AttackEffectType.APPLY_BURN);
        m.put("paralysis",        AttackEffectType.APPLY_PARALYSIS);
        m.put("sleep",            AttackEffectType.APPLY_SLEEP);
        m.put("confusion",        AttackEffectType.APPLY_CONFUSION);
        m.put("heal",             AttackEffectType.HEAL_SELF);
        m.put("self_damage",      AttackEffectType.SELF_DAMAGE);
        m.put("discard_energy",   AttackEffectType.DISCARD_ENERGY);
        m.put("coin_flip_extra",  AttackEffectType.COIN_FLIP_EXTRA_DAMAGE);
        m.put("bench_damage",     AttackEffectType.BENCH_DAMAGE);
        m.put("move_energy",      AttackEffectType.MOVE_ENERGY);
        m.put("force_switch",     AttackEffectType.FORCE_SWITCH);
        m.put("coin_flip_poison",    AttackEffectType.COIN_FLIP_POISON);
        m.put("coin_flip_burn",      AttackEffectType.COIN_FLIP_BURN);
        m.put("coin_flip_paralysis", AttackEffectType.COIN_FLIP_PARALYSIS);
        m.put("coin_flip_sleep",     AttackEffectType.COIN_FLIP_SLEEP);
        m.put("coin_flip_confusion", AttackEffectType.COIN_FLIP_CONFUSION);
        m.put("disable_attack",      AttackEffectType.DISABLE_ATTACK);
        m.put("prevent_damage",           AttackEffectType.PREVENT_DAMAGE);
        m.put("coin_flip_prevent_damage",  AttackEffectType.COIN_FLIP_PREVENT_DAMAGE);
        m.put("coin_flip_prevent_damage_60_or_less", AttackEffectType.COIN_FLIP_PREVENT_DAMAGE_60_OR_LESS);
        m.put("coin_flip_switch_self",     AttackEffectType.COIN_FLIP_SWITCH_SELF);
        m.put("heal_any",                 AttackEffectType.HEAL_ANY);
        m.put("heal_bench",               AttackEffectType.HEAL_BENCH);
        m.put("heal_all",                 AttackEffectType.HEAL_ALL);
        m.put("discard_opponent_energy",            AttackEffectType.DISCARD_OPPONENT_ENERGY);
        m.put("coin_flip_discard_opponent_energy",  AttackEffectType.COIN_FLIP_DISCARD_OPPONENT_ENERGY);
        m.put("stoke",                               AttackEffectType.STOKE);
        m.put("deranged_dance",                      AttackEffectType.DERANGED_DANCE);
        m.put("combustion_blast",                    AttackEffectType.COMBUSTION_BLAST);
        m.put("scorching_fang",                      AttackEffectType.SCORCHING_FANG);
        m.put("bright_garden",                       AttackEffectType.BRIGHT_GARDEN);
        m.put("ear_we_go",                           AttackEffectType.EAR_WE_GO);
        m.put("clairvoyant_eye",                     AttackEffectType.CLAIRVOYANT_EYE);
        m.put("call_for_family",                     AttackEffectType.CALL_FOR_FAMILY);
        m.put("quiver_dance",                        AttackEffectType.QUIVER_DANCE);
        TEXT_TO_TYPE = Collections.unmodifiableMap(m);
    }

    private final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> handlers;

    /**
     * Constructs the resolver with all effect handlers pre-registered.
     */
    public AttackEffectResolver() {
        final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> m =
                new EnumMap<>(AttackEffectType.class);
        m.put(AttackEffectType.NONE,
                (amount, ctx) -> { });
        m.put(AttackEffectType.COIN_FLIP_EXTRA_DAMAGE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.DERANGED_DANCE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.APPLY_POISON,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.ENVENENADO));
        m.put(AttackEffectType.APPLY_BURN,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.QUEMADO));
        m.put(AttackEffectType.APPLY_PARALYSIS,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.PARALIZADO));
        m.put(AttackEffectType.APPLY_SLEEP,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.DORMIDO));
        m.put(AttackEffectType.APPLY_CONFUSION,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.CONFUNDIDO));
        m.put(AttackEffectType.HEAL_SELF,
                (amount, ctx) -> ctx.getAttacker().heal(amount));
        m.put(AttackEffectType.SELF_DAMAGE,
                (amount, ctx) -> {
                    boolean hasProtectionCube = ctx.getAttacker().getAttachedTool()
                            .map(t -> t.getToolEffectId() == ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId.PROTECTION_CUBE)
                            .orElse(false);
                    if (!hasProtectionCube) {
                        ctx.getAttacker().addDamageCounters(amount / DAMAGE_PER_COUNTER);
                    }
                });
        m.put(AttackEffectType.DISCARD_ENERGY,
                (amount, ctx) -> ctx.getAttacker().removeEnergies(amount));
        m.put(AttackEffectType.BENCH_DAMAGE,
                (amount, ctx) -> {
                    // Bench damage: apply N damage to each of the opponent's Benched Pokémon.
                    // Weakness and Resistance do not apply to Benched Pokémon (XY1 §3).
                    final int counters = amount / DAMAGE_PER_COUNTER;
                    ctx.getDefenderBench().forEach(benched -> benched.addDamageCounters(counters));
                });
        m.put(AttackEffectType.COIN_FLIP_POISON,
                (amount, ctx) -> { if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.ENVENENADO); });
        m.put(AttackEffectType.COIN_FLIP_BURN,
                (amount, ctx) -> { if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.QUEMADO); });
        m.put(AttackEffectType.COIN_FLIP_PARALYSIS,
                (amount, ctx) -> { if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.PARALIZADO); });
        m.put(AttackEffectType.COIN_FLIP_SLEEP,
                (amount, ctx) -> { if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.DORMIDO); });
        m.put(AttackEffectType.COIN_FLIP_CONFUSION,
                (amount, ctx) -> { if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.CONFUNDIDO); });
        m.put(AttackEffectType.PREVENT_DAMAGE,
                (amount, ctx) -> ctx.getAttackerStatusManager().setDamagePreventedNextTurn(true));
        m.put(AttackEffectType.COIN_FLIP_PREVENT_DAMAGE,
                (amount, ctx) -> {
                    if (ctx.getCoinFlipper().flip()) {
                        ctx.getAttackerStatusManager().setDamagePreventedNextTurn(true);
                    }
                });
        m.put(AttackEffectType.COIN_FLIP_PREVENT_DAMAGE_60_OR_LESS,
                (amount, ctx) -> {
                    if (ctx.getCoinFlipper().flip()) {
                        ctx.getAttackerStatusManager().setDamagePreventedIf60OrLessNextTurn(true);
                    }
                });
        m.put(AttackEffectType.QUIVER_DANCE,
                (amount, ctx) -> {
                    final PlayerRuntime runtime = ctx.getAttackerRuntime();
                    if (runtime != null && runtime.getDeck().size() > 0) {
                        ctx.getMatchSession().setPendingSelectionRequest(
                                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.QUIVER_DANCE,
                                        null,
                                        1,
                                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK
                                )
                        );
                        ctx.getMatchSession().getTurnManager().interruptMainPhase();
                    }
                });
        m.put(AttackEffectType.DISABLE_ATTACK,
                (amount, ctx) -> {
                    var attacks = ctx.getDefender().getAttacks();
                    if (attacks != null && !attacks.isEmpty()) {
                        ctx.getDefenderStatusManager().setDisabledAttackName(attacks.get(0).name());
                    }
                });
        // FR-TODO: move_energy requires attacker bench runtime access — deferred.
        m.put(AttackEffectType.MOVE_ENERGY,
                (amount, ctx) -> { });
        m.put(AttackEffectType.FORCE_SWITCH,
                (amount, ctx) -> {
                    final PlayerRuntime attacker = ctx.getAttackerRuntime();
                    if (attacker != null && !attacker.getBench().getAll().isEmpty()) {
                        final BattlePokemonState oldActive = attacker.getActivePokemon();
                        final BattlePokemonState newActive = attacker.getBench().promote(0);
                        attacker.setActivePokemon(newActive);
                        attacker.getBench().place(oldActive);
                        attacker.getStatusEffectManager().clearAll();
                        attacker.recordPokemonEntered(oldActive);
                    }
                    final PlayerRuntime defender = ctx.getDefenderRuntime();
                    if (defender != null && !defender.getBench().getAll().isEmpty()) {
                        final BattlePokemonState oldActive = defender.getActivePokemon();
                        final BattlePokemonState newActive = defender.getBench().promote(0);
                        defender.setActivePokemon(newActive);
                        defender.getBench().place(oldActive);
                        defender.getStatusEffectManager().clearAll();
                        defender.recordPokemonEntered(oldActive);
                    }
                });
        m.put(AttackEffectType.COIN_FLIP_SWITCH_SELF,
                (amount, ctx) -> {
                    if (ctx.getCoinFlipper().flip()) {
                        final PlayerRuntime attacker = ctx.getAttackerRuntime();
                        if (attacker != null && !attacker.getBench().getAll().isEmpty()) {
                            final BattlePokemonState oldActive = attacker.getActivePokemon();
                            final BattlePokemonState newActive = attacker.getBench().promote(0);
                            attacker.setActivePokemon(newActive);
                            attacker.getBench().place(oldActive);
                            attacker.getStatusEffectManager().clearAll();
                            attacker.recordPokemonEntered(oldActive);
                        }
                    }
                });
        m.put(AttackEffectType.HEAL_ANY,
                (amount, ctx) -> {
                    final PlayerRuntime attacker = ctx.getAttackerRuntime();
                    if (attacker != null) {
                        BattlePokemonState target = null;
                        int maxDamage = 0;
                        if (attacker.getActivePokemon() != null) {
                            maxDamage = attacker.getActivePokemon().getDamageCounters();
                            target = attacker.getActivePokemon();
                        }
                        for (BattlePokemonState p : attacker.getBench().getAll()) {
                            if (p.getDamageCounters() > maxDamage) {
                                maxDamage = p.getDamageCounters();
                                target = p;
                            }
                        }
                        if (target != null) {
                            target.heal(amount);
                        }
                    }
                });
        m.put(AttackEffectType.HEAL_BENCH,
                (amount, ctx) -> {
                    final PlayerRuntime attacker = ctx.getAttackerRuntime();
                    if (attacker != null) {
                        BattlePokemonState target = null;
                        int maxDamage = 0;
                        for (BattlePokemonState p : attacker.getBench().getAll()) {
                            if (p.getDamageCounters() > maxDamage) {
                                maxDamage = p.getDamageCounters();
                                target = p;
                            }
                        }
                        if (target != null) {
                            target.heal(amount);
                        }
                    }
                });
        m.put(AttackEffectType.HEAL_ALL,
                (amount, ctx) -> {
                    final PlayerRuntime attacker = ctx.getAttackerRuntime();
                    if (attacker != null) {
                        if (attacker.getActivePokemon() != null) {
                            attacker.getActivePokemon().heal(amount);
                        }
                        for (BattlePokemonState p : attacker.getBench().getAll()) {
                            p.heal(amount);
                        }
                    }
                });
        m.put(AttackEffectType.DISCARD_OPPONENT_ENERGY,
                (amount, ctx) -> {
                    final BattlePokemonState defender = ctx.getDefender();
                    if (defender != null && !defender.getAttachedEnergies().isEmpty()) {
                        defender.removeEnergies(amount);
                    }
                });
        m.put(AttackEffectType.COIN_FLIP_DISCARD_OPPONENT_ENERGY,
                (amount, ctx) -> {
                    if (ctx.getCoinFlipper().flip()) {
                        final BattlePokemonState defender = ctx.getDefender();
                        if (defender != null && !defender.getAttachedEnergies().isEmpty()) {
                            defender.removeEnergies(amount);
                        }
                    }
                });
        m.put(AttackEffectType.STOKE,
                (amount, ctx) -> {
                    if (ctx.getCoinFlipper().flip()) {
                        final PlayerRuntime runtime = ctx.getAttackerRuntime();
                        if (runtime != null) {
                            final List<Card> basicEnergies = runtime.getDeck().searchAndRemove(
                                    c -> c instanceof EnergyCard ec && ec.isBasic(),
                                    3
                            );
                            for (Card card : basicEnergies) {
                                ctx.getAttacker().attachEnergy((EnergyCard) card);
                            }
                            runtime.getDeck().shuffle();
                        }
                    }
                });
        m.put(AttackEffectType.COMBUSTION_BLAST,
                (amount, ctx) -> {
                    ctx.getAttackerStatusManager().setSelfDisabledAttackName("Combustion Blast");
                    ctx.getAttackerStatusManager().setSelfDisabledAttackSetThisTurn(true);
                });
        m.put(AttackEffectType.SCORCHING_FANG,
                (amount, ctx) -> {
                    if (ctx.isScorchingFangDiscarded()) {
                        EnergyCard fireEnergy = ctx.getAttacker().getAttachedEnergyCards().stream()
                                .filter(ec -> ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE || ec.isProvidesAllTypes())
                                .findFirst()
                                .orElse(null);
                        if (fireEnergy != null) {
                            int energyIdx = -1;
                            int currentEnergyCount = 0;
                            for (int i = 0; i < ctx.getAttacker().getAttachedEnergyCards().size(); i++) {
                                final EnergyCard ec = ctx.getAttacker().getAttachedEnergyCards().get(i);
                                if (ec == fireEnergy) {
                                    energyIdx = currentEnergyCount;
                                    break;
                                }
                                currentEnergyCount += ec.getEnergyCount();
                            }
                            if (energyIdx != -1) {
                                ctx.getAttacker().removeEnergies(java.util.List.of(energyIdx));
                                if (ctx.getAttackerRuntime() != null) {
                                    ctx.getAttackerRuntime().getDiscardPile().add(fireEnergy);
                                }
                            }
                        }
                    }
                });
        m.put(AttackEffectType.CLAIRVOYANT_EYE,
                (amount, ctx) -> {
                    final PlayerRuntime runtime = ctx.getAttackerRuntime();
                    if (runtime != null) {
                        final int count = Math.min(3, runtime.getDeck().size());
                        if (count > 0) {
                            ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CLAIRVOYANT_EYE,
                                            null,
                                            count,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK
                                    )
                            );
                            ctx.getMatchSession().getTurnManager().interruptMainPhase();
                        }
                    }
                });
        m.put(AttackEffectType.CALL_FOR_FAMILY,
                (amount, ctx) -> {
                    final PlayerRuntime runtime = ctx.getAttackerRuntime();
                    if (runtime != null) {
                        int freeBenchSpace = 5 - runtime.getBench().getAll().size();
                        int maxSelect = Math.min(amount, freeBenchSpace);
                        if (maxSelect > 0 && runtime.getDeck().size() > 0) {
                            ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CALL_FOR_FAMILY,
                                            null,
                                            maxSelect,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK
                                    )
                            );
                            ctx.getMatchSession().getTurnManager().interruptMainPhase();
                        }
                    }
                });
        this.handlers = Collections.unmodifiableMap(m);
    }

    /**
     * Resolves the effect descriptor and executes the appropriate handler against {@code ctx}.
     *
     * @param ctx the current attack context (never null)
     */
    public void apply(final AttackContext ctx) {
        final String effectText = ctx.getEffectText();
        final AttackEffectType type = resolveType(effectText);
        final int amount = extractAmount(effectText);
        final BiConsumer<Integer, AttackContext> handler = handlers.get(type);
        if (handler != null) {
            handler.accept(amount, ctx);
        }
    }

    /**
     * Maps an effect-text descriptor to its {@link AttackEffectType}.
     * Package-private for direct test access.
     *
     * @param effectText the raw descriptor (may be null or blank)
     * @return resolved type, or {@link AttackEffectType#NONE} for unknown/blank input
     */
    AttackEffectType resolveType(final String effectText) {
        if (effectText == null || effectText.isBlank()) {
            return AttackEffectType.NONE;
        }
        final String key = effectText.contains(":")
                ? effectText.substring(0, effectText.indexOf(':'))
                : effectText;
        return TEXT_TO_TYPE.getOrDefault(key.toLowerCase(), AttackEffectType.NONE);
    }

    /**
     * Extracts the numeric amount from a descriptor of the form {@code "keyword:N"}.
     * Package-private for direct test access.
     *
     * @param effectText the raw descriptor
     * @return parsed amount, or {@code 0} if none present or parsing fails
     */
    int extractAmount(final String effectText) {
        if (effectText == null || !effectText.contains(":")) {
            return 0;
        }
        try {
            return Integer.parseInt(effectText.substring(effectText.indexOf(':') + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
