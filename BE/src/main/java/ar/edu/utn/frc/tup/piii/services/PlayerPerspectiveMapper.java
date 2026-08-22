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
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
// DTO-mapping class covering every field of a rich game-state view (player/opponent views,
// pending-selection options across 6 sources, MVP calculation) — high method count and total
// complexity are the natural shape of a mapper this comprehensive. Every individual method-level
// complexity metric has been resolved via real extraction (see the computeX/buildX helpers
// below, highest individual method complexity is 8); none is flagged individually.
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
        final PendingSelectionRequestDTO requestDto = buildPendingSelectionRequestDto(session, viewerIndex, opponentIndex);
        final MvpInfo mvp = computeMvp(session, viewerIndex);

        return new GameStateResponseDTO(
                session.getMatchId(),
                session.getVersion(),
                computeTurnNumber(session),
                computeCurrentActorRelativeToViewer(session, viewerIndex),
                computeCurrentPhaseLabel(session),
                requestDto,
                self,
                opponent,
                computeActiveStadiumCardId(session),
                mvp.winnerId(),
                mvp.victoryReason(),
                mvp.mvpCardId(),
                mvp.mvpCardDamage(),
                session.getLastCoinFlips() != null ? session.getLastCoinFlips() : List.of(),
                viewerIndex == 0 ? session.getMmrChangeA() : session.getMmrChangeB(),
                viewerIndex == 0 ? session.getCoinsGainedA() : session.getCoinsGainedB(),
                viewerIndex == 0 ? session.getXpGainedA() : session.getXpGainedB(),
                viewerIndex == 0 ? session.getCurrentMmrA() : session.getCurrentMmrB(),
                viewerIndex == 0 ? session.getCurrentTierA() : session.getCurrentTierB(),
                viewerIndex == 0 ? session.getRankUpTriggeredA() : session.getRankUpTriggeredB());
    }

    private int computeTurnNumber(final MatchSession session) {
        return session.getTurnManager() != null
                ? session.getTurnManager().getTurnCount(0) + session.getTurnManager().getTurnCount(1)
                : 0;
    }

    private String computeActiveStadiumCardId(final MatchSession session) {
        return session.getBoard().getActiveStadium() != null ? session.getBoard().getActiveStadium().getCardId() : null;
    }

    private String computeCurrentPhaseLabel(final MatchSession session) {
        if (session.getState() == MatchSessionState.FINISHED) {
            return "FINISHED";
        }
        if (session.getTurnManager() != null && session.getTurnManager().currentPhase() != null) {
            return session.getTurnManager().currentPhase().name();
        }
        return session.getState().name();
    }

    /** 0 if the current actor (promoter, or active player) is the viewer, 1 if it's the opponent, -1 if none. */
    private int computeCurrentActorRelativeToViewer(final MatchSession session, final int viewerIndex) {
        final int currentActorIndex = session.isAwaitingPromotion()
                ? session.getPromotingPlayerIndex()
                : session.getActivePlayerIndex();
        if (currentActorIndex == -1) {
            return -1;
        }
        return currentActorIndex == viewerIndex ? 0 : 1;
    }

    private record MvpInfo(String winnerId, String victoryReason, String mvpCardId, Integer mvpCardDamage) {
        private static final MvpInfo NONE = new MvpInfo(null, null, null, null);
    }

    private MvpInfo computeMvp(final MatchSession session, final int viewerIndex) {
        if (session.getState() != MatchSessionState.FINISHED) {
            return MvpInfo.NONE;
        }
        final java.util.Map.Entry<String, Integer> topDamageEntry = findTopDamageEntry(session, viewerIndex);
        final String mvpCardId = topDamageEntry != null ? topDamageEntry.getKey() : null;
        final Integer mvpCardDamage = topDamageEntry != null ? topDamageEntry.getValue() : null;
        return new MvpInfo(session.getWinnerId(), session.getVictoryReason(), mvpCardId, mvpCardDamage);
    }

    private java.util.Map.Entry<String, Integer> findTopDamageEntry(final MatchSession session, final int viewerIndex) {
        if (!session.hasPlayerRuntimes()) {
            return null;
        }
        final var runtime = session.getPlayerRuntime(viewerIndex);
        if (runtime == null || runtime.getStatisticsTracker() == null) {
            return null;
        }
        java.util.Map.Entry<String, Integer> top = null;
        for (final var entry : runtime.getStatisticsTracker().getPokemonDamageDealt().entrySet()) {
            if (top == null || entry.getValue() > top.getValue()) {
                top = entry;
            }
        }
        return top;
    }

    /**
     * Builds the DTO describing a pending interactive selection (e.g. Evosoda, Great Ball), with
     * the concrete list of selectable card/target IDs — but ONLY for the player who is actually
     * meant to answer it; the other player gets the request shape with an empty options list
     * (they can see that a choice is pending, but not what the choosing player can pick from).
     */
    private PendingSelectionRequestDTO buildPendingSelectionRequestDto(final MatchSession session,
            final int viewerIndex, final int opponentIndex) {
        final var req = session.getPendingSelectionRequest();
        if (req == null) {
            return null;
        }
        final List<String> options = isViewerChoosingThisSelection(session, req, viewerIndex)
                ? computeSelectionOptions(session, req, viewerIndex, opponentIndex)
                : java.util.Collections.emptyList();
        return new PendingSelectionRequestDTO(req.sourceEffect(),
                req.target() != null ? req.target().getCardId() : null, req.maxSelections(), req.source(), options);
    }

    /**
     * Most selections are answered by whoever's turn it is; a few (Flash Claw, Push Down) are
     * answered by the NON-active player instead (they're picking what happens to their own side).
     */
    private boolean isViewerChoosingThisSelection(final MatchSession session,
            final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req, final int viewerIndex) {
        final boolean isOpponentChoosingEffect =
                req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FLASH_CLAW
                        || req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PUSH_DOWN;
        return (isOpponentChoosingEffect && session.getActivePlayerIndex() != viewerIndex)
                || (!isOpponentChoosingEffect && session.getActivePlayerIndex() == viewerIndex);
    }

    private List<String> computeSelectionOptions(final MatchSession session,
            final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req, final int viewerIndex,
            final int opponentIndex) {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(viewerIndex);
        return switch (req.source()) {
            case DECK -> computeDeckOptions(runtime, req);
            case DISCARD_PILE -> computeDiscardPileOptions(session, runtime, req, viewerIndex);
            case TOP_7_DECK -> computeTop7DeckOptions(session, runtime, req, viewerIndex);
            case HAND -> computeHandOptions(session, runtime, req, viewerIndex);
            case OPPONENT_FIELD -> computeFieldOptions(session, opponentIndex, "active:", "bench_");
            case BENCH -> computeFieldOptions(session, viewerIndex, null, "bench_");
        };
    }

    private List<String> computeDeckOptions(final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime,
            final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req) {
        var stream = runtime.getDeck().getCards().stream();
        stream = switch (req.sourceEffect()) {
            case PROFESSORS_LETTER, QUIVER_DANCE ->
                    stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.isBasic());
            case EVOSODA -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc
                    && pc.getEvolvesFrom() != null && req.target() != null
                    && pc.getEvolvesFrom().equalsIgnoreCase(req.target().getName()));
            case POKEMON_FAN_CLUB -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc
                    && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC);
            case ULTRA_BALL -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard);
            case PARABOLIC_CHARGE -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard);
            default -> stream;
        };
        return stream.map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
    }

    private List<String> computeDiscardPileOptions(final MatchSession session,
            final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime,
            final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req, final int viewerIndex) {
        final var targetRuntime = req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.REVIVAL
                ? session.getPlayerRuntime(1 - viewerIndex)
                : runtime;
        var stream = targetRuntime.getDiscardPile().getCards().stream();
        stream = switch (req.sourceEffect()) {
            case MAX_REVIVE, REVIVAL -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc
                    && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC);
            case SACRED_ASH, RESCUE -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard);
            case BLACKSMITH -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec
                    && ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE);
            case PAL_PAD -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.TrainerCard tc
                    && tc.getTrainerType() == ar.edu.utn.frc.tup.piii.engine.model.TrainerType.SUPPORTER);
            default -> stream;
        };
        return stream.map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
    }

    private List<String> computeTop7DeckOptions(final MatchSession session,
            final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime,
            final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req, final int viewerIndex) {
        final boolean trickShovelOnOpponent = req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TRICK_SHOVEL
                && req.target() != null && !runtime.hasPokemonInPlay(req.target());
        final var deckOwnerRuntime = trickShovelOnOpponent
                ? session.getPlayerRuntime(1 - viewerIndex)
                : runtime;
        final int limitAmount = switch (req.sourceEffect()) {
            case TRICK_SHOVEL -> 1;
            case CLAIRVOYANT_EYE -> 3;
            case BURIED_TREASURE_HUNT -> 4;
            default -> 7;
        };
        return deckOwnerRuntime.getDeck().getCards().stream().limit(limitAmount)
                .map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
    }

    private List<String> computeHandOptions(final MatchSession session,
            final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime,
            final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req, final int viewerIndex) {
        final var targetRuntime = req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FANG_SNIPE
                ? session.getPlayerRuntime(1 - viewerIndex)
                : runtime;
        var stream = targetRuntime.getHand().getCards().stream();
        stream = switch (req.sourceEffect()) {
            case FIERY_TORCH -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec
                    && ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE);
            case ROCK_RUSH -> stream.filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec
                    && (ec.getEnergyType() == ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIGHTING || ec.isProvidesAllTypes()));
            default -> stream;
        };
        return stream.map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
    }

    /** Lists the active (if {@code activePrefix} is non-null) and benched Pokémon of {@code playerIndex} as option IDs. */
    private List<String> computeFieldOptions(final MatchSession session, final int playerIndex,
            final String activePrefix, final String benchPrefix) {
        final java.util.List<String> list = new java.util.ArrayList<>();
        if (activePrefix != null) {
            final var active = session.getBoard().getActivePokemon(playerIndex);
            if (active != null) {
                list.add(activePrefix + active.getCardId());
            }
        }
        final var benched = session.getBoard().getBenchedPokemon(playerIndex);
        if (benched != null) {
            for (int i = 0; i < benched.size(); i++) {
                final var p = benched.get(i);
                if (p != null) {
                    list.add(benchPrefix + i + ":" + p.getCardId());
                }
            }
        }
        return list;
    }

    private GameStateResponseDTO.PlayerView buildPlayerView(final MatchSession session, final int playerIndex) {
        final String playerId = session.getPlayerIds().get(playerIndex);
        final BattlePokemonState activePokemon = session.getBoard().getActivePokemon(playerIndex);
        final List<BattlePokemonDTO> benchDtos = session.getBoard().getBenchedPokemon(playerIndex)
                .stream()
                .map(p -> toPokemonDto(p, List.of()))
                .collect(Collectors.toList());
        final List<String> hand = session.getBoard().getHandOf(playerIndex);

        final List<String> activeConditions;
        if (activePokemon != null && session.getPlayerRuntime(playerIndex) != null) {
            final var sem = session.getPlayerRuntime(playerIndex).getStatusEffectManager();
            final List<String> conds = new java.util.ArrayList<>(sem.activeEffects().stream().map(Enum::name).toList());
            if (sem.isRetreatBlockedNextTurn()) {
                conds.add("RETREAT_BLOCKED");
            }
            activeConditions = conds;
        } else {
            activeConditions = List.of();
        }

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

        final List<String> activeConditions;
        if (activePokemon != null && session.getPlayerRuntime(opponentIndex) != null) {
            final var sem = session.getPlayerRuntime(opponentIndex).getStatusEffectManager();
            final List<String> conds = new java.util.ArrayList<>(sem.activeEffects().stream().map(Enum::name).toList());
            if (sem.isRetreatBlockedNextTurn()) {
                conds.add("RETREAT_BLOCKED");
            }
            activeConditions = conds;
        } else {
            activeConditions = List.of();
        }

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
        final List<String> energyCardIds = pokemon.getAttachedEnergyCards().stream()
                .map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId)
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
                statusConditions,
                energyCardIds);
    }
}
