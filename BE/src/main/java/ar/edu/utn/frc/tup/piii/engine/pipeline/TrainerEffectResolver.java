package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.StadiumEffect;
import ar.edu.utn.frc.tup.piii.engine.model.ToolEffect;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffect;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves a {@link TrainerEffectId} into an executable {@link TrainerEffect}.
 *
 * <p>Effects that require a coin flip (e.g. Roller Skates) capture the provided
 * {@link CoinFlipper} in a closure so the {@link TrainerEffect} interface remains
 * a simple two-arg strategy.</p>
 *
 * <p>All {@code resolveXxx} methods return {@link Optional} to honor the engine's
 * "no silent null" rule. Empty results signal that the effect is unimplemented
 * here or handled directly elsewhere in the pipeline.</p>
 */
public final class TrainerEffectResolver {

    private static final int DRAW_CARDS_2_AMOUNT = 2;
    private static final int DRAW_CARDS_3_AMOUNT = 3;
    private static final int HEAL_30_AMOUNT = 30;

    // Effects with no dependency on the coin flipper — a flat lookup table instead of a giant
    // switch keeps this a simple map traversal rather than ~34 branches, since every case not
    // listed here (RED_CARD, TEAM_FLARE_GRUNT, ... — resolved directly in GameFacade) is absent
    // and naturally falls through to Optional.empty() via Map#get returning null.
    private static final Map<TrainerEffectId, Supplier<TrainerEffect>> SIMPLE_EFFECT_HANDLERS = Map.of(
            TrainerEffectId.PROFESSOR_OAK, TrainerEffect::professorOak,
            TrainerEffectId.PROFESSOR_SYCAMORE, TrainerEffect::professorOak,
            TrainerEffectId.DRAW_CARDS_2, () -> TrainerEffect.drawCards(DRAW_CARDS_2_AMOUNT),
            TrainerEffectId.DRAW_CARDS_3, () -> TrainerEffect.drawCards(DRAW_CARDS_3_AMOUNT),
            TrainerEffectId.HEAL_30_DAMAGE, () -> TrainerEffect.healDamage(HEAL_30_AMOUNT),
            TrainerEffectId.SHAUNA, TrainerEffect::shauna,
            TrainerEffectId.SUPER_POTION, TrainerEffect::superPotion
    );

    /**
     * Resolves the given effect ID into a {@link TrainerEffect}, using the supplied
     * {@link CoinFlipper} for any coin-dependent effects.
     *
     * @param effectId the mapped identifier of the trainer effect (may be null → empty)
     * @param flipper  the coin-flip provider for coin-dependent effects (may be null for non-coin effects)
     * @return an {@link Optional} containing the matching {@link TrainerEffect}, or empty
     *         when the id is null, NONE, or is resolved directly elsewhere
     */
    public Optional<TrainerEffect> resolve(final TrainerEffectId effectId, final CoinFlipper flipper) {
        if (effectId == null) {
            return Optional.empty();
        }
        // ROLLER_SKATES needs the caller-supplied flipper, so it can't live in a no-arg Supplier map entry.
        if (effectId == TrainerEffectId.ROLLER_SKATES) {
            return Optional.of(TrainerEffect.rollerSkates(flipper != null ? flipper : () -> false));
        }
        return Optional.ofNullable(SIMPLE_EFFECT_HANDLERS.get(effectId)).map(Supplier::get);
    }

    /**
     * Resolves the given tool effect ID into a {@link ToolEffect}.
     *
     * @param effectId the mapped identifier of the tool effect
     * @return an {@link Optional} containing the matching {@link ToolEffect}, or empty
     *         when the id is null, NONE, or unimplemented
     */
    public Optional<ToolEffect> resolveTool(final PokemonToolEffectId effectId) {
        if (effectId == null) {
            return Optional.empty();
        }

        final ToolEffect effect = switch (effectId) {
            case MUSCLE_BAND -> (ctx, isAttacker) -> {
                if (isAttacker) {
                    ctx.addAttackerModifier(dmg -> dmg + 20);
                }
            };
            case HARD_CHARM -> (ctx, isAttacker) -> {
                if (!isAttacker) {
                    ctx.addDefenderModifier(dmg -> dmg > 0 ? 20 : 0);
                }
            };
            case PROTECTION_CUBE -> null; // Handled directly in AttackEffectResolver
            case NONE -> null;
        };
        return Optional.ofNullable(effect);
    }

    /**
     * Resolves the given stadium card ID into a {@link StadiumEffect}.
     *
     * @param cardId the ID of the stadium card
     * @return an {@link Optional} containing the matching {@link StadiumEffect}, or empty
     *         when the id is null or unimplemented
     */
    private static final String SHADOW_CIRCLE_CARD_ID = "xy1-126";

    public Optional<StadiumEffect> resolveStadium(final String cardId) {
        if (cardId == null) {
            return Optional.empty();
        }

        // Exact mappings by card ID (XY1 specific)
        if (SHADOW_CIRCLE_CARD_ID.equals(cardId)) { // Shadow Circle
            return Optional.of(ctx -> {
                if (ctx.getDefender().getPokemonType() == PokemonType.DARKNESS) {
                    ctx.setWeaknessSuppressed(true);
                }
            });
        }

        // Fairy Garden (xy1-117) affects retreat cost, which is handled elsewhere.
        return Optional.empty();
    }
}
