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

    /** True if Player 0 has placed their starting Active Pokémon. */
    private boolean player0PlacedActive = false;

    /** True if Player 1 has placed their starting Active Pokémon. */
    private boolean player1PlacedActive = false;

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

        if (this.battlefieldProvider.getActivePokemon(0) != null) {
            this.player0PlacedActive = true;
        }
        if (this.battlefieldProvider.getActivePokemon(1) != null) {
            this.player1PlacedActive = true;
        }
    }

    /**
     * For testing purposes, allows setting whether the initial active Pokémon placement is considered complete.
     *
     * @param p0Complete true if player 0 initial placement is complete
     * @param p1Complete true if player 1 initial placement is complete
     */
    public void setInitialPlacementComplete(final boolean p0Complete, final boolean p1Complete) {
        this.player0PlacedActive = p0Complete;
        this.player1PlacedActive = p1Complete;
    }

    /**
     * Called when a Pokémon is knocked out. Checks victory conditions once all
     * simultaneous knockouts in the current batch have been processed.
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
        
        // Defer victory checking if there are other pending K.O.s on the board in the same batch
        if (hasPendingKnockouts()) {
            return;
        }

        evaluateVictory();
    }

    /**
     * Evaluates all victory conditions (Prize cards and Bench-out) for both players.
     * Compares the number of conditions met by each player to declare a sole winner
     * or trigger a Sudden Death tiebreaker.
     */
    public void evaluateVictory() {
        if (victoryFired || activePlayerIndex == UNSTARTED_PLAYER_INDEX) {
            return;
        }

        if (battlefieldProvider.getActivePokemon(0) != null) {
            player0PlacedActive = true;
        }
        if (battlefieldProvider.getActivePokemon(1) != null) {
            player1PlacedActive = true;
        }

        // 1. Prize conditions
        final boolean p0WinsPrize = prizeProvider.getRemainingPrizes(0) == 0;
        final boolean p1WinsPrize = prizeProvider.getRemainingPrizes(1) == 0;

        // 2. Bench-out conditions (only if initial placement is complete)
        boolean p0WinsBench = false;
        boolean p1WinsBench = false;
        if (player0PlacedActive && player1PlacedActive) {
            final boolean p0ActiveNull = battlefieldProvider.getActivePokemon(0) == null;
            final boolean p1ActiveNull = battlefieldProvider.getActivePokemon(1) == null;

            final boolean p0BenchEmpty = benchProvider.getBenchSize(0) == 0;
            final boolean p1BenchEmpty = benchProvider.getBenchSize(1) == 0;

            p0WinsBench = p1ActiveNull && p1BenchEmpty; // Player 0 wins because Player 1 is benched out
            p1WinsBench = p0ActiveNull && p0BenchEmpty; // Player 1 wins because Player 0 is benched out
        }

        // Count conditions met
        int conditions0 = 0;
        if (p0WinsPrize) {
            conditions0++;
        }
        if (p0WinsBench) {
            conditions0++;
        }

        int conditions1 = 0;
        if (p1WinsPrize) {
            conditions1++;
        }
        if (p1WinsBench) {
            conditions1++;
        }

        if (conditions0 > 0 && conditions1 > 0) {
            if (conditions0 == conditions1) {
                fireVictory(new VictoryResult.SuddenDeath());
            } else if (conditions0 > conditions1) {
                // Player 0 met more conditions
                if (p0WinsPrize) {
                    fireVictory(new VictoryResult.PrizeVictory(0));
                } else {
                    fireVictory(new VictoryResult.BenchOutVictory(0));
                }
            } else {
                // Player 1 met more conditions
                if (p1WinsPrize) {
                    fireVictory(new VictoryResult.PrizeVictory(1));
                } else {
                    fireVictory(new VictoryResult.BenchOutVictory(1));
                }
            }
        } else if (conditions0 > 0) {
            if (p0WinsPrize) {
                fireVictory(new VictoryResult.PrizeVictory(0));
            } else {
                fireVictory(new VictoryResult.BenchOutVictory(0));
            }
        } else if (conditions1 > 0) {
            if (p1WinsPrize) {
                fireVictory(new VictoryResult.PrizeVictory(1));
            } else {
                fireVictory(new VictoryResult.BenchOutVictory(1));
            }
        }
    }

    /**
     * Evaluates field victory conditions. Delegated to evaluateVictory.
     */
    public void checkFieldVictory() {
        evaluateVictory();
    }

    private boolean hasPendingKnockouts() {
        for (int i = 0; i < PLAYER_COUNT; i++) {
            final BattlePokemonState active = battlefieldProvider.getActivePokemon(i);
            if (active != null && isKnockedOut(active)) {
                return true;
            }
            for (final BattlePokemonState benched : benchProvider.getBenchedPokemon(i)) {
                if (isKnockedOut(benched)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isKnockedOut(final BattlePokemonState state) {
        return state.getDamageCounters() * 10 >= state.getMaxHp();
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
            case ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase a -> { /* no-op */ }
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
