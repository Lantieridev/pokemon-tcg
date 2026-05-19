package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Sealed result type returned by RuleValidator.validate(). FR-005.
 *
 * <p>{@link Valid} signals the action is legal.
 * {@link Invalid} carries a reason code string identifying which rule was violated.</p>
 */
public sealed interface ValidationResult permits ValidationResult.Valid, ValidationResult.Invalid {

    /**
     * The action is legal; no rule was violated.
     */
    record Valid() implements ValidationResult {
    }

    /**
     * The action is illegal; reason identifies the violated rule.
     *
     * @param reason a non-null, non-empty string identifying the violated rule
     */
    record Invalid(String reason) implements ValidationResult {
    }
}
