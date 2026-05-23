package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.model.SelectionSource;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;

public record PendingSelectionRequestDTO(
        TrainerEffectId sourceEffect,
        String targetId,
        int maxSelections,
        SelectionSource source,
        java.util.List<String> options
) {
}
