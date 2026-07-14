/**
 * Strategy implementations of {@link ar.edu.utn.frc.tup.piii.engine.statuseffect.StatusEffect},
 * one per XY1 status condition: {@code AsleepEffect}, {@code ParalyzedEffect},
 * {@code ConfusedEffect}, {@code BurnedEffect}, {@code PoisonedEffect}, and
 * {@link ar.edu.utn.frc.tup.piii.engine.statuseffect.PrecisionBajaEffect} (Precisión Baja —
 * Smokescreen/Sand-Attack; does not block attack or retreat, fails the attack on a coin-flip
 * tail with no self-damage, unlike Confusion).
 *
 * <p>Dormido/Confundido/Paralizado are mutually exclusive (the most recent one replaces the
 * previous); Quemado, Envenenado, and Precisión Baja can coexist with any of them. See
 * {@code StatusEffectManager} in {@code engine.manager} for the between-turns processing order
 * and {@code docs/SKILLS/game-rules-reference.md} for the full rule text.</p>
 */
package ar.edu.utn.frc.tup.piii.engine.statuseffect;
