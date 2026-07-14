package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

import java.util.List;
import java.util.Objects;

/**
 * Increments the turns-in-play counter for each Pokémon at the end of every full turn.
 *
 * <p>Listens for {@link PhaseEvent.TurnEnded} events fired by {@link TurnManager}
 * and calls {@link PlayerRuntime#incrementAllTurnsInPlay()} on the runtime of the
 * player whose turn just ended. This enforces the XY1 evolution restriction:
 * a Pokémon must have been in play for at least 1 full turn before it can evolve
 * (Rulebook §2, FR-010).</p>
 *
 * <p>Pure POJO — zero Spring imports.</p>
 */
public final class TurnInPlayTracker implements PhaseListener {

    private final List<PlayerRuntime> playerRuntimes;

    /**
     * Constructs a TurnInPlayTracker.
     *
     * @param playerRuntimes live runtime state for both players (never null, size must be 2)
     */
    public TurnInPlayTracker(final List<PlayerRuntime> playerRuntimes) {
        this.playerRuntimes = Objects.requireNonNull(playerRuntimes, "playerRuntimes must not be null");
    }

    /**
     * Handles phase events. Only acts on {@link PhaseEvent.TurnEnded} to increment counters.
     *
     * @param event the phase event fired by TurnManager
     */
    @Override
    public void on(final PhaseEvent event) {
        switch (event) {
            case PhaseEvent.TurnEnded e -> {
                final PlayerRuntime runtime = playerRuntimes.get(e.playerIndex());
                runtime.incrementAllTurnsInPlay();
                runtime.resetAllAbilitiesUsedThisTurn();
            }
            case PhaseEvent.TurnStarted e -> { /* no-op */ }
            case PhaseEvent.PhaseEntered e -> { /* no-op */ }
            case PhaseEvent.PhaseExited e  -> { /* no-op */ }
        }
    }
}
