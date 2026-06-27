package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.impl.RankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private RankingServiceImpl rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingServiceImpl(userRepository);
    }

    @Test
    void getGlobalRankingTest() {
        // Arrange
        // We put different users with different MMR and match play counts to verify Tier mapping
        final List<RankingDto> repoRankings = List.of(
                new RankingDto("player-alice", 1850, "old_tier", 25), // Should be mapped to PLATINUM ("Platinum")
                new RankingDto("player-bob", 1100, "old_tier", 5),    // Less than 10 matches -> UNRANKED ("Unranked")
                new RankingDto("player-charlie", 2250, "old_tier", 35) // Should be mapped to MASTER ("Master")
        );
        final Slice<RankingDto> repoSlice = new SliceImpl<>(repoRankings, PageRequest.of(0, 10), false);
        
        when(userRepository.getGlobalRanking(PageRequest.of(0, 10))).thenReturn(repoSlice);

        // Act
        final Slice<RankingDto> result = rankingService.getGlobalRanking(PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());

        // Validate Alice (Platinum)
        final RankingDto alice = result.getContent().get(0);
        assertEquals("player-alice", alice.username());
        assertEquals(1850, alice.mmr());
        assertEquals("Platinum", alice.tier());
        assertEquals(25, alice.rankedMatchesPlayed());

        // Validate Bob (Unranked because matches < 10)
        final RankingDto bob = result.getContent().get(1);
        assertEquals("player-bob", bob.username());
        assertEquals(1100, bob.mmr());
        assertEquals("Unranked", bob.tier());
        assertEquals(5, bob.rankedMatchesPlayed());

        // Validate Charlie (Master)
        final RankingDto charlie = result.getContent().get(2);
        assertEquals("player-charlie", charlie.username());
        assertEquals(2250, charlie.mmr());
        assertEquals("Master", charlie.tier());
        assertEquals(35, charlie.rankedMatchesPlayed());

        verify(userRepository, times(1)).getGlobalRanking(PageRequest.of(0, 10));
    }
}
