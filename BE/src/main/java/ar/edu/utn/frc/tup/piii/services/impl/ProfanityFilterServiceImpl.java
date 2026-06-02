package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.services.ProfanityFilterService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service implementation that filters a predefined list of offensive words.
 * Handles accented characters, basic leet-speak variants (4→a, 3→e, 0→o, 1→i),
 * and common Spanish/English gaming insults.
 */
@Service
public class ProfanityFilterServiceImpl implements ProfanityFilterService {

    // Comprehensive blacklist: Spanish insults, English gaming terms, leetspeak variants
    private static final List<String> BLACKLIST = List.of(
            // Insultos básicos en español
            "tonto", "idiota", "estupido", "estúpido", "imbecil", "imbécil",
            "bobo", "burro", "animal", "inutil", "inútil",
            "mierda", "mierd4", "mi3rda",
            "puta", "put4", "perra", "perr4",
            "culo", "cul0",
            "pene", "p3ne",
            "gilipollas", "gilip0llas",
            "coño", "cono",
            "cabrón", "cabron", "cabr0n",
            "pendejo", "p3ndejo",
            "chinga", "ch1nga",
            "mamada", "m4mada",
            "bastardo", "b4stardo",
            "hdp", "hijodeputa", "hijo de puta",
            "malparido", "malp4rido",
            "marica", "mar1ca",
            "maricón", "maricon", "mar1con",
            "culero", "cul3ro",
            "tarado", "t4rado",
            "mogolico", "mogólico",
            "subnormal",
            "retrasado",
            "mongolico", "mongólico",
            // Insultos en inglés comunes en gaming
            "loser", "l0ser",
            "noob", "n00b",
            "cheat", "cheater",
            "idiot", "1d1ot",
            "stupid", "stup1d",
            "moron", "m0ron",
            "dumbass", "dumb4ss",
            "asshole", "4sshole", "assh0le",
            "bastard", "b4stard",
            "bitch", "b1tch", "b17ch",
            "damn", "d4mn",
            "crap",
            "hell",
            "shit", "sh1t", "5hit", "5h1t",
            "fuck", "f4ck", "fck",
            "fucking",
            "penis",
            "dick", "d1ck",
            "cock", "c0ck",
            "ass", "4ss",
            "cunt"
    );

    /**
     * Normalizes text: removes accents and converts leet-speak to plain characters.
     * e.g. "3st0p1d0" -> "estopido", "imbécil" -> "imbecil"
     */
    private String normalize(final String text) {
        if (text == null) {
            return null;
        }
        // Remove accents
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Basic leet-speak substitutions
        normalized = normalized
                .replace("4", "a")
                .replace("3", "e")
                .replace("1", "i")
                .replace("0", "o")
                .replace("5", "s")
                .replace("7", "t");
        return normalized;
    }

    @Override
    public String filter(final String message) {
        if (message == null) {
            return null;
        }
        String filtered = message;
        for (final String word : BLACKLIST) {
            // Match on the original and normalized versions
            final String normalizedWord = normalize(word);
            final String regex = "(?i)\\b" + Pattern.quote(normalizedWord) + "\\b";
            final String replacement = "*".repeat(word.length());
            filtered = normalize(filtered).replaceAll(regex, replacement);
        }
        // Re-filter on original text for exact matches
        for (final String word : BLACKLIST) {
            final String regex = "(?i)\\b" + Pattern.quote(word) + "\\b";
            final String replacement = "*".repeat(word.length());
            filtered = filtered.replaceAll(regex, replacement);
        }
        return filtered;
    }

    @Override
    public List<String> getProfaneWords(final String message) {
        if (message == null) {
            return List.of();
        }
        final String normalizedMsg = normalize(message);
        final List<String> detected = new java.util.ArrayList<>();
        for (final String word : BLACKLIST) {
            final String normalizedWord = normalize(word);
            // Check in normalized version (handles leetspeak + accents)
            final String regexNorm = "(?i)\\b" + Pattern.quote(normalizedWord) + "\\b";
            // Check in original (handles exact matches)
            final String regexOrig = "(?i)\\b" + Pattern.quote(word) + "\\b";
            if (Pattern.compile(regexNorm).matcher(normalizedMsg).find()
                    || Pattern.compile(regexOrig).matcher(message).find()) {
                if (!detected.contains(word)) {
                    detected.add(word);
                }
            }
        }
        return detected;
    }
}

