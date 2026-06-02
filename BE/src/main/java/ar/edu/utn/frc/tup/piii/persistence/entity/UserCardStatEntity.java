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

@Entity
@Table(name = "user_card_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCardStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Column(name = "times_played", nullable = false)
    @Builder.Default
    private int timesPlayed = 0;

    @Column(name = "damage_dealt", nullable = false)
    @Builder.Default
    private int damageDealt = 0;

    @Column(name = "damage_received", nullable = false)
    @Builder.Default
    private int damageReceived = 0;

    @Column(name = "kos_made", nullable = false)
    @Builder.Default
    private int kosMade = 0;

    @Column(name = "kos_suffered", nullable = false)
    @Builder.Default
    private int kosSuffered = 0;
}
