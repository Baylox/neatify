package io.neatify.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.neatify.TestHelper;
import io.neatify.core.contract.FileMover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FileExecutorErrorHandlingTest extends TestHelper {

    private final LocalFileMover mover = new LocalFileMover();

    @Test
    void execute_IOException_countsAsErrorNotSkip(@TempDir Path tempDir) throws IOException {
        Path nonExistentSource = tempDir.resolve("ghost.jpg");
        Path target = tempDir.resolve("Images").resolve("ghost.jpg");

        FileMover.Result result = mover.execute(List.of(createAction(nonExistentSource, target)), false, FileMover.CollisionStrategy.RENAME, null);

        assertEquals(0, result.moved(),   "moved should be 0");
        assertEquals(0, result.skipped(), "skipped should be 0 — errors must NOT inflate skipped");
        assertEquals(1, result.errors().size(), "1 error expected");
    }

    @Test
    void execute_mixedOutcome_countsAreIndependent(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "a.jpg");
        Path srcA = tempDir.resolve("a.jpg");
        Path tgtA = tempDir.resolve("Images").resolve("a.jpg");

        createTestFile(tempDir, "b.txt");
        Path targetDir = tempDir.resolve("Docs");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("b.txt"), "existing");
        Path srcB = tempDir.resolve("b.txt");
        Path tgtB = targetDir.resolve("b.txt");

        Path srcC = tempDir.resolve("ghost.pdf");
        Path tgtC = tempDir.resolve("Docs").resolve("ghost.pdf");

        List<FileMover.Action> actions = List.of(
            createAction(srcA, tgtA),
            createAction(srcB, tgtB),
            createAction(srcC, tgtC)
        );

        FileMover.Result result = mover.execute(actions, false, FileMover.CollisionStrategy.SKIP, null);

        assertEquals(1, result.moved(),   "only a.jpg should be moved");
        assertEquals(1, result.skipped(), "only b.txt should be skipped (collision)");
        assertEquals(1, result.errors().size(), "only ghost.pdf should produce an error");
    }

    @Test
    void execute_nullParentTarget_doesNotThrowNpe(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "file.txt");
        Path source = tempDir.resolve("file.txt");
        Path target = tempDir.resolve("A").resolve("B").resolve("file.txt");

        FileMover.Result result = mover.execute(List.of(createAction(source, target)), false, FileMover.CollisionStrategy.RENAME, null);

        assertEquals(1, result.moved());
        assertTrue(Files.exists(target));
    }
}
