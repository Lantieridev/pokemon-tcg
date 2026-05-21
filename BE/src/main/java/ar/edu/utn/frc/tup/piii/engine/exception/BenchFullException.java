package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when a Pokémon placement is attempted on a bench that already holds 5 Pokémon.
 * Per XY1 rules §1.2: a player's bench can hold a maximum of 5 Pokémon.
 */
public class BenchFullException extends RuntimeException {

    public BenchFullException() {
        super("Bench is full: maximum of 5 Pokémon allowed");
    }
}
