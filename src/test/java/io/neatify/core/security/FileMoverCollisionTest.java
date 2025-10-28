package io.neatify.core.security;

import io.neatify.core.FileMover;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests anti-TOCTOU pour FileMover - Gestion atomique des collisions de fichiers.
 */
class FileMoverCollisionTest extends FileMoverSecurityTestBase {

    @Test
    void testAtomicMove_NoCollision(@TempDir Path tempDir) throws IOException {
        // Créer un fichier source
        createTestFile(tempDir, "test.txt");

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
        createTestFile(tempDir, "test.txt", "new content");

        // Planifier
        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Créer le dossier cible avec un fichier existant APRÈS la planification
        setupCollisionScenario(tempDir, "test.txt", "existing");

        // Exécuter - devrait créer test_1.txt car test.txt existe déjà
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());

        Path targetDir = tempDir.resolve("Documents");
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
        createTestFile(tempDir, "test.txt", "v3");

        // Planifier
        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Créer le dossier cible avec plusieurs fichiers existants APRÈS la planification
        setupCollisionScenario(tempDir, "test.txt", "v0", "v1", "v2");

        // Exécuter - devrait créer test_3.txt
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());
        assertMultipleCollisionFilesExist(tempDir.resolve("Documents"), "v3");
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
