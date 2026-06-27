package ar.edu.utn.frc.tup.piii.dtos.deck;

import java.util.List;

public record SmartDeckRequestDTO(
        String theme,
        List<String> preferredTypes,
        Integer evolutionLinesCount,
        String generation
) {}
