package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for the Energy Grace ability:
 * Knock out Milotic, then attach 3 basic Energy from discard to 1 non-EX Pokémon.
 */
public final class EnergyGraceStrategy implements AbilityEffect {

    private static final int MAX_ENERGIES_TO_MOVE = 3;
    private static final int PRIZES_ON_KNOCKOUT = 1;

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        final boolean wasActive = action.source().equals(runtime.getActivePokemon());

        // Find target Pokémon (must be non-EX) BEFORE knockout to avoid bench index shifts
        final BattlePokemonState targetPokemon = resolveTarget(action, runtime);

        session.getKnockoutHandler().onKnockout(action.source(), PRIZES_ON_KNOCKOUT);

        if (wasActive && !runtime.getBench().isEmpty()) {
            session.setAwaitingPromotion(playerIndex);
            session.getTurnManager().interruptMainPhase();
        }

        if (targetPokemon != null && !targetPokemon.isEx()) {
            moveBasicEnergiesFromDiscard(runtime, targetPokemon);
        }
    }

    private BattlePokemonState resolveTarget(final UseAbilityAction action, final PlayerRuntime runtime) {
        if (action.targetIndex() == null || action.targetIndex() == -1) {
            return runtime.getActivePokemon();
        }
        if (action.targetIndex() >= 0 && action.targetIndex() < runtime.getBench().getAll().size()) {
            return runtime.getBench().getAll().get(action.targetIndex());
        }
        return null;
    }

    private void moveBasicEnergiesFromDiscard(final PlayerRuntime runtime, final BattlePokemonState targetPokemon) {
        final List<Card> basicEnergies = new ArrayList<>();
        for (Card card : runtime.getDiscardPile().getCards()) {
            if (card instanceof EnergyCard ec && ec.isBasic()) {
                basicEnergies.add(card);
                if (basicEnergies.size() == MAX_ENERGIES_TO_MOVE) {
                    break;
                }
            }
        }
        for (Card card : basicEnergies) {
            runtime.getDiscardPile().remove(card);
            targetPokemon.attachEnergy((EnergyCard) card);
        }
    }
}
