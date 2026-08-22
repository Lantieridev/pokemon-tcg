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

    private static final int EX_KNOCKOUT_PRIZES = 2;
    private static final int STANDARD_KNOCKOUT_PRIZES = 1;
    private static final int DAMAGE_PER_COUNTER = 10;

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final int opponentIndex = 1 - playerIndex;
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        final PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);

        discardOneMatchingEnergy(runtime);

        final int targetIdx = action.targetIndex() != null ? action.targetIndex() : -1;
        final BattlePokemonState targetPokemon = targetIdx < 0
                ? opponent.getActivePokemon()
                : opponent.getBench().getAll().get(targetIdx);
        if (targetPokemon == null) {
            return;
        }

        targetPokemon.addDamageCounters(damageCounters);
        if (targetPokemon.getDamageCounters() * DAMAGE_PER_COUNTER >= targetPokemon.getMaxHp()) {
            applyKnockout(session, opponent, opponentIndex, targetPokemon, targetIdx);
        }
    }

    private void discardOneMatchingEnergy(final PlayerRuntime runtime) {
        final Card energyCard = runtime.getHand().getCards().stream()
                .filter(c -> c instanceof EnergyCard ec && ec.getEnergyType() == requiredEnergyType)
                .findFirst().orElse(null);
        if (energyCard != null) {
            runtime.getHand().removeCard(energyCard.getCardId());
            runtime.getDiscardPile().add(energyCard);
        }
    }

    private void applyKnockout(final MatchSession session, final PlayerRuntime opponent, final int opponentIndex,
            final BattlePokemonState targetPokemon, final int targetIdx) {
        final int prizes = targetPokemon.isEx() ? EX_KNOCKOUT_PRIZES : STANDARD_KNOCKOUT_PRIZES;
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
