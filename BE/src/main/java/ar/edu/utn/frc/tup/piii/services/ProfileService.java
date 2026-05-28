package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.UpdateProfileRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UpdateShowcaseRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;

public interface ProfileService {
    UserProfileResponseDTO getProfile(String username);
    void updateProfile(String username, UpdateProfileRequestDTO request);
    void updateShowcase(String username, UpdateShowcaseRequestDTO request);
    void updateShowcaseDeck(String username, Long deckId);
    void awardXpAndCheckAchievements(Long userId, boolean won, boolean isPerfectWin, boolean isComebackWin, int kos);
    void trackDamageDealt(String username, int damage);
    void trackTrainerCardPlayed(String username);
    java.util.List<ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO> getAchievementsProgress(String username);
}
