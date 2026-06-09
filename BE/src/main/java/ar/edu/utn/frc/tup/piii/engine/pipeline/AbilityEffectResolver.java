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
            case DRIVE_OFF -> (session, action) -> {
                final int opponentIndex = 1 - session.getActivePlayerIndex();
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
                if (!opponent.getBench().isEmpty()) {
                    session.setAwaitingPromotion(opponentIndex);
                    session.getTurnManager().interruptMainPhase();
                }
            };
            case WATER_SHURIKEN -> (session, action) -> {
                final int playerIndex = session.getActivePlayerIndex();
                final int opponentIndex = 1 - playerIndex;
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
                
                final ar.edu.utn.frc.tup.piii.engine.model.Card waterEnergy = runtime.getHand().getCards().stream()
                        .filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.WATER)
                        .findFirst().orElse(null);
                
                if (waterEnergy != null) {
                    runtime.getHand().removeCard(waterEnergy.getCardId());
                    runtime.getDiscardPile().add(waterEnergy);
                }
                
                final int targetIdx = action.targetIndex() != null ? action.targetIndex() : -1;
                ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState targetPokemon = targetIdx < 0
                        ? opponent.getActivePokemon()
                        : opponent.getBench().getAll().get(targetIdx);
                
                if (targetPokemon != null) {
                    targetPokemon.addDamageCounters(3);
                    
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
            };
            case STANCE_CHANGE -> (session, action) -> {
                final int playerIndex = session.getActivePlayerIndex();
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
                
                ar.edu.utn.frc.tup.piii.engine.model.Card aegisHand = runtime.getHand().getCards().stream()
                        .filter(c -> c.getName().equalsIgnoreCase("Aegislash") && !c.getCardId().equals(action.source().getCardId()))
                        .findFirst().orElse(null);
                
                if (aegisHand instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard newAegis) {
                    runtime.getHand().removeCard(newAegis.getCardId());
                    
                    final ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState source = action.source();
                    final ar.edu.utn.frc.tup.piii.engine.model.Card oldBaseCard = source.getBaseCard();
                    
                    if (source instanceof ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon inPlay) {
                        inPlay.swapCard(newAegis);
                    }
                    
                    runtime.getHand().addCard(oldBaseCard);
                }
            };
            case UPSIDE_DOWN_EVOLUTION -> (session, action) -> {
                final int playerIndex = session.getActivePlayerIndex();
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
                
                final ar.edu.utn.frc.tup.piii.engine.model.Card malamar = runtime.getDeck().getCards().stream()
                        .filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc && pc.getEvolvesFrom() != null && pc.getEvolvesFrom().equalsIgnoreCase("Inkay"))
                        .findFirst().orElse(null);
                
                if (malamar instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard malamarCard) {
                    runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(malamarCard.getCardId()), 1);
                    action.source().evolveInto(malamarCard);
                    runtime.getDeck().shuffle();
                }
            };
            case SAFEGUARD, SWEET_VEIL, FUR_COAT, SPIKY_SHIELD, DESTINY_BURST, FOREST_CURSE -> null;
            case NONE -> null;
        };
        return Optional.ofNullable(effect);
    }
}
