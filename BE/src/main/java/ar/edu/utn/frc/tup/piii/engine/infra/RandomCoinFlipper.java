package ar.edu.utn.frc.tup.piii.engine.infra;

import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;

import java.util.Random;

/**
 * Production CoinFlipper backed by {@link java.util.Random}.
 * Accepts an injected Random for deterministic testing. FR-002, FR-018.
 */
public class RandomCoinFlipper implements CoinFlipper {

    private final Random random;

    /**
     * Constructs a RandomCoinFlipper using the given Random instance.
     * Use this constructor in tests with a seeded Random for determinism.
     *
     * @param random the Random to use for coin flips
     */
    public RandomCoinFlipper(final Random random) {
        this.random = random;
    }

    /**
     * Constructs a RandomCoinFlipper with a default (non-deterministic) Random.
     */
    public RandomCoinFlipper() {
        this(new Random());
    }

    @Override
    public boolean flip() {
        return random.nextBoolean();
    }
}
