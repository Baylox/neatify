package io.neatify.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.neatify.cli.core.UndoExecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for UndoExecutor: verifies that out-of-scope paths and symlinks
 * are properly blocked during undo operations.
 *
 * Regression for: undoRunFile only checked symlinks on "from", not on "to".
 */
class UndoSecurityTest {

    @Test
    void undoRun_outOfScopePath_isSkippedWithError(@TempDir Path root, @TempDir Path outside) throws IOException {
        // Create a real file inside the source root
        Path inRoot = root.resolve("Images").resolve("photo.jpg");
        Files.createDirectories(inRoot.getParent());
        Files.writeString(inRoot, "img");

        // Write a run file that pretends to restore to an OUT-OF-SCOPE location
        UndoExecutor.Move legitimateMove = new UndoExecutor.Move(root.resolve("photo.jpg"), inRoot);
        UndoExecutor.Move maliciousMove  = new UndoExecutor.Move(outside.resolve("stolen.jpg"), inRoot);

        // Manually create the run journal to inject the malicious move
        Path runsDir = root.resolve(".neatify").resolve("runs");
        Files.createDirectories(runsDir);
        long ts = System.currentTimeMillis();
        String json = String.format(
            "{\"time\":%d,\"onCollision\":\"rename\",\"moves\":[{\"from\":\"%s\",\"to\":\"%s\"}]}",
            ts,
            outside.resolve("stolen.jpg").toAbsolutePath().toString().replace("\\", "\\\\"),
            inRoot.toAbsolutePath().toString().replace("\\", "\\\\")
        );
        Files.writeString(runsDir.resolve(ts + ".json"), json);

        UndoExecutor.UndoResult result = UndoExecutor.undoLast(root);
        assertNotNull(result);
        // The malicious move should be skipped (out of scope), file must NOT appear outside root
        assertFalse(Files.exists(outside.resolve("stolen.jpg")),
            "File must not be restored to a location outside the source root");
        assertEquals(0, result.restored());
        assertTrue(result.skipped() > 0 || !result.errors().isEmpty(),
            "The out-of-scope move should be recorded as skipped or error");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)  // symbolic links require elevated privileges on Windows
    void undoRun_symlinkOnToPath_isRejected(@TempDir Path root, @TempDir Path linkTarget) throws IOException {
        // Create the real destination file
        Path realFile = root.resolve("Images").resolve("photo.jpg");
        Files.createDirectories(realFile.getParent());
        Files.writeString(realFile, "img");

        // Create a symlink inside root pointing to linkTarget directory
        Path symlink = root.resolve("Images").resolve("linked");
        try {
            Files.createSymbolicLink(symlink, linkTarget);
        } catch (UnsupportedOperationException | IOException e) {
            // Symlinks not supported in this environment — skip gracefully
            return;
        }

        // The "to" path goes through the symlink
        Path toViaSymlink = symlink.resolve("photo.jpg");
        Files.writeString(linkTarget.resolve("photo.jpg"), "via symlink");

        Path runsDir = root.resolve(".neatify").resolve("runs");
        Files.createDirectories(runsDir);
        long ts = System.currentTimeMillis();
        String json = String.format(
            "{\"time\":%d,\"onCollision\":\"rename\",\"moves\":[{\"from\":\"%s\",\"to\":\"%s\"}]}",
            ts,
            root.resolve("photo.jpg").toAbsolutePath().toString().replace("\\", "\\\\"),
            toViaSymlink.toAbsolutePath().toString().replace("\\", "\\\\")
        );
        Files.writeString(runsDir.resolve(ts + ".json"), json);

        UndoExecutor.UndoResult result = UndoExecutor.undoLast(root);
        assertNotNull(result);
        // The move through a symlink should have been blocked
        assertEquals(0, result.restored(), "No file should be restored through a symlink path");
    }

    @Test
    void listRuns_corruptJsonFile_doesNotCrash(@TempDir Path root) throws IOException {
        Path runsDir = root.resolve(".neatify").resolve("runs");
        Files.createDirectories(runsDir);
        Files.writeString(runsDir.resolve("9999999.json"), "NOT_VALID_JSON{{{{");

        // Must not throw
        List<UndoExecutor.RunMeta> runs = UndoExecutor.listRuns(root);
        // Corrupt file should be silently skipped
        assertEquals(0, runs.size());
    }
}
