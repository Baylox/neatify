package io.neatify.cli.util;

/**
 * Utilitaire pour les symboles Unicode/ASCII selon le support du terminal.
 * Permet un fallback automatique vers ASCII si Unicode n'est pas supporté.
 */
public final class AsciiSymbols {

    private static boolean useUnicode = detectUnicodeSupport();

    private AsciiSymbols() {
        // Classe utilitaire
    }

    /**
     * Détecte si le terminal supporte Unicode.
     */
    private static boolean detectUnicodeSupport() {
        // Vérifier l'encodage par défaut
        String encoding = System.getProperty("file.encoding");
        if (encoding != null && encoding.toLowerCase().contains("utf")) {
            return true;
        }

        // Sur Windows PowerShell supporte UTF-8
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Activer par défaut sur Windows moderne
            return true;
        }

        // Sur Unix/Linux/Mac, généralement supporté
        return true;
    }

    /**
     * Active ou désactive Unicode (force ASCII si false).
     */
    public static void setUseUnicode(boolean value) {
        useUnicode = value;
    }

    /**
     * Symbole de puce (bullet point).
     */
    public static String bullet() {
        return useUnicode ? "•" : "-";
    }

    /**
     * Symbole de flèche droite.
     */
    public static String arrow() {
        return useUnicode ? "→" : ">";
    }

    /**
     * Symbole de multiplication (pour les duplicatas).
     */
    public static String times() {
        return useUnicode ? "×" : "x";
    }

    /**
     * Symbole de plus (pour "N autres...").
     */
    public static String plus() {
        return "+";
    }
}
