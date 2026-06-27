package ar.edu.utn.frc.tup.piii.persistence.entity;

public enum Tier {
    UNRANKED("Unranked", 0),
    IRON("Iron", 1),
    BRONZE("Bronze", 2),
    SILVER("Silver", 3),
    GOLD("Gold", 4),
    PLATINUM("Platinum", 5),
    DIAMOND("Diamond", 6),
    MASTER("Master", 7),
    GRANDMASTER("Grandmaster", 8);

    private final String name;
    private final int rank;

    Tier(final String name, final int rank) {
        this.name = name;
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public int getRank() {
        return rank;
    }

    public boolean isHigherThan(final Tier other) {
        return this.rank > other.rank;
    }

    public static Tier fromMmrAndMatches(final Integer mmr, final Integer matchesPlayed) {
        if (mmr == null || matchesPlayed == null || matchesPlayed < 10) {
            return UNRANKED;
        }
        if (mmr < 1200) {
            return IRON;
        }
        if (mmr < 1400) {
            return BRONZE;
        }
        if (mmr < 1600) {
            return SILVER;
        }
        if (mmr < 1800) {
            return GOLD;
        }
        if (mmr < 2000) {
            return PLATINUM;
        }
        if (mmr < 2200) {
            return DIAMOND;
        }
        if (mmr < 2400) {
            return MASTER;
        }
        return GRANDMASTER;
    }
}
