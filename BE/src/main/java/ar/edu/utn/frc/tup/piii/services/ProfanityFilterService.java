package ar.edu.utn.frc.tup.piii.services;

/**
 * Service interface for filtering profanity and offensive language.
 */
public interface ProfanityFilterService {

    /**
     * Filters offensive words in a message, replacing them with asterisks.
     *
     * @param message the message to filter
     * @return the filtered message
     */
    String filter(String message);
}
