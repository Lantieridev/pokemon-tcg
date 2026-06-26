package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.AttackDTO;
import ar.edu.utn.frc.tup.piii.dtos.BattlePokemonDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PendingSelectionRequestDTO;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps a {@link MatchSession} to a player-specific {@link GameStateResponseDTO}.
 * Enforces war-fog: the opponent's hand is reduced to a count only.
 */
@Component
public final class PlayerPerspectiveMapper {

    private static final long INITIAL_VERSION = 1L;

    /**
     * Produces a {@link GameStateResponseDTO} tailored for the player at {@code viewerIndex}.
     * The viewer gets a full {@link GameStateResponseDTO.PlayerView}; the opponent gets
     * a {@link GameStateResponseDTO.OpponentView} with only {@code handSize}.
     *
     * @param session     the active match session (never null)
     * @param viewerIndex 0 or 1 — the index of the player receiving this view
     * @return a non-null response DTO
     */
    public GameStateResponseDTO toResponse(final MatchSession session, final int viewerIndex) {
        final int opponentIndex = 1 - viewerIndex;

        final GameStateResponseDTO.PlayerView self = buildPlayerView(session, viewerIndex);
        final GameStateResponseDTO.OpponentView opponent = buildOpponentView(session, opponentIndex);
        
        PendingSelectionRequestDTO requestDto = null;
        if (session.getPendingSelectionRequest() != null) {
            final var req = session.getPendingSelectionRequest();
            java.util.List<String> options = java.util.Collections.emptyList();
            final boolean isOpponentChoosing = req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FLASH_CLAW;
            final boolean isViewerChoosing = (isOpponentChoosing && session.getActivePlayerIndex() != viewerIndex)
                    || (!isOpponentChoosing && session.getActivePlayerIndex() == viewerIndex);
            if (isViewerChoosing) {
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(viewerIndex);
                if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK) {
                    var stream = runtime.getDeck().getCards().stream();
                    if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PROFESSORS_LETTER) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.isBasic());
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.QUIVER_DANCE) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.isBasic());
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EVOSODA) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc &&
                                pc.getEvolvesFrom() != null &&
                                req.target() != null &&
                                pc.getEvolvesFrom().equalsIgnoreCase(req.target().getName()));
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_FAN_CLUB) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC);
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ULTRA_BALL) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard);
                    }
                    options = stream.map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
                } else if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE) {
                    final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime targetDiscardRuntime = (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.REVIVAL)
                            ? session.getPlayerRuntime(1 - viewerIndex)
                            : runtime;
                    var stream = targetDiscardRuntime.getDiscardPile().getCards().stream();
                    if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.MAX_REVIVE
                            || req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.REVIVAL) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC);
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SACRED_ASH
                            || req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.RESCUE) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard);
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE);
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAL_PAD) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.TrainerCard tc && tc.getTrainerType() == ar.edu.utn.frc.tup.piii.engine.model.TrainerType.SUPPORTER);
                    }
                    options = stream.map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
                } else if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK) {
                    final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime deckOwnerRuntime = (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TRICK_SHOVEL
                            && req.target() != null && !runtime.hasPokemonInPlay(req.target()))
                            ? session.getPlayerRuntime(1 - viewerIndex)
                            : runtime;
                    final int limitAmount = (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TRICK_SHOVEL) ? 1
                            : (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CLAIRVOYANT_EYE) ? 3
                            : (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BURIED_TREASURE_HUNT) ? 4 : 7;
                    var stream = deckOwnerRuntime.getDeck().getCards().stream().limit(limitAmount);
                    options = stream.map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
                } else if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND) {
                    final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime targetHandRuntime = (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FANG_SNIPE)
                            ? session.getPlayerRuntime(1 - viewerIndex)
                            : runtime;
                    var stream = targetHandRuntime.getHand().getCards().stream();
                    if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FIERY_TORCH) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE);
                    } else if (req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ROCK_RUSH) {
                        stream = stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && (ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIGHTING || ec.isProvidesAllTypes()));
                    }
                    options = stream.map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
                } else if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD) {
                    final java.util.List<String> list = new java.util.ArrayList<>();
                    final ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState active = session.getBoard().getActivePokemon(opponentIndex);
                    if (active != null) {
                        list.add(active.getCardId());
                    }
                    final java.util.List<ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState> benched = session.getBoard().getBenchedPokemon(opponentIndex);
                    if (benched != null) {
                        for (var p : benched) {
                            if (p != null) {
                                list.add(p.getCardId());
                            }
                        }
                    }
                    options = list;
                }
            }
            requestDto = new PendingSelectionRequestDTO(req.sourceEffect(), req.target() != null ? req.target().getCardId() : null, req.maxSelections(), req.source(), options);
        }

        final int turnNumber = session.getTurnManager() != null ?
                (session.getTurnManager().getTurnCount(0) + session.getTurnManager().getTurnCount(1)) : 0;

        final String stadiumCardId = session.getBoard().getActiveStadium() != null
                ? session.getBoard().getActiveStadium().getCardId()
                : null;

        String winnerId = null;
        String victoryReason = null;
        String mvpCardId = null;
        Integer mvpCardDamage = null;

        if (session.getState() == MatchSessionState.FINISHED) {
            winnerId = session.getWinnerId();
            victoryReason = session.getVictoryReason();

            int maxDamage = -1;
            if (session.hasPlayerRuntimes()) {
                final var runtime = session.getPlayerRuntime(viewerIndex);
                if (runtime != null && runtime.getStatisticsTracker() != null) {
                    for (var entry : runtime.getStatisticsTracker().getPokemonDamageDealt().entrySet()) {
                        if (entry.getValue() > maxDamage) {
                            maxDamage = entry.getValue();
                            mvpCardId = entry.getKey();
                        }
                    }
                }
            }
            if (mvpCardId != null) {
                mvpCardDamage = maxDamage;
            }
        }

        return new GameStateResponseDTO(
                session.getMatchId(),
                session.getVersion(),
                turnNumber,
                (session.isAwaitingPromotion() ? session.getPromotingPlayerIndex() : session.getActivePlayerIndex()) == -1 ? -1 : ((session.isAwaitingPromotion() ? session.getPromotingPlayerIndex() : session.getActivePlayerIndex()) == viewerIndex ? 0 : 1),
                session.getState() == MatchSessionState.FINISHED ? "FINISHED" : (session.getTurnManager() != null && session.getTurnManager().currentPhase() != null ? session.getTurnManager().currentPhase().name() : session.getState().name()),
                requestDto,
                self,
                opponent,
                stadiumCardId,
                winnerId,
                victoryReason,
                mvpCardId,
                mvpCardDamage,
                session.getLastCoinFlips() != null ? session.getLastCoinFlips() : List.of(),
                viewerIndex == 0 ? session.getMmrChangeA() : session.getMmrChangeB());
    }

    private GameStateResponseDTO.PlayerView buildPlayerView(final MatchSession session, final int playerIndex) {
        final String playerId = session.getPlayerIds().get(playerIndex);
        final BattlePokemonState activePokemon = session.getBoard().getActivePokemon(playerIndex);
        final List<BattlePokemonDTO> benchDtos = session.getBoard().getBenchedPokemon(playerIndex)
                .stream()
                .map(p -> toPokemonDto(p, List.of()))
                .collect(Collectors.toList());
        final List<String> hand = session.getBoard().getHandOf(playerIndex);

        final List<String> activeConditions = (activePokemon != null && session.getPlayerRuntime(playerIndex) != null) ? session.getPlayerRuntime(playerIndex).getStatusEffectManager()
                .activeEffects().stream().map(Enum::name).toList() : List.of();

        return new GameStateResponseDTO.PlayerView(
                playerId,
                activePokemon != null ? toPokemonDto(activePokemon, activeConditions) : null,
                benchDtos,
                hand,
                session.getBoard().getDeckSize(playerIndex),
                session.getBoard().getRemainingPrizes(playerIndex));
    }

    private GameStateResponseDTO.OpponentView buildOpponentView(final MatchSession session, final int opponentIndex) {
        final String playerId = session.getPlayerIds().get(opponentIndex);
        final BattlePokemonState activePokemon = session.getBoard().getActivePokemon(opponentIndex);
        final List<BattlePokemonDTO> benchDtos = session.getBoard().getBenchedPokemon(opponentIndex)
                .stream()
                .map(p -> toPokemonDto(p, List.of()))
                .collect(Collectors.toList());
        final int handSize = session.getBoard().getHandOf(opponentIndex).size();

        final List<String> activeConditions = (activePokemon != null && session.getPlayerRuntime(opponentIndex) != null) ? session.getPlayerRuntime(opponentIndex).getStatusEffectManager()
                .activeEffects().stream().map(Enum::name).toList() : List.of();

        return new GameStateResponseDTO.OpponentView(
                playerId,
                activePokemon != null ? toPokemonDto(activePokemon, activeConditions) : null,
                benchDtos,
                handSize,
                session.getBoard().getDeckSize(opponentIndex),
                session.getBoard().getRemainingPrizes(opponentIndex));
    }

    private BattlePokemonDTO toPokemonDto(final BattlePokemonState pokemon, final List<String> statusConditions) {
        final List<AttackDTO> attackDtos = pokemon.getAttacks() == null ? List.of() :
                pokemon.getAttacks().stream()
                        .map(a -> new AttackDTO(a.name(), a.baseDamage(), a.requiredEnergies()))
                        .toList();
        final String toolCardId = pokemon.getAttachedTool()
                .map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId)
                .orElse(null);
        final List<ar.edu.utn.frc.tup.piii.dtos.AbilityDTO> abilityDtos = pokemon.getAbilities() == null ? List.of() :
                pokemon.getAbilities().stream()
                        .filter(ab -> ab.name() != null)
                        .map(ab -> new ar.edu.utn.frc.tup.piii.dtos.AbilityDTO(ab.name(), ab.text()))
                        .toList();
        return new BattlePokemonDTO(
                pokemon.getCardId(),
                pokemon.getName(),
                pokemon.getPokemonType(),
                pokemon.getMaxHp(),
                pokemon.getDamageCounters(),
                pokemon.isEx(),
                pokemon.getWeaknessType(),
                pokemon.getResistanceType(),
                pokemon.getAttachedEnergies(),
                pokemon.getRetreatCost(),
                pokemon.hasToolAttached(),
                toolCardId,
                attackDtos,
                abilityDtos,
                statusConditions);
    }
}
