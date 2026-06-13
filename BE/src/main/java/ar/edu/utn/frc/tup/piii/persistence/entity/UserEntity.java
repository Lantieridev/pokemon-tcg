package ar.edu.utn.frc.tup.piii.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showcased_deck_id")
    private DeckEntity showcasedDeck;

    @Column(name = "avatar_icon")
    @Builder.Default
    private String avatarIcon = "default_trainer";

    @Builder.Default
    private String description = "";

    @Column(name = "active_title")
    @Builder.Default
    private String activeTitle = "Novato";

    @Column(name = "selected_medals")
    @Builder.Default
    private String selectedMedals = "";

    @Builder.Default
    private Integer level = 1;

    @Builder.Default
    private Integer xp = 0;

    @Column(name = "show_recidivism_warning")
    @Builder.Default
    private Boolean showRecidivismWarning = false;

    @Builder.Default
    private Integer mmr = 1000;

    @Builder.Default
    private Integer pokecoins = 0;

    @Column(name = "battle_points")
    @Builder.Default
    private Integer battlePoints = 0;

    @Column(name = "perfect_wins")
    @Builder.Default
    private Integer perfectWins = 0;

    @Column(name = "comeback_wins")
    @Builder.Default
    private Integer comebackWins = 0;

    @Column(name = "total_kos")
    @Builder.Default
    private Integer totalKos = 0;

    @Column(name = "trainer_cards_played")
    @Builder.Default
    private Integer trainerCardsPlayed = 0;

    @Column(name = "total_damage_dealt")
    @Builder.Default
    private Integer totalDamageDealt = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_unlocked_titles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "title_name")
    @Builder.Default
    private Set<String> unlockedTitles = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_cleared_story_nodes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "node_id")
    @Builder.Default
    private Set<Integer> clearedStoryNodes = new HashSet<>();

    @Column(name = "ranked_matches_played")
    @Builder.Default
    private Integer rankedMatchesPlayed = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
