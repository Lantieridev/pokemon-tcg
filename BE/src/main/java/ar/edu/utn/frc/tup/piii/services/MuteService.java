package ar.edu.utn.frc.tup.piii.services;

import java.util.Set;

/**
 * Service for muting other users in matches.
 */
public interface MuteService {

    /**
     * Mutes a target user for the current user.
     *
     * @param username       the current user's username
     * @param targetUsername the username of the user to mute
     */
    void muteUser(String username, String targetUsername);

    /**
     * Unmutes a target user for the current user.
     *
     * @param username       the current user's username
     * @param targetUsername the username of the user to unmute
     */
    void unmuteUser(String username, String targetUsername);

    /**
     * Gets the set of users muted by a user.
     *
     * @param username the current user's username
     * @return the set of muted usernames
     */
    Set<String> getMutedUsers(String username);

    /**
     * Checks if a target user is muted by a user.
     *
     * @param username       the current user's username
     * @param targetUsername the username of the user to check
     * @return true if muted, false otherwise
     */
    boolean isMuted(String username, String targetUsername);
}
