package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;

/**
 * Callback invoked when a victory condition is met. Exactly one invocation per game.
 * FR-008.
 */
@FunctionalInterface
public interface VictoryHandler {

    /**
     * Called when a victory condition is confirmed.
     *
     * @param result the non-null victory result
     */
    void onVictory(VictoryResult result);
}
