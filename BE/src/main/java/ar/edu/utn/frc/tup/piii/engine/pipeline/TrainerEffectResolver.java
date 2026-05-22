package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffect;

/**
 * Parses a Trainer card's effect-text descriptor and resolves it into a {@link TrainerEffect}.
 * Matches natural text descriptions from the XY1 rulebook/database.
 */
public final class TrainerEffectResolver {

    /**
     * Resolves the given TrainerEffectId into an executable TrainerEffect.
     *
     * @param effectId the mapped identifier of the trainer effect
     * @return a {@link TrainerEffect} matching the id, or null if unknown/unimplemented
     */
    public TrainerEffect resolve(final ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId effectId) {
        if (effectId == null) {
            return null;
        }

        switch (effectId) {
            case PROFESSOR_OAK:
                return TrainerEffect.professorOak();
            case DRAW_CARDS_2:
                return TrainerEffect.drawCards(2);
            case DRAW_CARDS_3:
                return TrainerEffect.drawCards(3);
            case HEAL_30_DAMAGE:
                return TrainerEffect.healDamage(30);
            case NONE:
            default:
                return null;
        }
    }
}
