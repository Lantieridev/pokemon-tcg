package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Identifier for the specific effect a Trainer card executes.
 *
 * <p>XY1 card mapping:
 * <ul>
 *   <li>{@code PROFESSOR_OAK} / {@code PROFESSOR_SYCAMORE} — discard hand, draw 7 (xy1-122)</li>
 *   <li>{@code ROLLER_SKATES} — flip coin: heads → draw 3 (xy1-114)</li>
 *   <li>{@code SHAUNA} — shuffle hand into deck, draw 5 (xy1-127)</li>
 *   <li>{@code SUPER_POTION} — heal 60 damage, discard 1 energy from target (xy1-128)</li>
 *   <li>{@code RED_CARD} — opponent shuffles hand into deck, draws 4 (xy1-124)
 *       Resolved in {@code GameFacade} (requires opponent runtime).</li>
 *   <li>{@code TEAM_FLARE_GRUNT} — discard 1 energy from opponent's Active (xy1-129)
 *       Resolved in {@code GameFacade} (requires opponent runtime).</li>
 *   <li>{@code CASSIUS} — shuffle 1 of your Pokémon + attached cards into your deck (xy1-115)
 *       Resolved in {@code GameFacade} (requires bench mutation).</li>
 *   <li>{@code EVOSODA} — search deck for evolution card and evolve a Pokémon (xy1-116)
 *       Resolved in {@code GameFacade} (requires bench + deck access).</li>
 *   <li>{@code GREAT_BALL} — look at top 7, take first Pokémon, shuffle rest (xy1-118).</li>
 *   <li>{@code MAX_REVIVE} — put last Pokémon from discard on top of deck (xy1-120).</li>
 *   <li>{@code PROFESSORS_LETTER} — search deck for up to 2 basic Energy cards (xy1-123).</li>
 * </ul>
 * </p>
 */
public enum TrainerEffectId {
    DRAW_CARDS_2,
    DRAW_CARDS_3,
    PROFESSOR_OAK,
    PROFESSOR_SYCAMORE,
    HEAL_30_DAMAGE,
    ROLLER_SKATES,
    SHAUNA,
    SUPER_POTION,
    RED_CARD,
    TEAM_FLARE_GRUNT,
    CASSIUS,
    EVOSODA,
    GREAT_BALL,
    MAX_REVIVE,
    PROFESSORS_LETTER,
    LYSANDRE,
    SACRED_ASH,
    POKEMON_FAN_CLUB,
    MAGNETIC_STORM,
    FIERY_TORCH,
    TRICK_SHOVEL,
    STARTLING_MEGAPHONE,
    PAL_PAD,
    BLACKSMITH,
    POKEMON_CENTER_LADY,
    ULTRA_BALL,
    CLAIRVOYANT_EYE,
    CALL_FOR_FAMILY,
    QUIVER_DANCE,
    FLASH_CLAW,
    ROCK_RUSH,
    BRILLIANT_SEARCH,
    BURIED_TREASURE_HUNT,
    DUAL_BULLET,
    PAIN_PELLETS,
    BENCH_DAMAGE_ONE,
    CURSED_DROP,
    EAR_INFLUENCE,
    RESCUE,
    FANG_SNIPE,
    PARABOLIC_CHARGE,
    REVIVAL,
    NONE
}
