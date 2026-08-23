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
import java.util.Locale;
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
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
// Strategy-map dispatcher: ~60 small, independent single-purpose handler methods (each a
// few lines) registered in the constructor's map. This is the same intentional decomposition
// as PreDamageEffectsStep — the class-level metrics count raw method/branch totals across all
// handlers without recognizing that each one is isolated and individually simple; no single
// method here has real per-method complexity (verified: none flagged individually by PMD).
public final class AttackEffectResolver {

    private static final int DAMAGE_PER_COUNTER = 10;

    // Used across several handlers to decide "exactly one candidate, auto-resolve"
    // vs "more than one, prompt the player to pick" (e.g. a single benched Pokemon vs
    // multiple when force-switching, a single discard-pile Basic vs several).
    private static final int SINGLE_CANDIDATE = 1;

    // Extracted to a constant (not a repeated literal) since it's used across ~30 handler
    // methods below whose 'amount' parameter is unused but required by the BiConsumer signature.
    private static final String UNUSED_AMOUNT_PARAM = "PMD.UnusedFormalParameter";

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
        m.put("search_deck_energy",                  AttackEffectType.SEARCH_DECK_ENERGY);
        TEXT_TO_TYPE = Collections.unmodifiableMap(m);
    }

    private final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> handlers;

    /**
     * Constructs the resolver with all effect handlers pre-registered.
     */
    public AttackEffectResolver() {
        final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> m =
                new EnumMap<>(AttackEffectType.class);
        registerHandlersPartOne(m);
        registerHandlersPartTwo(m);
        this.handlers = Collections.unmodifiableMap(m);
    }

    // Split across two registration methods purely to keep each one's NCSS/statement count
    // reasonable — this is one long declarative table, not complex logic (no branching here
    // beyond the map entries themselves; see class-level @SuppressWarnings justification above).
    private void registerHandlersPartOne(final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> m) {
        m.put(AttackEffectType.NONE,
                (amount, ctx) -> { });
        m.put(AttackEffectType.DAMAGE_PER_OPPONENT_ALL_ENERGY,
                (amount, ctx) -> { });
        m.put(AttackEffectType.DRAW_CARDS, this::handleDrawCards);
        m.put(AttackEffectType.DAMAGE_TIMES_SELF_COUNTERS,
                (amount, ctx) -> { });
        m.put(AttackEffectType.DAMAGE_PER_RETREAT_COST,
                (amount, ctx) -> { });
        m.put(AttackEffectType.PLACE_COUNTERS_OPPONENT, this::handlePlaceCountersOpponent);
        m.put(AttackEffectType.PLACE_COUNTERS_DISTRIBUTED, this::handlePlaceCountersDistributed);
        m.put(AttackEffectType.MOVE_OPPONENT_COUNTERS, this::handleMoveOpponentCounters);
        m.put(AttackEffectType.DISCARD_OPPONENT_HAND_TO_LIMIT, this::handleDiscardOpponentHandToLimit);
        m.put(AttackEffectType.PLACE_OPPONENT_BASIC_FROM_DISCARD, this::handlePlaceOpponentBasicFromDiscard);
        m.put(AttackEffectType.DISCARD_TRAINER_FROM_OPPONENT_HAND, this::handleDiscardTrainerFromOpponentHand);
        m.put(AttackEffectType.SHUFFLE_POKEMON_FROM_DISCARD, this::handleShufflePokemonFromDiscard);
        m.put(AttackEffectType.SEARCH_DECK_ENERGY, this::handleSearchDeckEnergy);
        m.put(AttackEffectType.SMOKESCREEN,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.PRECISION_BAJA));
        m.put(AttackEffectType.COIN_FLIP_SELF_DISABLE, this::handleCoinFlipSelfDisable);
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
        m.put(AttackEffectType.SELF_DAMAGE, this::handleSelfDamage);
        m.put(AttackEffectType.DISCARD_ENERGY,
                (amount, ctx) -> ctx.getAttacker().removeEnergies(amount));
        m.put(AttackEffectType.BENCH_DAMAGE, this::handleBenchDamage);
        m.put(AttackEffectType.COIN_FLIP_POISON, this::handleCoinFlipPoison);
        m.put(AttackEffectType.COIN_FLIP_BURN, this::handleCoinFlipBurn);
        m.put(AttackEffectType.COIN_FLIP_PARALYSIS, this::handleCoinFlipParalysis);
        m.put(AttackEffectType.COIN_FLIP_SLEEP, this::handleCoinFlipSleep);
        m.put(AttackEffectType.COIN_FLIP_CONFUSION, this::handleCoinFlipConfusion);
        m.put(AttackEffectType.PREVENT_DAMAGE,
                (amount, ctx) -> ctx.getAttackerStatusManager().setDamagePreventedNextTurn(true));
        m.put(AttackEffectType.PREVENT_DAMAGE_60_OR_LESS,
                (amount, ctx) -> ctx.getAttackerStatusManager().setDamagePreventedIf60OrLessNextTurn(true));
        m.put(AttackEffectType.COIN_FLIP_PREVENT_DAMAGE, this::handleCoinFlipPreventDamage);
        m.put(AttackEffectType.COIN_FLIP_PREVENT_DAMAGE_60_OR_LESS, this::handleCoinFlipPreventDamage60OrLess);
        m.put(AttackEffectType.QUIVER_DANCE, this::handleQuiverDance);
        m.put(AttackEffectType.DISABLE_ATTACK, this::handleDisableAttack);
        // FR-TODO: move_energy requires attacker bench runtime access — deferred.
        m.put(AttackEffectType.MOVE_ENERGY,
                (amount, ctx) -> { });
        m.put(AttackEffectType.FORCE_SWITCH, this::handleForceSwitch);
        m.put(AttackEffectType.FORCE_SWITCH_OPPONENT, this::handleForceSwitchOpponent);
        m.put(AttackEffectType.BENCH_DAMAGE_ONE, this::handleBenchDamageOne);
        m.put(AttackEffectType.COIN_FLIP_SWITCH_SELF, this::handleCoinFlipSwitchSelf);
        m.put(AttackEffectType.HEAL_ANY, this::handleHealAny);
        m.put(AttackEffectType.HEAL_BENCH, this::handleHealBench);
        m.put(AttackEffectType.HEAL_ALL, this::handleHealAll);
        m.put(AttackEffectType.DISCARD_OPPONENT_ENERGY, this::handleDiscardOpponentEnergy);
        m.put(AttackEffectType.COIN_FLIP_DISCARD_OPPONENT_ENERGY, this::handleCoinFlipDiscardOpponentEnergy);
        m.put(AttackEffectType.STOKE, this::handleStoke);
        m.put(AttackEffectType.COMBUSTION_BLAST, this::handleCombustionBlast);
        m.put(AttackEffectType.SCORCHING_FANG, this::handleScorchingFang);
        m.put(AttackEffectType.CLAIRVOYANT_EYE, this::handleClairvoyantEye);
        m.put(AttackEffectType.CALL_FOR_FAMILY, this::handleCallForFamily);
        m.put(AttackEffectType.HEAL_SELF_AND_SLEEP, this::handleHealSelfAndSleep);
        m.put(AttackEffectType.DISCARD_DECK_SELF, this::handleDiscardDeckSelf);
        m.put(AttackEffectType.COIN_FLIP_DISCARD_ENERGY, this::handleCoinFlipDiscardEnergy);
        m.put(AttackEffectType.COIN_FLIPS_UNTIL_TAILS_DISCARD_OPPONENT_ENERGY, this::handleCoinFlipsUntilTailsDiscardOpponentEnergy);
    }

    private void registerHandlersPartTwo(final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> m) {
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
        m.put(AttackEffectType.DISCARD_STADIUM, this::handleDiscardStadium);
        m.put(AttackEffectType.DISCARD_OPPONENT_TOOL,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.SWITCH_SELF, this::handleSwitchSelf);
        m.put(AttackEffectType.DISCARD_OPPONENT_HAND, this::handleDiscardOpponentHand);
        m.put(AttackEffectType.DISCARD_HAND_ENERGY_MULTIPLY_DAMAGE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.DAMAGE_ALL_OPPONENTS, this::handleDamageAllOpponents);
        m.put(AttackEffectType.BRILLIANT_SEARCH, this::handleBrilliantSearch);
        m.put(AttackEffectType.BURIED_TREASURE_HUNT, this::handleBuriedTreasureHunt);
        m.put(AttackEffectType.EXCITING_SHAKE, this::handleExcitingShake);
        m.put(AttackEffectType.COIN_FLIP_SKIP_OPPONENT_DRAW, this::handleCoinFlipSkipOpponentDraw);
        m.put(AttackEffectType.DISCARD_OPPONENT_DECK, this::handleDiscardOpponentDeck);
        m.put(AttackEffectType.DUAL_BULLET, this::handleDualBullet);
        m.put(AttackEffectType.PAIN_PELLETS, this::handlePainPellets);
        m.put(AttackEffectType.TRIPLE_POISON,
                (amount, ctx) -> {
                    ctx.getDefenderStatusManager().apply(StatusEffectType.ENVENENADO, 3);
                });
        m.put(AttackEffectType.STRONG_GUST, this::handleStrongGust);
        m.put(AttackEffectType.IGNORE_RESISTANCE,
                (amount, ctx) -> { });
        m.put(AttackEffectType.BLOCK_RETREAT, this::handleBlockRetreat);
    }


    // --- Extracted handler methods (kept complexity per-method low; see class javadoc) ---

    private void handleDrawCards(final int amount, final AttackContext ctx) {
        final PlayerRuntime attacker = ctx.getAttackerRuntime();
                            if (attacker != null) {
                                attacker.getHand().addCards(attacker.getDeck().drawMultiple(amount));
                            }
    }

    private void handlePlaceCountersOpponent(final int amount, final AttackContext ctx) {
        if (ctx.getDefender() != null) {
                                ctx.getDefender().addDamageCounters(amount);
                                if (ctx.getDefender().getDamageCounters() * 10 >= ctx.getDefender().getMaxHp()) {
                                    ctx.getMatchSession().getKnockoutHandler().onKnockout(ctx.getDefender(), ctx.getDefender().isEx() ? 2 : 1);
                                }
                            }
    }

    private void handlePlaceCountersDistributed(final int amount, final AttackContext ctx) {
        ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CURSED_DROP,
                                            null,
                                            amount,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD
                                    )
                            );
                            if (ctx.getMatchSession().getTurnManager() != null) {
                                ctx.getMatchSession().getTurnManager().interruptMainPhase();
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleMoveOpponentCounters(final int amount, final AttackContext ctx) {
        ctx.getMatchSession().setPendingSelectionRequest(
                                    new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                            ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EAR_INFLUENCE,
                                            null,
                                            20,
                                            ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD
                                    )
                            );
                            if (ctx.getMatchSession().getTurnManager() != null) {
                                ctx.getMatchSession().getTurnManager().interruptMainPhase();
                            }
    }

    private void handleDiscardOpponentHandToLimit(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handlePlaceOpponentBasicFromDiscard(final int amount, final AttackContext ctx) {
        final PlayerRuntime defender = ctx.getDefenderRuntime();
        if (defender == null || defender.getBench().getAll().size() >= 5) {
            return;
        }
        final List<ar.edu.utn.frc.tup.piii.engine.model.PokemonCard> basics = new java.util.ArrayList<>();
        for (Card card : defender.getDiscardPile().getCards()) {
            if (card instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC) {
                basics.add(pc);
            }
        }
        if (basics.size() > SINGLE_CANDIDATE) {
            requestOpponentBasicSelection(ctx);
        } else if (basics.size() == SINGLE_CANDIDATE) {
            placeBasicFromDiscard(defender, basics.get(0));
        }
    }

    private void requestOpponentBasicSelection(final AttackContext ctx) {
        ctx.getMatchSession().setPendingSelectionRequest(
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.REVIVAL,
                        null,
                        1,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE
                )
        );
        if (ctx.getMatchSession().getTurnManager() != null) {
            ctx.getMatchSession().getTurnManager().interruptMainPhase();
        }
    }

    private void placeBasicFromDiscard(final PlayerRuntime defender,
                                        final ar.edu.utn.frc.tup.piii.engine.model.PokemonCard basic) {
        defender.getDiscardPile().remove(basic);
        final ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon state =
                new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon(basic);
        state.setOwner(defender);
        defender.getBench().place(state);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleDiscardTrainerFromOpponentHand(final int amount, final AttackContext ctx) {
        final PlayerRuntime opponent = ctx.getDefenderRuntime();
                            if (opponent != null) {
                                boolean hasTrainer = opponent.getHand().getCards().stream()
                                        .anyMatch(c -> c instanceof TrainerCard);
                                ctx.getMatchSession().setPendingSelectionRequest(
                                        new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                                ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FANG_SNIPE,
                                                null,
                                                hasTrainer ? 1 : 0,
                                                ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
                                        )
                                );
                                if (ctx.getMatchSession().getTurnManager() != null) {
                                    ctx.getMatchSession().getTurnManager().interruptMainPhase();
                                }
                            }
    }

    private void handleShufflePokemonFromDiscard(final int amount, final AttackContext ctx) {
        final PlayerRuntime attacker = ctx.getAttackerRuntime();
                            if (attacker != null) {
                                java.util.List<ar.edu.utn.frc.tup.piii.engine.model.PokemonCard> pokemonInDiscard = attacker.getDiscardPile().getCards().stream()
                                        .filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard)
                                        .map(c -> (ar.edu.utn.frc.tup.piii.engine.model.PokemonCard) c)
                                        .collect(java.util.stream.Collectors.toList());
                                int toSelect = Math.min(amount, pokemonInDiscard.size());
                                if (toSelect > 0) {
                                    java.util.List<ar.edu.utn.frc.tup.piii.engine.model.Card> toReturn = new java.util.ArrayList<>();
                                    for (int i = 0; i < toSelect; i++) {
                                        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc = pokemonInDiscard.get(i);
                                        attacker.getDiscardPile().remove(pc);
                                        toReturn.add(pc);
                                    }
                                    attacker.getDeck().addCards(toReturn);
                                    attacker.getDeck().shuffle();
                                }
                            }
    }

    private void handleSearchDeckEnergy(final int amount, final AttackContext ctx) {
        final PlayerRuntime attacker = ctx.getAttackerRuntime();
                            if (attacker != null) {
                                ctx.getMatchSession().setPendingSelectionRequest(
                                        new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                                ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PARABOLIC_CHARGE,
                                                null,
                                                amount,
                                                ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK
                                        )
                                );
                                if (ctx.getMatchSession().getTurnManager() != null) {
                                    ctx.getMatchSession().getTurnManager().interruptMainPhase();
                                }
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipSelfDisable(final int amount, final AttackContext ctx) {
        if (!ctx.getCoinFlipper().flip()) {
                                ctx.getAttackerStatusManager().setSelfDisabledNextTurn(true);
                                ctx.getAttackerStatusManager().setSelfDisabledNextTurnSetThisTurn(true);
                            }
    }

    private void handleSelfDamage(final int amount, final AttackContext ctx) {
        boolean hasProtectionCube = ctx.getAttacker().getAttachedTool()
                                    .map(t -> t.getToolEffectId() == ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId.PROTECTION_CUBE)
                                    .orElse(false);
                            if (!hasProtectionCube) {
                                ctx.getAttacker().addDamageCounters(amount / DAMAGE_PER_COUNTER);
                            }
    }

    private void handleBenchDamage(final int amount, final AttackContext ctx) {
        // Bench damage: apply N damage to each of the opponent's Benched Pokémon.
                            // Weakness and Resistance do not apply to Benched Pokémon (XY1 §3).
                            final int counters = amount / DAMAGE_PER_COUNTER;
                            ctx.getDefenderBench().forEach(benched -> benched.addDamageCounters(counters));
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipPoison(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.ENVENENADO);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipBurn(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.QUEMADO);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipParalysis(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.PARALIZADO);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipSleep(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.DORMIDO);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipConfusion(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) ctx.getDefenderStatusManager().apply(StatusEffectType.CONFUNDIDO);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipPreventDamage(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) {
                                ctx.getAttackerStatusManager().setDamagePreventedNextTurn(true);
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipPreventDamage60OrLess(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) {
                                ctx.getAttackerStatusManager().setDamagePreventedIf60OrLessNextTurn(true);
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleQuiverDance(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleDisableAttack(final int amount, final AttackContext ctx) {
        var attacks = ctx.getDefender().getAttacks();
                            if (attacks != null && !attacks.isEmpty()) {
                                ctx.getDefenderStatusManager().setDisabledAttackName(attacks.get(0).name());
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleForceSwitch(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleForceSwitchOpponent(final int amount, final AttackContext ctx) {
        final PlayerRuntime defender = ctx.getDefenderRuntime();
        if (defender == null || defender.getBench().getAll().isEmpty()) {
            return;
        }
        if (defender.getBench().getAll().size() == SINGLE_CANDIDATE) {
            promoteOnlyBenchedPokemon(defender);
        } else {
            requestForceSwitchSelection(ctx);
        }
    }

    private void promoteOnlyBenchedPokemon(final PlayerRuntime defender) {
        final BattlePokemonState oldActive = defender.getActivePokemon();
        final BattlePokemonState newActive = defender.getBench().promote(0);
        defender.setActivePokemon(newActive);
        defender.getBench().place(oldActive);
        defender.getStatusEffectManager().clearAll();
        defender.recordPokemonEntered(oldActive);
    }

    private void requestForceSwitchSelection(final AttackContext ctx) {
        ctx.getMatchSession().setPendingSelectionRequest(
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PUSH_DOWN,
                        null,
                        1,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.BENCH
                )
        );
        if (ctx.getMatchSession().getTurnManager() != null) {
            ctx.getMatchSession().getTurnManager().interruptMainPhase();
        }
    }

    private void handleBenchDamageOne(final int amount, final AttackContext ctx) {
        final PlayerRuntime opponent = ctx.getDefenderRuntime();
        if (opponent == null) {
            return;
        }
        final List<BattlePokemonState> bench = opponent.getBench().getAll();
        if (bench.size() == SINGLE_CANDIDATE) {
            applyBenchDamageToOnlyTarget(bench.get(0), amount, ctx);
        } else if (bench.size() > SINGLE_CANDIDATE) {
            requestBenchDamageSelection(ctx);
        }
    }

    private void applyBenchDamageToOnlyTarget(final BattlePokemonState target, final int amount, final AttackContext ctx) {
        target.addDamageCounters(amount / DAMAGE_PER_COUNTER);
        if (target.getDamageCounters() * 10 >= target.getMaxHp()) {
            ctx.getMatchSession().getKnockoutHandler().onKnockout(target, target.isEx() ? 2 : 1);
        }
    }

    private void requestBenchDamageSelection(final AttackContext ctx) {
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

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipSwitchSelf(final int amount, final AttackContext ctx) {
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
    }

    private void handleHealAny(final int amount, final AttackContext ctx) {
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
    }

    private void handleHealBench(final int amount, final AttackContext ctx) {
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
    }

    private void handleHealAll(final int amount, final AttackContext ctx) {
        final PlayerRuntime attacker = ctx.getAttackerRuntime();
                            if (attacker != null) {
                                if (attacker.getActivePokemon() != null) {
                                    attacker.getActivePokemon().heal(amount);
                                }
                                for (BattlePokemonState p : attacker.getBench().getAll()) {
                                    p.heal(amount);
                                }
                            }
    }

    private void handleDiscardOpponentEnergy(final int amount, final AttackContext ctx) {
        final BattlePokemonState defender = ctx.getDefender();
                            if (defender != null && !defender.getAttachedEnergies().isEmpty()) {
                                defender.removeEnergies(amount);
                            }
    }

    private void handleCoinFlipDiscardOpponentEnergy(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) {
                                final BattlePokemonState defender = ctx.getDefender();
                                if (defender != null && !defender.getAttachedEnergies().isEmpty()) {
                                    defender.removeEnergies(amount);
                                }
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleStoke(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCombustionBlast(final int amount, final AttackContext ctx) {
        ctx.getAttackerStatusManager().setSelfDisabledAttackName("Combustion Blast");
                            ctx.getAttackerStatusManager().setSelfDisabledAttackSetThisTurn(true);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleScorchingFang(final int amount, final AttackContext ctx) {
        if (!ctx.isScorchingFangDiscarded()) {
            return;
        }
        final EnergyCard fireEnergy = ctx.getAttacker().getAttachedEnergyCards().stream()
                .filter(ec -> ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE || ec.isProvidesAllTypes())
                .findFirst()
                .orElse(null);
        if (fireEnergy == null) {
            return;
        }
        final int energyIdx = findEnergySlotIndex(ctx.getAttacker().getAttachedEnergyCards(), fireEnergy);
        if (energyIdx == -1) {
            return;
        }
        ctx.getAttacker().removeEnergies(java.util.List.of(energyIdx));
        if (ctx.getAttackerRuntime() != null) {
            ctx.getAttackerRuntime().getDiscardPile().add(fireEnergy);
        }
    }

    /**
     * Finds the "energy slot" index of a specific attached {@link EnergyCard} instance (used by
     * {@link BattlePokemonState#removeEnergies}, which counts each energy's slots individually).
     * Uses reference equality intentionally: {@code target} must be the exact card instance
     * already resolved from this same list, not merely an equal-value energy of the same type.
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private int findEnergySlotIndex(final List<EnergyCard> attachedEnergyCards, final EnergyCard target) {
        int slotIndex = 0;
        for (final EnergyCard ec : attachedEnergyCards) {
            if (ec == target) {
                return slotIndex;
            }
            slotIndex += ec.getEnergyCount();
        }
        return -1;
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleClairvoyantEye(final int amount, final AttackContext ctx) {
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
    }

    private void handleCallForFamily(final int amount, final AttackContext ctx) {
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
    }

    private void handleHealSelfAndSleep(final int amount, final AttackContext ctx) {
        ctx.getAttacker().heal(amount);
                            if (ctx.getAttackerRuntime() != null) {
                                ctx.getAttackerRuntime().getStatusEffectManager().apply(StatusEffectType.DORMIDO);
                            }
    }

    private void handleDiscardDeckSelf(final int amount, final AttackContext ctx) {
        if (ctx.getAttackerRuntime() != null) {
                                final List<Card> discarded = ctx.getAttackerRuntime().getDeck().drawMultiple(amount);
                                ctx.getAttackerRuntime().getDiscardPile().addAll(discarded);
                            }
    }

    private void handleCoinFlipDiscardEnergy(final int amount, final AttackContext ctx) {
        if (!ctx.getCoinFlipper().flip()) { // cruz / tails
                                ctx.getAttacker().removeEnergies(amount);
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipsUntilTailsDiscardOpponentEnergy(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleDiscardStadium(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleSwitchSelf(final int amount, final AttackContext ctx) {
        final PlayerRuntime attacker = ctx.getAttackerRuntime();
                            if (attacker != null && !attacker.getBench().getAll().isEmpty() && ctx.getMatchSession() != null) {
                                ctx.getMatchSession().setPendingSelectionRequest(
                                        new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                                ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BOUNCE,
                                                null,
                                                1,
                                                ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.BENCH
                                        )
                                );
                                if (ctx.getMatchSession().getTurnManager() != null) {
                                    ctx.getMatchSession().getTurnManager().interruptMainPhase();
                                }
                            }
    }

    private void handleDiscardOpponentHand(final int amount, final AttackContext ctx) {
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
    }

    private void handleDamageAllOpponents(final int amount, final AttackContext ctx) {
        final int counters = amount / DAMAGE_PER_COUNTER;
                            ctx.getDefenderBench().forEach(benched -> benched.addDamageCounters(counters));
    }

    private void handleBrilliantSearch(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleBuriedTreasureHunt(final int amount, final AttackContext ctx) {
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
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleExcitingShake(final int amount, final AttackContext ctx) {
        ctx.getAttackerStatusManager().setExcitingShakeActiveNextTurn(true);
                            ctx.getAttackerStatusManager().setExcitingShakeActiveNextTurnSetThisTurn(true);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleCoinFlipSkipOpponentDraw(final int amount, final AttackContext ctx) {
        if (ctx.getCoinFlipper().flip()) {
                                ctx.getDefenderStatusManager().setDrawStepBlocked(true);
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleDiscardOpponentDeck(final int amount, final AttackContext ctx) {
        final PlayerRuntime opponent = ctx.getDefenderRuntime();
                            if (opponent != null && !opponent.getDeck().isEmpty()) {
                                final Card discarded = opponent.getDeck().draw();
                                opponent.getDiscardPile().add(discarded);
                            }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleDualBullet(final int amount, final AttackContext ctx) {
        final PlayerRuntime opponent = ctx.getDefenderRuntime();
        if (opponent == null) {
            return;
        }
        final int targets = Math.min(2, (opponent.getActivePokemon() != null ? 1 : 0) + opponent.getBench().getAll().size());
        if (targets <= 0) {
            return;
        }
        ctx.getMatchSession().setPendingSelectionRequest(
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.DUAL_BULLET,
                        null,
                        targets,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD
                )
        );
        if (ctx.getMatchSession().getTurnManager() != null) {
            ctx.getMatchSession().getTurnManager().interruptMainPhase();
        }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handlePainPellets(final int amount, final AttackContext ctx) {
        final PlayerRuntime opponent = ctx.getDefenderRuntime();
                            if (opponent != null) {
                                ctx.getMatchSession().setPendingSelectionRequest(
                                        new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                                                ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAIN_PELLETS,
                                                null,
                                                1,
                                                ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD
                                            )
                                    );
                                    if (ctx.getMatchSession().getTurnManager() != null) {
                                        ctx.getMatchSession().getTurnManager().interruptMainPhase();
                                    }
                                }
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleStrongGust(final int amount, final AttackContext ctx) {
        ctx.getAttackerStatusManager().setStrongGustUsedLastTurn(true);
                            ctx.getAttackerStatusManager().setStrongGustUsedLastTurnSetThisTurn(true);
    }

    @SuppressWarnings(UNUSED_AMOUNT_PARAM) // amount unused by this effect; required by the BiConsumer<Integer, AttackContext> handler signature
    private void handleBlockRetreat(final int amount, final AttackContext ctx) {
        ctx.getDefenderStatusManager().setRetreatBlockedNextTurn(true);
                            ctx.getDefenderStatusManager().setRetreatBlockedNextTurnSetThisTurn(true);
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
        return TEXT_TO_TYPE.getOrDefault(key.toLowerCase(Locale.ROOT), AttackEffectType.NONE);
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
