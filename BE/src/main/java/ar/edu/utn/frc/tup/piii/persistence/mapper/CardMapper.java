package ar.edu.utn.frc.tup.piii.persistence.mapper;

import ar.edu.utn.frc.tup.piii.engine.model.Ability;
import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maps a {@link CardEntity} (JPA/persistence) to the appropriate engine domain Card subtype.
 * Lives in the persistence layer — may reference JPA entities; engine classes must not depend on this.
 */
@Slf4j
@Component
// PMD False Positive: CardMapper is the central persistence-to-domain mapper for Pokemon, Trainer, and Energy cards
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity"})
public final class CardMapper {

    private static final Map<String, PokemonType> TYPE_BY_NAME;
    private static final Map<String, TrainerType> TRAINER_TYPE_BY_SUBTYPE;
    private static final Map<String, EvolutionStage> EVOLUTION_STAGE_BY_SUBTYPE;
    private static final Map<String, AbilityEffectId> ABILITY_EFFECT_ID_BY_NAME;
    private static final Map<String, TrainerEffectId> TRAINER_EFFECT_BY_CARD_ID;
    private static final Map<String, PokemonToolEffectId> TOOL_EFFECT_BY_CARD_ID;

    private static final String FLIP_A_COIN = "flip a coin";
    private static final String OPPONENT = "opponent";
    private static final String REGEX_MORE_DAMAGE = "does\\s+(\\d+)\\s+more\\s+damage";
    private static final String ENERGY = "energy";
    private static final String DISCARD = "discard";
    private static final String REGEX_DOES_DAMAGE = "does\\s+(\\d+)\\s+damage";

    private static final String PETAL_BLIZZARD = "petal blizzard";
    private static final String CALL_FOR_FAMILY = "call for family";
    private static final String BOUNCE = "bounce";
    private static final String DAMAGE_20 = "20";
    private static final String UP_TO_2 = "up to 2";

