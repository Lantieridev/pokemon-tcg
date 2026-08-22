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

        final BattlePokemonState fromPokemon = resolvePokemon(board, playerIndex, action.sourceIndex());
        final BattlePokemonState toPokemon = resolvePokemon(board, playerIndex, action.targetIndex());
        if (fromPokemon == null || toPokemon == null
                || action.selectedEnergyIndices() == null || action.selectedEnergyIndices().isEmpty()) {
            return;
        }

        final int energyIndex = action.selectedEnergyIndices().get(0);
        if (energyIndex < 0 || energyIndex >= fromPokemon.getAttachedEnergyCards().size()) {
            return;
        }

        final var energy = fromPokemon.getAttachedEnergyCards().get(energyIndex);
        if (isTransferAllowed(energy)) {
            fromPokemon.removeEnergies(List.of(energyIndex));
            toPokemon.attachEnergy(energy);
        }
    }

    private BattlePokemonState resolvePokemon(final MatchBoard board, final int playerIndex, final int index) {
        return index < 0
                ? board.getActivePokemon(playerIndex)
                : board.getBenchedPokemon(playerIndex).get(index);
    }

    private boolean isTransferAllowed(final ar.edu.utn.frc.tup.piii.engine.model.EnergyCard energy) {
        final boolean isAllowedType = energy.getEnergyType() == allowedEnergyType;
        final boolean isAllowedSpecial = allowedSpecialEnergyIds.stream()
                .anyMatch(id -> energy.getCardId() != null && energy.getCardId().contains(id));
        return isAllowedType || isAllowedSpecial;
    }
}
