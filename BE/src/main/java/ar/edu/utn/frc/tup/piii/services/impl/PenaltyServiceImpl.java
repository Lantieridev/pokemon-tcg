package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PenaltyServiceImpl implements PenaltyService {

    private final ChatReportRepository chatReportRepository;

    // In-memory active suspensions (bans)
    private final Map<String, LocalDateTime> suspensions = new ConcurrentHashMap<>();

    // In-memory active muted matches remaining
    private final Map<String, Integer> mutedMatches = new ConcurrentHashMap<>();

    // In-memory permanent bans
    private final Map<String, Boolean> permaBans = new ConcurrentHashMap<>();

    // In-memory pending penalties (deferred until match finishes)
    private final Map<String, PendingPenalty> pendingPenalties = new ConcurrentHashMap<>();

    // In-memory lobby notifications
    private final Map<String, List<String>> pendingNotifications = new ConcurrentHashMap<>();

    // In-memory recidivism warning flags
    private final Map<String, Boolean> recidivismWarnings = new ConcurrentHashMap<>();

    // Track the last report count for which the user was penalized
    private final Map<String, Long> lastPenalizedCount = new ConcurrentHashMap<>();

    private static class PendingPenalty {
        final String type; // "MUTE", "BAN", "PERMA"
        final Object value; // Integer, LocalDateTime or null

        PendingPenalty(final String type, final Object value) {
            this.type = type;
            this.value = value;
        }
    }

    public PenaltyServiceImpl(final ChatReportRepository chatReportRepository) {
        this.chatReportRepository = Objects.requireNonNull(chatReportRepository, "chatReportRepository must not be null");
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
        final LocalDateTime suspensionExpiration = suspensions.get(username);
        if (suspensionExpiration != null) {
            if (LocalDateTime.now().isBefore(suspensionExpiration)) {
                return true;
            } else {
                // Suspension expired just now
                suspensions.remove(username);
                recidivismWarnings.put(username, true);
            }
        }

        // Check active mute matches remaining
        final Integer remainingMutes = mutedMatches.get(username);
        if (remainingMutes != null && remainingMutes > 0) {
            return true;
        }

        return false;
    }

    @Override
    public LocalDateTime getPenaltyExpiration(final String username) {
        if (username == null) {
            return null;
        }
        final LocalDateTime expiration = suspensions.get(username);
        if (expiration != null && LocalDateTime.now().isBefore(expiration)) {
            return expiration;
        }
        return null;
    }

    @Override
    public Integer getMatchesPenalizedRemaining(final String username) {
        if (username == null) {
            return 0;
        }
        return mutedMatches.getOrDefault(username, 0);
    }

    @Override
    public String getPenaltyType(final String username) {
        if (username == null) {
            return "NONE";
        }
        if (isPermanentlyBanned(username)) {
            return "BAN"; // treated as permanent ban
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
    public boolean isPermanentlyBanned(final String username) {
        if (username == null) {
            return false;
        }
        return permaBans.getOrDefault(username, false);
    }

    @Override
    public List<String> getPendingNotifications(final String username) {
        if (username == null) {
            return List.of();
        }
        return pendingNotifications.getOrDefault(username, List.of());
    }

    @Override
    public void clearPendingNotifications(final String username) {
        if (username != null) {
            pendingNotifications.remove(username);
        }
    }

    @Override
    public boolean shouldShowRecidivismWarning(final String username) {
        if (username == null) {
            return false;
        }
        return recidivismWarnings.getOrDefault(username, false);
    }

    @Override
    public void clearRecidivismWarning(final String username) {
        if (username != null) {
            recidivismWarnings.remove(username);
        }
    }

    @Override
    public void checkAndApplyPenalty(final String username) {
        if (username == null) {
            return;
        }

        final long reportCount = chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username);
        if (reportCount >= 3) {
            final Long lastCount = lastPenalizedCount.get(username);
            // Penalize only if never penalized at this count
            if (lastCount == null || reportCount > lastCount) {
                lastPenalizedCount.put(username, reportCount);

                PendingPenalty pending = null;
                if (reportCount >= 20) {
                    pending = new PendingPenalty("PERMA", null);
                } else if (reportCount >= 12) {
                    pending = new PendingPenalty("BAN", LocalDateTime.now().plusDays(7));
                } else if (reportCount >= 8) {
                    pending = new PendingPenalty("BAN", LocalDateTime.now().plusDays(3));
                } else if (reportCount >= 5) {
                    pending = new PendingPenalty("MUTE", 3);
                } else {
                    pending = new PendingPenalty("MUTE", 1);
                }

                pendingPenalties.put(username, pending);
            }
        }
    }

    @Override
    public void registerMatchFinished(final String username, final boolean completedLegitimately) {
        if (username == null) {
            return;
        }

        // 1. Consolidate pending penalty if exists
        boolean consolidatedNew = false;
        final PendingPenalty pending = pendingPenalties.remove(username);
        if (pending != null) {
            consolidatedNew = true;
            final List<String> notifications = pendingNotifications.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
            switch (pending.type) {
                case "PERMA" -> {
                    permaBans.put(username, true);
                    notifications.add("Tu cuenta ha sido permanentemente suspendida (Baneo Permanente) por acumulación de reportes de comportamiento inapropiado.");
                }
                case "BAN" -> {
                    final LocalDateTime expiration = (LocalDateTime) pending.value;
                    suspensions.put(username, expiration);
                    notifications.add("Tu cuenta ha sido suspendida temporalmente por comportamiento antideportivo. Expiración: " + expiration);
                }
                case "MUTE" -> {
                    final Integer matches = (Integer) pending.value;
                    mutedMatches.put(username, matches);
                    notifications.add("Has sido silenciado del chat por las próximas " + matches + " partidas debido a comportamiento antideportivo.");
                }
            }
        }

        // 2. Decrement remaining muted matches if not currently suspended, match was completed legitimately, and it was not just consolidated
        if (!consolidatedNew && isPenalized(username) && getPenaltyType(username).equals("MUTE")) {
            if (completedLegitimately) {
                final int remaining = getMatchesPenalizedRemaining(username);
                if (remaining > 1) {
                    mutedMatches.put(username, remaining - 1);
                } else {
                    mutedMatches.remove(username);
                    // Mute ended, flag warning
                    recidivismWarnings.put(username, true);
                }
            }
        }
    }
}
