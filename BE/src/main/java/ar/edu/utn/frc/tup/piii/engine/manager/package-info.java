/**
 * State machine, rule validation, and specialized action executors for the game engine.
 *
 * <p>See {@code README.md} in this package for the full breakdown of
 * {@link ar.edu.utn.frc.tup.piii.engine.manager.TurnManager} (State pattern + Observer),
 * {@link ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator},
 * {@link ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager}, and the per-action
 * executors ({@code RetreatExecutor}, {@code EvolveExecutor}, {@code DrawPhaseExecutor}). All
 * classes here are pure Java — no Spring, no JPA (see ADR 0001).</p>
 */
package ar.edu.utn.frc.tup.piii.engine.manager;
