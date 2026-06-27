package io.neatify.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileMetadata.formattedSize().
 */
class FileMetadataFormattedSizeTest {

    /** Creates a file of exactly {@code size} bytes filled with zeros. */
    private FileMetadata metadataOfSize(@TempDir Path dir, long size) throws IOException {
        Path file = dir.resolve("test_" + size + ".bin");
        Files.write(file, new byte[(int) size]);
        return FileMetadata.from(file);
    }

    @Test
    void formattedSize_zeroBytes(@TempDir Path dir) throws IOException {
        String formatted = metadataOfSize(dir, 0).formattedSize();
        assertEquals("0 B", formatted);
    }

    @Test
    void formattedSize_under1KB(@TempDir Path dir) throws IOException {
        String formatted = metadataOfSize(dir, 512).formattedSize();
        assertTrue(formatted.endsWith(" B"), "Expected bytes unit, got: " + formatted);
        assertEquals("512 B", formatted);
    }

    @Test
    void formattedSize_between1KBand1MB(@TempDir Path dir) throws IOException {
        // 2048 bytes = 2.00 KB
        String formatted = metadataOfSize(dir, 2048).formattedSize();
        assertTrue(formatted.endsWith(" KB"), "Expected KB unit, got: " + formatted);
        assertEquals("2.00 KB", formatted);
    }

    @Test
    void formattedSize_between1MBand1GB(@TempDir Path dir) throws IOException {
        // 2 * 1024 * 1024 = 2 MB
        long twoMb = 2L * 1024 * 1024;
        String formatted = metadataOfSize(dir, twoMb).formattedSize();
        assertTrue(formatted.endsWith(" MB"), "Expected MB unit, got: " + formatted);
        assertEquals("2.00 MB", formatted);
    }

    @Test
    void formattedSize_exactlyOneKB(@TempDir Path dir) throws IOException {
        String formatted = metadataOfSize(dir, 1024).formattedSize();
        assertEquals("1.00 KB", formatted);
    }
}
