package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PenaltyServiceImpl implements PenaltyService {

    private final ChatReportRepository chatReportRepository;

    // Stores user penalty expiration times
    private final Map<String, LocalDateTime> penalties = new ConcurrentHashMap<>();

    // Stores the last count of reports for which the user was penalized
    private final Map<String, Long> lastPenalizedCount = new ConcurrentHashMap<>();

    public PenaltyServiceImpl(final ChatReportRepository chatReportRepository) {
        this.chatReportRepository = Objects.requireNonNull(chatReportRepository, "chatReportRepository must not be null");
    }

    @Override
    public boolean isPenalized(final String username) {
        if (username == null) {
            return false;
        }

        final LocalDateTime expiration = penalties.get(username);
        if (expiration != null && LocalDateTime.now().isBefore(expiration)) {
            return true;
        }

        // Penalty expired or not in memory, check if new reports require a new penalty
        checkAndApplyPenalty(username);

        final LocalDateTime newExpiration = penalties.get(username);
        return newExpiration != null && LocalDateTime.now().isBefore(newExpiration);
    }

    @Override
    public LocalDateTime getPenaltyExpiration(final String username) {
        if (username == null) {
            return null;
        }
        final LocalDateTime expiration = penalties.get(username);
        if (expiration != null && LocalDateTime.now().isBefore(expiration)) {
            return expiration;
        }
        return null;
    }

    @Override
    public void checkAndApplyPenalty(final String username) {
        if (username == null) {
            return;
        }

        final long reportCount = chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username);
        if (reportCount >= 3) {
            final Long lastCount = lastPenalizedCount.get(username);
            // Penalize if never penalized before OR if the report count has increased
            if (lastCount == null || reportCount > lastCount) {
                penalties.put(username, LocalDateTime.now().plusMinutes(5));
                lastPenalizedCount.put(username, reportCount);
            }
        }
    }
}
