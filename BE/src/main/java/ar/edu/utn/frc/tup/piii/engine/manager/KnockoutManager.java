package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener;
import ar.edu.utn.frc.tup.piii.engine.model.AttackPhase;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;

import java.util.Objects;

/**
 * Detects and reports Pokémon knockouts after relevant phase transitions.
 *
 * <p>Invariant: The external coordinator MUST call StatusEffectManager.processBetweenTurns()
 * BEFORE TurnManager.endBetweenTurns(). This class relies on
 * {@code PhaseExited(BetweenTurnsPhase)} firing AFTER status damage has been applied.</p>
 *
 * <p>FR-008, FR-009, FR-010, FR-011.</p>
 */
public final class KnockoutManager implements PhaseListener {

    /** Damage points represented by each damage counter. */
    private static final int DAMAGE_PER_COUNTER = 10;

    /** Number of prizes for knocking out a standard Pokémon. */
    private static final int STANDARD_PRIZES = 1;

    /** Number of prizes for knocking out an EX Pokémon. */
    private static final int EX_PRIZES = 2;

    /** Number of players on the battlefield. */
    private static final int PLAYER_COUNT = 2;

    private final BattlefieldStateProvider provider;
    private final BenchStateProvider benchProvider;
    private final KnockoutHandler handler;

    /**
     * Creates a new KnockoutManager.
     *
     * @param provider      source of active Pokémon states per player (must not be null)
     * @param benchProvider source of benched Pokémon states per player (must not be null)
     * @param handler       callback invoked when a knockout is detected (must not be null)
     * @throws NullPointerException if any argument is null
     */
    public KnockoutManager(final BattlefieldStateProvider provider,
                           final BenchStateProvider benchProvider,
                           final KnockoutHandler handler) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.benchProvider = Objects.requireNonNull(benchProvider, "benchProvider must not be null");
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    /**
     * Receives a phase event and triggers knockout checks after AttackPhase or BetweenTurnsPhase.
     *
     * @param event the event fired by the TurnManager
     */
    @Override
    public void on(final PhaseEvent event) {
        switch (event) {
            case PhaseEvent.PhaseExited e -> handlePhaseExited(e);
            case PhaseEvent.PhaseEntered e -> { /* no-op */ }
            case PhaseEvent.TurnStarted e -> { /* no-op */ }
            case PhaseEvent.TurnEnded e -> { /* no-op */ }
        }
    }

    /**
     * Handles a PhaseExited event; triggers knockout check only for relevant phases.
     *
     * @param event the PhaseExited event
     */
    private void handlePhaseExited(final PhaseEvent.PhaseExited event) {
        switch (event.phase()) {
            case AttackPhase a -> checkBothPlayers();
            case BetweenTurnsPhase b -> checkBothPlayers();
            case DrawPhase d -> { /* no-op */ }
            case MainPhase m -> { /* no-op */ }
        }
    }

    /**
     * Checks both players' active and benched Pokémon for a knockout condition.
     */
    private void checkBothPlayers() {
        for (int i = 0; i < PLAYER_COUNT; i++) {
            BattlePokemonState active = provider.getActivePokemon(i);
            if (active != null && isKnockedOut(active)) {
                handler.onKnockout(active, prizesFor(active));
            }
            for (BattlePokemonState benched : benchProvider.getBenchedPokemon(i)) {
                if (isKnockedOut(benched)) {
                    handler.onKnockout(benched, prizesFor(benched));
                }
            }
        }
    }

    /**
     * Returns {@code true} if the Pokémon's accumulated damage meets or exceeds its max HP.
     *
     * @param state the Pokémon state to evaluate
     * @return true if knocked out
     */
    private boolean isKnockedOut(final BattlePokemonState state) {
        return state.getDamageCounters() * DAMAGE_PER_COUNTER >= state.getMaxHp();
    }

    /**
     * Returns the number of prizes the opponent takes for knocking out this Pokémon.
     *
     * @param state the knocked-out Pokémon
     * @return 2 if EX, 1 otherwise
     */
    private int prizesFor(final BattlePokemonState state) {
        return state.isEx() ? EX_PRIZES : STANDARD_PRIZES;
    }
}
