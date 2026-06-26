package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RankingServiceImplTest {

    private UserRepository userRepository;
    private RankingServiceImpl rankingService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        rankingService = new RankingServiceImpl(userRepository);
    }

    @Test
    public void testGetGlobalRankingTiers() {
        // We will create DTOs representing different MMR and matches played to test all determineTier branches
        RankingDto unranked1 = new RankingDto("user1", null, null, 15);
        RankingDto unranked2 = new RankingDto("user2", 1500, null, 5);
        RankingDto unranked3 = new RankingDto("user3", 1500, null, null);
        RankingDto iron = new RankingDto("user4", 1000, null, 10);
        RankingDto bronze = new RankingDto("user5", 1300, null, 10);
        RankingDto silver = new RankingDto("user6", 1500, null, 10);
        RankingDto gold = new RankingDto("user7", 1700, null, 10);
        RankingDto platinum = new RankingDto("user8", 1900, null, 10);
        RankingDto diamond = new RankingDto("user9", 2100, null, 10);
        RankingDto master = new RankingDto("user10", 2300, null, 10);
        RankingDto grandmaster = new RankingDto("user11", 2500, null, 10);

        List<RankingDto> sourceList = List.of(
                unranked1, unranked2, unranked3, iron, bronze, silver, gold, platinum, diamond, master, grandmaster
        );
        Pageable pageable = PageRequest.of(0, 20);
        Slice<RankingDto> sourceSlice = new SliceImpl<>(sourceList, pageable, false);

        when(userRepository.getGlobalRanking(pageable)).thenReturn(sourceSlice);

        Slice<RankingDto> resultSlice = rankingService.getGlobalRanking(pageable);

        assertNotNull(resultSlice);
        List<RankingDto> resultList = resultSlice.getContent();
        assertEquals(sourceList.size(), resultList.size());

        assertEquals("Unranked", resultList.get(0).tier());
        assertEquals("Unranked", resultList.get(1).tier());
        assertEquals("Unranked", resultList.get(2).tier());
        assertEquals("Iron", resultList.get(3).tier());
        assertEquals("Bronze", resultList.get(4).tier());
        assertEquals("Silver", resultList.get(5).tier());
        assertEquals("Gold", resultList.get(6).tier());
        assertEquals("Platinum", resultList.get(7).tier());
        assertEquals("Diamond", resultList.get(8).tier());
        assertEquals("Master", resultList.get(9).tier());
        assertEquals("Grandmaster", resultList.get(10).tier());
    }
}
