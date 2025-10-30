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
 * Anti-TOCTOU tests for FileMover - Atomic file collision handling.
 */
class FileMoverCollisionTest extends FileMoverSecurityTestBase {

    @Test
    void testAtomicMove_NoCollision(@TempDir Path tempDir) throws IOException {
        // Create a source file
        createTestFile(tempDir, "test.txt");

        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Execute
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());
        assertTrue(Files.exists(tempDir.resolve("Documents/test.txt")));
    }

    @Test
    void testAtomicMove_WithCollision(@TempDir Path tempDir) throws IOException {
        // Create a source file
        createTestFile(tempDir, "test.txt", "new content");

        // Plan
        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Create the target folder with an existing file AFTER planning
        setupCollisionScenario(tempDir, "test.txt", "existing");

        // Execute - should create test_1.txt because test.txt already exists
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());

        Path targetDir = tempDir.resolve("Documents");
        // The original file still exists
        assertTrue(Files.exists(targetDir.resolve("test.txt")));
        assertEquals("existing", Files.readString(targetDir.resolve("test.txt")));

        // The new file has a suffix
        assertTrue(Files.exists(targetDir.resolve("test_1.txt")));
        assertEquals("new content", Files.readString(targetDir.resolve("test_1.txt")));
    }

    @Test
    void testAtomicMove_MultipleCollisions(@TempDir Path tempDir) throws IOException {
        // Create a new file
        createTestFile(tempDir, "test.txt", "v3");

        // Plan
        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        // Create the target folder with multiple existing files AFTER planning
        setupCollisionScenario(tempDir, "test.txt", "v0", "v1", "v2");

        // Execute - should create test_3.txt
        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(1, result.moved());
        assertMultipleCollisionFilesExist(tempDir.resolve("Documents"), "v3");
    }

    private void assertMultipleCollisionFilesExist(Path targetDir, String expectedNewContent) throws IOException {
        // All files exist
        assertTrue(Files.exists(targetDir.resolve("test.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_1.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_2.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_3.txt")));

        // The new one has the correct content
        assertEquals(expectedNewContent, Files.readString(targetDir.resolve("test_3.txt")));
    }
}
