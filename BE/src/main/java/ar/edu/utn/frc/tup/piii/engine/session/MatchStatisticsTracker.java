package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks match-specific statistics for a player in real time.
 */
public class MatchStatisticsTracker {

    private final Map<String, Integer> pokemonPlayedCounts = new HashMap<>();
    private final Map<String, Integer> pokemonDamageDealt = new HashMap<>();
    private final Map<String, Integer> pokemonDamageReceived = new HashMap<>();
    private final Map<String, Integer> pokemonKOsMade = new HashMap<>();
    private final Map<String, Integer> pokemonKOsSuffered = new HashMap<>();
    private final Map<PokemonType, Integer> energyAttachedCounts = new HashMap<>();

    public void incrementPokemonPlayed(String cardId) {
        pokemonPlayedCounts.put(cardId, pokemonPlayedCounts.getOrDefault(cardId, 0) + 1);
    }

    public void addDamageDealt(String cardId, int amount) {
        if (amount <= 0 || cardId == null) return;
        pokemonDamageDealt.put(cardId, pokemonDamageDealt.getOrDefault(cardId, 0) + amount);
    }

    public void addDamageReceived(String cardId, int amount) {
        if (amount <= 0 || cardId == null) return;
        pokemonDamageReceived.put(cardId, pokemonDamageReceived.getOrDefault(cardId, 0) + amount);
    }

    public void incrementKOsMade(String cardId) {
        if (cardId == null) return;
        pokemonKOsMade.put(cardId, pokemonKOsMade.getOrDefault(cardId, 0) + 1);
    }

    public void incrementKOsSuffered(String cardId) {
        if (cardId == null) return;
        pokemonKOsSuffered.put(cardId, pokemonKOsSuffered.getOrDefault(cardId, 0) + 1);
    }

    public void incrementEnergyAttached(PokemonType type) {
        if (type == null) return;
        energyAttachedCounts.put(type, energyAttachedCounts.getOrDefault(type, 0) + 1);
    }

    public Map<String, Integer> getPokemonPlayedCounts() { return pokemonPlayedCounts; }
    public Map<String, Integer> getPokemonDamageDealt() { return pokemonDamageDealt; }
    public Map<String, Integer> getPokemonDamageReceived() { return pokemonDamageReceived; }
    public Map<String, Integer> getPokemonKOsMade() { return pokemonKOsMade; }
    public Map<String, Integer> getPokemonKOsSuffered() { return pokemonKOsSuffered; }
    public Map<PokemonType, Integer> getEnergyAttachedCounts() { return energyAttachedCounts; }
}
