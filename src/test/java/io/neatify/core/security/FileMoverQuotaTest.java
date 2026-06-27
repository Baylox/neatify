package io.neatify.core.security;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.neatify.core.contract.FileMover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FileMoverQuotaTest extends FileMoverSecurityTestBase {

    @Test
    void testQuota_UnderLimit(@TempDir Path tempDir) throws IOException {
        createMultipleFiles(tempDir, "file", "txt", 5);

        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 10, List.of(), List.of(), true);

        assertEquals(5, actions.size());
    }

    @Test
    void testQuota_ExceedsLimit(@TempDir Path tempDir) throws IOException {
        createMultipleFiles(tempDir, "file", "txt", 15);

        Map<String, String> rules = Map.of("txt", "Documents");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> mover.plan(tempDir, rules, 10, List.of(), List.of(), true));

        assertTrue(exception.getMessage().contains("File quota exceeded"));
        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    void testQuota_DefaultQuota(@TempDir Path tempDir) throws IOException {
        createMultipleFiles(tempDir, "file", "txt", 10);

        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        assertEquals(10, actions.size());
    }

    @Test
    void testQuota_InvalidQuota(@TempDir Path tempDir) {
        Map<String, String> rules = Map.of("txt", "Documents");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> mover.plan(tempDir, rules, -1, List.of(), List.of(), true));

        assertTrue(exception.getMessage().toLowerCase().contains("quota"));
        assertTrue(exception.getMessage().toLowerCase().contains("positive"));
    }
}
