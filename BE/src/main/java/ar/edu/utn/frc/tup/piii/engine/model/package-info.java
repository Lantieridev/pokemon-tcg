/**
 * Card hierarchy, battle state, engine actions, and turn phases — the engine's data model.
 *
 * <p>See {@code README.md} in this package for the {@link ar.edu.utn.frc.tup.piii.engine.model.Card}
 * hierarchy, the {@code BattlePokemonState}/{@code InPlayPokemon} split, and the runtime
 * aggregates ({@code Deck}, {@code Hand}, {@code Bench}, {@code DiscardPile}).
 * {@link ar.edu.utn.frc.tup.piii.engine.model.Action} and
 * {@link ar.edu.utn.frc.tup.piii.engine.model.TurnPhase} are {@code sealed} interfaces — every
 * {@code switch} over them is compiler-exhaustive, no {@code default} branch needed.</p>
 */
package ar.edu.utn.frc.tup.piii.engine.model;
