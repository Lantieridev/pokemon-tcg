package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;

import java.util.List;

/**
 * Strategy for transferring an energy of a specific type or matching special card IDs between Pokémon.
 */
public final class FairyTransferStrategy implements AbilityEffect {

    private final PokemonType allowedEnergyType;
    private final List<String> allowedSpecialEnergyIds;

    public FairyTransferStrategy(PokemonType allowedEnergyType, List<String> allowedSpecialEnergyIds) {
        this.allowedEnergyType = allowedEnergyType;
        this.allowedSpecialEnergyIds = allowedSpecialEnergyIds;
    }

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final MatchBoard board = session.getBoard();
        
        BattlePokemonState fromPokemon = action.sourceIndex() < 0 
                ? board.getActivePokemon(playerIndex) 
                : board.getBenchedPokemon(playerIndex).get(action.sourceIndex());
        BattlePokemonState toPokemon = action.targetIndex() < 0 
                ? board.getActivePokemon(playerIndex) 
                : board.getBenchedPokemon(playerIndex).get(action.targetIndex());
        
        if (fromPokemon != null && toPokemon != null && action.selectedEnergyIndices() != null && !action.selectedEnergyIndices().isEmpty()) {
            int energyIndex = action.selectedEnergyIndices().get(0);
            if (energyIndex >= 0 && energyIndex < fromPokemon.getAttachedEnergyCards().size()) {
                var energy = fromPokemon.getAttachedEnergyCards().get(energyIndex);
                boolean isAllowedType = energy.getEnergyType() == allowedEnergyType;
                boolean isAllowedSpecial = allowedSpecialEnergyIds.stream()
                        .anyMatch(id -> energy.getCardId() != null && energy.getCardId().contains(id));
                
                if (isAllowedType || isAllowedSpecial) {
                    fromPokemon.removeEnergies(List.of(energyIndex));
                    toPokemon.attachEnergy(energy);
                }
            }
        }
    }
}
