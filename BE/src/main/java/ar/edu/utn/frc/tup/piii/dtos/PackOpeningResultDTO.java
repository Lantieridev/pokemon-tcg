package ar.edu.utn.frc.tup.piii.dtos;

import java.util.List;

public record PackOpeningResultDTO(
    List<PulledCardDTO> cards,
    int coinsRefunded
) {
    public record PulledCardDTO(
        String cardId,
        boolean isFoil,
        boolean isDuplicate
    ) {}
}
