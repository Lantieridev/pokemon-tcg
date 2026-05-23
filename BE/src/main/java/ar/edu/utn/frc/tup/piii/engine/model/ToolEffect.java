package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackContext;

/**
 * Strategy for a Pokémon Tool's passive runtime effect during an attack.
 */
@FunctionalInterface
public interface ToolEffect {

    /**
     * Applies this tool's effect to the attack context.
     *
     * @param ctx        the current attack context
     * @param isAttacker true if the tool is attached to the attacking Pokémon,
     *                   false if attached to the defending Pokémon
     */
    void apply(AttackContext ctx, boolean isAttacker);

}
