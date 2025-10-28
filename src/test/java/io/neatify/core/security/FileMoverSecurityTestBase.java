package io.neatify.core.security;

import io.neatify.core.FileMover;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de base pour les tests de sécurité de FileMover.
 * Fournit des méthodes utilitaires communes.
 */
public abstract class FileMoverSecurityTestBase {

    // =====================================================
    // HELPER METHODS - Création de fichiers
    // =====================================================

    protected void createTestFile(Path tempDir, String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }

    protected void createTestFile(Path tempDir, String filename) throws IOException {
        createTestFile(tempDir, filename, "content");
    }

    protected void createMultipleFiles(Path tempDir, String prefix, String extension, int count) throws IOException {
        for (int i = 1; i <= count; i++) {
            createTestFile(tempDir, prefix + i + "." + extension);
        }
    }

    // =====================================================
    // HELPER METHODS - Assertions sur les actions
    // =====================================================

    protected void assertActionExists(List<FileMover.Action> actions, String filename) {
        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals(filename)),
            "Action should exist for file: " + filename);
    }

    protected void assertActionNotExists(List<FileMover.Action> actions, String filename, String message) {
        assertFalse(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals(filename)), message);
    }

    // =====================================================
    // HELPER METHODS - Path Traversal
    // =====================================================

    protected void assertMaliciousRuleBlockedForFile(Path tempDir, String filename, String extension,
                                                       String maliciousTarget) throws IOException {
        createTestFile(tempDir, filename);
        Map<String, String> maliciousRules = Map.of(extension, maliciousTarget);
        List<FileMover.Action> actions = FileMover.plan(tempDir, maliciousRules);
        assertEquals(0, actions.size(),
            "Les règles avec path traversal ne devraient générer aucune action");
    }

    // =====================================================
    // HELPER METHODS - Collisions
    // =====================================================

    protected void setupCollisionScenario(Path tempDir, String baseFilename, String... existingContents)
            throws IOException {
        Path targetDir = tempDir.resolve("Documents");
        Files.createDirectories(targetDir);

        Files.writeString(targetDir.resolve(baseFilename), existingContents[0]);
        for (int i = 1; i < existingContents.length; i++) {
            String filename = baseFilename.replaceFirst("\\.", "_" + i + ".");
            Files.writeString(targetDir.resolve(filename), existingContents[i]);
        }
    }
}
