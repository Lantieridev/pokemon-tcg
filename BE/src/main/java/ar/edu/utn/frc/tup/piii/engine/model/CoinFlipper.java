package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Abstraction over a coin flip so probabilistic behavior can be injected deterministically
 * in tests. True = HEADS, false = TAILS. FR-002.
 */
@FunctionalInterface
public interface CoinFlipper {

    /**
     * Flips the coin.
     *
     * @return {@code true} for HEADS, {@code false} for TAILS
     */
    boolean flip();
}
