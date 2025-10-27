package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de sécurité pour FileMover - Protection contre Path Traversal.
 * Second niveau de protection après Rules.sanitizeFolderName().
 */
class FileMoverSecurityTest {

    @Test
    void testPathTraversal_SecondLevelProtection(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("test.jpg"), "content");

        // Simuler une règle malveillante (en pratique bloquée par Rules)
        Map<String, String> maliciousRules = Map.of(
            "jpg", "ValidFolder/../../../etc"
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, maliciousRules);

        assertEquals(0, actions.size(),
            "Les règles avec path traversal ne devraient générer aucune action");
    }

    @Test
    void testResolvedPath_StaysInSourceRoot(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("document.pdf"), "content");

        Map<String, String> maliciousRules = Map.of(
            "pdf", "../../../Windows"
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, maliciousRules);

        assertEquals(0, actions.size());
    }

    @Test
    void testValidNestedPath_Works(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("photo.jpg"), "content");

        Map<String, String> validRules = Map.of(
            "jpg", "Media/Photos/Vacation"
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, validRules);

        assertEquals(1, actions.size());

        FileMover.Action action = actions.get(0);
        Path resolvedTarget = action.target().normalize();
        Path normalizedSource = tempDir.normalize();

        assertTrue(resolvedTarget.startsWith(normalizedSource),
            "Le chemin résolu devrait rester dans le dossier source");
        assertTrue(resolvedTarget.toString().contains("Media"));
        assertTrue(resolvedTarget.toString().contains("Photos"));
        assertTrue(resolvedTarget.toString().contains("Vacation"));
    }

    @Test
    void testAbsolutePath_Blocked(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("script.exe"), "content");

        Map<String, String> maliciousRules = Map.of(
            "exe", "/etc/malicious"
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, maliciousRules);

        assertEquals(0, actions.size());
    }

    @Test
    void testMixedRules_OnlyValidProcessed(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("image.jpg"), "content");
        Files.writeString(tempDir.resolve("document.pdf"), "content");
        Files.writeString(tempDir.resolve("script.exe"), "content");

        Map<String, String> mixedRules = Map.of(
            "jpg", "Images",                    // Valide
            "pdf", "../../../etc",              // Malveillante
            "exe", "Applications/Tools"         // Valide
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, mixedRules);

        assertEquals(2, actions.size(),
            "Seules les règles valides devraient générer des actions");

        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals("image.jpg")));

        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals("script.exe")));

        assertFalse(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals("document.pdf")),
            "document.pdf ne devrait PAS être traité (règle malveillante)");
    }

    @Test
    void testPathNormalization(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "content");

        Map<String, String> rules = Map.of(
            "txt", "Documents/./Subfolder"
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(1, actions.size());

        Path targetPath = actions.get(0).target().normalize();
        assertFalse(targetPath.toString().contains("/./" ),
            "Le chemin ne devrait pas contenir './' après normalisation");
    }
}
