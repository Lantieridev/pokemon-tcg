package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.PackOpeningResultDTO;

public interface PackService {
    /**
     * Opens a pack for the specified user.
     * @param username the username of the player
     * @param packType the type of pack to open
     * @return a result containing the cards pulled and any duplicate compensation
     */
    PackOpeningResultDTO openPack(String username, String packType);
}
