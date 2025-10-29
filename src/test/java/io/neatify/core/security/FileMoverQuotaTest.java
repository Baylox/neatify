package io.neatify.core.security;

import io.neatify.core.FileMover;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests anti-DOS pour FileMover - Protection contre le traitement de trop nombreux fichiers.
 */
class FileMoverQuotaTest extends FileMoverSecurityTestBase {

    @Test
    void testQuota_UnderLimit(@TempDir Path tempDir) throws IOException {
        // Créer 5 fichiers (en dessous de la limite)
        createMultipleFiles(tempDir, "file", "txt", 5);

        Map<String, String> rules = Map.of("txt", "Documents");

        // Quota de 10 fichiers
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules, 10);

        assertEquals(5, actions.size());
    }

    @Test
    void testQuota_ExceedsLimit(@TempDir Path tempDir) throws IOException {
        // Créer 15 fichiers (au-dessus de la limite de 10)
        createMultipleFiles(tempDir, "file", "txt", 15);

        Map<String, String> rules = Map.of("txt", "Documents");

        // Quota de 10 fichiers
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> FileMover.plan(tempDir, rules, 10));

        assertTrue(exception.getMessage().contains("File quota exceeded"));
        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    void testQuota_DefaultQuota(@TempDir Path tempDir) throws IOException {
        // Créer quelques fichiers (bien en dessous du quota par défaut de 100k)
        createMultipleFiles(tempDir, "file", "txt", 10);

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

        assertTrue(exception.getMessage().toLowerCase().contains("quota"));
        assertTrue(exception.getMessage().toLowerCase().contains("positive"));
    }
}
