package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserHonorEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserHonorRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.impl.HonorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class HonorServiceTest {

    private UserHonorRepository userHonorRepository;
    private UserRepository userRepository;
    private HonorService honorService;

    @BeforeEach
    void setUp() {
        userHonorRepository = mock(UserHonorRepository.class);
        userRepository = mock(UserRepository.class);
        honorService = new HonorServiceImpl(userHonorRepository, userRepository);
    }

    @Test
    void shouldAwardAndRetrieveHonors() {
        final String giverName = "ash";
        final String receiverName = "gary";

        final UserEntity giver = UserEntity.builder().id(1L).username(giverName).build();
        final UserEntity receiver = UserEntity.builder().id(2L).username(receiverName).build();

        when(userRepository.findByUsername(giverName)).thenReturn(Optional.of(giver));
        when(userRepository.findByUsername(receiverName)).thenReturn(Optional.of(receiver));

        final List<UserHonorEntity> mockHonors = new ArrayList<>();
        mockHonors.add(UserHonorEntity.builder().giver(giver).receiver(receiver).honorType(HonorType.GOOD_SPORTSMAN).build());
        mockHonors.add(UserHonorEntity.builder().giver(giver).receiver(receiver).honorType(HonorType.GOOD_SPORTSMAN).build());
        mockHonors.add(UserHonorEntity.builder().giver(giver).receiver(receiver).honorType(HonorType.FRIENDLY).build());

        when(userHonorRepository.findByReceiverUsername(receiverName)).thenReturn(mockHonors);

        honorService.awardHonor(giverName, receiverName, HonorType.GOOD_SPORTSMAN);
        honorService.awardHonor(giverName, receiverName, HonorType.GOOD_SPORTSMAN);
        honorService.awardHonor(giverName, receiverName, HonorType.FRIENDLY);

        final Map<HonorType, Integer> honors = honorService.getHonors(receiverName);
        assertNotNull(honors);
        assertEquals(2, honors.get(HonorType.GOOD_SPORTSMAN));
        assertEquals(1, honors.get(HonorType.FRIENDLY));
        assertEquals(0, honors.get(HonorType.GREAT_STRATEGIST));
    }

    @Test
    void shouldNotAwardHonorToSelf() {
        final String player = "ash";
        honorService.awardHonor(player, player, HonorType.GREAT_STRATEGIST);

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void shouldReturnZeroHonorsForUnknownUser() {
        when(userHonorRepository.findByReceiverUsername("unknown")).thenReturn(new ArrayList<>());
        final Map<HonorType, Integer> honors = honorService.getHonors("unknown");
        assertNotNull(honors);
        for (final HonorType type : HonorType.values()) {
            assertEquals(0, honors.get(type));
        }
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        honorService.awardHonor(null, "gary", HonorType.GOOD_SPORTSMAN);
        honorService.awardHonor("ash", null, HonorType.GOOD_SPORTSMAN);
        honorService.awardHonor("ash", "gary", null);

        verify(userRepository, never()).findByUsername(anyString());
    }
}
