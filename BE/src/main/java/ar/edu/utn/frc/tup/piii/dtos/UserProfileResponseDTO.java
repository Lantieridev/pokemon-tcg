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
    private Integer level;
    private Integer xp;
    private Integer xpToNextLevel;

    private Statistics statistics;
    private Map<HonorType, Integer> honors;
    private Set<String> unlockedTitles;
    private List<ShowcaseSlot> showcase;
    private ShowcasedDeck showcasedDeck;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        private Integer matchesPlayed;
        private Integer matchesWon;
        private Integer matchesLost;
        private Double winRate;
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
}
