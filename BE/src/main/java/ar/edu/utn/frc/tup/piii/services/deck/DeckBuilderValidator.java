package ar.edu.utn.frc.tup.piii.services.deck;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public final class DeckBuilderValidator {

    private static final int REQUIRED_DECK_SIZE = 60;
    private static final int MAX_COPIES_PER_NAME = 4;
    private static final int MAX_ACE_SPEC = 1;

    public void validate(final List<DeckEntry> entries, final ar.edu.utn.frc.tup.piii.engine.model.DeckStatus status) {
        validate(entries, status, null);
    }

    public void validate(final List<DeckEntry> entries, final ar.edu.utn.frc.tup.piii.engine.model.DeckStatus status, final String username) {
        validate(entries, status, username, null);
    }

    public void validate(final List<DeckEntry> entries, final ar.edu.utn.frc.tup.piii.engine.model.DeckStatus status, final String username, final String deckName) {
        if (status == ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.DRAFT) {
            validateDraftSize(entries);
        } else {
            validateTotalSize(entries);
            validateHasBasicPokemon(entries);
        }
        validateMaxCopiesPerName(entries, username, deckName);
        validateAceSpec(entries);
    }

    private void validateDraftSize(final List<DeckEntry> entries) {
        final int total = entries.stream().mapToInt(DeckEntry::quantity).sum();
        if (total > REQUIRED_DECK_SIZE) {
            throw new InvalidDeckException(
                    "Draft deck cannot contain more than 60 cards, found " + total);
        }
    }

    private void validateTotalSize(final List<DeckEntry> entries) {
        final int total = entries.stream().mapToInt(DeckEntry::quantity).sum();
        if (total != REQUIRED_DECK_SIZE) {
            throw new InvalidDeckException(
                    "Deck must contain exactly 60 cards, but has " + total);
        }
    }

    private void validateHasBasicPokemon(final List<DeckEntry> entries) {
        final boolean hasBasic = entries.stream().anyMatch(DeckEntry::isBasicPokemon);
        if (!hasBasic) {
            throw new InvalidDeckException(
                    "Deck must contain at least 1 Basic Pokémon");
        }
    }

    private void validateMaxCopiesPerName(final List<DeckEntry> entries, final String username, final String deckName) {
        final Map<String, Integer> countByName = entries.stream()
                .filter(e -> !e.isBasicEnergy())
                .collect(Collectors.toMap(
                        DeckEntry::name,
                        DeckEntry::quantity,
                        Integer::sum));

        countByName.forEach((name, count) -> {
            if (count > MAX_COPIES_PER_NAME) {
                throw new InvalidDeckException(
                        "Card \"" + name + "\" exceeds the limit of 4 copies (found " + count + ")");
            }
        });
    }

    private void validateAceSpec(final List<DeckEntry> entries) {
        final int aceSpecCount = entries.stream()
                .filter(DeckEntry::isAceSpec)
                .mapToInt(DeckEntry::quantity)
                .sum();
        if (aceSpecCount > MAX_ACE_SPEC) {
            throw new InvalidDeckException(
                    "Deck may contain at most 1 ACE SPEC card (found " + aceSpecCount + ")");
        }
    }
}
