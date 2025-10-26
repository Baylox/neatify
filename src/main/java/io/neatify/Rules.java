package io.neatify;

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
 * Les règles sont définies dans un fichier .properties au format : extension=DossierCible
 */
public final class Rules {

    private Rules() {
        // Classe utilitaire, pas d'instanciation
    }

    /**
     * Charge les règles depuis un fichier .properties.
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

        if (!Files.exists(propertiesFile)) {
            throw new IOException("Fichier de règles introuvable : " + propertiesFile);
        }

        if (!Files.isRegularFile(propertiesFile)) {
            throw new IllegalArgumentException("Le chemin doit pointer vers un fichier : " + propertiesFile);
        }

        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(propertiesFile)) {
            props.load(input);
        }

        Map<String, String> rules = new HashMap<>();

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key).trim();

            if (key.isBlank()) {
                throw new IllegalArgumentException("Extension vide détectée dans les règles");
            }

            if (value.isBlank()) {
                throw new IllegalArgumentException("Dossier cible vide pour l'extension : " + key);
            }

            // Normaliser l'extension (sans point, en minuscules)
            String normalizedKey = key.trim().toLowerCase().replaceFirst("^\\.", "");

            // Sanitize le nom de dossier (éviter les caractères dangereux)
            String sanitizedValue = sanitizeFolderName(value);

            rules.put(normalizedKey, sanitizedValue);
        }

        if (rules.isEmpty()) {
            throw new IllegalArgumentException("Aucune règle valide trouvée dans le fichier : " + propertiesFile);
        }

        return Collections.unmodifiableMap(rules);
    }

    /**
     * Nettoie un nom de dossier pour éviter les caractères interdits.
     * Note : le slash (/) est conservé pour permettre les sous-dossiers.
     *
     * @param folderName le nom du dossier à nettoyer
     * @return le nom nettoyé
     */
    private static String sanitizeFolderName(String folderName) {
        // Supprime les caractères interdits sur Windows/Linux : < > : " \ | ? *
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
