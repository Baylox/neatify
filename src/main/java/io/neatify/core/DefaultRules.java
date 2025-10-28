package io.neatify.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Définit les règles par défaut pour l'organisation des fichiers.
 */
final class DefaultRules {

    private DefaultRules() {
        // Classe utilitaire
    }

    static Map<String, String> create() {
        Map<String, String> rules = new HashMap<>();

        addRules(rules, "Images", "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico");

        addRules(rules, "Documents", "pdf", "doc", "docx", "txt", "odt", "rtf", "md");
        addRules(rules, "Documents/Tableurs", "xls", "xlsx", "csv", "ods");
        addRules(rules, "Documents/Presentations", "ppt", "pptx", "odp");

        addRules(rules, "Archives", "zip", "rar", "7z", "tar", "gz", "bz2");

        addRules(rules, "Videos", "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm");
        addRules(rules, "Musique", "mp3", "wav", "flac", "aac", "ogg", "m4a");

        addRules(rules, "Code", "java", "py", "js", "ts", "cpp", "c", "h", "cs", "go",
                 "rs", "php", "rb", "html", "css", "json", "xml", "yaml", "yml");

        addRules(rules, "Executables", "exe", "msi", "dmg", "pkg", "deb", "rpm");

        rules.put("iso", "Images_Disque");
        rules.put("torrent", "Torrents");

        return rules;
    }

    private static void addRules(Map<String, String> map, String folder, String... extensions) {
        for (String ext : extensions) {
            map.put(ext, folder);
        }
    }
}
