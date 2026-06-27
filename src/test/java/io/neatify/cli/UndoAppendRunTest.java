package io.neatify.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.neatify.cli.core.UndoExecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UndoExecutor.appendRun robustness and timestamp ordering.
 */
class UndoAppendRunTest {

    private UndoExecutor.Move move(Path from, Path to) {
        return new UndoExecutor.Move(from, to);
    }

    @Test
    void appendRun_sameMillisecond_producesDistinctFiles(@TempDir Path root) throws IOException {
        // Pre-create a run file with the current timestamp to simulate a collision
        Path runsDir = root.resolve(".neatify").resolve("runs");
        Files.createDirectories(runsDir);

        long now = System.currentTimeMillis();
        // Plant a file that would collide with the first attempt
        Files.writeString(runsDir.resolve(now + ".json"), "{}");

        List<UndoExecutor.Move> moves = List.of(move(root.resolve("a.txt"), root.resolve("Docs/a.txt")));
        Path result = UndoExecutor.appendRun(root, "rename", moves);

        assertNotNull(result, "appendRun must succeed even when the primary filename is taken");
        assertTrue(Files.exists(result), "The returned path must exist");

        // The collision file and the new file must be different
        assertNotEquals(runsDir.resolve(now + ".json"), result);
    }

    @Test
    void undoLastV2_picksHighestTimestamp(@TempDir Path root) throws IOException {
        // Create two run files with moves so that undoLast picks the newer one
        Path srcA = root.resolve("a.jpg");
        Path dstA = root.resolve("Images").resolve("a.jpg");
        Path srcB = root.resolve("b.pdf");
        Path dstB = root.resolve("Docs").resolve("b.pdf");

        Files.createDirectories(root.resolve("Images"));
        Files.createDirectories(root.resolve("Docs"));
        Files.writeString(dstA, "img");
        Files.writeString(dstB, "doc");

        // Write the OLDER run (file A)
        UndoExecutor.appendRun(root, "rename", List.of(move(srcA, dstA)));
        // Small sleep to guarantee distinct timestamps
        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        // Write the NEWER run (file B)
        UndoExecutor.appendRun(root, "rename", List.of(move(srcB, dstB)));

        // undoLast should undo the NEWER run (B), restoring b.pdf
        UndoExecutor.UndoResult result = UndoExecutor.undoLast(root);
        assertNotNull(result);
        assertTrue(Files.exists(srcB), "b.pdf should be restored (newer run undone)");
        assertFalse(Files.exists(dstB), "Docs/b.pdf should be gone after undo");
        // a.jpg's run is still present (older run not yet undone)
        assertTrue(Files.exists(dstA), "Images/a.jpg should still exist (older run not yet undone)");
    }

    @Test
    void appendRun_emptyMoves_returnsNull(@TempDir Path root) throws IOException {
        Path result = UndoExecutor.appendRun(root, "rename", List.of());
        assertNull(result, "appendRun should return null when there are no moves");
    }

    @Test
    void appendRun_createsGitignore(@TempDir Path root) throws IOException {
        List<UndoExecutor.Move> moves = List.of(move(root.resolve("x.txt"), root.resolve("Docs/x.txt")));
        UndoExecutor.appendRun(root, "rename", moves);

        Path gitignore = root.resolve(".neatify").resolve(".gitignore");
        assertTrue(Files.exists(gitignore), ".gitignore should be created in .neatify/");
        String content = Files.readString(gitignore);
        assertTrue(content.contains("*"), ".gitignore should ignore everything");
        assertTrue(content.contains("!.gitignore"), ".gitignore should preserve itself");
    }
}
