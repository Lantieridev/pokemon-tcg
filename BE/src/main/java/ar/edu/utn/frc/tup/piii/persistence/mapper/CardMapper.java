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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.EnumMap;
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
public final class CardMapper {

    private static final Map<String, PokemonType> TYPE_BY_NAME;
    private static final Map<String, TrainerType> TRAINER_TYPE_BY_SUBTYPE;
    private static final Map<String, EvolutionStage> EVOLUTION_STAGE_BY_SUBTYPE;
    private static final Map<String, AbilityEffectId> ABILITY_EFFECT_ID_BY_NAME;

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
    }

    private final ObjectMapper objectMapper;
    private final Map<String, Function<CardEntity, Card>> dispatchers;

    public CardMapper(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        final Map<String, Function<CardEntity, Card>> d = new HashMap<>();
        d.put("Pokémon", this::mapPokemon);
        d.put("Trainer", this::mapTrainer);
        d.put("Energy",  this::mapEnergy);
        this.dispatchers = Collections.unmodifiableMap(d);
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
     *
     * <p>Checked in order: attacks whose effect depends only on their name (a plain lookup),
     * then a handful of exact-name attacks whose effect also depends on the text (Petal
     * Blizzard, Call for Family, Bounce), then a cascade of text-pattern rules grouped by
     * category — each category is its own method so this dispatcher stays a flat sequence
     * of "does this category match?" checks instead of one long nested chain.</p>
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

        if ("petal blizzard".equals(lowerName)) {
            if (lower.contains("20")) {
                return "damage_all_opponents:20";
            }
            return "damage_all_opponents:10";
        }
        if ("call for family".equals(lowerName)) {
            if (lower.contains("up to 2")) {
                return "call_for_family:2";
            }
            return "call_for_family:1";
        }
        if ("bounce".equals(lowerName)) {
            if (lower.contains("flip a coin")) {
                return "coin_flip_switch_self";
            }
            return "switch_self";
        }

        String effect = inferDrawCardsEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferDamageModifierEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferCoinFlipEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferSleepAndStatusEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferDamagePreventionEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferHealAndSelfDamageEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferDiscardEffect(lower);
        if (effect != null) {
            return effect;
        }
        effect = inferCounterAndBenchEffect(lower);
        if (effect != null) {
            return effect;
        }

        return "";
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
        if (lower.contains("times the number of damage counters on this pok")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage\\s+times").matcher(lower);
            if (m.find()) {
                return "damage_times_self_counters:" + m.group(1);
            }
        }
        if (lower.contains("for each") && lower.contains("opponent") && lower.contains("retreat cost")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+more\\s+damage").matcher(lower);
            if (m.find()) {
                return "damage_per_retreat_cost:" + m.group(1);
            }
        }
        if (lower.contains("more damage for each energy attached to your opponent")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+more\\s+damage").matcher(lower);
            if (m.find()) {
                return "damage_per_opponent_all_energy:" + m.group(1);
            }
        }
        if (lower.contains("more damage for each") && lower.contains("energy")) {
            final java.util.regex.Matcher mDamage = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+more\\s+damage").matcher(lower);
            final java.util.regex.Matcher mEnergy = java.util.regex.Pattern.compile("for each\\s+(\\w+)\\s+energy").matcher(lower);
            if (mDamage.find() && mEnergy.find()) {
                return "damage_per_energy_type:" + mEnergy.group(1) + ":" + mDamage.group(1);
            }
        }
        if (lower.contains("already has any damage counters")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+more\\s+damage").matcher(lower);
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
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+more\\s+damage").matcher(lower);
            if (m.find()) {
                return "revenge_damage:" + m.group(1);
            }
        }
        if (lower.contains("times the number of prize cards your opponent has taken")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+damage\\s+times").matcher(lower);
            if (m.find()) {
                return "damage_per_opponent_prize:" + m.group(1);
            }
        }
        return null;
    }

    private String inferCoinFlipEffect(final String lower) {
        if (lower.contains("until you get tails")) {
            if (lower.contains("discard") && lower.contains("energy") && (lower.contains("opponent") || lower.contains("defending") || lower.contains("active"))) {
                return "coin_flips_until_tails_discard_opponent_energy";
            }
            if (lower.contains("more damage")) {
                final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage").matcher(lower);
                if (m.find()) {
                    return "coin_flips_until_tails_extra:" + m.group(1);
                }
            }
        }

        if (lower.contains("flip") && lower.contains("coin") && lower.contains("times the number of heads")) {
            if (lower.contains("until you get tails")) {
                final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+damage").matcher(lower);
                if (m.find()) {
                    return "coin_flips_until_tails:" + m.group(1);
                }
            } else if (lower.contains("for each") && lower.contains("energy")) {
                final java.util.regex.Matcher mEnergy = java.util.regex.Pattern.compile("for each\\s+(\\w+)\\s+energy").matcher(lower);
                final java.util.regex.Matcher mDamage = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+damage").matcher(lower);
                if (mEnergy.find() && mDamage.find()) {
                    return "coin_flips_per_energy:" + mEnergy.group(1) + ":" + mDamage.group(1);
                }
            } else if (lower.contains("for each damage counter")) {
                final java.util.regex.Matcher m = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+damage").matcher(lower);
                if (m.find()) {
                    return "coin_flips_per_damage_counter:" + m.group(1);
                }
            } else {
                final java.util.regex.Matcher mCoins = java.util.regex.Pattern.compile("flip\\s+(\\d+)\\s+coins").matcher(lower);
                final java.util.regex.Matcher mDamage = java.util.regex.Pattern.compile("does\\s+(\\d+)\\s+damage").matcher(lower);
                if (mCoins.find() && mDamage.find()) {
                    return "coin_flips_multiplier:" + mCoins.group(1) + ":" + mDamage.group(1);
                }
            }
        }

        if (lower.contains("flip a coin") && lower.contains("more damage")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage").matcher(lower);
            if (m.find()) {
                return "coin_flip_extra:" + m.group(1);
            }
        }

        if (lower.contains("flip a coin") && (lower.contains("if tails, this attack does nothing") || lower.contains("if tails, that attack does nothing"))) {
            return "coin_flip_fail";
        }

        if (lower.contains("flip a coin") && lower.contains("switch this pok") && lower.contains("benched")) {
            return "coin_flip_switch_self";
        }

        if (lower.contains("flip a coin") || lower.contains("flip 2 coins")) {
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

        if (lower.contains("is now poisoned") || lower.contains("the defending pokémon is now poisoned") || lower.contains("now poisoned")) {
            return "poison";
        }
        if (lower.contains("is now burned") || lower.contains("the defending pokémon is now burned") || lower.contains("now burned")) {
            return "burn";
        }
        if (lower.contains("is now paralyzed") || lower.contains("the defending pokémon is now paralyzed") || lower.contains("now paralyzed")) {
            return "paralysis";
        }
        if (lower.contains("is now asleep") || lower.contains("the defending pokémon is now asleep") || lower.contains("now asleep")) {
            return "sleep";
        }
        if (lower.contains("is now confused") || lower.contains("the defending pokémon is now confused") || lower.contains("now confused")) {
            return "confusion";
        }

        if (lower.contains("can't use that attack") || lower.contains("cant use that attack")) {
            return "disable_attack";
        }
        return null;
    }

    private String inferDamagePreventionEffect(final String lower) {
        if (lower.contains("prevent all damage done to this pok")
                || lower.contains("prevent all effects of attacks, including damage, done to this pok")
                || lower.contains("prevent that attack's damage done to this pok")) {
            if (lower.contains("60 or less")) {
                if (lower.contains("flip a coin")) {
                    return "coin_flip_prevent_damage_60_or_less";
                }
                return "prevent_damage_60_or_less";
            }
            if (lower.contains("flip a coin")) {
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
        if (lower.contains("discard") && lower.contains("top") && lower.contains("your deck")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("discard\\s+the\\s+top\\s+(\\d+)\\s+cards").matcher(lower);
            if (m.find()) {
                return "discard_deck_self:" + m.group(1);
            }
        }

        if (lower.contains("discard") && (lower.contains("energy") || lower.contains("a darkness energy"))) {
            final boolean opponent = lower.contains("opponent") || lower.contains("defending");
            final boolean coinFlip = lower.contains("flip a coin") || lower.contains("flip");
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

        if (lower.contains("move") && lower.contains("damage counters") && lower.contains("opponent")) {
            return "move_opponent_counters";
        }

        // Ordering matches the pre-extraction if-chain: this discard check ran between
        // "move counters" and "basic from discard" below, interleaved with the counter/bench
        // checks rather than grouped with the other discard checks in inferDiscardEffect().
        if (lower.contains("discard") && lower.contains("opponent's hand") && lower.contains("until") && lower.contains("card")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("until.*\\s+(\\d+)\\s+card").matcher(lower);
            if (m.find()) {
                return "discard_opponent_hand_to_limit:" + m.group(1);
            }
            return "discard_opponent_hand_to_limit:4";
        }

        if (lower.contains("basic pok") && lower.contains("opponent's discard") && lower.contains("bench")) {
            return "place_opponent_basic_from_discard";
        }

        // Same as above: this ran between "basic from discard" and "shuffle" originally.
        if (lower.contains("reveals") && lower.contains("hand") && lower.contains("trainer") && lower.contains("discard")) {
            return "discard_trainer_from_opponent_hand";
        }

        if (lower.contains("shuffle") && lower.contains("pok") && lower.contains("discard pile into your deck")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("shuffle\\s+(\\d+)\\s+pok").matcher(lower);
            if (m.find()) {
                return "shuffle_pokemon_from_discard:" + m.group(1);
            }
            return "shuffle_pokemon_from_discard:3";
        }

        if (lower.contains("each of your opponent's benched pok")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage").matcher(lower);
            if (m.find()) {
                return "bench_damage:" + m.group(1);
            }
        } else if (lower.contains("1 of your opponent's benched pok")
                || lower.contains("one of your opponent's benched pok")
                || (lower.contains("opponent's benched") && (lower.contains("1 of") || lower.contains("one of")))) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage").matcher(lower);
            if (m.find()) {
                return "bench_damage_one:" + m.group(1);
            }
        } else if (lower.contains("opponent's benched")) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+damage").matcher(lower);
            if (m.find()) {
                return "bench_damage:" + m.group(1);
            }
        }

        if (lower.contains("your opponent switches") && (lower.contains("benched") || lower.contains("bench"))) {
            return "force_switch_opponent";
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
        // Card ID matching takes priority over text-based heuristics (more specific and reliable).
        if ("xy1-115".equals(cardId)) { return TrainerEffectId.CASSIUS; }
        if ("xy1-116".equals(cardId)) { return TrainerEffectId.EVOSODA; }
        if ("xy1-118".equals(cardId)) { return TrainerEffectId.GREAT_BALL; }
        if ("xy1-120".equals(cardId)) { return TrainerEffectId.MAX_REVIVE; }
        if ("xy1-123".equals(cardId)) { return TrainerEffectId.PROFESSORS_LETTER; }
        
        // Flashfire Trainer Mappings
        if ("xy2-88".equals(cardId) || "xy2-88a".equals(cardId)) { return TrainerEffectId.BLACKSMITH; }
        if ("xy2-89".equals(cardId)) { return TrainerEffectId.FIERY_TORCH; }
        if ("xy2-90".equals(cardId) || "xy2-104".equals(cardId)) { return TrainerEffectId.LYSANDRE; }
        if ("xy2-91".equals(cardId)) { return TrainerEffectId.MAGNETIC_STORM; }
        if ("xy2-92".equals(cardId)) { return TrainerEffectId.PAL_PAD; }
        if ("xy2-93".equals(cardId) || "xy2-105".equals(cardId)) { return TrainerEffectId.POKEMON_CENTER_LADY; }
        if ("xy2-94".equals(cardId) || "xy2-106".equals(cardId)) { return TrainerEffectId.POKEMON_FAN_CLUB; }
        if ("xy2-96".equals(cardId)) { return TrainerEffectId.SACRED_ASH; }
        if ("xy2-97".equals(cardId)) { return TrainerEffectId.STARTLING_MEGAPHONE; }
        if ("xy2-98".equals(cardId)) { return TrainerEffectId.TRICK_SHOVEL; }
        if ("xy2-99".equals(cardId)) { return TrainerEffectId.ULTRA_BALL; }

        if (text == null || text.isBlank()) {
            return TrainerEffectId.NONE;
        }
        final String lower = text.toLowerCase(Locale.ROOT);
        
        // ── Professor Sycamore / Professor's Research ───────────────────────────
        if (lower.contains("discard your hand and draw 7 cards")) {
            return TrainerEffectId.PROFESSOR_OAK;
        }
        // ── Healing ─────────────────────────────────────────────────────────────
        if (lower.contains("heal 60 damage") && lower.contains("discard")) {
            return TrainerEffectId.SUPER_POTION; // xy1-128
        }
        if (lower.contains("heal 30 damage")) {
            return TrainerEffectId.HEAL_30_DAMAGE; // Potion
        }
        // ── Draw effects ────────────────────────────────────────────────────────
        if (lower.contains("flip a coin") && lower.contains("draw 3 cards")) {
            return TrainerEffectId.ROLLER_SKATES; // xy1-114
        }
        if (lower.contains("shuffle your hand into your deck") && lower.contains("draw 5 cards")) {
            return TrainerEffectId.SHAUNA; // xy1-127
        }
        if (lower.contains("draw 3 cards")) {
            return TrainerEffectId.DRAW_CARDS_3; // Tierno, Hau
        }
        if (lower.contains("draw 2 cards")) {
            return TrainerEffectId.DRAW_CARDS_2; // Cheren
        }
        // ── Opponent-targeting effects ───────────────────────────────────────────
        if (lower.contains("opponent shuffles") && lower.contains("draws 4 cards")
                || lower.contains("shuffle his or her hand") && lower.contains("draw 4 cards")) {
            return TrainerEffectId.RED_CARD; // xy1-124
        }
        if (lower.contains("discard an energy") && lower.contains("opponent")) {
            return TrainerEffectId.TEAM_FLARE_GRUNT; // xy1-129
        }
        return TrainerEffectId.NONE;
    }

    private PokemonToolEffectId inferToolEffectId(final String cardId, final String text) {
        if (cardId == null) {
            return PokemonToolEffectId.NONE;
        }
        final String cid = cardId.toLowerCase(Locale.ROOT);
        if (cid.contains("xy1-121") || (text != null && text.toLowerCase(Locale.ROOT).contains("muscle band"))) {
            return PokemonToolEffectId.MUSCLE_BAND; // xy1-121
        }
        if (cid.contains("xy1-119") || (text != null && text.toLowerCase(Locale.ROOT).contains("hard charm"))) {
            return PokemonToolEffectId.HARD_CHARM; // xy1-119
        }
        if (cid.contains("xy2-95") || (text != null && text.toLowerCase(Locale.ROOT).contains("protection cube"))) {
            return PokemonToolEffectId.PROTECTION_CUBE; // xy2-95
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
        } catch (final Exception e) {
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
        } catch (final Exception e) {
            return Collections.emptyList();
        }
    }
}
