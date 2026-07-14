/**
 * Observer-pattern contracts the engine publishes to, and read-only state providers it consumes
 * through, without depending on any concrete session/persistence type.
 *
 * <p>{@link ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener} receives
 * {@link ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent} notifications from
 * {@code TurnManager} (see {@code engine.manager}) — this is the engine's Observer implementation.
 * {@link ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler} and
 * {@link ar.edu.utn.frc.tup.piii.engine.listener.VictoryHandler} are the callback seams the
 * attack pipeline and victory checker invoke into. The {@code *StateProvider} interfaces
 * (bench, deck, hand, discard pile, prize, stadium, battlefield) expose read-only views of
 * {@code PlayerRuntime}/{@code PlayerState} to code that must not mutate them.</p>
 */
package ar.edu.utn.frc.tup.piii.engine.listener;
