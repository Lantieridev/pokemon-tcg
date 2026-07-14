package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPenaltyEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPendingNotificationEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserPenaltyRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserPendingNotificationRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PenaltyServiceImpl implements PenaltyService {

    private static final String PENALTY_BAN = "BAN";
    private static final String PENALTY_MUTE = "MUTE";
    private static final String PENALTY_PERMA = "PERMA";
    private static final String PENALTY_RANKED_BAN = "RANKED_BAN";
    private static final String PENALTY_NONE = "NONE";

    private static final long REPORT_THRESHOLD_TO_PENALIZE = 3;
    private static final long REPORT_THRESHOLD_ESCALATED_MUTE = 5;
    private static final long REPORT_THRESHOLD_SHORT_BAN = 8;
    private static final long REPORT_THRESHOLD_LONG_BAN = 12;
    private static final long REPORT_THRESHOLD_PERMA_BAN = 20;

    private static final long SHORT_BAN_DAYS = 3;
    private static final long LONG_BAN_DAYS = 7;
    private static final int FIRST_MUTE_MATCHES = 1;
    private static final int ESCALATED_MUTE_MATCHES = 3;
    // Distinct from FIRST_MUTE_MATCHES above: that's the *duration* of a first-offense
    // mute, this is the *decrement threshold* ("still more than the last match left?").
    // They coincide at 1 today, but changing one must not silently change the other.
    private static final int LAST_REMAINING_MUTED_MATCH = 1;

    private final ChatReportRepository chatReportRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final UserPendingNotificationRepository userPendingNotificationRepository;
    private final UserRepository userRepository;

    public PenaltyServiceImpl(final ChatReportRepository chatReportRepository,
                              final UserPenaltyRepository userPenaltyRepository,
                              final UserPendingNotificationRepository userPendingNotificationRepository,
                              final UserRepository userRepository) {
        this.chatReportRepository = Objects.requireNonNull(chatReportRepository, "chatReportRepository must not be null");
        this.userPenaltyRepository = Objects.requireNonNull(userPenaltyRepository, "userPenaltyRepository must not be null");
        this.userPendingNotificationRepository = Objects.requireNonNull(userPendingNotificationRepository, "userPendingNotificationRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    private List<UserPenaltyEntity> activeNonPendingPenalties(final String username) {
        return userPenaltyRepository.findByUserUsernameAndIsActiveTrue(username).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsPending()))
                .toList();
    }

    @Override
    public boolean isPenalized(final String username) {
        if (username == null) {
            return false;
        }

        if (isPermanentlyBanned(username)) {
            return true;
        }

        for (final UserPenaltyEntity penalty : activeNonPendingPenalties(username)) {
            if (isBanActive(penalty, username) || isMuteActive(penalty)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBanActive(final UserPenaltyEntity penalty, final String username) {
        if (!PENALTY_BAN.equalsIgnoreCase(penalty.getPenaltyType()) || penalty.getExpiration() == null) {
            return false;
        }
        if (LocalDateTime.now().isBefore(penalty.getExpiration())) {
            return true;
        }
        // Suspension expired just now
        penalty.setIsActive(false);
        userPenaltyRepository.save(penalty);
        userRepository.findFirstByUsername(username).ifPresent(user -> {
            user.setShowRecidivismWarning(true);
            userRepository.save(user);
        });
        return false;
    }

    private boolean isMuteActive(final UserPenaltyEntity penalty) {
        if (!PENALTY_MUTE.equalsIgnoreCase(penalty.getPenaltyType())) {
            return false;
        }
        final Integer remainingMutes = penalty.getMatchesRemaining();
        return remainingMutes != null && remainingMutes > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getPenaltyExpiration(final String username) {
        if (username == null) {
            return null;
        }
        for (final UserPenaltyEntity penalty : activeNonPendingPenalties(username)) {
            if (PENALTY_BAN.equalsIgnoreCase(penalty.getPenaltyType())
                    && penalty.getExpiration() != null
                    && LocalDateTime.now().isBefore(penalty.getExpiration())) {
                return penalty.getExpiration();
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getMatchesPenalizedRemaining(final String username) {
        if (username == null) {
            return 0;
        }
        for (final UserPenaltyEntity penalty : activeNonPendingPenalties(username)) {
            if (PENALTY_MUTE.equalsIgnoreCase(penalty.getPenaltyType()) && penalty.getMatchesRemaining() != null) {
                return penalty.getMatchesRemaining();
            }
        }
        return 0;
    }

    @Override
    public String getPenaltyType(final String username) {
        if (username == null) {
            return PENALTY_NONE;
        }
        if (isPermanentlyBanned(username)) {
            return PENALTY_BAN; // treated as permanent ban in original
        }
        if (getPenaltyExpiration(username) != null) {
            return PENALTY_BAN;
        }
        if (getMatchesPenalizedRemaining(username) > 0) {
            return PENALTY_MUTE;
        }
        return PENALTY_NONE;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPermanentlyBanned(final String username) {
        if (username == null) {
            return false;
        }
        return activeNonPendingPenalties(username).stream()
                .anyMatch(p -> PENALTY_PERMA.equalsIgnoreCase(p.getPenaltyType()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPendingNotifications(final String username) {
        if (username == null) {
            return List.of();
        }
        return userPendingNotificationRepository.findByUserUsername(username).stream()
                .map(UserPendingNotificationEntity::getMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void clearPendingNotifications(final String username) {
        if (username != null) {
            final List<UserPendingNotificationEntity> notifications = userPendingNotificationRepository.findByUserUsername(username);
            userPendingNotificationRepository.deleteAll(notifications);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldShowRecidivismWarning(final String username) {
        if (username == null) {
            return false;
        }
        return userRepository.findFirstByUsername(username)
                .map(UserEntity::getShowRecidivismWarning)
                .orElse(false);
    }

    @Override
    public void clearRecidivismWarning(final String username) {
        if (username != null) {
            userRepository.findFirstByUsername(username).ifPresent(user -> {
                user.setShowRecidivismWarning(false);
                userRepository.save(user);
            });
        }
    }

    private record PenaltySeverity(String type, Integer matches, LocalDateTime expiration) {
    }

    private PenaltySeverity determineSeverity(final long reportCount) {
        if (reportCount >= REPORT_THRESHOLD_PERMA_BAN) {
            return new PenaltySeverity(PENALTY_PERMA, null, null);
        }
        if (reportCount >= REPORT_THRESHOLD_LONG_BAN) {
            return new PenaltySeverity(PENALTY_BAN, null, LocalDateTime.now().plusDays(LONG_BAN_DAYS));
        }
        if (reportCount >= REPORT_THRESHOLD_SHORT_BAN) {
            return new PenaltySeverity(PENALTY_BAN, null, LocalDateTime.now().plusDays(SHORT_BAN_DAYS));
        }
        if (reportCount >= REPORT_THRESHOLD_ESCALATED_MUTE) {
            return new PenaltySeverity(PENALTY_MUTE, ESCALATED_MUTE_MATCHES, null);
        }
        return new PenaltySeverity(PENALTY_MUTE, FIRST_MUTE_MATCHES, null);
    }

    private boolean alreadyQueuedAtSeverity(final List<UserPenaltyEntity> existingPenalties, final PenaltySeverity severity) {
        for (final UserPenaltyEntity existing : existingPenalties) {
            if (!existing.getPenaltyType().equalsIgnoreCase(severity.type())) {
                continue;
            }
            if (PENALTY_MUTE.equals(severity.type())) {
                return Objects.equals(existing.getMatchesRemaining(), severity.matches());
            }
            if (PENALTY_BAN.equals(severity.type())) {
                return existing.getExpiration() != null;
            }
            if (PENALTY_PERMA.equals(severity.type())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkAndApplyPenalty(final String username) {
        if (username == null) {
            return;
        }

        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        final UserEntity user = userOpt.get();

        final long reportCount = chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username);
        if (reportCount < REPORT_THRESHOLD_TO_PENALIZE) {
            return;
        }

        final PenaltySeverity severity = determineSeverity(reportCount);
        final List<UserPenaltyEntity> existingPenalties = userPenaltyRepository.findByUserAndIsActiveTrue(user);
        if (alreadyQueuedAtSeverity(existingPenalties, severity)) {
            return;
        }

        userPenaltyRepository.save(UserPenaltyEntity.builder()
                .user(user)
                .penaltyType(severity.type())
                .matchesRemaining(severity.matches())
                .expiration(severity.expiration())
                .isActive(false) // Not active yet
                .isPending(true)  // Queued until match finishes
                .build());
    }

    @Override
    public void registerMatchFinished(final String username, final boolean completedLegitimately) {
        if (username == null) {
            return;
        }

        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        final UserEntity user = userOpt.get();
        final List<UserPenaltyEntity> penalties = userPenaltyRepository.findByUser(user);

        final boolean consolidatedNew = consolidatePendingPenalties(user, penalties);

        if (!consolidatedNew && completedLegitimately && PENALTY_MUTE.equals(getPenaltyType(username))) {
            decrementActiveMutes(user, penalties);
        }
    }

    private boolean consolidatePendingPenalties(final UserEntity user, final List<UserPenaltyEntity> penalties) {
        boolean consolidatedNew = false;
        for (final UserPenaltyEntity penalty : penalties) {
            if (!Boolean.TRUE.equals(penalty.getIsPending())) {
                continue;
            }
            consolidatedNew = true;
            penalty.setIsPending(false);
            penalty.setIsActive(true);
            userPenaltyRepository.save(penalty);

            userPendingNotificationRepository.save(UserPendingNotificationEntity.builder()
                    .user(user)
                    .message(buildConsolidationMessage(penalty))
                    .build());
        }
        return consolidatedNew;
    }

    private String buildConsolidationMessage(final UserPenaltyEntity penalty) {
        return switch (penalty.getPenaltyType().toUpperCase(Locale.ROOT)) {
            case PENALTY_PERMA -> "Tu cuenta ha sido permanentemente suspendida (Baneo Permanente) por acumulación de reportes de comportamiento inapropiado.";
            case PENALTY_BAN -> "Tu cuenta ha sido suspendida temporalmente por comportamiento antideportivo. Expiración: " + penalty.getExpiration();
            case PENALTY_MUTE -> "Has sido silenciado del chat por las próximas " + penalty.getMatchesRemaining() + " partidas debido a comportamiento antideportivo.";
            default -> {
                log.warn("Unrecognized penalty type '{}' while building consolidation notification for penalty {}", penalty.getPenaltyType(), penalty.getId());
                yield "";
            }
        };
    }

    private void decrementActiveMutes(final UserEntity user, final List<UserPenaltyEntity> penalties) {
        for (final UserPenaltyEntity penalty : penalties) {
            if (Boolean.TRUE.equals(penalty.getIsPending())
                    || !Boolean.TRUE.equals(penalty.getIsActive())
                    || !PENALTY_MUTE.equalsIgnoreCase(penalty.getPenaltyType())) {
                continue;
            }
            final int remaining = penalty.getMatchesRemaining() != null ? penalty.getMatchesRemaining() : 0;
            if (remaining > LAST_REMAINING_MUTED_MATCH) {
                penalty.setMatchesRemaining(remaining - 1);
                userPenaltyRepository.save(penalty);
            } else {
                penalty.setMatchesRemaining(0);
                penalty.setIsActive(false);
                userPenaltyRepository.save(penalty);

                // Mute ended, flag warning
                user.setShowRecidivismWarning(true);
                userRepository.save(user);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRankedBanned(final String username) {
        if (username == null) {
            return false;
        }

        if (isPermanentlyBanned(username)) {
            return true;
        }

        for (final UserPenaltyEntity penalty : activeNonPendingPenalties(username)) {
            if (isRankedBanActive(penalty)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRankedBanActive(final UserPenaltyEntity penalty) {
        if (PENALTY_RANKED_BAN.equalsIgnoreCase(penalty.getPenaltyType()) && penalty.getExpiration() != null) {
            if (LocalDateTime.now().isBefore(penalty.getExpiration())) {
                return true;
            }
            // Penalty expired
            penalty.setIsActive(false);
            userPenaltyRepository.save(penalty);
            return false;
        }
        // A general BAN also prevents playing ranked
        return PENALTY_BAN.equalsIgnoreCase(penalty.getPenaltyType())
                && penalty.getExpiration() != null
                && LocalDateTime.now().isBefore(penalty.getExpiration());
    }

    @Override
    public void applyRankedBan(final String username, final int minutes) {
        if (username == null) {
            return;
        }

        userRepository.findFirstByUsername(username).ifPresent(user -> {
            userPenaltyRepository.save(UserPenaltyEntity.builder()
                    .user(user)
                    .penaltyType(PENALTY_RANKED_BAN)
                    .expiration(LocalDateTime.now().plusMinutes(minutes))
                    .isActive(true)
                    .isPending(false)
                    .build());
        });
    }
}
