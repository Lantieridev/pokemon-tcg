package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckSummaryDTO;

import java.util.List;

public interface DeckService {

    List<DeckSummaryDTO> getAll();

    List<DeckSummaryDTO> getByUserId(Long userId);

    DeckResponseDTO getById(Long id);

    DeckResponseDTO create(DeckRequestDTO request);

    DeckResponseDTO update(Long id, DeckRequestDTO request);

    void delete(Long id);
}
