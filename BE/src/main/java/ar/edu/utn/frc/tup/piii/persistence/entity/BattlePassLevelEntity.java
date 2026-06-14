package ar.edu.utn.frc.tup.piii.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "battle_pass_levels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattlePassLevelEntity {

    @Id
    private Integer level;

    @Column(name = "required_xp", nullable = false)
    private Integer requiredXp;

    @Column(name = "free_reward_type")
    private String freeRewardType;

    @Column(name = "free_reward_amount")
    @Builder.Default
    private Integer freeRewardAmount = 0;

    @Column(name = "free_reward_value")
    private String freeRewardValue;

    @Column(name = "premium_reward_type")
    private String premiumRewardType;

    @Column(name = "premium_reward_amount")
    @Builder.Default
    private Integer premiumRewardAmount = 0;

    @Column(name = "premium_reward_value")
    private String premiumRewardValue;
}