    static {
        final Map<String, PokemonType> types = new HashMap<>();
        types.put("Grass",      PokemonType.GRASS);
        types.put("Fire",       PokemonType.FIRE);
        types.put("Water",      PokemonType.WATER);
        types.put("Lightning",  PokemonType.LIGHTNING);
        types.put("Psychic",    PokemonType.PSYCHIC);
        types.put("Fighting",   PokemonType.FIGHTING);
        types.put("Darkness",   PokemonType.DARKNESS);
        types.put("Metal",      PokemonType.METAL);
        types.put("Fairy",      PokemonType.FAIRY);
        types.put("Dragon",     PokemonType.DRAGON);
        types.put("Colorless",  PokemonType.COLORLESS);
        TYPE_BY_NAME = Collections.unmodifiableMap(types);

        final Map<String, TrainerType> tt = new HashMap<>();
        tt.put("Item",          TrainerType.ITEM);
        tt.put("Supporter",     TrainerType.SUPPORTER);
        tt.put("Stadium",       TrainerType.STADIUM);
        tt.put("Pokémon Tool",  TrainerType.POKEMON_TOOL);
        TRAINER_TYPE_BY_SUBTYPE = Collections.unmodifiableMap(tt);

        final Map<String, EvolutionStage> es = new HashMap<>();
        es.put("Stage 2", EvolutionStage.STAGE_2);
        es.put("Stage 1", EvolutionStage.STAGE_1);
        es.put("MEGA",    EvolutionStage.MEGA);
        es.put("Basic",   EvolutionStage.BASIC);
        EVOLUTION_STAGE_BY_SUBTYPE = Collections.unmodifiableMap(es);

        final Map<String, AbilityEffectId> ab = new HashMap<>();
        ab.put("Fairy Transfer", AbilityEffectId.FAIRY_TRANSFER);
        ab.put("Sweet Veil",     AbilityEffectId.SWEET_VEIL);
        ab.put("Mystical Fire",  AbilityEffectId.MYSTICAL_FIRE);
        ab.put("Magnetic Draw",  AbilityEffectId.MAGNETIC_DRAW);
        ab.put("Safeguard",      AbilityEffectId.SAFEGUARD);
        ab.put("Spiky Shield",   AbilityEffectId.SPIKY_SHIELD);
        ab.put("Destiny Burst",  AbilityEffectId.DESTINY_BURST);
        ab.put("Water Shuriken", AbilityEffectId.WATER_SHURIKEN);
        ab.put("Upside-Down Evolution", AbilityEffectId.UPSIDE_DOWN_EVOLUTION);
        ab.put("Stance Change",  AbilityEffectId.STANCE_CHANGE);
        ab.put("Drive Off",      AbilityEffectId.DRIVE_OFF);
        ab.put("Fur Coat",       AbilityEffectId.FUR_COAT);
        ab.put("Forest's Curse", AbilityEffectId.FOREST_CURSE);
        ab.put("Intimidating Mane", AbilityEffectId.INTIMIDATING_MANE);
        ab.put("Leaf Draw",      AbilityEffectId.LEAF_DRAW);
        ab.put("Energy Grace",   AbilityEffectId.ENERGY_GRACE);
        ab.put("Hand Lock",      AbilityEffectId.HAND_LOCK);
        ab.put("Shadow Void",    AbilityEffectId.SHADOW_VOID);
        ab.put("Adaptive Evolution", AbilityEffectId.ADAPTIVE_EVOLUTION);
        ab.put("Counterattack Quills", AbilityEffectId.COUNTERATTACK_QUILLS);
        ab.put("Flower Veil",          AbilityEffectId.FLOWER_VEIL);
        ab.put("Poison Barrier",       AbilityEffectId.POISON_BARRIER);
        ab.put("Stir and Snooze",      AbilityEffectId.STIR_AND_SNOOZE);
        ab.put("Thorn Tempest",        AbilityEffectId.THORN_TEMPEST);
        ab.put("Big Jump",             AbilityEffectId.BIG_JUMP);
        ab.put("Gooey Regeneration",   AbilityEffectId.GOOEY_REGENERATION);
        ABILITY_EFFECT_ID_BY_NAME = Collections.unmodifiableMap(ab);

        final Map<String, TrainerEffectId> te = new HashMap<>();
        te.put("xy1-115", TrainerEffectId.CASSIUS);
        te.put("xy1-116", TrainerEffectId.EVOSODA);
        te.put("xy1-118", TrainerEffectId.GREAT_BALL);
        te.put("xy1-120", TrainerEffectId.MAX_REVIVE);
        te.put("xy1-123", TrainerEffectId.PROFESSORS_LETTER);
        te.put("xy2-88", TrainerEffectId.BLACKSMITH);
        te.put("xy2-88a", TrainerEffectId.BLACKSMITH);
        te.put("xy2-89", TrainerEffectId.FIERY_TORCH);
        te.put("xy2-90", TrainerEffectId.LYSANDRE);
        te.put("xy2-104", TrainerEffectId.LYSANDRE);
        te.put("xy2-91", TrainerEffectId.MAGNETIC_STORM);
        te.put("xy2-92", TrainerEffectId.PAL_PAD);
        te.put("xy2-93", TrainerEffectId.POKEMON_CENTER_LADY);
        te.put("xy2-105", TrainerEffectId.POKEMON_CENTER_LADY);
        te.put("xy2-94", TrainerEffectId.POKEMON_FAN_CLUB);
        te.put("xy2-106", TrainerEffectId.POKEMON_FAN_CLUB);
        te.put("xy2-96", TrainerEffectId.SACRED_ASH);
        te.put("xy2-97", TrainerEffectId.STARTLING_MEGAPHONE);
        te.put("xy2-98", TrainerEffectId.TRICK_SHOVEL);
        te.put("xy2-99", TrainerEffectId.ULTRA_BALL);
        TRAINER_EFFECT_BY_CARD_ID = Collections.unmodifiableMap(te);

        final Map<String, PokemonToolEffectId> tool = new HashMap<>();
        tool.put("xy1-121", PokemonToolEffectId.MUSCLE_BAND);
        tool.put("xy1-119", PokemonToolEffectId.HARD_CHARM);
        tool.put("xy2-95", PokemonToolEffectId.PROTECTION_CUBE);
        TOOL_EFFECT_BY_CARD_ID = Collections.unmodifiableMap(tool);
    }

    private final ObjectMapper objectMapper;
    private final Map<String, Function<CardEntity, Card>> dispatchers;
    private final List<Function<String, String>> attackEffectInferrers;

    public CardMapper(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        final Map<String, Function<CardEntity, Card>> d = new HashMap<>();
        d.put("Pokémon", this::mapPokemon);
        d.put("Trainer", this::mapTrainer);
        d.put("Energy",  this::mapEnergy);
        this.dispatchers = Collections.unmodifiableMap(d);

        this.attackEffectInferrers = List.of(
                this::inferDrawCardsEffect,
                this::inferDamageModifierEffect,
                this::inferCoinFlipEffect,
                this::inferSleepAndStatusEffect,
                this::inferDamagePreventionEffect,
                this::inferHealAndSelfDamageEffect,
                this::inferDiscardEffect,
                this::inferCounterAndBenchEffect
        );
    }

    /**
     * Maps a {@link CardEntity} to the correct {@link Card} domain subtype.
     *
     * @param entity the JPA entity (must not be null)
     * @return the mapped domain card
     * @throws NullPointerException     if entity is null
     * @throws IllegalArgumentException if the entity's supertype is unrecognised
     */
    public Card map(final CardEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        String supertype = entity.getSupertype();
        if (supertype != null && supertype.startsWith("Pok")) {
            supertype = "Pokémon";
        }
        final Function<CardEntity, Card> fn = dispatchers.get(supertype);
        if (fn == null) {
            throw new IllegalArgumentException("Unknown supertype: " + entity.getSupertype());
        }
        return fn.apply(entity);
    }

