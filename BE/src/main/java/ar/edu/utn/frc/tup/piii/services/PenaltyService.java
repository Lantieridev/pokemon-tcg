package ar.edu.utn.frc.tup.piii.services;

import java.time.LocalDateTime;

public interface PenaltyService {
    /**
     * Checks if a user is currently penalized.
     * If they have 3 or more reports and haven't been penalized for the current count,
     * a new 5-minute penalty is applied.
     *
     * @param username the username to check
     * @return true if penalized, false otherwise
     */
    boolean isPenalized(String username);

    /**
     * Retrieves the expiration time of the current penalty.
     *
     * @param username the username
     * @return the expiration date/time, or null if not penalized
     */
    LocalDateTime getPenaltyExpiration(String username);

    /**
     * Force checks reports count and applies a penalty if eligible.
     * Useful when a new report is submitted.
     *
     * @param username the username to check
     */
    void checkAndApplyPenalty(String username);
}
