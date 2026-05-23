package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.services.impl.PenaltyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceTest {

    @Mock
    private ChatReportRepository chatReportRepository;

    private PenaltyService penaltyService;

    @BeforeEach
    void setUp() {
        penaltyService = new PenaltyServiceImpl(chatReportRepository);
    }

    @Test
    void shouldNotBePenalizedWithLessThanThreeReports() {
        final String username = "testuser";
        when(chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username)).thenReturn(2L);

        penaltyService.checkAndApplyPenalty(username);
        penaltyService.registerMatchFinished(username, true);

        assertFalse(penaltyService.isPenalized(username));
        assertEquals("NONE", penaltyService.getPenaltyType(username));
    }

    @Test
    void shouldBeMutedForOneMatchWithThreeReports() {
        final String username = "toxicuser";
        when(chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username)).thenReturn(3L);

        // Before match finishes, penalty is pending, so not active
        penaltyService.checkAndApplyPenalty(username);
        assertFalse(penaltyService.isPenalized(username));

        // After match finishes, penalty is consolidated
        penaltyService.registerMatchFinished(username, true);
        assertTrue(penaltyService.isPenalized(username));
        assertEquals("MUTE", penaltyService.getPenaltyType(username));
        assertEquals(1, penaltyService.getMatchesPenalizedRemaining(username));

        // Notification is pending
        final List<String> notifications = penaltyService.getPendingNotifications(username);
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).contains("silenciado del chat por las próximas 1 partidas"));

        // Play and finish another match legitimately to clear the mute
        penaltyService.registerMatchFinished(username, true);
        assertFalse(penaltyService.isPenalized(username));
        assertEquals(0, penaltyService.getMatchesPenalizedRemaining(username));
        assertTrue(penaltyService.shouldShowRecidivismWarning(username));
    }

    @Test
    void shouldBeSuspendedForThreeDaysWithEightReports() {
        final String username = "toxicuser";
        when(chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username)).thenReturn(8L);

        penaltyService.checkAndApplyPenalty(username);
        penaltyService.registerMatchFinished(username, true);

        assertTrue(penaltyService.isPenalized(username));
        assertEquals("BAN", penaltyService.getPenaltyType(username));
        assertNotNull(penaltyService.getPenaltyExpiration(username));

        final List<String> notifications = penaltyService.getPendingNotifications(username);
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).contains("suspendida temporalmente por comportamiento antideportivo"));
    }

    @Test
    void shouldBePermaBannedWithTwentyReports() {
        final String username = "toxicuser";
        when(chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username)).thenReturn(20L);

        penaltyService.checkAndApplyPenalty(username);
        penaltyService.registerMatchFinished(username, true);

        assertTrue(penaltyService.isPenalized(username));
        assertTrue(penaltyService.isPermanentlyBanned(username));
        assertEquals("BAN", penaltyService.getPenaltyType(username));

        final List<String> notifications = penaltyService.getPendingNotifications(username);
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).contains("Baneo Permanente"));
    }

    @Test
    void shouldNotDecrementMuteIfMatchNotCompletedLegitimately() {
        final String username = "toxicuser";
        when(chatReportRepository.countByReportedUsernameAndIsValidatedTrue(username)).thenReturn(5L); // 3 matches mute

        penaltyService.checkAndApplyPenalty(username);
        penaltyService.registerMatchFinished(username, true);

        assertEquals(3, penaltyService.getMatchesPenalizedRemaining(username));

        // Match finishes but NOT completed legitimately (e.g. penalized user forfeited)
        penaltyService.registerMatchFinished(username, false);
        assertEquals(3, penaltyService.getMatchesPenalizedRemaining(username)); // stays at 3
    }
}
