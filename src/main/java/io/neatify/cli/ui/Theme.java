package io.neatify.cli.ui;

/**
 * Immutable provider of colored text and Unicode/ASCII symbols.
 *
 * <p>Replaces the static {@code Ansi} and {@code AsciiSymbols} utilities, which carried mutable
 * process-wide flags. A {@code Theme} is built once from {@link DisplayOptions} in the
 * composition root and passed explicitly to the renderers that need it.
 */
public final class Theme {

    // ANSI codes (built from the ESC control character to keep the source ASCII-only)
    private static final String ESC = Character.toString((char) 27);
    private static final String RESET = ESC + "[0m";
    private static final String CYAN = ESC + "[36m";
    private static final String YELLOW = ESC + "[33m";
    private static final String DIM = ESC + "[2m";
    private static final String GREEN = ESC + "[32m";
    private static final String RED = ESC + "[31m";

    // Unicode symbols (code points keep the source ASCII-only, as the original code did)
    private static final String BULLET = Character.toString(0x2022); // U+2022 bullet
    private static final String ARROW = Character.toString(0x2192); // U+2192 arrow
    private static final String TIMES = Character.toString(0x00D7); // U+00D7 times

    private final boolean color;
    private final boolean unicode;

    public Theme(DisplayOptions options) {
        this.color = options.color();
        this.unicode = options.unicode();
    }

    /** A plain theme: no color, ASCII only. Convenient for tests and non-tty output. */
    public static Theme plain() {
        return new Theme(new DisplayOptions(false, false));
    }

    public boolean unicode() {
        return unicode;
    }

    // ============ Colors ============

    private String colorize(String text, String code) {
        return color ? code + text + RESET : text;
    }

    public String cyan(String text) {
        return colorize(text, CYAN);
    }

    public String yellow(String text) {
        return colorize(text, YELLOW);
    }

    public String dim(String text) {
        return colorize(text, DIM);
    }

    public String green(String text) {
        return colorize(text, GREEN);
    }

    public String red(String text) {
        return colorize(text, RED);
    }

    // ============ Symbols ============

    /** Bullet point symbol ('*' rendered as a dot, or '-' in ASCII mode). */
    public String bullet() {
        return unicode ? BULLET : "-";
    }

    /** Right arrow symbol ('->' in ASCII mode). */
    public String arrow() {
        return unicode ? ARROW : "->";
    }

    /** Multiplication symbol for duplicates ('x' in ASCII mode). */
    public String times() {
        return unicode ? TIMES : "x";
    }

    /** Plus symbol (for "N more..."). */
    public String plus() {
        return "+";
    }
}
