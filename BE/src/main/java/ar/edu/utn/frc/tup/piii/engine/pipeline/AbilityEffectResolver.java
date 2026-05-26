package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;

import java.util.Optional;

/**
 * Resolves a {@link AbilityEffectId} into an executable {@link AbilityEffect}.
 */
public final class AbilityEffectResolver {

    /**
     * Resolves the given ability effect ID into an {@link AbilityEffect}.
     *
     * @param effectId the mapped identifier of the ability effect
     * @return an {@link Optional} containing the matching {@link AbilityEffect}, or
     *         {@link Optional#empty()} when the id is null, NONE, or passive
     *         (handled directly by the pipeline).
     */
    public Optional<AbilityEffect> resolve(final AbilityEffectId effectId) {
        if (effectId == null) {
            return Optional.empty();
        }

        final AbilityEffect effect = switch (effectId) {
            case FAIRY_TRANSFER -> (session, action) -> {
                final int playerIndex = session.getActivePlayerIndex();
                final ar.edu.utn.frc.tup.piii.engine.session.MatchBoard board = session.getBoard();
                
                ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState fromPokemon = action.sourceIndex() < 0 ? board.getActivePokemon(playerIndex) : board.getBenchedPokemon(playerIndex).get(action.sourceIndex());
                ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState toPokemon = action.targetIndex() < 0 ? board.getActivePokemon(playerIndex) : board.getBenchedPokemon(playerIndex).get(action.targetIndex());
                
                if (fromPokemon != null && toPokemon != null && action.selectedEnergyIndices() != null && !action.selectedEnergyIndices().isEmpty()) {
                    int energyIndex = action.selectedEnergyIndices().get(0);
                    if (energyIndex >= 0 && energyIndex < fromPokemon.getAttachedEnergyCards().size()) {
                        var energy = fromPokemon.getAttachedEnergyCards().get(energyIndex);
                        // In Pokémon TCG, Rainbow Energy provides Fairy Energy when in play, so it can be moved.
                        // For now, we only move energies whose base type is FAIRY or which count as FAIRY.
                        if (energy.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FAIRY || energy.getCardId().contains("rainbow")) {
                            fromPokemon.removeEnergies(java.util.List.of(energyIndex));
                            toPokemon.attachEnergy(energy);
                        }
                    }
                }
            };
            case MYSTICAL_FIRE -> (session, action) -> {
                final int playerIndex = session.getActivePlayerIndex();
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
                final int currentHandSize = runtime.getHand().size();
                if (currentHandSize < 6) {
                    final int cardsToDraw = 6 - currentHandSize;
                    java.util.List<ar.edu.utn.frc.tup.piii.engine.model.Card> drawn = runtime.getDeck().drawMultiple(cardsToDraw);
                    for (ar.edu.utn.frc.tup.piii.engine.model.Card card : drawn) {
                        runtime.getHand().addCard(card);
                    }
                }
            };
            case MAGNETIC_DRAW -> (session, action) -> {
                final int playerIndex = session.getActivePlayerIndex();
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
                final int currentHandSize = runtime.getHand().size();
                if (currentHandSize < 4) {
                    final int cardsToDraw = 4 - currentHandSize;
                    java.util.List<ar.edu.utn.frc.tup.piii.engine.model.Card> drawn = runtime.getDeck().drawMultiple(cardsToDraw);
                    for (ar.edu.utn.frc.tup.piii.engine.model.Card card : drawn) {
                        runtime.getHand().addCard(card);
                    }
                }
            };
            case SAFEGUARD, SWEET_VEIL -> null; // Passive abilities are typically checked directly in the pipeline
            case NONE -> null;
        };
        return Optional.ofNullable(effect);
    }
}
