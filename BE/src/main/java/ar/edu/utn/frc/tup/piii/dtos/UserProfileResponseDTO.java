package ar.edu.utn.frc.tup.piii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDTO {

    private String username;
    private LocalDateTime createdAt;
    private String avatarIcon;
    private String description;
    private String activeTitle;
    private String selectedMedals;
    private Integer level;
    private Integer xp;
    private Integer xpToNextLevel;
    private Integer mmr;
    private Integer pokecoins;
    private Integer battlePoints;
    private Integer packs;
    private Map<String, Integer> packsInventory;
    private Integer stardust;

    private Statistics statistics;
    private Map<String, Integer> honors;

    private List<String> unlockedTitles;
    private List<String> unlockedAvatars;

    private List<ShowcaseSlot> showcase;
    private ShowcasedDeck showcasedDeck;
    private AdvancedStatsDTO advancedStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        private Integer matchesPlayed;
        private Integer matchesWon;
        private Integer matchesLost;
        private Double winRate;
        private Integer perfectWins;
        private Integer comebackWins;
        private Integer totalKos;
        private Integer trainerCardsPlayed;
        private Integer totalDamageDealt;
        private Integer winStreak;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowcaseSlot {
        private Integer slotPosition;
        private String cardId;
        private String cardName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowcasedDeck {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardStatDTO {
        private String cardId;
        private String cardName;
        private String pokemonType;
        private int timesPlayed;
        private int damageDealt;
        private int damageReceived;
        private int kosMade;
        private int kosSuffered;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnergyStatDTO {
        private String energyType;
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdvancedStatsDTO {
        private List<CardStatDTO> pokemonStats;
        private List<EnergyStatDTO> energyStats;
        private int totalDamageDealt;
        private int totalDamageReceived;
        private int totalKOsMade;
        private int totalKOsSuffered;
    }
}
