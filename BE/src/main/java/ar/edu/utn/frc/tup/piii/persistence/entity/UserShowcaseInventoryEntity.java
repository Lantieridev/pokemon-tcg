package ar.edu.utn.frc.tup.piii.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_showcase_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserShowcaseInventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Column(name = "is_foil")
    @Builder.Default
    private Boolean isFoil = false;

    @CreationTimestamp
    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;
}
