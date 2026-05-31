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

    /**
     * Detects offensive words in a message and returns them.
     *
     * @param message the message to check
     * @return list of detected offensive words
     */
    java.util.List<String> getProfaneWords(String message);
}
