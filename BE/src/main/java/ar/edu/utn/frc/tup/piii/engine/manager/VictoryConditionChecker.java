package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener;
import ar.edu.utn.frc.tup.piii.engine.listener.PrizeStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.VictoryHandler;
import ar.edu.utn.frc.tup.piii.engine.model.AttackPhase;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;

import java.util.Objects;

/**
 * Evaluates victory conditions after knockout events and draw-phase transitions.
 *
 * <p>Implements both {@link KnockoutHandler} and {@link PhaseListener} so it can be
 * registered with KnockoutManager and TurnManager respectively.</p>
 *
 * <p>Victory conditions checked (in priority order on KO):</p>
 * <ol>
 *   <li>Prize victory — active player took their last prize card (FR-017)</li>
 *   <li>Bench-out victory — defender has no Pokémon left on bench after KO (FR-018)</li>
 *   <li>Deck-out victory — drawing player has an empty deck at DrawPhase start (FR-019)</li>
 * </ol>
 *
 * <p>A one-shot latch prevents duplicate {@link VictoryHandler} invocations. FR-016.</p>
 */
public final class VictoryConditionChecker implements KnockoutHandler, PhaseListener {

    /** Total number of players in a two-player game. */
    private static final int PLAYER_COUNT = 2;

    /** Sentinel value indicating no TurnStarted event has been received yet. */
    private static final int UNSTARTED_PLAYER_INDEX = -1;

    private final PrizeStateProvider prizeProvider;
    private final DeckStateProvider deckProvider;
    private final BenchStateProvider benchProvider;
    @SuppressWarnings("unused")
    private final BattlefieldStateProvider battlefieldProvider;
    private final VictoryHandler victoryHandler;

    /** Zero-based index of the currently active player; -1 until first TurnStarted. */
    private int activePlayerIndex = UNSTARTED_PLAYER_INDEX;

    /** True once a VictoryResult has been delivered — prevents double-firing. */
    private boolean victoryFired = false;

    /**
     * Creates a new VictoryConditionChecker.
     *
     * @param prizeProvider       source of remaining prize counts per player (must not be null)
     * @param deckProvider        source of deck sizes per player (must not be null)
     * @param benchProvider       source of bench sizes per player (must not be null)
     * @param battlefieldProvider source of active Pokémon states per player (must not be null)
     * @param victoryHandler      callback to invoke when a victory condition is confirmed (must not be null)
     * @throws NullPointerException if any argument is null
     */
    public VictoryConditionChecker(final PrizeStateProvider prizeProvider,
                                   final DeckStateProvider deckProvider,
                                   final BenchStateProvider benchProvider,
                                   final BattlefieldStateProvider battlefieldProvider,
                                   final VictoryHandler victoryHandler) {
        this.prizeProvider = Objects.requireNonNull(prizeProvider, "prizeProvider must not be null");
        this.deckProvider = Objects.requireNonNull(deckProvider, "deckProvider must not be null");
        this.benchProvider = Objects.requireNonNull(benchProvider, "benchProvider must not be null");
        this.battlefieldProvider = Objects.requireNonNull(battlefieldProvider,
                "battlefieldProvider must not be null");
        this.victoryHandler = Objects.requireNonNull(victoryHandler, "victoryHandler must not be null");
    }

    /**
     * Called when a Pokémon is knocked out. Checks prize victory, then bench-out victory.
     * Skipped entirely if no TurnStarted event has been received yet.
     *
     * @param knocked      the Pokémon that was knocked out
     * @param prizesToTake number of prize cards the opponent should take
     */
    @Override
    public void onKnockout(final BattlePokemonState knocked, final int prizesToTake) {
        if (victoryFired || activePlayerIndex == UNSTARTED_PLAYER_INDEX) {
            return;
        }
        int attacker = activePlayerIndex;
        int defender = PLAYER_COUNT - 1 - attacker;

        boolean attackerWins = prizeProvider.getRemainingPrizes(attacker) == 0;
        boolean defenderWins = prizeProvider.getRemainingPrizes(defender) == 0;

        if (attackerWins && defenderWins) {
            fireVictory(new VictoryResult.SuddenDeath());
            return;
        }
        if (attackerWins) {
            fireVictory(new VictoryResult.PrizeVictory(attacker));
            return;
        }
        if (defenderWins) {
            fireVictory(new VictoryResult.PrizeVictory(defender));
            return;
        }
        boolean defenderBenchEmpty = benchProvider.getBenchSize(defender) == 0;
        boolean attackerBenchEmpty = benchProvider.getBenchSize(attacker) == 0;

        if (defenderBenchEmpty && attackerBenchEmpty) {
            fireVictory(new VictoryResult.SuddenDeath());
        } else if (defenderBenchEmpty) {
            fireVictory(new VictoryResult.BenchOutVictory(attacker));
        } else if (attackerBenchEmpty) {
            fireVictory(new VictoryResult.BenchOutVictory(defender));
        }
    }

    /**
     * Receives a phase event. Updates the active player on TurnStarted;
     * checks deck-out on PhaseEntered(DrawPhase).
     *
     * @param event the event fired by TurnManager
     */
    @Override
    public void on(final PhaseEvent event) {
        switch (event) {
            case PhaseEvent.TurnStarted s  -> activePlayerIndex = s.playerIndex();
            case PhaseEvent.PhaseEntered p -> handlePhaseEntered(p);
            case PhaseEvent.PhaseExited e  -> { /* no-op */ }
            case PhaseEvent.TurnEnded e    -> { /* no-op */ }
        }
    }

    /**
     * Handles a PhaseEntered event; checks deck-out only when entering DrawPhase.
     *
     * @param event the PhaseEntered event
     */
    private void handlePhaseEntered(final PhaseEvent.PhaseEntered event) {
        switch (event.phase()) {
            case DrawPhase d        -> checkDeckOut(event.playerIndex());
            case MainPhase m        -> { /* no-op */ }
            case AttackPhase a      -> { /* no-op */ }
            case BetweenTurnsPhase b -> { /* no-op */ }
        }
    }

    /**
     * Fires a DeckOutVictory for the opponent if the drawing player's deck is empty.
     *
     * @param drawingPlayerIndex zero-based index of the player attempting to draw
     */
    private void checkDeckOut(final int drawingPlayerIndex) {
        if (victoryFired) {
            return;
        }
        if (deckProvider.getDeckSize(drawingPlayerIndex) == 0) {
            fireVictory(new VictoryResult.DeckOutVictory(PLAYER_COUNT - 1 - drawingPlayerIndex));
        }
    }

    /**
     * Delivers the victory result through the handler and sets the latch.
     *
     * @param result the non-null victory result to deliver
     */
    private void fireVictory(final VictoryResult result) {
        victoryFired = true;
        victoryHandler.onVictory(result);
    }
}
