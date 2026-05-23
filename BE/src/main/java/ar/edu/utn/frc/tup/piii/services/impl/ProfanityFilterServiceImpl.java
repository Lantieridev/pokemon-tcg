package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.services.ProfanityFilterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Service implementation that filters a predefined list of offensive words.
 */
@Service
public class ProfanityFilterServiceImpl implements ProfanityFilterService {

    // Predefined blacklist of offensive words (both English and Spanish common terms)
    private static final List<String> BLACKLIST = List.of(
            "loser", "noob", "cheat", "tonto", "idiota", "estupido", "mierda"
    );

    @Override
    public String filter(final String message) {
        if (message == null) {
            return null;
        }
        String filtered = message;
        for (final String word : BLACKLIST) {
            // Case-insensitive word boundary matching
            final String regex = "(?i)\\b" + Pattern.quote(word) + "\\b";
            final String replacement = "*".repeat(word.length());
            filtered = filtered.replaceAll(regex, replacement);
        }
        return filtered;
    }
}
