package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPenaltyEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPendingNotificationEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserPenaltyRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserPendingNotificationRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PenaltyServiceImpl implements PenaltyService {

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

    @Override
    public boolean isPenalized(final String username) {
        if (username == null) {
            return false;
        }

        if (isPermanentlyBanned(username)) {
            return true;
        }

        // Check active suspension
        final List<UserPenaltyEntity> activePenalties = userPenaltyRepository.findByUserUsernameAndIsActiveTrue(username);
        for (final UserPenaltyEntity penalty : activePenalties) {
            if (Boolean.TRUE.equals(penalty.getIsPending())) {
                continue;
            }

            if ("BAN".equalsIgnoreCase(penalty.getPenaltyType()) && penalty.getExpiration() != null) {
                if (LocalDateTime.now().isBefore(penalty.getExpiration())) {
                    return true;
                } else {
                    // Suspension expired just now
                    penalty.setIsActive(false);
                    userPenaltyRepository.save(penalty);

                    userRepository.findFirstByUsername(username).ifPresent(user -> {
                        user.setShowRecidivismWarning(true);
                        userRepository.save(user);
                    });
                }
            }

            if ("MUTE".equalsIgnoreCase(penalty.getPenaltyType())) {
                final Integer remainingMutes = penalty.getMatchesRemaining();
                if (remainingMutes != null && remainingMutes > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getPenaltyExpiration(final String username) {
        if (username == null) {
            return null;
        }
        final List<UserPenaltyEntity> active = userPenaltyRepository.findByUserUsernameAndIsActiveTrue(username);
        for (final UserPenaltyEntity penalty : active) {
            if (Boolean.TRUE.equals(penalty.getIsPending())) {
                continue;
            }
            if ("BAN".equalsIgnoreCase(penalty.getPenaltyType()) && penalty.getExpiration() != null) {
                if (LocalDateTime.now().isBefore(penalty.getExpiration())) {
                    return penalty.getExpiration();
                }
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
        final List<UserPenaltyEntity> active = userPenaltyRepository.findByUserUsernameAndIsActiveTrue(username);
        for (final UserPenaltyEntity penalty : active) {
            if (Boolean.TRUE.equals(penalty.getIsPending())) {
                continue;
            }
            if ("MUTE".equalsIgnoreCase(penalty.getPenaltyType()) && penalty.getMatchesRemaining() != null) {
                return penalty.getMatchesRemaining();
            }
        }
        return 0;
    }

    @Override
    public String getPenaltyType(final String username) {
        if (username == null) {
            return "NONE";
        }
        if (isPermanentlyBanned(username)) {
            return "BAN"; // treated as permanent ban in original
        }
        if (getPenaltyExpiration(username) != null) {
            return "BAN";
        }
        if (getMatchesPenalizedRemaining(username) > 0) {
            return "MUTE";
        }
        return "NONE";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPermanentlyBanned(final String username) {
        if (username == null) {
            return false;
        }
        final List<UserPenaltyEntity> active = userPenaltyRepository.findByUserUsernameAndIsActiveTrue(username);
        for (final UserPenaltyEntity penalty : active) {
            if (Boolean.TRUE.equals(penalty.getIsPending())) {
                continue;
            }
            if ("PERMA".equalsIgnoreCase(penalty.getPenaltyType())) {
                return true;
            }
        }
        return false;
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
        if (reportCount >= 3) {
            // Determine penalty severity
            String targetType = null;
            Integer targetMatches = null;
            LocalDateTime targetExpiration = null;

            if (reportCount >= 20) {
                targetType = "PERMA";
            } else if (reportCount >= 12) {
                targetType = "BAN";
                targetExpiration = LocalDateTime.now().plusDays(7);
            } else if (reportCount >= 8) {
                targetType = "BAN";
                targetExpiration = LocalDateTime.now().plusDays(3);
            } else if (reportCount >= 5) {
                targetType = "MUTE";
                targetMatches = 3;
            } else {
                targetType = "MUTE";
                targetMatches = 1;
            }

            // Check if we already applied or queued a penalty of this severity level
            final List<UserPenaltyEntity> existingPenalties = userPenaltyRepository.findByUserAndIsActiveTrue(user);
            boolean alreadyHasIt = false;
            for (final UserPenaltyEntity existing : existingPenalties) {
                if (existing.getPenaltyType().equalsIgnoreCase(targetType)) {
                    if ("MUTE".equals(targetType) && Objects.equals(existing.getMatchesRemaining(), targetMatches)) {
                        alreadyHasIt = true;
                        break;
                    }
                    if ("BAN".equals(targetType) && existing.getExpiration() != null) {
                        alreadyHasIt = true;
                        break;
                    }
                    if ("PERMA".equals(targetType)) {
                        alreadyHasIt = true;
                        break;
                    }
                }
            }

            if (!alreadyHasIt) {
                userPenaltyRepository.save(UserPenaltyEntity.builder()
                        .user(user)
                        .penaltyType(targetType)
                        .matchesRemaining(targetMatches)
                        .expiration(targetExpiration)
                        .isActive(false) // Not active yet
                        .isPending(true)  // Queued until match finishes
                        .build());
            }
        }
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

        // 1. Consolidate pending penalty if exists
        boolean consolidatedNew = false;
        final List<UserPenaltyEntity> penalties = userPenaltyRepository.findByUser(user);

        for (final UserPenaltyEntity penalty : penalties) {
            if (Boolean.TRUE.equals(penalty.getIsPending())) {
                consolidatedNew = true;
                penalty.setIsPending(false);
                penalty.setIsActive(true);
                userPenaltyRepository.save(penalty);

                String msg = "";
                switch (penalty.getPenaltyType().toUpperCase()) {
                    case "PERMA" -> msg = "Tu cuenta ha sido permanentemente suspendida (Baneo Permanente) por acumulación de reportes de comportamiento inapropiado.";
                    case "BAN" -> msg = "Tu cuenta ha sido suspendida temporalmente por comportamiento antideportivo. Expiración: " + penalty.getExpiration();
                    case "MUTE" -> msg = "Has sido silenciado del chat por las próximas " + penalty.getMatchesRemaining() + " partidas debido a comportamiento antideportivo.";
                }

                userPendingNotificationRepository.save(UserPendingNotificationEntity.builder()
                        .user(user)
                        .message(msg)
                        .build());
            }
        }

        // 2. Decrement remaining muted matches if not currently suspended, match was completed legitimately, and it was not just consolidated
        if (!consolidatedNew && isPenalized(username) && getPenaltyType(username).equals("MUTE")) {
            if (completedLegitimately) {
                for (final UserPenaltyEntity penalty : penalties) {
                    if (!Boolean.TRUE.equals(penalty.getIsPending()) && Boolean.TRUE.equals(penalty.getIsActive()) && "MUTE".equalsIgnoreCase(penalty.getPenaltyType())) {
                        final int remaining = penalty.getMatchesRemaining() != null ? penalty.getMatchesRemaining() : 0;
                        if (remaining > 1) {
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

        final List<UserPenaltyEntity> activePenalties = userPenaltyRepository.findByUserUsernameAndIsActiveTrue(username);
        for (final UserPenaltyEntity penalty : activePenalties) {
            if (Boolean.TRUE.equals(penalty.getIsPending())) {
                continue;
            }

            if ("RANKED_BAN".equalsIgnoreCase(penalty.getPenaltyType()) && penalty.getExpiration() != null) {
                if (LocalDateTime.now().isBefore(penalty.getExpiration())) {
                    return true;
                } else {
                    // Penalty expired
                    penalty.setIsActive(false);
                    userPenaltyRepository.save(penalty);
                }
            } else if ("BAN".equalsIgnoreCase(penalty.getPenaltyType()) && penalty.getExpiration() != null) {
                if (LocalDateTime.now().isBefore(penalty.getExpiration())) {
                    return true; // A general BAN also prevents playing ranked
                }
            }
        }
        return false;
    }

    @Override
    public void applyRankedBan(final String username, final int minutes) {
        if (username == null) {
            return;
        }

        userRepository.findFirstByUsername(username).ifPresent(user -> {
            userPenaltyRepository.save(UserPenaltyEntity.builder()
                    .user(user)
                    .penaltyType("RANKED_BAN")
                    .expiration(LocalDateTime.now().plusMinutes(minutes))
                    .isActive(true)
                    .isPending(false)
                    .build());
        });
    }
}
