package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import java.util.Map;

/**
 * Service for awarding and querying user honors.
 */
public interface HonorService {

    /**
     * Awards honor to a target player.
     *
     * @param username       the player giving the honor
     * @param targetUsername the player receiving the honor
     * @param honorType      the type of honor to award
     */
    void awardHonor(String username, String targetUsername, HonorType honorType);

    /**
     * Retrieves the count of all honors received by a player.
     *
     * @param username the player's username
     * @return a map showing the count of each honor type received
     */
    Map<HonorType, Integer> getHonors(String username);
}
