package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;

import java.util.List;
import java.util.Objects;

/**
 * Validates deck composition rules. RF-02b.
 * Currently enforces the 1-ACE-SPEC-per-deck limit.
 */
public final class DeckValidator {

    private static final long MAX_ACE_SPEC_PER_DECK = 1L;
    private static final String TOO_MANY_ACE_SPEC = "too_many_ace_spec";

    /**
     * Validates the given deck against all composition rules.
     *
     * @param deck the list of cards to validate (must not be null)
     * @return {@link ValidationResult.Valid} if legal, {@link ValidationResult.Invalid} otherwise
     */
    public ValidationResult validate(final List<Card> deck) {
        Objects.requireNonNull(deck, "deck must not be null");
        final long aceSpecCount = deck.stream().filter(Card::isAceSpec).count();
        if (aceSpecCount > MAX_ACE_SPEC_PER_DECK) {
            return new ValidationResult.Invalid(TOO_MANY_ACE_SPEC);
        }
        return new ValidationResult.Valid();
    }
}
