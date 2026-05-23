package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackContext;

/**
 * Strategy for a Stadium card's passive runtime effect during an attack.
 */
@FunctionalInterface
public interface StadiumEffect {

    /**
     * Applies this stadium's effect to the attack context.
     *
     * @param ctx the current attack context
     */
    void apply(AttackContext ctx);

}
