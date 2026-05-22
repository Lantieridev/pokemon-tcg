package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthLoginRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.auth.AuthRegisterRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponseDTO;

public interface AuthService {
    void register(AuthRegisterRequestDTO request);
    AuthResponseDTO login(AuthLoginRequestDTO request);
}
