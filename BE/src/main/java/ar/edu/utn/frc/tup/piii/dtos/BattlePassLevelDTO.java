package ar.edu.utn.frc.tup.piii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattlePassLevelDTO {
    private int level;
    private int requiredXp;
    
    private String freeRewardType;
    private int freeRewardAmount;
    private String freeRewardValue;
    
    private String premiumRewardType;
    private int premiumRewardAmount;
    private String premiumRewardValue;
}
