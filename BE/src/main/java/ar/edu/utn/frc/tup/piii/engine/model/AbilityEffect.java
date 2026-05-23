package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;

/**
 * Strategy for a Pokémon Ability's runtime effect (Active or Passive initialization).
 * Most passive abilities are intercepted via the AttackPipeline or StatusEffectManager,
 * but active abilities are resolved here.
 */
@FunctionalInterface
public interface AbilityEffect {
    
    /**
     * Applies this ability's effect.
     *
     * @param session the match session
     * @param source  the Pokémon using the ability
     */
    void apply(MatchSession session, BattlePokemonState source);

}
