package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.AttackDTO;
import ar.edu.utn.frc.tup.piii.dtos.BattlePokemonDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PendingSelectionRequestDTO;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
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
            if (session.getActivePlayerIndex() == viewerIndex) {
                final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = session.getPlayerRuntime(viewerIndex);
                if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK) {
                    options = runtime.getDeck().getCards().stream().map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
                } else if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE) {
                    options = runtime.getDiscardPile().getCards().stream().map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
                } else if (req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK) {
                    options = runtime.getDeck().getCards().stream().limit(7).map(ar.edu.utn.frc.tup.piii.engine.model.Card::getCardId).toList();
                }
            }
            requestDto = new PendingSelectionRequestDTO(req.sourceEffect(), req.target() != null ? req.target().getCardId() : null, req.maxSelections(), req.source(), options);
        }

        return new GameStateResponseDTO(
                session.getMatchId(),
                INITIAL_VERSION,
                session.getActivePlayerIndex(),
                session.getTurnManager() != null && session.getTurnManager().currentPhase() != null ? session.getTurnManager().currentPhase().name() : session.getState().name(),
                requestDto,
                self,
                opponent);
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
                attackDtos,
                statusConditions);
    }
}
