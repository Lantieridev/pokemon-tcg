package ar.edu.utn.frc.tup.piii.dtos;

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

        return new GameStateResponseDTO(
                session.getMatchId(),
                INITIAL_VERSION,
                0,
                session.getState().name(),
                self,
                opponent);
    }

    private GameStateResponseDTO.PlayerView buildPlayerView(final MatchSession session, final int playerIndex) {
        final String playerId = session.getPlayerIds().get(playerIndex);
        final BattlePokemonState activePokemon = session.getBoard().getActivePokemon(playerIndex);
        final List<BattlePokemonDTO> benchDtos = session.getBoard().getBenchedPokemon(playerIndex)
                .stream()
                .map(this::toPokemonDto)
                .collect(Collectors.toList());
        final List<String> hand = session.getBoard().getHandOf(playerIndex);

        return new GameStateResponseDTO.PlayerView(
                playerId,
                activePokemon != null ? toPokemonDto(activePokemon) : null,
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
                .map(this::toPokemonDto)
                .collect(Collectors.toList());
        final int handSize = session.getBoard().getHandOf(opponentIndex).size();

        return new GameStateResponseDTO.OpponentView(
                playerId,
                activePokemon != null ? toPokemonDto(activePokemon) : null,
                benchDtos,
                handSize,
                session.getBoard().getDeckSize(opponentIndex),
                session.getBoard().getRemainingPrizes(opponentIndex));
    }

    private BattlePokemonDTO toPokemonDto(final BattlePokemonState pokemon) {
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
                pokemon.hasToolAttached());
    }
}
