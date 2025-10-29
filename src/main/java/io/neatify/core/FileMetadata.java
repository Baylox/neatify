package io.neatify.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Represents file metadata.
 * Simple immutable record for essential information.
 */
public record FileMetadata(
    Path path,
    String extension,
    long sizeInBytes,
    LocalDateTime lastModified
) {

    /**
     * Creates FileMetadata from a file path.
     *
     * @param filePath file path to analyze
     * @return metadata for the file
     * @throws IOException if the file does not exist or is not accessible
     */
    public static FileMetadata from(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "File path cannot be null");

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Path must point to a regular file: " + filePath);
        }

        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

        String ext = extractExtension(filePath);
        long size = attrs.size();
        LocalDateTime modified = LocalDateTime.ofInstant(
            attrs.lastModifiedTime().toInstant(),
            ZoneId.systemDefault()
        );

        return new FileMetadata(filePath, ext, size, modified);
    }

    /**
     * Extracts a file extension (without the dot).
     *
     * @param filePath file path
     * @return lowercase extension or empty string if none
     */
    private static String extractExtension(Path filePath) {
        return extensionOf(filePath.getFileName().toString());
    }

    /**
     * Extracts the extension from a file name.
     *
     * @param fileName file name
     * @return lowercase extension or empty string if none
     */
    public static String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * @return file name (with extension)
     */
    public String fileName() {
        return path.getFileName().toString();
    }

    /**
     * @return true if the file has no extension
     */
    public boolean hasNoExtension() {
        return extension.isEmpty();
    }

    /**
     * Formats the size into a human-readable string (KB, MB, GB).
     */
    public String formattedSize() {
        if (sizeInBytes < 1024) return sizeInBytes + " B";
        if (sizeInBytes < 1024 * 1024) return String.format("%.2f KB", sizeInBytes / 1024.0);
        if (sizeInBytes < 1024 * 1024 * 1024) return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024));
        return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024));
    }
}
