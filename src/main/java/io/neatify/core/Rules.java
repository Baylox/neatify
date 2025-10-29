package io.neatify.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads and validates file-organization rules.
 * Rules are defined in a .properties file as: extension=TargetFolder
 */
public final class Rules {

    private Rules() {
        // Utility class, no instantiation
    }

    /**
     * Returns a set of built-in sensible default rules.
     *
     * @return immutable Map [extension → target folder]
     */
    public static Map<String, String> getDefaults() {
        return Collections.unmodifiableMap(DefaultRules.create());
    }

    /**
     * Loads rules from a .properties file.
     *
     * Expected format:
     * <pre>
     * jpg=Images
     * png=Images
     * pdf=Documents
     * txt=Documents
     * mp4=Videos
     * </pre>
     *
     * @param propertiesFile path to rules.properties
     * @return immutable Map [extension -> target folder]
     * @throws IOException if file does not exist or cannot be read
     * @throws IllegalArgumentException if the format is invalid
     */
    public static Map<String, String> load(Path propertiesFile) throws IOException {
        Objects.requireNonNull(propertiesFile, "Rules file path cannot be null");
        validateFileExists(propertiesFile);

        Properties props = loadProperties(propertiesFile);
        Map<String, String> rules = parseRules(props);

        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No valid rules found in file: " + propertiesFile);
        }

        return Collections.unmodifiableMap(rules);
    }

    /**
     * Validates that the file exists and is a regular file.
     */
    private static void validateFileExists(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("Rules file not found: " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Path must point to a regular file: " + file);
        }
    }

    /**
     * Loads properties from file.
     */
    private static Properties loadProperties(Path file) throws IOException {
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            props.load(input);
        }
        return props;
    }

    /**
     * Parses properties and builds the rules map.
     */
    private static Map<String, String> parseRules(Properties props) {
        Map<String, String> rules = new HashMap<>();

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key).trim();
            validateRule(key, value);

            String normalizedKey = normalizeExtension(key);
            String sanitizedValue = sanitizeFolderName(value);

            rules.put(normalizedKey, sanitizedValue);
        }

        return rules;
    }

    /**
     * Validates a single rule (extension + folder).
     */
    private static void validateRule(String key, String value) {
        if (key.isBlank()) {
            throw new IllegalArgumentException("Empty extension found in rules");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Empty target folder for extension: " + key);
        }
    }

    /**
     * Normalizes an extension (no dot, lowercase).
     */
    private static String normalizeExtension(String extension) {
        return extension.trim().toLowerCase().replaceFirst("^\\.", "");
    }

    /**
     * Sanitizes a folder name to avoid illegal characters and path traversal.
     * Note: slash (/) is kept to allow subfolders.
     *
     * @param folderName folder name to sanitize
     * @return sanitized name
     * @throws IllegalArgumentException if the path attempts traversal
     */
    private static String sanitizeFolderName(String folderName) {
        try {
            PathSecurity.validateRelativeSubpath(folderName);
        } catch (SecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        // Remove illegal characters on Windows/Linux: < > : " \ | ? *
        // Slash (/) is kept for subfolders
        return folderName.replaceAll("[<>:\"\\\\|?*]", "_");
    }

    /**
     * Finds the target folder for a given extension.
     *
     * @param rules rules map
     * @param extension extension to look for (no dot, lowercase)
     * @return target folder, or null if no rule matches
     */
    public static String getTargetFolder(Map<String, String> rules, String extension) {
        Objects.requireNonNull(rules, "Rules cannot be null");

        if (extension == null || extension.isBlank()) {
            return null;
        }

        return rules.get(extension.toLowerCase());
    }
}
