package io.neatify.core.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.neatify.core.contract.FileMover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FileMoverCollisionTest extends FileMoverSecurityTestBase {

    @Test
    void testAtomicMove_NoCollision(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.txt");

        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        FileMover.Result result = mover.execute(actions, false, FileMover.CollisionStrategy.RENAME, null);

        assertEquals(1, result.moved());
        assertTrue(Files.exists(tempDir.resolve("Documents/test.txt")));
    }

    @Test
    void testAtomicMove_WithCollision(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.txt", "new content");

        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        setupCollisionScenario(tempDir, "test.txt", "existing");

        FileMover.Result result = mover.execute(actions, false, FileMover.CollisionStrategy.RENAME, null);

        assertEquals(1, result.moved());

        Path targetDir = tempDir.resolve("Documents");
        assertTrue(Files.exists(targetDir.resolve("test.txt")));
        assertEquals("existing", Files.readString(targetDir.resolve("test.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_1.txt")));
        assertEquals("new content", Files.readString(targetDir.resolve("test_1.txt")));
    }

    @Test
    void testAtomicMove_MultipleCollisions(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.txt", "v3");

        Map<String, String> rules = Map.of("txt", "Documents");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        setupCollisionScenario(tempDir, "test.txt", "v0", "v1", "v2");

        FileMover.Result result = mover.execute(actions, false, FileMover.CollisionStrategy.RENAME, null);

        assertEquals(1, result.moved());
        assertMultipleCollisionFilesExist(tempDir.resolve("Documents"), "v3");
    }

    private void assertMultipleCollisionFilesExist(Path targetDir, String expectedNewContent) throws IOException {
        assertTrue(Files.exists(targetDir.resolve("test.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_1.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_2.txt")));
        assertTrue(Files.exists(targetDir.resolve("test_3.txt")));
        assertEquals(expectedNewContent, Files.readString(targetDir.resolve("test_3.txt")));
    }
}
