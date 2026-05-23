package ar.edu.utn.frc.tup.piii.services;

import java.time.LocalDateTime;
import java.util.List;

public interface PenaltyService {
    /**
     * Checks if a user is currently penalized (muted, suspended, or perma-banned).
     *
     * @param username the username to check
     * @return true if penalized, false otherwise
     */
    boolean isPenalized(String username);

    /**
     * Retrieves the expiration date/time of the current suspension.
     *
     * @param username the username
     * @return the expiration date/time, or null if not suspended
     */
    LocalDateTime getPenaltyExpiration(String username);

    /**
     * Retrieves the number of remaining muted matches.
     *
     * @param username the username
     * @return the remaining matches count, or 0
     */
    Integer getMatchesPenalizedRemaining(String username);

    /**
     * Retrieves the type of current penalty.
     *
     * @param username the username
     * @return "MUTE", "BAN", or "NONE"
     */
    String getPenaltyType(String username);

    /**
     * Checks if the user is permanently banned.
     *
     * @param username the username
     * @return true if permanently banned
     */
    boolean isPermanentlyBanned(String username);

    /**
     * Retrieves the list of pending notifications for the lobby.
     *
     * @param username the username
     * @return the list of message strings
     */
    List<String> getPendingNotifications(String username);

    /**
     * Clears all pending notifications for a user.
     *
     * @param username the username
     */
    void clearPendingNotifications(String username);

    /**
     * Checks if a warning about future recidivism and permaban escalation should be shown.
     *
     * @param username the username
     * @return true if warning should be shown
     */
    boolean shouldShowRecidivismWarning(String username);

    /**
     * Clears the recidivism warning flag.
     *
     * @param username the username
     */
    void clearRecidivismWarning(String username);

    /**
     * Recalculates reports count and schedules a pending penalty if the user exceeds a threshold.
     * The penalty is applied as "pending" so it doesn't kick in mid-match.
     *
     * @param username the username to check
     */
    void checkAndApplyPenalty(String username);

    /**
     * Registers that a match involving the user has finished.
     * - Decrements remaining muted matches if the match was completed legitimately.
     * - Consolidates any pending penalties (bans, mutes) so they become active now that the match has ended.
     *
     * @param username             the user participating in the match
     * @param completedLegitimately true if match finished normally or opponent surrendered with 5+ turns,
     *                              false if the penalized user forfeited/abandoned
     */
    void registerMatchFinished(String username, boolean completedLegitimately);
}
