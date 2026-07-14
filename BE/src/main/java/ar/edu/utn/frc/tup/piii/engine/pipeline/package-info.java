/**
 * Attack resolution — a Chain of Responsibility of 9 {@code AttackPipelineStep}s over a shared
 * {@code AttackContext}, plus the effect resolvers each step delegates to.
 *
 * <p>See {@code README.md} in this package for the full step order (validation, confusion,
 * tool/stadium modifiers, cancellation, damage calculation/application, post-damage effects,
 * knockout check) and the {@code AttackEffectResolver}/{@code TrainerEffectResolver}/
 * {@code AbilityEffectResolver} Strategy maps that avoid switch/instanceof chains per effect.</p>
 */
package ar.edu.utn.frc.tup.piii.engine.pipeline;
