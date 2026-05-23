package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Spring configuration for match engine collaborators and scheduling.
 */
@Configuration
public class MatchEngineConfig {

    private static final int ABANDONMENT_POOL_SIZE = 4;

    /**
     * Provides a shared {@link RuleValidator} bean backed by default no-op providers.
     * Per-session validators are injected via MatchService in a future phase.
     *
     * @return a configured RuleValidator instance
     */
    @Bean
    public RuleValidator ruleValidator() {
        final TurnManager turnManager = new TurnManager();
        final StatusEffectManager statusEffectManager = new StatusEffectManager(new RandomCoinFlipper());
        final BenchStateProvider noBench = new BenchStateProvider() {
            @Override
            public int getBenchSize(final int playerIndex) {
                return 0;
            }

            @Override
            public List<BattlePokemonState> getBenchedPokemon(final int playerIndex) {
                return List.of();
            }
        };
        final HandStateProvider noHand = (playerIndex, cardId) -> null;
        return new RuleValidator(turnManager, statusEffectManager, pokemon -> 0, noBench, noHand);
    }

    /**
     * Provides the scheduled executor used to fire abandonment timers.
     *
     * @return a thread-pool based scheduled executor
     */
    @Bean
    public ScheduledExecutorService abandonmentScheduler() {
        return Executors.newScheduledThreadPool(ABANDONMENT_POOL_SIZE);
    }
}
