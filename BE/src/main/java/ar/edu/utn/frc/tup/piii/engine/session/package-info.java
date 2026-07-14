/**
 * Per-match mutable runtime state: the objects that live for the duration of one game and are
 * serialized to/from JSON for persistence and reconnection.
 *
 * <p>{@link ar.edu.utn.frc.tup.piii.engine.session.MatchSession} is the root aggregate (both
 * players' {@link ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime}, the active
 * {@code TurnManager}, match-level state). {@link ar.edu.utn.frc.tup.piii.engine.session.MatchBoard}
 * binds board-sourced and runtime-sourced Pokémon references to the same live object
 * ({@code bindRuntimes()}) so identity comparisons stay valid after a deserialization round-trip.
 * {@link ar.edu.utn.frc.tup.piii.engine.session.PlayerState} is the immutable, read-only
 * counterpart used by the {@code engine.listener} state-provider interfaces.</p>
 */
package ar.edu.utn.frc.tup.piii.engine.session;
