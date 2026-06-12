package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.persistence.entity.SeasonEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.SeasonRecordEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.SeasonRecordRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.SeasonRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final SeasonRecordRepository seasonRecordRepository;
    private final UserRepository userRepository;
    private final MmrCalculationService mmrCalculationService;

    // Run every day at midnight to check if the season has expired
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkAndRotateSeasons() {
        seasonRepository.findActiveSeason().ifPresent(activeSeason -> {
            // Check if 2 months have passed
            if (activeSeason.getStartDate().plusMonths(2).isBefore(LocalDateTime.now())) {
                log.info("Season {} has ended. Executing MMR soft-reset.", activeSeason.getName());
                
                // 1. Close current season
                activeSeason.setStatus("CLOSED");
                activeSeason.setEndDate(LocalDateTime.now());
                seasonRepository.save(activeSeason);

                // 2. Save records and apply soft reset
                List<UserEntity> allUsers = userRepository.findAll();
                for (UserEntity user : allUsers) {
                    // Only process users who played ranked
                    if (user.getRankedMatchesPlayed() > 0) {
                        // Save record
                        SeasonRecordEntity record = SeasonRecordEntity.builder()
                                .season(activeSeason)
                                .user(user)
                                .finalMmr(user.getMmr())
                                .matchesPlayed(user.getRankedMatchesPlayed())
                                .build();
                        seasonRecordRepository.save(record);

                        // Soft reset
                        int newMmr = mmrCalculationService.calculateSoftResetMmr(user.getMmr());
                        user.setMmr(newMmr);
                        user.setRankedMatchesPlayed(0); // Reset placements
                        userRepository.save(user);
                    }
                }

                // 3. Open new season
                SeasonEntity newSeason = SeasonEntity.builder()
                        .name("Season " + (activeSeason.getId() + 1))
                        .status("ACTIVE")
                        .build();
                seasonRepository.save(newSeason);
                log.info("New season {} has started.", newSeason.getName());
            }
        });
    }
}