    // --- private mappers ---

    private PokemonCard mapPokemon(final CardEntity entity) {
        final List<Map<String, Object>> attacksRaw  = toListOfMaps(entity.getAttacks());
        final List<Attack> attacks = attacksRaw.stream().map(this::parseAttack).toList();

        final List<Map<String, Object>> weaknessesRaw   = toListOfMaps(entity.getWeaknesses());
        final List<Map<String, Object>> resistancesRaw  = toListOfMaps(entity.getResistances());
        final List<Object>              retreatRaw      = toList(entity.getRetreatCost());

        final PokemonType pokemonType   = inferPokemonType(attacks);
        final PokemonType weaknessType  = weaknessesRaw.isEmpty()  ? null : parseType((String) weaknessesRaw.get(0).get("type"));
        final PokemonType resistType    = resistancesRaw.isEmpty() ? null : parseType((String) resistancesRaw.get(0).get("type"));

        final List<Ability> abilities = parseAbilities(entity.getAbilities());

        final String subtype = subtype(entity);
        return new PokemonCard.Builder(entity.getId(), entity.getName(),
                entity.getHp() != null ? entity.getHp() : 0, pokemonType)
                .weaknessType(weaknessType)
                .resistanceType(resistType)
                .retreatCost(retreatRaw.size())
                .ex(subtype.contains("EX"))
                .evolutionStage(parseEvolutionStage(subtype))
                .evolvesFrom(entity.getEvolvesFrom())
                .abilities(abilities)
                .attacks(attacks)
                .build();
    }

    private TrainerCard mapTrainer(final CardEntity entity) {
        final String subtype = subtype(entity);
        TrainerType trainerType = TrainerType.ITEM;
        if (subtype != null) {
            if (subtype.contains("Tool")) {
                trainerType = TrainerType.POKEMON_TOOL;
            } else if (subtype.contains("Supporter")) {
                trainerType = TrainerType.SUPPORTER;
            } else if (subtype.contains("Stadium")) {
                trainerType = TrainerType.STADIUM;
            }
        }

        final List<Object> rules = toList(entity.getRules());
        final boolean aceSpec = rules.stream()
                .anyMatch(r -> r instanceof String s && s.contains("ACE SPEC"));
        
        final String effectText = String.join("\n", rules.stream()
                .map(String::valueOf)
                .filter(s -> !s.contains("ACE SPEC"))
                .toList());

        return new TrainerCard.Builder(entity.getId(), entity.getName(), trainerType)
                .aceSpec(aceSpec)
                .effectText(effectText)
                .effectId(inferTrainerEffectId(entity.getId(), effectText))
                .toolEffectId(inferToolEffectId(entity.getId(), effectText))
                .build();
    }

