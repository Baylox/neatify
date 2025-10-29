package io.neatify.cli.util;

/**
 * ANSI codes utility (terminal colors).
 * Auto-detects ANSI support with ability to disable.
 */
public final class Ansi {

    private static boolean enabled = detectAnsiSupport();

    private Ansi() {
        // Utility class
    }

    /**
     * Detects whether the terminal supports ANSI codes.
     * Checks environment variables and standard output hints.
     */
    private static boolean detectAnsiSupport() {
        // If NO_COLOR is set, disable colors
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        // If TERM is defined and equals "dumb", disable
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) {
            return false;
        }

        // Windows 10+ generally supports ANSI
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return true;
        }

        // On Unix/Linux/Mac, usually supported
        return term != null && !term.isEmpty();
    }

    /**
     * Enables or disables ANSI codes.
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }


    /**
     * Applies a color to text if ANSI is enabled.
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

    // Public helpers
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
