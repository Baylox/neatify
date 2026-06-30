package io.neatify.cli.ui;

import java.util.Locale;

/**
 * Immutable display preferences (color + Unicode), resolved once at startup.
 *
 * <p>Replaces the former mutable {@code static} flags on {@code Ansi}/{@code AsciiSymbols}.
 * Built in the composition root from auto-detection ({@link #detect()}) and narrowed by the
 * {@code --no-color} / {@code --ascii} CLI flags ({@link #withoutColor()}, {@link #asciiOnly()}).
 */
public record DisplayOptions(boolean color, boolean unicode) {

    /** Auto-detects terminal capabilities from environment variables and JVM encoding. */
    public static DisplayOptions detect() {
        return new DisplayOptions(detectAnsiSupport(), detectUnicodeSupport());
    }

    /** Returns a copy with color disabled (e.g. {@code --no-color}). */
    public DisplayOptions withoutColor() {
        return new DisplayOptions(false, unicode);
    }

    /** Returns a copy restricted to ASCII symbols (e.g. {@code --ascii}). */
    public DisplayOptions asciiOnly() {
        return new DisplayOptions(color, false);
    }

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
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return true;
        }
        // On Unix/Linux/Mac, usually supported when a terminal type is present
        return term != null && !term.isEmpty();
    }

    private static boolean detectUnicodeSupport() {
        if ("true".equalsIgnoreCase(System.getenv("NEATIFY_FORCE_UNICODE"))) {
            return true;
        }
        // Be conservative: only enable Unicode when the JVM reports a UTF-capable encoding.
        // This avoids garbled characters on terminals not configured for UTF-8.
        String encoding = System.getProperty("file.encoding", "").toLowerCase(Locale.ROOT);
        return encoding.contains("utf");
    }
}
