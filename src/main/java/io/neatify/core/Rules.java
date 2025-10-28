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
 * Gère le chargement et la validation des règles de rangement.
 * Les règles sont définies dans un fichier.properties au format : extension=DossierCible
 */
public final class Rules {

    private Rules() {
        // Classe utilitaire, pas d'instanciation
    }

    /**
     * Retourne un ensemble de règles par défaut intégrées.
     *
     * @return une Map immuable [extension → dossier cible] avec des règles sensées
     */
    public static Map<String, String> getDefaults() {
        return Collections.unmodifiableMap(DefaultRules.create());
    }

    /**
     * Charge les règles depuis un fichier.properties.
     *
     * Format attendu :
     * <pre>
     * jpg=Images
     * png=Images
     * pdf=Documents
     * txt=Documents
     * mp4=Videos
     * </pre>
     *
     * @param propertiesFile chemin vers le fichier rules.properties
     * @return une Map immuable [extension -> dossier cible]
     * @throws IOException si le fichier n'existe pas ou ne peut pas être lu
     * @throws IllegalArgumentException si le format est invalide
     */
    public static Map<String, String> load(Path propertiesFile) throws IOException {
        Objects.requireNonNull(propertiesFile, "Le chemin du fichier de règles ne peut pas être null");
        validateFileExists(propertiesFile);

        Properties props = loadProperties(propertiesFile);
        Map<String, String> rules = parseRules(props);

        if (rules.isEmpty()) {
            throw new IllegalArgumentException("Aucune règle valide trouvée dans le fichier : " + propertiesFile);
        }

        return Collections.unmodifiableMap(rules);
    }

    /**
     * Valide que le fichier existe et est un fichier régulier.
     */
    private static void validateFileExists(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("Fichier de règles introuvable : " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Le chemin doit pointer vers un fichier : " + file);
        }
    }

    /**
     * Charge les propriétés depuis le fichier.
     */
    private static Properties loadProperties(Path file) throws IOException {
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            props.load(input);
        }
        return props;
    }

    /**
     * Parse les propriétés et construit la map de règles.
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
     * Valide une règle (extension + dossier).
     */
    private static void validateRule(String key, String value) {
        if (key.isBlank()) {
            throw new IllegalArgumentException("Extension vide détectée dans les règles");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Dossier cible vide pour l'extension : " + key);
        }
    }

    /**
     * Normalise une extension (sans point, en minuscules).
     */
    private static String normalizeExtension(String extension) {
        return extension.trim().toLowerCase().replaceFirst("^\\.", "");
    }

    /**
     * Nettoie un nom de dossier pour éviter les caractères interdits et les path traversal.
     * Note : le slash (/) est conservé pour permettre les sous-dossiers.
     *
     * @param folderName le nom du dossier à nettoyer
     * @return le nom nettoyé
     * @throws IllegalArgumentException si le chemin contient des tentatives de path traversal
     */
    private static String sanitizeFolderName(String folderName) {
        // 1. Bloquer explicitement ".." pour éviter les path traversal
        if (folderName.contains("..")) {
            throw new IllegalArgumentException("Path traversal interdit (\"..\" détecté) : " + folderName);
        }

        // 2. Bloquer les chemins absolus Unix/Linux (commence par /)
        if (folderName.startsWith("/")) {
            throw new IllegalArgumentException("Chemin absolu Unix interdit : " + folderName);
        }

        // 3. Bloquer les chemins absolus Windows (ex: C:\, D:\)
        if (folderName.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Chemin absolu Windows interdit : " + folderName);
        }

        // 4. Supprime les caractères interdits sur Windows/Linux : < > : " \ | ? *
        // Le slash (/) est conservé pour les sous-dossiers
        return folderName.replaceAll("[<>:\"\\\\|?*]", "_");
    }

    /**
     * Trouve le dossier cible pour une extension donnée.
     *
     * @param rules la map de règles
     * @param extension l'extension à chercher (sans point, minuscules)
     * @return le dossier cible, ou null si aucune règle ne correspond
     */
    public static String getTargetFolder(Map<String, String> rules, String extension) {
        Objects.requireNonNull(rules, "Les règles ne peuvent pas être null");

        if (extension == null || extension.isBlank()) {
            return null;
        }

        return rules.get(extension.toLowerCase());
    }
}
