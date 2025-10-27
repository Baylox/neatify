package io.neatify.cli.util;

/**
 * Utilitaire pour les codes ANSI (couleurs terminal).
 * Détection automatique du support ANSI avec possibilité de désactivation.
 */
public final class Ansi {

    private static boolean enabled = detectAnsiSupport();

    private Ansi() {
        // Classe utilitaire
    }

    /**
     * Détecte si le terminal supporte les codes ANSI.
     * Vérifie les variables d'environnement et la sortie standard.
     */
    private static boolean detectAnsiSupport() {
        // Si NO_COLOR est défini, désactiver les couleurs
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        // Si TERM est défini et contient "dumb", désactiver
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) {
            return false;
        }

        // Sur Windows 10+, ANSI est supporté par défaut depuis 2016
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows Terminal, ConEmu, ou PowerShell moderne
            String wt = System.getenv("WT_SESSION");
            String conemu = System.getenv("ConEmuANSI");

            // Activer par défaut sur Windows 10+ (la plupart des terminaux modernes)
            return true;
        }

        // Sur Unix/Linux/Mac, généralement supporté
        return term != null && !term.isEmpty();
    }

    /**
     * Active ou désactive les codes ANSI.
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }


    /**
     * Applique une couleur au texte si ANSI est activé.
     */
    private static String colorize(String text, String code) {
        if (!enabled) {
            return text;
        }
        return code + text + RESET;
    }

    // Codes ANSI
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String DIM = "\u001B[2m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";

    // Méthodes publiques
    public static String cyan(String text) {
        return colorize(text, CYAN);
    }

    public static String yellow(String text) {
        return colorize(text, YELLOW);
    }

    public static String dim(String text) {
        return colorize(text, DIM);
    }

    public static String green(String text) {
        return colorize(text, GREEN);
    }

    public static String red(String text) {
        return colorize(text, RED);
    }
}
