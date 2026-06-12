package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PrizeStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.StadiumStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;

import java.util.ArrayList;
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
                   HandStateProvider,
                   PrizeStateProvider,
                   PokemonTurnInPlayProvider,
                   StadiumStateProvider,
                   ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider {

    private static final int REQUIRED_PLAYER_COUNT = 2;

    private final List<PlayerState> players;
    private TrainerCard activeStadium;

    /**
     * Live runtime references; non-null once {@link #bindRuntimes(List)} is called.
     * When bound, ALL mutable data (active Pokémon, bench, hand, attacks, prize count,
     * deck size, turns-in-play) is read from runtimes rather than from the immutable
     * {@link PlayerState} snapshots.
     */
    private List<PlayerRuntime> boundRuntimes;

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

    /**
     * Binds live {@link PlayerRuntime} objects so that mutable game state
     * (prize count, deck size) is read from the runtimes rather than the immutable
     * {@link PlayerState} snapshots. Must be called once during match setup.
     *
     * @param runtimes list of exactly two PlayerRuntime instances (never null)
     */
    public void bindRuntimes(final List<PlayerRuntime> runtimes) {
        Objects.requireNonNull(runtimes, "runtimes must not be null");
        if (runtimes.size() != REQUIRED_PLAYER_COUNT) {
            throw new IllegalArgumentException(
                    "bindRuntimes requires exactly 2 runtimes, got: " + runtimes.size());
        }
        this.boundRuntimes = new ArrayList<>(runtimes);
    }

    @Override
    public BattlePokemonState getActivePokemon(final int playerIndex) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getActivePokemon();
        }
        return players.get(playerIndex).getActivePokemon();
    }

    @Override
    public int getBenchSize(final int playerIndex) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getBench().size();
        }
        return players.get(playerIndex).getBench().size();
    }

    @Override
    public List<BattlePokemonState> getBenchedPokemon(final int playerIndex) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getBench().getAll();
        }
        return players.get(playerIndex).getBench();
    }

    @Override
    public int getDeckSize(final int playerIndex) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getDeck().size();
        }
        return players.get(playerIndex).getDeckSize();
    }

    @Override
    public int getRemainingPrizes(final int playerIndex) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getPrizeCount();
        }
        return players.get(playerIndex).getPrizeCount();
    }

    @Override
    public int getTurnsInPlay(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        if (boundRuntimes != null) {
            for (final PlayerRuntime runtime : boundRuntimes) {
                if (runtime.hasPokemonInPlay(pokemon)) {
                    return runtime.getTurnsInPlay(pokemon);
                }
            }
            return 0;
        }
        for (final PlayerState player : players) {
            final int turns = player.getTurnsInPlay(pokemon);
            if (turns > 0) {
                return turns;
            }
        }
        return 0;
    }

    /**
     * Returns the hand as a list of card IDs for the specified player.
     * Reads from live runtimes when bound (after match setup).
     *
     * @param playerIndex 0 or 1
     * @return hand card IDs (never null)
     */
    public List<String> getHandOf(final int playerIndex) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getHand().getCards().stream()
                    .map(Card::getCardId)
                    .toList();
        }
        return players.get(playerIndex).getHand();
    }

    @Override
    public int getHandSize(final int playerIndex) {
        return getHandOf(playerIndex).size();
    }

    @Override
    public java.util.Optional<Card> getCardInHand(final int playerIndex, final String cardId) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getHand().getCards().stream()
                    .filter(c -> c.getCardId().equals(cardId))
                    .findFirst();
        }
        // The snapshot (PlayerState) doesn't have the full Card objects, only IDs.
        return java.util.Optional.empty();
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
     * Reads from the live active Pokémon when runtimes are bound (reflects post-evolution state).
     *
     * @param playerIndex 0 or 1
     * @return attacks list (never null; may be empty)
     */
    public List<Attack> getActiveAttacks(final int playerIndex) {
        if (boundRuntimes != null) {
            final BattlePokemonState active = boundRuntimes.get(playerIndex).getActivePokemon();
            if (active != null) {
                return active.getAttacks();
            }
            return List.of();
        }
        return players.get(playerIndex).getActiveAttacks();
    }

    /**
     * Returns the currently active Stadium, or {@code null} if none is in play.
     * RF-02d.
     */
    public TrainerCard getActiveStadium() {
        return activeStadium;
    }

    /**
     * Places a new Stadium card into play, replacing the previous one.
     * The caller is responsible for discarding the returned card if non-null.
     * RF-02d.
     *
     * @param newStadium the Stadium card being played (must not be null)
     * @return the previously active Stadium, or {@code null} if none was in play
     */
    public TrainerCard replaceStadium(final TrainerCard newStadium) {
        Objects.requireNonNull(newStadium, "newStadium must not be null");
        final TrainerCard previous = activeStadium;
        activeStadium = newStadium;
        return previous;
    }

    @Override
    public List<Card> getDiscardPile(final int playerIndex) {
        if (boundRuntimes != null) {
            return boundRuntimes.get(playerIndex).getDiscardPile().getCards();
        }
        return List.of();
    }
}
