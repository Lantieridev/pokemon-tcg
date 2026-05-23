package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;

/**
 * Resolves a {@link AbilityEffectId} into an executable {@link AbilityEffect}.
 */
public final class AbilityEffectResolver {

    /**
     * Resolves the given ability effect ID into an {@link AbilityEffect}.
     *
     * @param effectId the mapped identifier of the ability effect
     * @return an {@link AbilityEffect} matching the id, or {@code null} if unknown/unimplemented
     */
    public AbilityEffect resolve(final AbilityEffectId effectId) {
        if (effectId == null) {
            return null;
        }

        return switch (effectId) {
            case FAIRY_TRANSFER -> (session, source) -> {
                // Implementation for Fairy Transfer:
                // As often as you like during your turn (before your attack), 
                // you may move a Fairy Energy attached to 1 of your Pokémon to another of your Pokémon.
                // NOTE: This requires targeting logic not fully modeled in UseAbilityAction yet.
            };
            case SAFEGUARD, SWEET_VEIL -> null; // Passive abilities are typically checked directly in the pipeline
            case NONE -> null;
        };
    }
}
