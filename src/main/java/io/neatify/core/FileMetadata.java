package io.neatify.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Représente les métadonnées d'un fichier.
 * Record simple et immutable pour stocker les informations essentielles.
 */
public record FileMetadata(
    Path path,
    String extension,
    long sizeInBytes,
    LocalDateTime lastModified
) {

    /**
     * Crée un FileMetadata à partir d'un chemin de fichier.
     *
     * @param filePath le chemin du fichier à analyser
     * @return les métadonnées du fichier
     * @throws IOException si le fichier n'existe pas ou n'est pas accessible
     */
    public static FileMetadata from(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "Le chemin du fichier ne peut pas être null");

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Le chemin doit pointer vers un fichier régulier : " + filePath);
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
     * Extrait l'extension d'un fichier (sans le point).
     *
     * @param filePath le chemin du fichier
     * @return l'extension en minuscules, ou une chaîne vide si pas d'extension
     */
    private static String extractExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }

        return ""; // Pas d'extension
    }

    /**
     * @return le nom du fichier (avec extension)
     */
    public String fileName() {
        return path.getFileName().toString();
    }

    /**
     * @return true si le fichier n'a pas d'extension
     */
    public boolean hasNoExtension() {
        return extension.isEmpty();
    }

    /**
     * Formate la taille en une chaîne lisible (KB, MB, GB).
     */
    public String formattedSize() {
        if (sizeInBytes < 1024) return sizeInBytes + " B";
        if (sizeInBytes < 1024 * 1024) return String.format("%.2f KB", sizeInBytes / 1024.0);
        if (sizeInBytes < 1024 * 1024 * 1024) return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024));
        return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024));
    }
}
