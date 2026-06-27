package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.friends.PublicProfileDTO;
import ar.edu.utn.frc.tup.piii.services.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PublicProfileServiceImplTest {

    private ProfileService profileService;
    private PublicProfileServiceImpl publicProfileService;

    @BeforeEach
    public void setUp() {
        profileService = mock(ProfileService.class);
        publicProfileService = new PublicProfileServiceImpl(profileService);
    }

    @Test
    public void testGetPublicProfile() {
        UserProfileResponseDTO fullProfile = UserProfileResponseDTO.builder()
                .username("lucas")
                .avatarIcon("icon1")
                .description("Desc")
                .activeTitle("Title")
                .selectedMedals("Medals")
                .level(5)
                .mmr(1500)
                .statistics(null)
                .unlockedTitles(List.of("Title"))
                .showcase(Collections.emptyList())
                .showcasedDeck(null)
                .advancedStats(null)
                .build();

        when(profileService.getProfile("lucas")).thenReturn(fullProfile);

        PublicProfileDTO result = publicProfileService.getPublicProfile("lucas");

        assertNotNull(result);
        assertEquals("lucas", result.getUsername());
        assertEquals("icon1", result.getAvatarIcon());
        assertEquals("Desc", result.getDescription());
        assertEquals("Title", result.getActiveTitle());
        assertEquals("Medals", result.getSelectedMedals());
        assertEquals(5, result.getLevel());
        assertEquals(1500, result.getMmr());
        assertTrue(result.getUnlockedTitles().contains("Title"));
    }
}
