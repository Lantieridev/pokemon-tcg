package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.friends.PublicProfileDTO;

@FunctionalInterface
public interface PublicProfileService {
    PublicProfileDTO getPublicProfile(String username);
}
