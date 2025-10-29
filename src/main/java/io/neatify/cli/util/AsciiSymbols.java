package io.neatify.cli.util;

/**
 * Utility for Unicode/ASCII symbols depending on terminal support.
 * Uses escaped Unicode sequences to avoid encoding issues.
 */
public final class AsciiSymbols {

    private static boolean useUnicode = detectUnicodeSupport();

    private AsciiSymbols() {
        // Classe utilitaire
    }

    /**
     * Detects if the terminal likely supports Unicode.
     */
    private static boolean detectUnicodeSupport() {
        String encoding = System.getProperty("file.encoding");
        if (encoding != null && encoding.toLowerCase().contains("utf")) {
            return true;
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Recent Windows: Unicode generally supported
            return true;
        }

        // Assume support by default
        return true;
    }

    /** Enables or disables Unicode (forces ASCII if false). */
    public static void setUseUnicode(boolean value) {
        useUnicode = value;
    }

    /** Returns whether Unicode mode is active. */
    public static boolean useUnicode() {
        return useUnicode;
    }

    /** Bullet point symbol. */
    public static String bullet() {
        // \u2022 = '•'
        return useUnicode ? "\u2022" : "-";
    }

    /** Right arrow symbol. */
    public static String arrow() {
        // \u2192 = '→'
        return useUnicode ? "\u2192" : "->";
    }

    /** Multiplication symbol (for duplicates). */
    public static String times() {
        // \u00D7 = '×'
        return useUnicode ? "\u00D7" : "x";
    }

    /** Plus symbol (for "N more..."). */
    public static String plus() {
        return "+";
    }
}
