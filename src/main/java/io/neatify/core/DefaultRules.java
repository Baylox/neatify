package io.neatify.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines default rules for file organization.
 */
final class DefaultRules {

    private DefaultRules() {
        // Utility class
    }

    static Map<String, String> create() {
        Map<String, String> rules = new HashMap<>();

        addRules(rules, "Images", "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico");

        addRules(rules, "Documents", "pdf", "doc", "docx", "txt", "odt", "rtf", "md");
        addRules(rules, "Documents/Spreadsheets", "xls", "xlsx", "csv", "ods");
        addRules(rules, "Documents/Presentations", "ppt", "pptx", "odp");

        addRules(rules, "Archives", "zip", "rar", "7z", "tar", "gz", "bz2");

        addRules(rules, "Videos", "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm");
        addRules(rules, "Music", "mp3", "wav", "flac", "aac", "ogg", "m4a");

        addRules(rules, "Code", "java", "py", "js", "ts", "cpp", "c", "h", "cs", "go",
                 "rs", "php", "rb", "html", "css", "json", "xml", "yaml", "yml");

        addRules(rules, "Executables", "exe", "msi", "dmg", "pkg", "deb", "rpm");

        rules.put("iso", "DiskImages");
        rules.put("torrent", "Torrents");

        return rules;
    }

    private static void addRules(Map<String, String> map, String folder, String... extensions) {
        for (String ext : extensions) {
            map.put(ext, folder);
        }
    }
}
