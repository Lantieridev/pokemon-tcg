/**
 * Domain exceptions thrown by rule validation and action execution in the game engine.
 *
 * <p>Each exception maps to one illegal-move condition from the XY1 rulebook (e.g. attacking
 * while {@link ar.edu.utn.frc.tup.piii.engine.exception.PokemonAsleepException asleep}, retreating
 * with an {@link ar.edu.utn.frc.tup.piii.engine.exception.IllegalMatchStateTransitionException
 * illegal turn-phase transition}). They are unchecked and carry no framework dependency — the
 * engine stays pure Java, and the translation to an HTTP/WebSocket error response happens in the
 * service layer, not here.</p>
 */
package ar.edu.utn.frc.tup.piii.engine.exception;
