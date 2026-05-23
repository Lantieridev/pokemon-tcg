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
    NONE
}
