package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import java.util.List;

/**
 * Strategy for the Gooey Regeneration ability (Goodra).
 * Discards an attached energy to heal 60 damage from this Pokémon.
 */
public final class GooeyRegenerationStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        final BattlePokemonState source = action.source();

        if (source == null) {
            return;
        }

        final List<Integer> indices = action.selectedEnergyIndices();
        int energyIndex = -1;
        if (indices != null && !indices.isEmpty()) {
            energyIndex = indices.get(0);
        } else {
            if (!source.getAttachedEnergyCards().isEmpty()) {
                energyIndex = 0;
            }
        }

        if (energyIndex >= 0 && energyIndex < source.getAttachedEnergyCards().size()) {
            final EnergyCard energy = source.getAttachedEnergyCards().get(energyIndex);
            
            // Remove from Pokémon
            source.removeEnergies(List.of(energyIndex));
            
            // Discard
            runtime.getDiscardPile().add(energy);
            
            // Heal 60 HP
            source.heal(60);
        }
    }
}
