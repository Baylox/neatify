package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests essentiels pour FileMetadata.
 */
class FileMetadataTest {

    @Test
    void testExtractExtension_Standard(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.jpg");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("jpg", metadata.extension());
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
    void testExtractExtension_Uppercase(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.PNG");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("png", metadata.extension());
    }

    @Test
    void testExtractExtension_MultipleDots(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("archive.tar.gz");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("gz", metadata.extension());
    }

    @Test
    void testExtractExtension_HiddenFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve(".hidden");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("", metadata.extension());
    }

    @Test
    void testFrom_RejectsDirectory(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> FileMetadata.from(tempDir));
    }
}
