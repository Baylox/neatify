package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests essentiels pour FileMetadata - Focus sur extraction d'extension.
 */
class FileMetadataTest {

    @Test
    void testExtractExtension_Standard(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.jpg");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("jpg", metadata.extension());
        assertEquals("test.jpg", metadata.fileName());
    }

    @Test
    void testExtractExtension_NoExtension(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("README");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("", metadata.extension());
        assertTrue(metadata.hasNoExtension());
    }

    @Test
    void testExtractExtension_NormalizedToLowercase(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.PNG");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("png", metadata.extension());
    }

    @Test
    void testFrom_RejectsDirectory(@TempDir Path tempDir) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileMetadata.from(tempDir)
        );

        assertNotNull(exception.getMessage());
    }
}