    private EnergyCard mapEnergy(final CardEntity entity) {
        final String name    = entity.getName() != null ? entity.getName() : "";
        final String subtype = subtype(entity);
        final boolean basic  = "Basic".equals(subtype);

        final PokemonType energyType = TYPE_BY_NAME.entrySet().stream()
                .filter(e -> name.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(PokemonType.COLORLESS);

        // Special energy detection by card name (XY1 set).
        if (name.contains("Rainbow")) {
            // Rainbow Energy: provides all types, deals 1 damage counter when attached.
            return new EnergyCard(entity.getId(), name, PokemonType.COLORLESS, false, 1, true);
        }
        if (name.contains("Double Colorless")) {
            // Double Colorless Energy: provides 2 Colorless energy units.
            return new EnergyCard(entity.getId(), name, PokemonType.COLORLESS, false, 2, false);
        }

        return new EnergyCard(entity.getId(), entity.getName(), energyType, basic);
    }

    // --- parsing helpers ---

    private Attack parseAttack(final Map<String, Object> raw) {
        final String name = (String) raw.get("name");
        final String damageStr = String.valueOf(raw.getOrDefault("damage", ""));
        final int baseDamage = parseDamage(damageStr);

        final List<?> costRaw = raw.get("cost") instanceof List<?> list ? list : List.of();
        final List<PokemonType> requiredEnergies = costRaw.stream()
                .map(c -> parseType(String.valueOf(c)))
                .toList();

        final String text = String.valueOf(raw.getOrDefault("text", ""));
        final String effectText = inferAttackEffectText(name, text);

        return new Attack(name, baseDamage, requiredEnergies, effectText);
    }

    private static final Map<String, String> EXACT_ATTACK_NAME_EFFECTS = Map.ofEntries(
            Map.entry("deranged dance", "deranged_dance"),
            Map.entry("parabolic charge", "search_deck_energy:2"),
            Map.entry("brilliant search", "search_deck_any:3"),
            Map.entry("buried treasure hunt", "look_top_4_take_2_discard_rest"),
            Map.entry("stoke", "stoke"),
            Map.entry("combustion blast", "combustion_blast"),
            Map.entry("scorching fang", "scorching_fang"),
            Map.entry("bright garden", "bright_garden"),
            Map.entry("ear we go", "ear_we_go"),
            Map.entry("clairvoyant eye", "clairvoyant_eye"),
            Map.entry("quiver dance", "quiver_dance"),
            Map.entry("powerful friends", "powerful_friends:70"),
            Map.entry("smokescreen", "smokescreen"),
            Map.entry("sand-attack", "smokescreen"),
            Map.entry("icy wind", "sleep"),
            Map.entry("sitdown bounce", "coin_flip_self_disable"),
            Map.entry("frost barrier", "prevent_damage_20"),
            Map.entry("shatter", "discard_stadium"),
            Map.entry("peck off", "discard_opponent_tool"),
            Map.entry("flash claw", "discard_opponent_hand:1"),
            Map.entry("rock rush", "discard_hand_energy_multiply_damage:fighting:30"),
            Map.entry("exciting shake", "exciting_shake"),
            Map.entry("heart wink", "coin_flip_skip_opponent_draw"),
            Map.entry("stomp off", "discard_opponent_deck:1"),
            Map.entry("wild blaze", "discard_deck_self:5"),
            Map.entry("dual bullet", "dual_bullet"),
            Map.entry("pain pellets", "pain_pellets"),
            Map.entry("triple poison", "triple_poison"),
            Map.entry("strong gust", "strong_gust"),
            Map.entry("smash uppercut", "ignore_resistance"),
            Map.entry("clutch", "block_retreat"),
            Map.entry("corner", "block_retreat"),
            Map.entry("dark clamp", "block_retreat")
    );

    /**
     * Infers a machine-readable effect id (e.g. {@code "heal:20"}, {@code "coin_flip_paralysis"})
     * from an attack's name and raw rules text, since the source data only carries prose.
     */
    private String inferAttackEffectText(final String attackName, final String text) {
        if (text == null || text.isBlank() || "null".equals(text)) {
            return "";
        }
        final String lowerName = attackName != null ? attackName.toLowerCase(Locale.ROOT) : "";
        final String lower = text.toLowerCase(Locale.ROOT).replace("’", "'").replace("`", "'");

        final String exactEffect = EXACT_ATTACK_NAME_EFFECTS.get(lowerName);
        if (exactEffect != null) {
            return exactEffect;
        }

        final String specialNamedEffect = inferSpecialNamedAttackEffect(lowerName, lower);
        if (specialNamedEffect != null) {
            return specialNamedEffect;
        }

        for (final Function<String, String> inferrer : attackEffectInferrers) {
            final String effect = inferrer.apply(lower);
            if (effect != null) {
                return effect;
            }
        }

        return "";
    }

    private String inferSpecialNamedAttackEffect(final String lowerName, final String lower) {
        if (PETAL_BLIZZARD.equals(lowerName)) {
            return lower.contains(DAMAGE_20) ? "damage_all_opponents:20" : "damage_all_opponents:10";
        }
        if (CALL_FOR_FAMILY.equals(lowerName)) {
            return lower.contains(UP_TO_2) ? "call_for_family:2" : "call_for_family:1";
        }
        if (BOUNCE.equals(lowerName)) {
            return lower.contains(FLIP_A_COIN) ? "coin_flip_switch_self" : "switch_self";
        }
        return null;
    }

    private String inferDrawCardsEffect(final String lower) {
        if (lower.contains("draw a card")) {
            return "draw_cards:1";
        }
        final java.util.regex.Matcher mDraw = java.util.regex.Pattern.compile("draw\\s+(\\d+)\\s+cards").matcher(lower);
        if (mDraw.find()) {
            return "draw_cards:" + mDraw.group(1);
        }
        return null;
    }

    private String inferDamageModifierEffect(final String lower) {
        final String effect = inferSelfAndRetreatDamageModifier(lower);
        if (effect != null) {
            return effect;
        }
        return inferEnergyAndOtherDamageModifier(lower);
    }

    private String inferSelfAndRetreatDamageModifier(final String lower) {
        if (lower.contains("times the number of damage counters on this pok")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage\\s+times").matcher(lower);
            if (m.find()) {
                return "damage_times_self_counters:" + m.group(1);
            }
        }
        return inferRetreatCostOrOpponentEnergyDamage(lower);
    }

    private String inferRetreatCostOrOpponentEnergyDamage(final String lower) {
        if (lower.contains("for each") && lower.contains(OPPONENT) && lower.contains("retreat cost")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(REGEX_MORE_DAMAGE).matcher(lower);
            if (m.find()) {
                return "damage_per_retreat_cost:" + m.group(1);
            }
        }
        if (lower.contains("more damage for each energy attached to your opponent")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(REGEX_MORE_DAMAGE).matcher(lower);
            if (m.find()) {
                return "damage_per_opponent_all_energy:" + m.group(1);
            }
        }
        if (lower.contains("more damage for each") && lower.contains(ENERGY)) {
            final java.util.regex.Matcher mDamage = java.util.regex.Pattern.compile(REGEX_MORE_DAMAGE).matcher(lower);
            final java.util.regex.Matcher mEnergy = java.util.regex.Pattern.compile("for each\\s+(\\w+)\\s+energy").matcher(lower);
            if (mDamage.find() && mEnergy.find()) {
                return "damage_per_energy_type:" + mEnergy.group(1) + ":" + mDamage.group(1);
            }
        }
        return null;
    }

    private String inferEnergyAndOtherDamageModifier(final String lower) {
        if (lower.contains("already has any damage counters")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(REGEX_MORE_DAMAGE).matcher(lower);
            if (m.find()) {
                return "damage_if_target_damaged:" + m.group(1);
            }
        }
        if (lower.contains("minus") && lower.contains("damage for each damage counter on this")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("minus\\s+(\\d+)\\s+damage\\s+for\\s+each").matcher(lower);
            if (m.find()) {
                return "damage_minus_per_counter:" + m.group(1);
            }
        }
        if (lower.contains("if any of your pokémon were knocked out") && lower.contains("last turn")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(REGEX_MORE_DAMAGE).matcher(lower);
            if (m.find()) {
                return "revenge_damage:" + m.group(1);
            }
        }
        if (lower.contains("times the number of prize cards your opponent has taken")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(REGEX_DOES_DAMAGE).matcher(lower);
            if (m.find()) {
                return "damage_per_opponent_prize:" + m.group(1);
            }
        }
        return null;
    }

    private String inferCoinFlipEffect(final String lower) {
        String effect = inferTailsCoinFlipEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferMultiplierCoinFlipEffect(lower);
        if (effect != null) {
            return effect;
        }
        return inferStatusOrExtraCoinFlipEffect(lower);
    }

    private String inferTailsCoinFlipEffect(final String lower) {
        if (lower.contains("until you get tails")) {
            if (lower.contains(DISCARD) && lower.contains(ENERGY) && (lower.contains(OPPONENT) || lower.contains("defending") || lower.contains("active"))) {
                return "coin_flips_until_tails_discard_opponent_energy";
            }
            if (lower.contains("more damage")) {
                final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage").matcher(lower);
                if (m.find()) {
                    return "coin_flips_until_tails_extra:" + m.group(1);
                }
            }
        }
        return null;
    }

    private String inferMultiplierCoinFlipEffect(final String lower) {
        if (!lower.contains("flip") || !lower.contains("coin") || !lower.contains("times the number of heads")) {
            return null;
        }
        if (lower.contains("until you get tails")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(REGEX_DOES_DAMAGE).matcher(lower);
            if (m.find()) {
                return "coin_flips_until_tails:" + m.group(1);
            }
        }
        return inferEnergyOrCoinsMultiplierEffect(lower);
    }

    private String inferEnergyOrCoinsMultiplierEffect(final String lower) {
        if (lower.contains("for each") && lower.contains(ENERGY)) {
            final java.util.regex.Matcher mEnergy = java.util.regex.Pattern.compile("for each\\s+(\\w+)\\s+energy").matcher(lower);
            final java.util.regex.Matcher mDamage = java.util.regex.Pattern.compile(REGEX_DOES_DAMAGE).matcher(lower);
            if (mEnergy.find() && mDamage.find()) {
                return "coin_flips_per_energy:" + mEnergy.group(1) + ":" + mDamage.group(1);
            }
        } else if (lower.contains("for each damage counter")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(REGEX_DOES_DAMAGE).matcher(lower);
            if (m.find()) {
                return "coin_flips_per_damage_counter:" + m.group(1);
            }
        } else {
            final java.util.regex.Matcher mCoins = java.util.regex.Pattern.compile("flip\\s+(\\d+)\\s+coins").matcher(lower);
            final java.util.regex.Matcher mDamage = java.util.regex.Pattern.compile(REGEX_DOES_DAMAGE).matcher(lower);
            if (mCoins.find() && mDamage.find()) {
                return "coin_flips_multiplier:" + mCoins.group(1) + ":" + mDamage.group(1);
            }
        }
        return null;
    }

    private String inferStatusOrExtraCoinFlipEffect(final String lower) {
        if (lower.contains(FLIP_A_COIN) && lower.contains("more damage")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage").matcher(lower);
            if (m.find()) {
                return "coin_flip_extra:" + m.group(1);
            }
        }

        if (lower.contains(FLIP_A_COIN) && (lower.contains("if tails, this attack does nothing") || lower.contains("if tails, that attack does nothing"))) {
            return "coin_flip_fail";
        }

        if (lower.contains(FLIP_A_COIN) && lower.contains("switch this pok") && lower.contains("benched")) {
            return "coin_flip_switch_self";
        }

        return inferCoinFlipStatusCondition(lower);
    }

    private String inferCoinFlipStatusCondition(final String lower) {
        if (!lower.contains(FLIP_A_COIN) && !lower.contains("flip 2 coins")) {
            return null;
        }
        if (lower.contains("paralyzed")) {
            return "coin_flip_paralysis";
        }
        if (lower.contains("asleep")) {
            return "coin_flip_sleep";
        }
        if (lower.contains("poisoned")) {
            return "coin_flip_poison";
        }
        if (lower.contains("burned")) {
            return "coin_flip_burn";
        }
        if (lower.contains("confused")) {
            return "coin_flip_confusion";
        }
        return null;
    }

    private String inferSleepAndStatusEffect(final String lower) {
        if (lower.contains("this pokémon is now asleep") || lower.contains("this pokemon is now asleep")) {
            if (lower.contains("heal")) {
                final java.util.regex.Matcher m = java.util.regex.Pattern.compile("heal\\s+(\\d+)").matcher(lower);
                if (m.find()) {
                    return "heal_and_sleep:" + m.group(1);
                }
            }
            return "sleep_self";
        }

        final String statusEffect = inferDefendingStatusConditionEffect(lower);
        if (statusEffect != null) {
            return statusEffect;
        }

        if (lower.contains("can't use that attack") || lower.contains("cant use that attack")) {
            return "disable_attack";
        }
        return null;
    }

    private static final Map<String, String> STATUS_CONDITION_MAP = Map.of(
            "poisoned", "poison",
            "burned", "burn",
            "paralyzed", "paralysis",
            "asleep", "sleep",
            "confused", "confusion"
    );

    private String inferDefendingStatusConditionEffect(final String lower) {
        for (final Map.Entry<String, String> entry : STATUS_CONDITION_MAP.entrySet()) {
            final String keyword = entry.getKey();
            if (lower.contains("is now " + keyword)
                    || lower.contains("the defending pokémon is now " + keyword)
                    || lower.contains("now " + keyword)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String inferDamagePreventionEffect(final String lower) {
        if (lower.contains("prevent all damage done to this pok")
                || lower.contains("prevent all effects of attacks, including damage, done to this pok")
                || lower.contains("prevent that attack's damage done to this pok")) {
            if (lower.contains("60 or less")) {
                if (lower.contains(FLIP_A_COIN)) {
                    return "coin_flip_prevent_damage_60_or_less";
                }
                return "prevent_damage_60_or_less";
            }
            if (lower.contains(FLIP_A_COIN)) {
                return "coin_flip_prevent_damage";
            }
            return "prevent_damage";
        }
        return null;
    }

    private String inferHealAndSelfDamageEffect(final String lower) {
        if (lower.contains("heal")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("heal\\s+(\\d+)").matcher(lower);
            if (m.find()) {
                final String amt = m.group(1);
                if (lower.contains("1 of your benched pok")) {
                    return "heal_bench:" + amt;
                } else if (lower.contains("1 of your pok")) {
                    return "heal_any:" + amt;
                } else if (lower.contains("each of your pok")) {
                    return "heal_all:" + amt;
                } else {
                    return "heal:" + amt;
                }
            }
        }

        if (lower.contains("does") && lower.contains("damage to itself")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+damage\\s+to\\s+itself").matcher(lower);
            if (m.find()) {
                return "self_damage:" + m.group(1);
            }
        }
        return null;
    }

    private String inferDiscardEffect(final String lower) {
        if (lower.contains(DISCARD) && lower.contains("top") && lower.contains("your deck")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("discard\\s+the\\s+top\\s+(\\d+)\\s+cards").matcher(lower);
            if (m.find()) {
                return "discard_deck_self:" + m.group(1);
            }
        }
        return inferEnergyDiscardEffect(lower);
    }

    private String inferEnergyDiscardEffect(final String lower) {
        if (lower.contains(DISCARD) && (lower.contains(ENERGY) || lower.contains("a darkness energy"))) {
            final boolean opponent = lower.contains(OPPONENT) || lower.contains("defending");
            final boolean coinFlip = lower.contains(FLIP_A_COIN) || lower.contains("flip");
            int amount = 1;
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("discard\\s+(\\d+)").matcher(lower);
            if (m.find()) {
                amount = Integer.parseInt(m.group(1));
            }

            final String prefix = coinFlip ? "coin_flip_" : "";
            if (opponent) {
                return prefix + "discard_opponent_energy:" + amount;
            } else {
                return prefix + "discard_energy:" + amount;
            }
        }
        return null;
    }

    private String inferCounterAndBenchEffect(final String lower) {
        final String counterEffect = inferDamageCounterEffect(lower);
        if (counterEffect != null) {
            return counterEffect;
        }
        final String handEffect = inferHandAndDiscardEffect(lower);
        if (handEffect != null) {
            return handEffect;
        }
        return inferBenchDamageEffect(lower);
    }

    private String inferDamageCounterEffect(final String lower) {
        if (lower.contains("put") && lower.contains("damage counter") && lower.contains("opponent's active")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("put\\s+(\\d+)\\s+damage\\s+counter").matcher(lower);
            if (m.find()) {
                return "place_counters_opponent:" + m.group(1);
            }
        }
        if (lower.contains("put") && lower.contains("damage counters") && lower.contains("in any way")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("put\\s+(\\d+)\\s+damage\\s+counters").matcher(lower);
            if (m.find()) {
                return "place_counters_distributed:" + m.group(1);
            }
        }
        if (lower.contains("move") && lower.contains("damage counters") && lower.contains(OPPONENT)) {
            return "move_opponent_counters";
        }
        return null;
    }

    private String inferHandAndDiscardEffect(final String lower) {
        final String limitEffect = inferDiscardHandLimitEffect(lower);
        if (limitEffect != null) {
            return limitEffect;
        }
        final String basicTrainerEffect = inferBasicAndTrainerHandEffect(lower);
        if (basicTrainerEffect != null) {
            return basicTrainerEffect;
        }
        return inferShuffleFromDiscardEffect(lower);
    }

    private String inferDiscardHandLimitEffect(final String lower) {
        if (lower.contains(DISCARD) && lower.contains("opponent's hand") && lower.contains("until") && lower.contains("card")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("until.*\\s+(\\d+)\\s+card").matcher(lower);
            if (m.find()) {
                return "discard_opponent_hand_to_limit:" + m.group(1);
            }
            return "discard_opponent_hand_to_limit:4";
        }
        return null;
    }

    private String inferBasicAndTrainerHandEffect(final String lower) {
        if (lower.contains("basic pok") && lower.contains("opponent's discard") && lower.contains("bench")) {
            return "place_opponent_basic_from_discard";
        }
        if (lower.contains("reveals") && lower.contains("hand") && lower.contains("trainer") && lower.contains(DISCARD)) {
            return "discard_trainer_from_opponent_hand";
        }
        return null;
    }

    private String inferShuffleFromDiscardEffect(final String lower) {
        if (lower.contains("shuffle") && lower.contains("pok") && lower.contains("discard pile into your deck")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("shuffle\\s+(\\d+)\\s+pok").matcher(lower);
            if (m.find()) {
                return "shuffle_pokemon_from_discard:" + m.group(1);
            }
            return "shuffle_pokemon_from_discard:3";
        }
        return null;
    }

    private String inferBenchDamageEffect(final String lower) {
        final String allBench = inferAllBenchDamage(lower);
        if (allBench != null) {
            return allBench;
        }

        if (lower.contains("your opponent switches") && (lower.contains("benched") || lower.contains("bench"))) {
            return "force_switch_opponent";
        }
        return null;
    }

    private String inferAllBenchDamage(final String lower) {
        if (lower.contains("each of your opponent's benched pok")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage").matcher(lower);
            if (m.find()) {
                return "bench_damage:" + m.group(1);
            }
        }
        if (lower.contains("1 of your opponent's benched pok")
                || lower.contains("one of your opponent's benched pok")
                || (lower.contains("opponent's benched") && (lower.contains("1 of") || lower.contains("one of")))) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage").matcher(lower);
            if (m.find()) {
                return "bench_damage_one:" + m.group(1);
            }
        }
        if (lower.contains("opponent's benched")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage").matcher(lower);
            if (m.find()) {
                return "bench_damage:" + m.group(1);
            }
        }
        return null;
    }

    private int parseDamage(final String damageStr) {
        if (damageStr == null || damageStr.isBlank()) {
            return 0;
        }
        final String digits = damageStr.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private PokemonType parseType(final String typeStr) {
        return TYPE_BY_NAME.getOrDefault(typeStr, PokemonType.COLORLESS);
    }

    private EvolutionStage parseEvolutionStage(final String subtype) {
        for (final Map.Entry<String, EvolutionStage> entry : EVOLUTION_STAGE_BY_SUBTYPE.entrySet()) {
            if (subtype.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return EvolutionStage.BASIC;
    }

    private List<Ability> parseAbilities(final Object rawAbilities) {
        final List<Map<String, Object>> abilitiesRaw = toListOfMaps(rawAbilities);
        return abilitiesRaw.stream().map(raw -> {
            final String name = String.valueOf(raw.getOrDefault("name", ""));
            final String text = String.valueOf(raw.getOrDefault("text", ""));
            final AbilityEffectId effectId = ABILITY_EFFECT_ID_BY_NAME.getOrDefault(name, AbilityEffectId.NONE);
            return new Ability(name, text, effectId);
        }).toList();
    }

    private PokemonType inferPokemonType(final List<Attack> attacks) {
        return attacks.stream()
                .flatMap(a -> a.requiredEnergies().stream())
                .filter(t -> t != PokemonType.COLORLESS)
                .findFirst()
                .orElse(PokemonType.COLORLESS);
    }

    private TrainerEffectId inferTrainerEffectId(final String cardId, final String text) {
        final TrainerEffectId directId = TRAINER_EFFECT_BY_CARD_ID.get(cardId);
        if (directId != null) {
            return directId;
        }

        if (text == null || text.isBlank()) {
            return TrainerEffectId.NONE;
        }
        final String lower = text.toLowerCase(Locale.ROOT);
        return inferTrainerEffectFromText(lower);
    }

    private TrainerEffectId inferTrainerEffectFromText(final String lower) {
        final TrainerEffectId drawEffect = inferTrainerDrawEffect(lower);
        if (drawEffect != null) {
            return drawEffect;
        }
        if (lower.contains("heal 60 damage") && lower.contains(DISCARD)) {
            return TrainerEffectId.SUPER_POTION;
        }
        if (lower.contains("heal 30 damage")) {
            return TrainerEffectId.HEAL_30_DAMAGE;
        }
        if ((lower.contains("opponent shuffles") && lower.contains("draws 4 cards"))
                || (lower.contains("shuffle his or her hand") && lower.contains("draw 4 cards"))) {
            return TrainerEffectId.RED_CARD;
        }
        if (lower.contains("discard an energy") && lower.contains(OPPONENT)) {
            return TrainerEffectId.TEAM_FLARE_GRUNT;
        }
        return TrainerEffectId.NONE;
    }

    private TrainerEffectId inferTrainerDrawEffect(final String lower) {
        if (lower.contains("discard your hand and draw 7 cards")) {
            return TrainerEffectId.PROFESSOR_OAK;
        }
        if (lower.contains(FLIP_A_COIN) && lower.contains("draw 3 cards")) {
            return TrainerEffectId.ROLLER_SKATES;
        }
        if (lower.contains("shuffle your hand into your deck") && lower.contains("draw 5 cards")) {
            return TrainerEffectId.SHAUNA;
        }
        if (lower.contains("draw 3 cards")) {
            return TrainerEffectId.DRAW_CARDS_3;
        }
        if (lower.contains("draw 2 cards")) {
            return TrainerEffectId.DRAW_CARDS_2;
        }
        return null;
    }

    private PokemonToolEffectId inferToolEffectId(final String cardId, final String text) {
        if (cardId == null) {
            return PokemonToolEffectId.NONE;
        }
        final String cid = cardId.toLowerCase(Locale.ROOT);
        final PokemonToolEffectId directTool = TOOL_EFFECT_BY_CARD_ID.get(cid);
        if (directTool != null) {
            return directTool;
        }
        return inferToolEffectFromText(text);
    }

    private PokemonToolEffectId inferToolEffectFromText(final String text) {
        if (text == null) {
            return PokemonToolEffectId.NONE;
        }
        final String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("muscle band")) {
            return PokemonToolEffectId.MUSCLE_BAND;
        }
        if (lower.contains("hard charm")) {
            return PokemonToolEffectId.HARD_CHARM;
        }
        if (lower.contains("protection cube")) {
            return PokemonToolEffectId.PROTECTION_CUBE;
        }
        return PokemonToolEffectId.NONE;
    }

    private static String subtype(final CardEntity entity) {
        return entity.getSubtype() != null ? entity.getSubtype() : "";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toListOfMaps(final Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        try {
            if (raw instanceof String str) {
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    str = objectMapper.readValue(str, String.class);
                }
                return objectMapper.readValue(str, new TypeReference<List<Map<String, Object>>>() { });
            }
            return objectMapper.convertValue(raw, new TypeReference<List<Map<String, Object>>>() { });
        } catch (final IOException | IllegalArgumentException e) {
            log.warn("Failed to map raw value to List<Map<String,Object>>: class={} raw={}", raw.getClass(), raw, e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> toList(final Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        try {
            if (raw instanceof byte[] bytes) {
                return objectMapper.readValue(bytes, new TypeReference<List<Object>>() { });
            }
            if (raw instanceof String str) {
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    str = objectMapper.readValue(str, String.class);
                }
                return objectMapper.readValue(str, new TypeReference<List<Object>>() { });
            }
            return objectMapper.convertValue(raw, new TypeReference<List<Object>>() { });
        } catch (final IOException | IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }
}
