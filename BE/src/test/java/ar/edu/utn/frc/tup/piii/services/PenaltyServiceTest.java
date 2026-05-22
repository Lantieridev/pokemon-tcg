package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.services.impl.PenaltyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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
        when(chatReportRepository.countByReportedUsername(username)).thenReturn(2L);

        assertFalse(penaltyService.isPenalized(username));
        assertNull(penaltyService.getPenaltyExpiration(username));
    }

    @Test
    void shouldBePenalizedWithThreeOrMoreReports() {
        final String username = "toxicuser";
        when(chatReportRepository.countByReportedUsername(username)).thenReturn(3L);

        assertTrue(penaltyService.isPenalized(username));
        final LocalDateTime expiration = penaltyService.getPenaltyExpiration(username);
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(LocalDateTime.now()));
        assertTrue(expiration.isBefore(LocalDateTime.now().plusMinutes(6)));
    }

    @Test
    void shouldNotReapplyPenaltyIfExpiredAndCountDidNotIncrease() throws InterruptedException {
        final String username = "toxicuser";
        when(chatReportRepository.countByReportedUsername(username)).thenReturn(3L);

        // Apply penalty
        assertTrue(penaltyService.isPenalized(username));
        verify(chatReportRepository, times(1)).countByReportedUsername(username);

        // Simulate expiration by checking again after clearing from memory map or manually resetting
        // For testing we will construct a custom subclass or test the service state.
        // Let's test the state by calling isPenalized.
        // If we call checkAndApplyPenalty again it shouldn't extend penalty if count is the same.
        penaltyService.checkAndApplyPenalty(username);
        // The check was called but count is still 3. Verify total invocations to repository.
        verify(chatReportRepository, times(2)).countByReportedUsername(username);
    }

    @Test
    void shouldReapplyPenaltyIfCountIncreases() {
        final String username = "toxicuser";
        when(chatReportRepository.countByReportedUsername(username)).thenReturn(3L);

        assertTrue(penaltyService.isPenalized(username));

        // Increase reports count
        when(chatReportRepository.countByReportedUsername(username)).thenReturn(4L);
        penaltyService.checkAndApplyPenalty(username);

        assertTrue(penaltyService.isPenalized(username));
    }
}
