package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPenaltyEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPendingNotificationEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserPenaltyRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserPendingNotificationRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.impl.PenaltyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceTest {

    @Mock
    private ChatReportRepository chatReportRepository;

    private UserPenaltyRepository userPenaltyRepository;
    private UserPendingNotificationRepository userPendingNotificationRepository;
    private UserRepository userRepository;

    private PenaltyService penaltyService;

    private List<UserPenaltyEntity> penaltiesDb;
    private List<UserPendingNotificationEntity> notificationsDb;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        userPenaltyRepository = mock(UserPenaltyRepository.class);
        userPendingNotificationRepository = mock(UserPendingNotificationRepository.class);
        userRepository = mock(UserRepository.class);

        penaltiesDb = new ArrayList<>();
        notificationsDb = new ArrayList<>();

        userEntity = UserEntity.builder()
                .id(1L)
                .username("toxicuser")
                .showRecidivismWarning(false)
                .build();

        // Mocks de UserRepository
        lenient().when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(userEntity));
        lenient().when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        lenient().when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            userEntity = invocation.getArgument(0);
            return userEntity;
        });

        // Simular save de penalties
        lenient().when(userPenaltyRepository.save(any(UserPenaltyEntity.class))).thenAnswer(invocation -> {
            final UserPenaltyEntity p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId((long) (penaltiesDb.size() + 1));
                penaltiesDb.add(p);
            } else {
                // Actualizar elemento existente
                for (int i = 0; i < penaltiesDb.size(); i++) {
                    if (penaltiesDb.get(i).getId().equals(p.getId())) {
                        penaltiesDb.set(i, p);
                        break;
                    }
                }
            }
            return p;
        });

        // Simular find de penalties
        lenient().when(userPenaltyRepository.findByUser(any(UserEntity.class))).thenAnswer(invocation ->
                penaltiesDb.stream().filter(p -> p.getIsActive() || p.getIsPending()).collect(Collectors.toList())
        );
        lenient().when(userPenaltyRepository.findByUserAndIsActiveTrue(any(UserEntity.class))).thenAnswer(invocation ->
                penaltiesDb.stream().filter(UserPenaltyEntity::getIsActive).collect(Collectors.toList())
        );
        lenient().when(userPenaltyRepository.findByUserUsernameAndIsActiveTrue(anyString())).thenAnswer(invocation ->
                penaltiesDb.stream().filter(UserPenaltyEntity::getIsActive).collect(Collectors.toList())
        );

        // Simular save de notifications
        lenient().when(userPendingNotificationRepository.save(any(UserPendingNotificationEntity.class))).thenAnswer(invocation -> {
            final UserPendingNotificationEntity n = invocation.getArgument(0);
            n.setId((long) (notificationsDb.size() + 1));
            notificationsDb.add(n);
            return n;
        });

        // Simular find de notifications
        lenient().when(userPendingNotificationRepository.findByUserUsername(anyString())).thenAnswer(invocation ->
                new ArrayList<>(notificationsDb)
        );
        lenient().doAnswer(invocation -> {
            notificationsDb.clear();
            return null;
        }).when(userPendingNotificationRepository).deleteAll(anyList());

        penaltyService = new PenaltyServiceImpl(chatReportRepository, userPenaltyRepository, userPendingNotificationRepository, userRepository);
    }

    @Test
    void shouldNotBePenalizedWithLessThanThreeReports() {
        final String username = "toxicuser";
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
