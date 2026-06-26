package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
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
        m.put("prevent_damage_60_or_less", AttackEffectType.PREVENT_DAMAGE_60_OR_LESS);
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
        m.put("heal_and_sleep",                      AttackEffectType.HEAL_SELF_AND_SLEEP);
        m.put("discard_deck_self",                   AttackEffectType.DISCARD_DECK_SELF);
        m.put("coin_flip_discard_energy",            AttackEffectType.COIN_FLIP_DISCARD_ENERGY);
        m.put("coin_flips_until_tails_discard_opponent_energy", AttackEffectType.COIN_FLIPS_UNTIL_TAILS_DISCARD_OPPONENT_ENERGY);
        m.put("coin_flips_until_tails_extra",        AttackEffectType.COIN_FLIPS_UNTIL_TAILS_EXTRA);
        m.put("powerful_friends",                    AttackEffectType.POWERFUL_FRIENDS);
        m.put("damage_per_energy_type",             AttackEffectType.DAMAGE_PER_ENERGY_TYPE);
        m.put("damage_if_target_damaged",            AttackEffectType.DAMAGE_IF_TARGET_DAMAGED);
        m.put("damage_minus_per_counter",            AttackEffectType.DAMAGE_MINUS_PER_COUNTER);
        m.put("revenge_damage",                      AttackEffectType.REVENGE_DAMAGE);
        m.put("damage_per_opponent_prize",           AttackEffectType.DAMAGE_PER_OPPONENT_PRIZE);
        m.put("smokescreen",                         AttackEffectType.SMOKESCREEN);
        m.put("coin_flip_self_disable",              AttackEffectType.COIN_FLIP_SELF_DISABLE);
        m.put("prevent_damage_20",                   AttackEffectType.PREVENT_DAMAGE_20);
        m.put("discard_stadium",                     AttackEffectType.DISCARD_STADIUM);
        m.put("discard_opponent_tool",               AttackEffectType.DISCARD_OPPONENT_TOOL);
        m.put("switch_self",                         AttackEffectType.SWITCH_SELF);
        m.put("discard_opponent_hand",               AttackEffectType.DISCARD_OPPONENT_HAND);
        m.put("discard_hand_energy_multiply_damage", AttackEffectType.DISCARD_HAND_ENERGY_MULTIPLY_DAMAGE);
        m.put("damage_all_opponents",                AttackEffectType.DAMAGE_ALL_OPPONENTS);
        m.put("search_deck_any",                     AttackEffectType.BRILLIANT_SEARCH);
        m.put("look_top_4_take_2_discard_rest",      AttackEffectType.BURIED_TREASURE_HUNT);
        m.put("exciting_shake",                      AttackEffectType.EXCITING_SHAKE);
        m.put("coin_flip_skip_opponent_draw",        AttackEffectType.COIN_FLIP_SKIP_OPPONENT_DRAW);
        m.put("discard_opponent_deck",               AttackEffectType.DISCARD_OPPONENT_DECK);
        m.put("dual_bullet",                         AttackEffectType.DUAL_BULLET);
        m.put("pain_pellets",                        AttackEffectType.PAIN_PELLETS);
        m.put("triple_poison",                       AttackEffectType.TRIPLE_POISON);
        m.put("strong_gust",                         AttackEffectType.STRONG_GUST);
        m.put("ignore_resistance",                   AttackEffectType.IGNORE_RESISTANCE);
        m.put("block_retreat",                       AttackEffectType.BLOCK_RETREAT);
        m.put("damage_per_opponent_all_energy",      AttackEffectType.DAMAGE_PER_OPPONENT_ALL_ENERGY);
        m.put("bench_damage_one",                    AttackEffectType.BENCH_DAMAGE_ONE);
        m.put("force_switch_opponent",                AttackEffectType.FORCE_SWITCH_OPPONENT);
        m.put("draw_cards",                          AttackEffectType.DRAW_CARDS);
        m.put("damage_times_self_counters",          AttackEffectType.DAMAGE_TIMES_SELF_COUNTERS);
        m.put("damage_per_retreat_cost",              AttackEffectType.DAMAGE_PER_RETREAT_COST);
        m.put("place_counters_opponent",              AttackEffectType.PLACE_COUNTERS_OPPONENT);
        m.put("place_counters_distributed",          AttackEffectType.PLACE_COUNTERS_DISTRIBUTED);
        m.put("move_opponent_counters",              AttackEffectType.MOVE_OPPONENT_COUNTERS);
        m.put("discard_opponent_hand_to_limit",      AttackEffectType.DISCARD_OPPONENT_HAND_TO_LIMIT);
        m.put("place_opponent_basic_from_discard",    AttackEffectType.PLACE_OPPONENT_BASIC_FROM_DISCARD);
        m.put("discard_trainer_from_opponent_hand",  AttackEffectType.DISCARD_TRAINER_FROM_OPPONENT_HAND);
        m.put("shuffle_pokemon_from_discard",        AttackEffectType.SHUFFLE_POKEMON_FROM_DISCARD);
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
        m.put(AttackEffectType.DAMAGE_PER_OPPONENT_ALL_ENERGY,
                (amount, ctx) -> { });
        m.put(AttackEffectType.DRAW_CARDS,
                (amount, ctx) -> {
                    final PlayerRuntime attacker = ctx.getAttackerRuntime();
                    if (attacker != null) {
                        attacker.getHand().addCards(attacker.getDeck().drawMultiple(amount));
                    }
                });
        m.put(AttackEffectType.DAMAGE_TIMES_SELF_COUNTERS,
                (amount, ctx) -> { });
        m.put(AttackEffectType.DAMAGE_PER_RETREAT_COST,
                (amount, ctx) -> { });
        m.put(AttackEffectType.PLACE_COUNTERS_OPPONENT,
                (amount, ctx) -> {
                    if (ctx.getDefender() != null) {
                        ctx.getDefender().addDamageCounters(amount);
                        if (ctx.getDefender().getDamageCounters() * 10 >= ctx.getDefender().getMaxHp()) {
                            ctx.getMatchSession().getKnockoutHandler().onKnockout(ctx.getDefender(), ctx.getDefender().isEx() ? 2 : 1);
                        }
                    }
                });
        m.put(AttackEffectType.PLACE_COUNTERS_DISTRIBUTED,
                (amount, ctx) -> {
                    ctx.getMatchSession().setPendingSelectionRequest(
                            new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                    ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CURSED_DROP,
                                    null,
                                    amount,
                                    ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                            )
                    );
                    if (ctx.getMatchSession().getTurnManager() != null) {
                        ctx.getMatchSession().getTurnManager().interruptMainPhase();
                    }
                });
        m.put(AttackEffectType.MOVE_OPPONENT_COUNTERS,
                (amount, ctx) -> {
                    ctx.getMatchSession().setPendingSelectionRequest(
                            new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                    ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EAR_INFLUENCE,
                                    null,
                                    20,
                                    ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                            )
                    );
                    if (ctx.getMatchSession().getTurnManager() != null) {
                        ctx.getMatchSession().getTurnManager().interruptMainPhase();
                    }
                });
        m.put(AttackEffectType.DISCARD_OPPONENT_HAND_TO_LIMIT,
                (amount, ctx) -> {
                    final PlayerRuntime defender = ctx.getDefenderRuntime();
                    if (defender != null) {
                        List<Card> hand = defender.getHand().getCards();
                        if (hand.size() > amount) {
                            List<Card> mutableHand = new java.util.ArrayList<>(hand);
                            java.util.Collections.shuffle(mutableHand);
                            int toDiscard = hand.size() - amount;
                            for (int i = 0; i < toDiscard; i++) {
                                Card c = mutableHand.get(i);
                                defender.getHand().removeCard(c.getCardId());
                                defender.getDiscardPile().add(c);
                            }
                        }
                    }
                });
        m.put(AttackEffectType.PLACE_OPPONENT_BASIC_FROM_DISCARD,
                (amount, ctx) -> {
                    final PlayerRuntime defender = ctx.getDefenderRuntime();
                    if (defender != null && defender.getBench().getAll().size() < 5) {
                        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard basic = null;
                        for (Card card : defender.getDiscardPile().getCards()) {
                            if (card instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC) {
                                basic = pc;
                                break;
                            }
                        }
                        if (basic != null) {
                            defender.getDiscardPile().remove(basic);
                            ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon state = new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon(basic);
                            state.setOwner(defender);
                            defender.getBench().place(state);
                        }
                    }
                });
        m.put(AttackEffectType.DISCARD_TRAINER_FROM_OPPONENT_HAND,
                (amount, ctx) -> {
                    final PlayerRuntime opponent = ctx.getDefenderRuntime();
                    if (opponent != null) {
                        boolean hasTrainer = opponent.getHand().getCards().stream()
                                .anyMatch(c -> c instanceof TrainerCard);
                        if (hasTrainer) {
                            ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FANG_SNIPE,
                                            null,
                                            1,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                                    )
                            );
                            if (ctx.getMatchSession().getTurnManager() != null) {
                                ctx.getMatchSession().getTurnManager().interruptMainPhase();
                            }
                        }
                    }
                });
        m.put(AttackEffectType.SHUFFLE_POKEMON_FROM_DISCARD,
                (amount, ctx) -> {
                    final PlayerRuntime attacker = ctx.getAttackerRuntime();
                    if (attacker != null) {
                        long pokemonCount = attacker.getDiscardPile().getCards().stream()
                                .filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard).count();
                        int toSelect = Math.min(amount, (int) pokemonCount);
                        if (toSelect > 0) {
                            ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.RESCUE,
                                            null,
                                            toSelect,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE
                                    )
                            );
                            if (ctx.getMatchSession().getTurnManager() != null) {
                                ctx.getMatchSession().getTurnManager().interruptMainPhase();
                            }
                        }
                    }
                });
        m.put(AttackEffectType.SMOKESCREEN,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.PRECISION_BAJA));
        m.put(AttackEffectType.COIN_FLIP_SELF_DISABLE,
                (amount, ctx) -> {
                    if (!ctx.getCoinFlipper().flip()) {
                        ctx.getAttackerStatusManager().setSelfDisabledNextTurn(true);
                        ctx.getAttackerStatusManager().setSelfDisabledNextTurnSetThisTurn(true);
                    }
                });
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
        m.put(AttackEffectType.PREVENT_DAMAGE_60_OR_LESS,
                (amount, ctx) -> ctx.getAttackerStatusManager().setDamagePreventedIf60OrLessNextTurn(true));
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
                    if (runtime == null) return;
                    final boolean hasBasicEnergy = runtime.getDeck().getCards().stream()
                            .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.isBasic());
                    if (hasBasicEnergy) {
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
                    // If no basic energy, the attack still consumes the turn (no selection needed)
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
        m.put(AttackEffectType.FORCE_SWITCH_OPPONENT,
                (amount, ctx) -> {
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
        m.put(AttackEffectType.BENCH_DAMAGE_ONE,
                (amount, ctx) -> {
                    final PlayerRuntime opponent = ctx.getDefenderRuntime();
                    if (opponent != null) {
                        List<BattlePokemonState> bench = opponent.getBench().getAll();
                        if (bench.size() == 1) {
                            bench.get(0).addDamageCounters(amount / DAMAGE_PER_COUNTER);
                            if (bench.get(0).getDamageCounters() * 10 >= bench.get(0).getMaxHp()) {
                                ctx.getMatchSession().getKnockoutHandler().onKnockout(bench.get(0), bench.get(0).isEx() ? 2 : 1);
                            }
                        } else if (bench.size() > 1) {
                            ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BENCH_DAMAGE_ONE,
                                            null,
                                            1,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                                    )
                            );
                            if (ctx.getMatchSession().getTurnManager() != null) {
                                ctx.getMatchSession().getTurnManager().interruptMainPhase();
                            }
                        }
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
                    if (runtime == null) return;
                    int freeBenchSpace = 5 - runtime.getBench().getAll().size();
                    int toPlace = Math.min(amount, freeBenchSpace);
                    // Search deck in order for basic Pokémon and place automatically
                    int placed = 0;
                    while (placed < toPlace) {
                        final java.util.List<ar.edu.utn.frc.tup.piii.engine.model.Card> found =
                                runtime.getDeck().searchAndRemove(
                                        c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc
                                                && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC,
                                        1
                                );
                        if (found.isEmpty()) break;
                        final ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon inPlay =
                                new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon(
                                        (ar.edu.utn.frc.tup.piii.engine.model.PokemonCard) found.get(0));
                        runtime.getBench().place(inPlay);
                        runtime.recordPokemonEntered(inPlay);
                        placed++;
                    }
                    runtime.getDeck().shuffle();
                });
        m.put(AttackEffectType.HEAL_SELF_AND_SLEEP,
                (amount, ctx) -> {
                    ctx.getAttacker().heal(amount);
                    if (ctx.getAttackerRuntime() != null) {
                        ctx.getAttackerRuntime().getStatusEffectManager().apply(StatusEffectType.DORMIDO);
                    }
                });
        m.put(AttackEffectType.DISCARD_DECK_SELF,
                (amount, ctx) -> {
                    if (ctx.getAttackerRuntime() != null) {
                        final List<Card> discarded = ctx.getAttackerRuntime().getDeck().drawMultiple(amount);
                        ctx.getAttackerRuntime().getDiscardPile().addAll(discarded);
                    }
                });
        m.put(AttackEffectType.COIN_FLIP_DISCARD_ENERGY,
                (amount, ctx) -> {
                    if (!ctx.getCoinFlipper().flip()) { // cruz / tails
                        ctx.getAttacker().removeEnergies(amount);
                    }
                });
        m.put(AttackEffectType.COIN_FLIPS_UNTIL_TAILS_DISCARD_OPPONENT_ENERGY,
                (amount, ctx) -> {
                    int heads = 0;
                    while (ctx.getCoinFlipper().flip()) {
                        heads++;
                    }
                    if (heads > 0) {
                        final BattlePokemonState defender = ctx.getDefender();
                        if (defender != null && !defender.getAttachedEnergies().isEmpty()) {
                            defender.removeEnergies(heads);
                        }
                    }
                });
        m.put(AttackEffectType.COIN_FLIPS_UNTIL_TAILS_EXTRA,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.POWERFUL_FRIENDS,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.DAMAGE_PER_ENERGY_TYPE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.DAMAGE_IF_TARGET_DAMAGED,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.DAMAGE_MINUS_PER_COUNTER,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.REVENGE_DAMAGE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.DAMAGE_PER_OPPONENT_PRIZE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.PREVENT_DAMAGE_20,
                (amount, ctx) -> {
                    ctx.getAttackerStatusManager().setDamageReducedBy20NextTurn(true);
                });
        m.put(AttackEffectType.DISCARD_STADIUM,
                (amount, ctx) -> {
                    final int ownerIdx = ctx.getMatchSession().getBoard().getActiveStadiumOwnerIndex();
                    final TrainerCard stadium = ctx.getMatchSession().getBoard().removeStadium();
                    if (stadium != null) {
                        int finalOwnerIdx = ownerIdx;
                        if (finalOwnerIdx == -1 && ctx.getMatchSession().getTurnManager() != null) {
                            finalOwnerIdx = ctx.getMatchSession().getTurnManager().activePlayerIndex();
                        }
                        if (finalOwnerIdx != -1) {
                            ctx.getMatchSession().getPlayerRuntime(finalOwnerIdx).getDiscardPile().add(stadium);
                        } else if (ctx.getAttackerRuntime() != null) {
                            ctx.getAttackerRuntime().getDiscardPile().add(stadium);
                        }
                    }
                });
        m.put(AttackEffectType.DISCARD_OPPONENT_TOOL,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.SWITCH_SELF,
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
                });
        m.put(AttackEffectType.DISCARD_OPPONENT_HAND,
                (amount, ctx) -> {
                    final PlayerRuntime opponent = ctx.getDefenderRuntime();
                    if (opponent != null && !opponent.getHand().getCards().isEmpty()) {
                        ctx.getMatchSession().setPendingSelectionRequest(
                                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FLASH_CLAW,
                                        null,
                                        amount,
                                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                                )
                        );
                        if (ctx.getMatchSession().getTurnManager() != null) {
                            ctx.getMatchSession().getTurnManager().interruptMainPhase();
                        }
                    }
                });
        m.put(AttackEffectType.DISCARD_HAND_ENERGY_MULTIPLY_DAMAGE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.DAMAGE_ALL_OPPONENTS,
                (amount, ctx) -> {
                    final int counters = amount / DAMAGE_PER_COUNTER;
                    ctx.getDefenderBench().forEach(benched -> benched.addDamageCounters(counters));
                });
        m.put(AttackEffectType.BRILLIANT_SEARCH,
                (amount, ctx) -> {
                    final PlayerRuntime runtime = ctx.getAttackerRuntime();
                    if (runtime == null) return;
                    final int deckSize = runtime.getDeck().size();
                    if (deckSize > 0) {
                        final int selectAmount = Math.min(amount, deckSize);
                        ctx.getMatchSession().setPendingSelectionRequest(
                                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BRILLIANT_SEARCH,
                                        null,
                                        selectAmount,
                                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK
                                )
                        );
                        if (ctx.getMatchSession().getTurnManager() != null) {
                            ctx.getMatchSession().getTurnManager().interruptMainPhase();
                        }
                    }
                });
        m.put(AttackEffectType.BURIED_TREASURE_HUNT,
                (amount, ctx) -> {
                    final PlayerRuntime runtime = ctx.getAttackerRuntime();
                    if (runtime == null) return;
                    final int deckSize = runtime.getDeck().size();
                    if (deckSize > 0) {
                        final int selectAmount = Math.min(2, deckSize);
                        ctx.getMatchSession().setPendingSelectionRequest(
                                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BURIED_TREASURE_HUNT,
                                        null,
                                        selectAmount,
                                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK
                                )
                        );
                        if (ctx.getMatchSession().getTurnManager() != null) {
                            ctx.getMatchSession().getTurnManager().interruptMainPhase();
                        }
                    }
                });
        m.put(AttackEffectType.EXCITING_SHAKE,
                (amount, ctx) -> {
                    ctx.getAttackerStatusManager().setExcitingShakeActiveNextTurn(true);
                    ctx.getAttackerStatusManager().setExcitingShakeActiveNextTurnSetThisTurn(true);
                });
        m.put(AttackEffectType.COIN_FLIP_SKIP_OPPONENT_DRAW,
                (amount, ctx) -> {
                    if (ctx.getCoinFlipper().flip()) {
                        ctx.getDefenderStatusManager().setDrawStepBlocked(true);
                    }
                });
        m.put(AttackEffectType.DISCARD_OPPONENT_DECK,
                (amount, ctx) -> {
                    final PlayerRuntime opponent = ctx.getDefenderRuntime();
                    if (opponent != null && !opponent.getDeck().isEmpty()) {
                        final Card discarded = opponent.getDeck().draw();
                        opponent.getDiscardPile().add(discarded);
                    }
                });
        m.put(AttackEffectType.DUAL_BULLET,
                (amount, ctx) -> {
                    final PlayerRuntime opponent = ctx.getDefenderRuntime();
                    if (opponent != null) {
                        int targets = Math.min(2, (opponent.getActivePokemon() != null ? 1 : 0) + opponent.getBench().getAll().size());
                        if (targets > 0) {
                            ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.DUAL_BULLET,
                                            null,
                                            targets,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                                    )
                            );
                            if (ctx.getMatchSession().getTurnManager() != null) {
                                ctx.getMatchSession().getTurnManager().interruptMainPhase();
                            }
                        }
                    }
                });
        m.put(AttackEffectType.PAIN_PELLETS,
                (amount, ctx) -> {
                    final PlayerRuntime opponent = ctx.getDefenderRuntime();
                    if (opponent != null) {
                        ctx.getMatchSession().setPendingSelectionRequest(
                                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAIN_PELLETS,
                                        null,
                                        1,
                                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                                    )
                            );
                            if (ctx.getMatchSession().getTurnManager() != null) {
                                ctx.getMatchSession().getTurnManager().interruptMainPhase();
                            }
                        }
                    });
        m.put(AttackEffectType.TRIPLE_POISON,
                (amount, ctx) -> {
                    ctx.getDefenderStatusManager().apply(StatusEffectType.ENVENENADO, 3);
                });
        m.put(AttackEffectType.STRONG_GUST,
                (amount, ctx) -> {
                    ctx.getAttackerStatusManager().setStrongGustUsedLastTurn(true);
                    ctx.getAttackerStatusManager().setStrongGustUsedLastTurnSetThisTurn(true);
                });
        m.put(AttackEffectType.IGNORE_RESISTANCE,
                (amount, ctx) -> { });
        m.put(AttackEffectType.BLOCK_RETREAT,
                (amount, ctx) -> {
                    ctx.getDefenderStatusManager().setRetreatBlockedNextTurn(true);
                    ctx.getDefenderStatusManager().setRetreatBlockedNextTurnSetThisTurn(true);
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
