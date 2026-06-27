package ar.edu.utn.frc.tup.piii.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TierTest {

    @Test
    public void testFromMmrAndMatchesUnranked() {
        assertEquals(Tier.UNRANKED, Tier.fromMmrAndMatches(1500, 5));
        assertEquals(Tier.UNRANKED, Tier.fromMmrAndMatches(null, 15));
        assertEquals(Tier.UNRANKED, Tier.fromMmrAndMatches(1200, null));
    }

    @Test
    public void testFromMmrAndMatchesTiers() {
        assertEquals(Tier.IRON, Tier.fromMmrAndMatches(1100, 10));
        assertEquals(Tier.BRONZE, Tier.fromMmrAndMatches(1250, 12));
        assertEquals(Tier.SILVER, Tier.fromMmrAndMatches(1450, 15));
        assertEquals(Tier.GOLD, Tier.fromMmrAndMatches(1650, 20));
        assertEquals(Tier.PLATINUM, Tier.fromMmrAndMatches(1850, 25));
        assertEquals(Tier.DIAMOND, Tier.fromMmrAndMatches(2050, 30));
        assertEquals(Tier.MASTER, Tier.fromMmrAndMatches(2250, 35));
        assertEquals(Tier.GRANDMASTER, Tier.fromMmrAndMatches(2450, 40));
    }

    @Test
    public void testIsHigherThan() {
        assertTrue(Tier.GOLD.isHigherThan(Tier.SILVER));
        assertFalse(Tier.IRON.isHigherThan(Tier.BRONZE));
        assertTrue(Tier.GRANDMASTER.isHigherThan(Tier.UNRANKED));
        assertFalse(Tier.UNRANKED.isHigherThan(Tier.IRON));
    }

    @Test
    public void testGetNameAndRank() {
        assertEquals("Gold", Tier.GOLD.getName());
        assertEquals(4, Tier.GOLD.getRank());
        assertEquals("Unranked", Tier.UNRANKED.getName());
        assertEquals(0, Tier.UNRANKED.getRank());
    }
}
