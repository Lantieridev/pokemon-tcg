package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckSummaryDTO;

import java.util.List;

public interface DeckService {

    List<DeckSummaryDTO> getByUsername(String username);

    DeckResponseDTO getById(Long id, String requestingUsername);

    DeckResponseDTO create(DeckRequestDTO request, String requestingUsername);

    DeckResponseDTO update(Long id, DeckRequestDTO request, String requestingUsername);

    void delete(Long id, String requestingUsername);
}
