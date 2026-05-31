package ar.edu.utn.frc.tup.piii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateShowcaseRequestDTO {

    private List<ShowcaseSlot> slots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowcaseSlot {
        private Integer slotPosition;
        private String cardId;
    }
}
