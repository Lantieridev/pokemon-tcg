package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;

/**
 * Strategy for Dusknoir's Shadow Void ability.
 * Moves 1 damage counter from 1 of your Pokémon to Dusknoir (the source).
 */
public final class ShadowVoidStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final MatchBoard board = session.getBoard();

        BattlePokemonState targetPokemon = action.targetIndex() < 0
                ? board.getActivePokemon(playerIndex)
                : board.getBenchedPokemon(playerIndex).get(action.targetIndex());

        BattlePokemonState source = action.source();

        if (targetPokemon != null && source != null) {
            targetPokemon.heal(10);
            source.addDamageCounters(1);
        }
    }
}
