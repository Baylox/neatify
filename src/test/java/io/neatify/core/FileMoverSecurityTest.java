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

    // =====================================================
    // TESTS ANTI-DOS - Quota de fichiers
    // =====================================================

    @Test
    void testQuota_UnderLimit(@TempDir Path tempDir) throws IOException {
        // Créer 5 fichiers (en dessous de la limite)
        for (int i = 1; i <= 5; i++) {
            Files.writeString(tempDir.resolve("file" + i + ".txt"), "content");
        }

        Map<String, String> rules = Map.of("txt", "Documents");

        // Quota de 10 fichiers
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules, 10);

        assertEquals(5, actions.size());
    }

    @Test
    void testQuota_ExceedsLimit(@TempDir Path tempDir) throws IOException {
        // Créer 15 fichiers (au-dessus de la limite de 10)
        for (int i = 1; i <= 15; i++) {
            Files.writeString(tempDir.resolve("file" + i + ".txt"), "content");
        }

        Map<String, String> rules = Map.of("txt", "Documents");

        // Quota de 10 fichiers
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> FileMover.plan(tempDir, rules, 10));

        assertTrue(exception.getMessage().contains("Quota de fichiers dépassé"));
        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    void testQuota_DefaultQuota(@TempDir Path tempDir) throws IOException {
        // Créer quelques fichiers (bien en dessous du quota par défaut de 100k)
        for (int i = 1; i <= 10; i++) {
            Files.writeString(tempDir.resolve("file" + i + ".txt"), "content");
        }

        Map<String, String> rules = Map.of("txt", "Documents");

        // Utiliser la méthode sans quota explicite (utilise DEFAULT_MAX_FILES)
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(10, actions.size());
    }

    @Test
    void testQuota_InvalidQuota(@TempDir Path tempDir) {
        Map<String, String> rules = Map.of("txt", "Documents");

        // Quota négatif devrait échouer
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> FileMover.plan(tempDir, rules, -1));

        assertTrue(exception.getMessage().contains("quota"));
        assertTrue(exception.getMessage().contains("positif"));
    }

    // =====================================================
    // TESTS ANTI-TOCTOU - Gestion des collisions atomique
    // =====================================================

    @Test
    void testAtomicMove_NoCollision(@TempDir Path tempDir) throws IOException {
        // Créer un fichier source
        Path source = tempDir.resolve("test.txt");
        Files.writeString(source, "content");

        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Exécuter
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());
        assertTrue(Files.exists(tempDir.resolve("Documents/test.txt")));
    }

    @Test
    void testAtomicMove_WithCollision(@TempDir Path tempDir) throws IOException {
        // Créer un fichier source
        Path source = tempDir.resolve("test.txt");
        Files.writeString(source, "new content");

        // Planifier
        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Créer le dossier cible avec un fichier existant APRÈS la planification
        Path targetDir = tempDir.resolve("Documents");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("test.txt"), "existing");

        // Exécuter - devrait créer test_1.txt car test.txt existe déjà
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());

        // Le fichier original existe toujours
        assertTrue(Files.exists(targetDir.resolve("test.txt")));
        assertEquals("existing", Files.readString(targetDir.resolve("test.txt")));

        // Le nouveau fichier a un suffixe
        assertTrue(Files.exists(targetDir.resolve("test_1.txt")));
        assertEquals("new content", Files.readString(targetDir.resolve("test_1.txt")));
    }

    @Test
    void testAtomicMove_MultipleCollisions(@TempDir Path tempDir) throws IOException {
        // Créer un nouveau fichier
        Path source = tempDir.resolve("test.txt");
        Files.writeString(source, "v3");

        // Planifier
        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Créer le dossier cible avec plusieurs fichiers existants APRÈS la planification
        Path targetDir = tempDir.resolve("Documents");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("test.txt"), "v0");
        Files.writeString(targetDir.resolve("test_1.txt"), "v1");
        Files.writeString(targetDir.resolve("test_2.txt"), "v2");

        // Exécuter - devrait créer test_3.txt
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());
        assertMultipleCollisionFilesExist(targetDir, "v3");
    }

    private void assertMultipleCollisionFilesExist(Path targetDir, String expectedNewContent) throws IOException {
        // Tous les fichiers existent
        assertTrue(Files.exists(targetDir.resolve("test.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_1.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_2.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_3.txt")));

        // Le nouveau a le bon contenu
        assertEquals(expectedNewContent, Files.readString(targetDir.resolve("test_3.txt")));
    }
}
