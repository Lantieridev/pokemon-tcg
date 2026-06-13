package ar.edu.utn.frc.tup.piii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignNodeDTO {
    private Integer id;
    private String name;
    private String botName;
    private String status; // "LOCKED", "UNLOCKED", "CLEARED"
    private Integer rewardCoins;
    private Integer rewardXp;
}
