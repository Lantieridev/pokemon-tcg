package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Sealed result type describing the outcome of a victory condition check. FR-006.
 *
 * <p>Only one non-{@link NoVictory} result will be produced per game instance;
 * the victory latch in VictoryConditionChecker ensures exactly-once delivery.</p>
 */
public sealed interface VictoryResult
        permits VictoryResult.NoVictory, VictoryResult.PrizeVictory,
                VictoryResult.BenchOutVictory, VictoryResult.DeckOutVictory {

    /**
     * No victory condition has been met yet.
     */
    record NoVictory() implements VictoryResult {
    }

    /**
     * A player won by taking their last prize card.
     *
     * @param winnerPlayerIndex zero-based index of the winning player
     */
    record PrizeVictory(int winnerPlayerIndex) implements VictoryResult {
    }

    /**
     * A player won because their opponent has no Pokémon on the bench after a knockout.
     *
     * @param winnerPlayerIndex zero-based index of the winning player
     */
    record BenchOutVictory(int winnerPlayerIndex) implements VictoryResult {
    }

    /**
     * A player won because their opponent cannot draw a card at the start of their turn.
     *
     * @param winnerPlayerIndex zero-based index of the winning player
     */
    record DeckOutVictory(int winnerPlayerIndex) implements VictoryResult {
    }
}
