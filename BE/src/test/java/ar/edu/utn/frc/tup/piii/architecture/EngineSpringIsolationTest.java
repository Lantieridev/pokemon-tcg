package ar.edu.utn.frc.tup.piii.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architectural guardrail: asserts zero {@code org.springframework.*} imports in the engine package.
 * Uses a plain file-scan (no ArchUnit) to avoid external dependencies.
 */
class EngineSpringIsolationTest {

    @Test
    void shouldHaveZeroSpringImportsInEnginePackage() throws Exception {
        final Path engineSrc = Paths.get("src/main/java/ar/edu/utn/frc/tup/piii/engine");
        final List<Path> violations = Files.walk(engineSrc)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        return Files.readString(p).contains("import org.springframework");
                    } catch (final IOException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        assertTrue(violations.isEmpty(),
                "Engine files must not import Spring: " + violations);
    }
}
