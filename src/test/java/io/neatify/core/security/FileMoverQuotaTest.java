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
 * Anti-DOS tests for FileMover - Protection against processing too many files.
 */
class FileMoverQuotaTest extends FileMoverSecurityTestBase {

    @Test
    void testQuota_UnderLimit(@TempDir Path tempDir) throws IOException {
        // Create 5 files (below the limit)
        createMultipleFiles(tempDir, "file", "txt", 5);

        Map<String, String> rules = Map.of("txt", "Documents");

        // Quota of 10 files
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules, 10);

        assertEquals(5, actions.size());
    }

    @Test
    void testQuota_ExceedsLimit(@TempDir Path tempDir) throws IOException {
        // Create 15 files (above the limit of 10)
        createMultipleFiles(tempDir, "file", "txt", 15);

        Map<String, String> rules = Map.of("txt", "Documents");

        // Quota of 10 files
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> FileMover.plan(tempDir, rules, 10));

        assertTrue(exception.getMessage().contains("File quota exceeded"));
        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    void testQuota_DefaultQuota(@TempDir Path tempDir) throws IOException {
        // Create a few files (well below the default quota of 100k)
        createMultipleFiles(tempDir, "file", "txt", 10);

        Map<String, String> rules = Map.of("txt", "Documents");

        // Use the method without explicit quota (uses DEFAULT_MAX_FILES)
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(10, actions.size());
    }

    @Test
    void testQuota_InvalidQuota(@TempDir Path tempDir) {
        Map<String, String> rules = Map.of("txt", "Documents");

        // Negative quota should fail
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> FileMover.plan(tempDir, rules, -1));

        assertTrue(exception.getMessage().toLowerCase().contains("quota"));
        assertTrue(exception.getMessage().toLowerCase().contains("positive"));
    }
}
