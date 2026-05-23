package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.StadiumEffect;
import ar.edu.utn.frc.tup.piii.engine.model.ToolEffect;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffect;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;

/**
 * Resolves a {@link TrainerEffectId} into an executable {@link TrainerEffect}.
 *
 * <p>Effects that require a coin flip (e.g. Roller Skates) capture the provided
 * {@link CoinFlipper} in a closure so the {@link TrainerEffect} interface remains
 * a simple two-arg strategy.</p>
 */
public final class TrainerEffectResolver {

    /**
     * Resolves the given effect ID into a {@link TrainerEffect}, using the supplied
     * {@link CoinFlipper} for any coin-dependent effects.
     *
     * @param effectId the mapped identifier of the trainer effect (may be null → null returned)
     * @param flipper  the coin-flip provider for coin-dependent effects (may be null for non-coin effects)
     * @return a {@link TrainerEffect} matching the id, or {@code null} if unknown/unimplemented
     */
    public TrainerEffect resolve(final TrainerEffectId effectId, final CoinFlipper flipper) {
        if (effectId == null) {
            return null;
        }

        return switch (effectId) {
            case PROFESSOR_OAK, PROFESSOR_SYCAMORE -> TrainerEffect.professorOak();
            case DRAW_CARDS_2      -> TrainerEffect.drawCards(2);
            case DRAW_CARDS_3      -> TrainerEffect.drawCards(3);
            case HEAL_30_DAMAGE    -> TrainerEffect.healDamage(30);
            case ROLLER_SKATES     -> TrainerEffect.rollerSkates(flipper != null ? flipper : () -> false);
            case SHAUNA            -> TrainerEffect.shauna();
            case SUPER_POTION      -> TrainerEffect.superPotion();
            // Resolved directly in GameFacade (interactive selection, bench mutation, or opponent access).
            case RED_CARD, TEAM_FLARE_GRUNT, CASSIUS, EVOSODA, GREAT_BALL, MAX_REVIVE, PROFESSORS_LETTER -> null;
            case NONE              -> null;
        };
    }

    /**
     * Resolves the given tool effect ID into a {@link ToolEffect}.
     *
     * @param effectId the mapped identifier of the tool effect
     * @return a {@link ToolEffect} matching the id, or {@code null} if unknown/unimplemented
     */
    public ToolEffect resolveTool(final PokemonToolEffectId effectId) {
        if (effectId == null) {
            return null;
        }

        return switch (effectId) {
            case MUSCLE_BAND -> (ctx, isAttacker) -> {
                if (isAttacker) {
                    ctx.addAttackerModifier(dmg -> dmg + 20);
                }
            };
            case HARD_CHARM -> (ctx, isAttacker) -> {
                if (!isAttacker) {
                    ctx.addDefenderModifier(dmg -> Math.max(0, dmg - 20));
                }
            };
            case NONE -> null;
        };
    }

    /**
     * Resolves the given stadium card ID into a {@link StadiumEffect}.
     *
     * @param cardId the ID of the stadium card
     * @return a {@link StadiumEffect} for the card, or {@code null} if unknown/unimplemented
     */
    public StadiumEffect resolveStadium(final String cardId) {
        if (cardId == null) {
            return null;
        }

        // Exact mappings by card ID (XY1 specific)
        if ("xy1-126".equals(cardId)) { // Shadow Circle
            return ctx -> {
                if (ctx.getDefender().getPokemonType() == PokemonType.DARKNESS) {
                    ctx.setWeaknessSuppressed(true);
                }
            };
        }

        // Fairy Garden (xy1-117) affects retreat cost, which is handled elsewhere.
        return null;
    }
}
