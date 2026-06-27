package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.services.deck.InvalidDeckException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Maps domain exceptions to HTTP status codes.
 * Centralises error handling so individual controllers stay clean.
 */
@RestControllerAdvice
public final class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(final NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(final IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidActionException.class)
    public ResponseEntity<String> handleInvalidAction(final InvalidActionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidDeckException.class)
    public ResponseEntity<String> handleInvalidDeck(final InvalidDeckException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
