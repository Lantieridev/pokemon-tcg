package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.friends.PublicProfileDTO;
import ar.edu.utn.frc.tup.piii.services.ProfileService;
import ar.edu.utn.frc.tup.piii.services.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicProfileServiceImpl implements PublicProfileService {

    private final ProfileService profileService;

    @Override
    public PublicProfileDTO getPublicProfile(String username) {
        UserProfileResponseDTO fullProfile = profileService.getProfile(username);
        
        return PublicProfileDTO.builder()
                .username(fullProfile.getUsername())
                .avatarIcon(fullProfile.getAvatarIcon())
                .description(fullProfile.getDescription())
                .activeTitle(fullProfile.getActiveTitle())
                .selectedMedals(fullProfile.getSelectedMedals())
                .level(fullProfile.getLevel())
                .mmr(fullProfile.getMmr())
                .statistics(fullProfile.getStatistics())
                .unlockedTitles(fullProfile.getUnlockedTitles())
                .showcase(fullProfile.getShowcase())
                .showcasedDeck(fullProfile.getShowcasedDeck())
                .advancedStats(fullProfile.getAdvancedStats())
                .build();
    }
}
