package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.friends.PublicProfileDTO;

public interface PublicProfileService {
    PublicProfileDTO getPublicProfile(String username);
}
