package ar.edu.utn.frc.tup.piii.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Persistable;
import jakarta.persistence.Transient;

@Entity
@Table(name = "user_battle_pass")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBattlePassEntity implements Persistable<Long> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    @Column(name = "is_premium")
    @Builder.Default
    private Boolean isPremium = false;

    @Column(name = "claimed_free_level")
    @Builder.Default
    private Integer claimedFreeLevel = 0;

    @Column(name = "claimed_premium_level")
    @Builder.Default
    private Integer claimedPremiumLevel = 0;
}
