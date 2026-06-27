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
public class CampaignProgressResponseDTO {
    private Integer clearedNodesCount;
    private Integer totalNodesCount;
    private List<CampaignNodeDTO> nodes;
}
