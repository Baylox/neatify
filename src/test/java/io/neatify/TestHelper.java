package io.neatify;

import io.neatify.core.FileMover;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for all tests with common helpers.
 * Reduces code duplication between different test classes.
 */
public abstract class TestHelper {

    // =====================================================
    // HELPER METHODS - File creation
    // =====================================================

    /**
     * Creates a test file with custom content.
     */
    protected void createTestFile(Path tempDir, String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }

    /**
     * Creates a test file with default content.
     */
    protected void createTestFile(Path tempDir, String filename) throws IOException {
        createTestFile(tempDir, filename, "content");
    }

    /**
     * Creates multiple test files with a naming pattern.
     * For example: createMultipleFiles(tempDir, "file", "txt", 5)
     * will create file1.txt, file2.txt, ..., file5.txt
     */
    protected void createMultipleFiles(Path tempDir, String prefix, String extension, int count) throws IOException {
        for (int i = 1; i <= count; i++) {
            createTestFile(tempDir, prefix + i + "." + extension);
        }
    }

    // =====================================================
    // HELPER METHODS - FileMover action creation
    // =====================================================

    /**
     * Creates a FileMover action with default label "test".
     */
    protected FileMover.Action createAction(Path source, Path target) {
        return new FileMover.Action(source, target, "test");
    }

    /**
     * Creates a FileMover action with a custom label.
     */
    protected FileMover.Action createAction(Path source, Path target, String label) {
        return new FileMover.Action(source, target, label);
    }
}
