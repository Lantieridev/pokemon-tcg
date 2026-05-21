package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener;
import ar.edu.utn.frc.tup.piii.engine.listener.VictoryHandler;
import ar.edu.utn.frc.tup.piii.engine.model.AttackPhase;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

import java.util.List;
import java.util.Objects;

/**
 * Handles the automatic card draw at the start of each turn's DrawPhase.
 *
 * <p>Rules (XY1 §2):
 * <ul>
 *   <li>Starting player skips their draw on turn 1.</li>
 *   <li>If the drawing player's deck is empty, the opponent wins by deck-out.</li>
 *   <li>Otherwise, the top card of the player's deck is moved to their hand.</li>
 * </ul>
 * </p>
 *
 * <p>Pure POJO — zero Spring imports. FR-001.</p>
 */
public final class DrawPhaseExecutor implements PhaseListener {

    private final List<PlayerRuntime> playerRuntimes;
    private final TurnManager turnManager;
    private final VictoryHandler victoryHandler;

    /**
     * @param playerRuntimes live runtime state for both players (never null, size must be 2)
     * @param turnManager    used to query starting player and first-turn status (never null)
     * @param victoryHandler called when a deck-out occurs (never null)
     */
    public DrawPhaseExecutor(final List<PlayerRuntime> playerRuntimes,
                              final TurnManager turnManager,
                              final VictoryHandler victoryHandler) {
        this.playerRuntimes = Objects.requireNonNull(playerRuntimes, "playerRuntimes must not be null");
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager must not be null");
        this.victoryHandler = Objects.requireNonNull(victoryHandler, "victoryHandler must not be null");
    }

    @Override
    public void on(final PhaseEvent event) {
        switch (event) {
            case PhaseEvent.PhaseEntered p -> handlePhaseEntered(p);
            case PhaseEvent.TurnStarted s  -> { /* no-op */ }
            case PhaseEvent.PhaseExited e  -> { /* no-op */ }
            case PhaseEvent.TurnEnded e    -> { /* no-op */ }
        }
    }

    private void handlePhaseEntered(final PhaseEvent.PhaseEntered event) {
        switch (event.phase()) {
            case DrawPhase d         -> executeDraw(event.playerIndex());
            case MainPhase m         -> { /* no-op */ }
            case AttackPhase a       -> { /* no-op */ }
            case BetweenTurnsPhase b -> { /* no-op */ }
        }
    }

    private void executeDraw(final int playerIndex) {
        final boolean isStartingPlayer = turnManager.getStartingPlayerIndex() == playerIndex;
        final boolean isFirstTurn = turnManager.isFirstTurnOfPlayer(playerIndex);

        if (isStartingPlayer && isFirstTurn) {
            return;
        }

        final PlayerRuntime runtime = playerRuntimes.get(playerIndex);

        if (runtime.getDeck().isEmpty()) {
            final int opponentIndex = 1 - playerIndex;
            victoryHandler.onVictory(new VictoryResult.DeckOutVictory(opponentIndex));
            return;
        }

        runtime.getHand().addCard(runtime.getDeck().draw());
    }
}
