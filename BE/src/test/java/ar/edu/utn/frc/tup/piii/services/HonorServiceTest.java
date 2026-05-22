package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.services.impl.HonorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HonorServiceTest {

    private HonorService honorService;

    @BeforeEach
    void setUp() {
        honorService = new HonorServiceImpl();
    }

    @Test
    void shouldAwardAndRetrieveHonors() {
        final String giver = "ash";
        final String receiver = "gary";

        honorService.awardHonor(giver, receiver, HonorType.GOOD_SPORTSMAN);
        honorService.awardHonor(giver, receiver, HonorType.GOOD_SPORTSMAN);
        honorService.awardHonor(giver, receiver, HonorType.FRIENDLY);

        final Map<HonorType, Integer> honors = honorService.getHonors(receiver);
        assertNotNull(honors);
        assertEquals(2, honors.get(HonorType.GOOD_SPORTSMAN));
        assertEquals(1, honors.get(HonorType.FRIENDLY));
        assertEquals(0, honors.get(HonorType.GREAT_STRATEGIST));
    }

    @Test
    void shouldNotAwardHonorToSelf() {
        final String player = "ash";
        honorService.awardHonor(player, player, HonorType.GREAT_STRATEGIST);

        final Map<HonorType, Integer> honors = honorService.getHonors(player);
        assertEquals(0, honors.get(HonorType.GREAT_STRATEGIST));
    }

    @Test
    void shouldReturnZeroHonorsForUnknownUser() {
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

        final Map<HonorType, Integer> honors = honorService.getHonors("gary");
        assertEquals(0, honors.get(HonorType.GOOD_SPORTSMAN));
    }
}
