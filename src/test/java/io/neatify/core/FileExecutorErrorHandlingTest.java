package io.neatify.core;

import io.neatify.TestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that I/O errors are counted separately from intentional skips.
 * Regression for: IOException was incorrectly incrementing skipped++.
 */
class FileExecutorErrorHandlingTest extends TestHelper {

    @Test
    void execute_IOException_countsAsErrorNotSkip(@TempDir Path tempDir) throws IOException {
        // Source file does not exist — the move will fail with IOException
        Path nonExistentSource = tempDir.resolve("ghost.jpg");
        Path target = tempDir.resolve("Images").resolve("ghost.jpg");

        FileMover.Action action = createAction(nonExistentSource, target);
        FileMover.Result result = FileMover.execute(List.of(action), false);

        assertEquals(0, result.moved(),   "moved should be 0");
        assertEquals(0, result.skipped(), "skipped should be 0 — errors must NOT inflate skipped");
        assertEquals(1, result.errors().size(), "1 error expected");
    }

    @Test
    void execute_mixedOutcome_countsAreIndependent(@TempDir Path tempDir) throws IOException {
        // File A: normal move → moved
        createTestFile(tempDir, "a.jpg");
        Path srcA = tempDir.resolve("a.jpg");
        Path tgtA = tempDir.resolve("Images").resolve("a.jpg");

        // File B: target already exists → SKIP strategy → skipped
        createTestFile(tempDir, "b.txt");
        Path targetDir = tempDir.resolve("Docs");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("b.txt"), "existing");
        Path srcB = tempDir.resolve("b.txt");
        Path tgtB = targetDir.resolve("b.txt");

        // File C: source does not exist → error
        Path srcC = tempDir.resolve("ghost.pdf");
        Path tgtC = tempDir.resolve("Docs").resolve("ghost.pdf");

        List<FileMover.Action> actions = List.of(
            createAction(srcA, tgtA),
            createAction(srcB, tgtB),
            createAction(srcC, tgtC)
        );

        FileMover.Result result = FileMover.execute(actions, false, FileMover.CollisionStrategy.SKIP);

        assertEquals(1, result.moved(),   "only a.jpg should be moved");
        assertEquals(1, result.skipped(), "only b.txt should be skipped (collision)");
        assertEquals(1, result.errors().size(), "only ghost.pdf should produce an error");
    }

    @Test
    void execute_nullParentTarget_doesNotThrowNpe(@TempDir Path tempDir) throws IOException {
        // An action whose target is in a deep new directory — getParent() must not be null
        createTestFile(tempDir, "file.txt");
        Path source = tempDir.resolve("file.txt");
        // Resolve a target nested two levels deep (getParent() is non-null here)
        Path target = tempDir.resolve("A").resolve("B").resolve("file.txt");

        FileMover.Result result = FileMover.execute(List.of(createAction(source, target)), false);

        assertEquals(1, result.moved());
        assertTrue(Files.exists(target));
    }
}
