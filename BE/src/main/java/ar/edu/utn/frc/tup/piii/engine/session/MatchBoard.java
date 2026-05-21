package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PrizeStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

import java.util.List;
import java.util.Objects;

/**
 * Runtime snapshot of the match board implementing all engine provider interfaces.
 * Pure POJO — zero Spring imports. FR-008, FR-013.
 */
public final class MatchBoard
        implements BattlefieldStateProvider,
                   BenchStateProvider,
                   DeckStateProvider,
                   PrizeStateProvider,
                   PokemonTurnInPlayProvider {

    private static final int REQUIRED_PLAYER_COUNT = 2;

    private final List<PlayerState> players;
    private String activeStadiumCardId;

    /**
     * Constructs a MatchBoard from exactly two PlayerState objects.
     *
     * @param players list of two PlayerState instances (one per player)
     * @throws NullPointerException     if players is null
     * @throws IllegalArgumentException if the list does not contain exactly two players
     */
    public MatchBoard(final List<PlayerState> players) {
        Objects.requireNonNull(players, "players must not be null");
        if (players.size() != REQUIRED_PLAYER_COUNT) {
            throw new IllegalArgumentException(
                    "MatchBoard requires exactly 2 players, got: " + players.size());
        }
        this.players = List.copyOf(players);
    }

    @Override
    public BattlePokemonState getActivePokemon(final int playerIndex) {
        return players.get(playerIndex).getActivePokemon();
    }

    @Override
    public int getBenchSize(final int playerIndex) {
        return players.get(playerIndex).getBench().size();
    }

    @Override
    public List<BattlePokemonState> getBenchedPokemon(final int playerIndex) {
        return players.get(playerIndex).getBench();
    }

    @Override
    public int getDeckSize(final int playerIndex) {
        return players.get(playerIndex).getDeckSize();
    }

    @Override
    public int getRemainingPrizes(final int playerIndex) {
        return players.get(playerIndex).getPrizeCount();
    }

    @Override
    public int getTurnsInPlay(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        for (final PlayerState player : players) {
            final int turns = player.getTurnsInPlay(pokemon);
            if (turns > 0) {
                return turns;
            }
        }
        return 0;
    }

    /**
     * Returns the hand (card ID list) for the specified player.
     *
     * @param playerIndex 0 or 1
     * @return hand list (never null)
     */
    public List<String> getHandOf(final int playerIndex) {
        return players.get(playerIndex).getHand();
    }

    /**
     * Returns the PlayerState for the specified player.
     *
     * @param playerIndex 0 or 1
     * @return PlayerState (never null)
     */
    public PlayerState getPlayerState(final int playerIndex) {
        return players.get(playerIndex);
    }

    /**
     * Returns the attacks available to the active Pokémon of the specified player.
     *
     * @param playerIndex 0 or 1
     * @return attacks list (never null; may be empty)
     */
    public List<Attack> getActiveAttacks(final int playerIndex) {
        return players.get(playerIndex).getActiveAttacks();
    }

    /**
     * Returns the card ID of the currently active Stadium, or {@code null} if none is in play.
     * RF-02d.
     */
    public String getActiveStadiumCardId() {
        return activeStadiumCardId;
    }

    /**
     * Places a new Stadium card into play, replacing the previous one.
     * The caller is responsible for discarding the returned card ID if non-null.
     * RF-02d.
     *
     * @param newCardId the card ID of the Stadium being played (must not be null)
     * @return the card ID of the previously active Stadium, or {@code null} if none was in play
     */
    public String replaceStadium(final String newCardId) {
        Objects.requireNonNull(newCardId, "newCardId must not be null");
        final String previous = activeStadiumCardId;
        activeStadiumCardId = newCardId;
        return previous;
    }
}
