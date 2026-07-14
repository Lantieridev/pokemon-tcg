/**
 * Production implementations of engine-defined seams that need a real-world source of randomness
 * or I/O, kept out of {@code engine.model} so the engine's core types stay pure/testable.
 *
 * <p>{@link ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper} is the production
 * {@link ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper}, backed by {@link java.util.Random}.
 * Tests inject deterministic flippers ({@code () -> true} / {@code () -> false}) instead — the
 * engine itself never calls {@code new Random()} directly.</p>
 */
package ar.edu.utn.frc.tup.piii.engine.infra;
