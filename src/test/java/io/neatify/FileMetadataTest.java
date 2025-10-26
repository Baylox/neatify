package io.neatify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour FileMetadata.
 */
class FileMetadataTest {

    @Test
    void testExtractExtension_WithStandardExtension(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.jpg");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("jpg", metadata.extension());
    }

    @Test
    void testExtractExtension_WithMultipleDots(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("archive.tar.gz");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("gz", metadata.extension());
    }

    @Test
    void testExtractExtension_WithNoExtension(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("README");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("", metadata.extension());
        assertTrue(metadata.hasNoExtension());
    }

    @Test
    void testExtractExtension_WithUppercase(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.PNG");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("png", metadata.extension());
    }

    @Test
    void testFileSize(@TempDir Path tempDir) throws IOException {
        String content = "Hello World!";
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, content);

        FileMetadata metadata = FileMetadata.from(file);

        assertTrue(metadata.sizeInBytes() > 0);
        assertEquals(content.getBytes().length, metadata.sizeInBytes());
    }

    @Test
    void testFormattedSize_Bytes(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("small.txt");
        Files.writeString(file, "Hi");

        FileMetadata metadata = FileMetadata.from(file);

        assertTrue(metadata.formattedSize().contains("B"));
    }

    @Test
    void testFormattedSize_Kilobytes(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("medium.txt");
        Files.writeString(file, "x".repeat(2048));

        FileMetadata metadata = FileMetadata.from(file);

        assertTrue(metadata.formattedSize().contains("KB"));
    }

    @Test
    void testFileName(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("document.pdf");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("document.pdf", metadata.fileName());
    }

    @Test
    void testLastModified(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertNotNull(metadata.lastModified());
    }

    @Test
    void testFrom_WithNullPath() {
        assertThrows(NullPointerException.class, () -> FileMetadata.from(null));
    }

    @Test
    void testFrom_WithNonExistentFile(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does-not-exist.txt");

        assertThrows(IllegalArgumentException.class, () -> FileMetadata.from(nonExistent));
    }

    @Test
    void testFrom_WithDirectory(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> FileMetadata.from(tempDir));
    }

    @Test
    void testHasNoExtension_True(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("README");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertTrue(metadata.hasNoExtension());
    }

    @Test
    void testHasNoExtension_False(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertFalse(metadata.hasNoExtension());
    }

    @Test
    void testExtension_DotAtStart(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve(".hidden");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("", metadata.extension());
    }

    @Test
    void testExtension_DotAtEnd(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.");
        Files.writeString(file, "test");

        FileMetadata metadata = FileMetadata.from(file);

        assertEquals("", metadata.extension());
    }
}
