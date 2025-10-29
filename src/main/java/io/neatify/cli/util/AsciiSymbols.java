package io.neatify.cli.util;

/**
 * Utilitaire pour les symboles Unicode/ASCII selon le support du terminal.
 * Utilise des sequences Unicode echappees pour eviter les problemes d'encodage.
 */
public final class AsciiSymbols {

    private static boolean useUnicode = detectUnicodeSupport();

    private AsciiSymbols() {
        // Classe utilitaire
    }

    /**
     * Detecte si le terminal supporte Unicode.
     */
    private static boolean detectUnicodeSupport() {
        String encoding = System.getProperty("file.encoding");
        if (encoding != null && encoding.toLowerCase().contains("utf")) {
            return true;
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows recent: Unicode generalement supporte
            return true;
        }

        // Par defaut, on suppose support present
        return true;
    }

    /** Active ou desactive Unicode (force ASCII si false). */
    public static void setUseUnicode(boolean value) {
        useUnicode = value;
    }

    /** Indique si le mode Unicode est actif. */
    public static boolean useUnicode() {
        return useUnicode;
    }

    /** Symbole de puce (bullet point). */
    public static String bullet() {
        // \u2022 = '•'
        return useUnicode ? "\u2022" : "-";
    }

    /** Symbole de fleche droite. */
    public static String arrow() {
        // \u2192 = '→'
        return useUnicode ? "\u2192" : "->";
    }

    /** Symbole de multiplication (pour les duplicatas). */
    public static String times() {
        // \u00D7 = '×'
        return useUnicode ? "\u00D7" : "x";
    }

    /** Symbole de plus (pour "N autres..."). */
    public static String plus() {
        return "+";
    }
}
