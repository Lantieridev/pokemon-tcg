package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

/**
 * Strategy for the Water Shuriken ability, discarding a specific type of energy from hand to deal damage to any target.
 */
public final class WaterShurikenStrategy implements AbilityEffect {

    private final PokemonType requiredEnergyType;
    private final int damageCounters;

    public WaterShurikenStrategy(PokemonType requiredEnergyType, int damageCounters) {
        this.requiredEnergyType = requiredEnergyType;
        this.damageCounters = damageCounters;
    }

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final int opponentIndex = 1 - playerIndex;
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        final PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
        
        final Card energyCard = runtime.getHand().getCards().stream()
                .filter(c -> c instanceof EnergyCard ec && ec.getEnergyType() == requiredEnergyType)
                .findFirst().orElse(null);
        
        if (energyCard != null) {
            runtime.getHand().removeCard(energyCard.getCardId());
            runtime.getDiscardPile().add(energyCard);
        }
        
        final int targetIdx = action.targetIndex() != null ? action.targetIndex() : -1;
        BattlePokemonState targetPokemon = targetIdx < 0
                ? opponent.getActivePokemon()
                : opponent.getBench().getAll().get(targetIdx);
        
        if (targetPokemon != null) {
            targetPokemon.addDamageCounters(damageCounters);
            
            if (targetPokemon.getDamageCounters() * 10 >= targetPokemon.getMaxHp()) {
                final int prizes = targetPokemon.isEx() ? 2 : 1;
                session.getKnockoutHandler().onKnockout(targetPokemon, prizes);
                
                if (targetIdx < 0) {
                    opponent.clearActivePokemon();
                    if (!opponent.getBench().isEmpty()) {
                        session.setAwaitingPromotion(opponentIndex);
                    }
                } else {
                    opponent.getBench().remove(targetIdx);
                }
                opponent.getDiscardPile().add(targetPokemon.getBaseCard());
                targetPokemon.getUnderlyingCards().forEach(opponent.getDiscardPile()::add);
                targetPokemon.getAttachedEnergyCards().forEach(opponent.getDiscardPile()::add);
                targetPokemon.getAttachedTool().ifPresent(opponent.getDiscardPile()::add);
                opponent.removePokemonFromPlay(targetPokemon);
            }
        }
    }
}
