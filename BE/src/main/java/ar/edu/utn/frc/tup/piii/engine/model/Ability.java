package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.Objects;

/**
 * Represents a Pokémon Ability (either passive or active).
 *
 * @param name      the name of the ability
 * @param text      the descriptive text of what the ability does
 * @param effectId  the mapped engine ID for this ability
 */
public record Ability(String name, String text, AbilityEffectId effectId) {
    public Ability {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(effectId, "effectId must not be null");
    }
}
