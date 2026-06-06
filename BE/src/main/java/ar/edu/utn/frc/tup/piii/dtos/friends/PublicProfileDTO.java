package ar.edu.utn.frc.tup.piii.dtos.friends;

import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProfileDTO {
    private String username;
    private String avatarIcon;
    private String description;
    private String activeTitle;
    private String selectedMedals;
    private Integer level;
    private Integer mmr;
    
    private UserProfileResponseDTO.Statistics statistics;
    private Set<String> unlockedTitles;
    private List<UserProfileResponseDTO.ShowcaseSlot> showcase;
    private UserProfileResponseDTO.ShowcasedDeck showcasedDeck;
    private UserProfileResponseDTO.AdvancedStatsDTO advancedStats;
}
