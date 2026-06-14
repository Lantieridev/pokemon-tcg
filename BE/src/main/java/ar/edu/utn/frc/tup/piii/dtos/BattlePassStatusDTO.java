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
public class BattlePassStatusDTO {
    private boolean isPremium;
    private int currentXp;
    private int currentLevel;
    private int claimedFreeLevel;
    private int claimedPremiumLevel;
    private List<BattlePassLevelDTO> levels;
}
