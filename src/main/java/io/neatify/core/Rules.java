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
        Map<String, String> defaults = new HashMap<>();

        // Images
        defaults.put("jpg", "Images");
        defaults.put("jpeg", "Images");
        defaults.put("png", "Images");
        defaults.put("gif", "Images");
        defaults.put("bmp", "Images");
        defaults.put("svg", "Images");
        defaults.put("webp", "Images");
        defaults.put("ico", "Images");

        // Documents
        defaults.put("pdf", "Documents");
        defaults.put("doc", "Documents");
        defaults.put("docx", "Documents");
        defaults.put("txt", "Documents");
        defaults.put("odt", "Documents");
        defaults.put("rtf", "Documents");
        defaults.put("md", "Documents");

        // Tableurs
        defaults.put("xls", "Documents/Tableurs");
        defaults.put("xlsx", "Documents/Tableurs");
        defaults.put("csv", "Documents/Tableurs");
        defaults.put("ods", "Documents/Tableurs");

        // Présentations
        defaults.put("ppt", "Documents/Presentations");
        defaults.put("pptx", "Documents/Presentations");
        defaults.put("odp", "Documents/Presentations");

        // Archives
        defaults.put("zip", "Archives");
        defaults.put("rar", "Archives");
        defaults.put("7z", "Archives");
        defaults.put("tar", "Archives");
        defaults.put("gz", "Archives");
        defaults.put("bz2", "Archives");

        // Vidéos
        defaults.put("mp4", "Videos");
        defaults.put("avi", "Videos");
        defaults.put("mkv", "Videos");
        defaults.put("mov", "Videos");
        defaults.put("wmv", "Videos");
        defaults.put("flv", "Videos");
        defaults.put("webm", "Videos");

        // Audio
        defaults.put("mp3", "Musique");
        defaults.put("wav", "Musique");
        defaults.put("flac", "Musique");
        defaults.put("aac", "Musique");
        defaults.put("ogg", "Musique");
        defaults.put("m4a", "Musique");

        // Code source
        defaults.put("java", "Code");
        defaults.put("py", "Code");
        defaults.put("js", "Code");
        defaults.put("ts", "Code");
        defaults.put("cpp", "Code");
        defaults.put("c", "Code");
        defaults.put("h", "Code");
        defaults.put("cs", "Code");
        defaults.put("go", "Code");
        defaults.put("rs", "Code");
        defaults.put("php", "Code");
        defaults.put("rb", "Code");
        defaults.put("html", "Code");
        defaults.put("css", "Code");
        defaults.put("json", "Code");
        defaults.put("xml", "Code");
        defaults.put("yaml", "Code");
        defaults.put("yml", "Code");

        // Exécutables
        defaults.put("exe", "Executables");
        defaults.put("msi", "Executables");
        defaults.put("dmg", "Executables");
        defaults.put("pkg", "Executables");
        defaults.put("deb", "Executables");
        defaults.put("rpm", "Executables");

        // Autres
        defaults.put("iso", "Images_Disque");
        defaults.put("torrent", "Torrents");

        return Collections.unmodifiableMap(defaults);
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
