package ar.edu.utn.frc.tup.piii.services.deck;

import java.util.List;

public record DeckEntry(
        String cardId,
        String name,
        String supertype,
        String subtype,
        List<String> rules,
        int quantity
) {

    public boolean isBasicPokemon() {
        return "Pokémon".equals(supertype)
                && subtype != null
                && subtype.contains("Basic")
                && !subtype.contains("Stage");
    }

    public boolean isBasicEnergy() {
        return "Energy".equals(supertype)
                && subtype != null
                && subtype.contains("Basic");
    }

    public boolean isAceSpec() {
        return rules != null
                && rules.stream().anyMatch(r -> r != null && r.contains("ACE SPEC"));
    }
}
