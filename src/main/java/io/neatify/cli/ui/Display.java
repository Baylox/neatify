package io.neatify.cli.ui;

import io.neatify.cli.AppInfo;

import java.util.Scanner;

/**
 * Lightweight console display utility.
 * Provides banner, formatted outputs and user input helpers.
 */
public final class Display {

    public static final int LINE_WIDTH = 63;
    private static final Scanner scanner = new Scanner(System.in);

    private Display() {
        // Utility class
    }

    // ============ Basic output ============

    public static void print(String text) {
        System.out.print(text);
    }

    public static void println(String text) {
        System.out.println(text);
    }

    public static void println() {
        System.out.println();
    }

    public static void printError(String text) {
        System.err.println(text);
    }

    // ============ Formatted messages ============

    public static void printSuccess(String message) {
        println("[OK] " + message);
    }

    public static void printInfo(String message) {
        println("[i] " + message);
    }

    public static void printWarning(String message) {
        println("[!] " + message);
    }

    public static void printErr(String message) {
        printError("[ERR] " + message);
    }

    // ============ Structure ============

    public static void printLine() { println(line()); }

    public static String line() {
        return "=".repeat(LINE_WIDTH);
    }

    public static void printSection(String title) { printSectionCentered(title); }

    public static void printSectionCentered(String title) {
        println();
        printLine();
        println(center(title));
        printLine();
    }

    public static String center(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        int width = Math.max(1, LINE_WIDTH);
        int padding = Math.max(0, (width - trimmed.length()) / 2);
        return " ".repeat(padding) + trimmed;
    }

    // ============ Banner ============

    public static void printBanner(AppInfo appInfo) {
        println();
        if (io.neatify.cli.util.AsciiSymbols.useUnicode()) {
            println("    ███╗   ██╗███████╗ █████╗ ████████╗██╗███████╗██╗   ██╗");
            println("    ████╗  ██║██╔════╝██╔══██╗╚══██╔══╝██║██╔════╝╚██╗ ██╔╝");
            println("    ██╔██╗ ██║█████╗  ███████║   ██║   ██║█████╗   ╚████╔╝ ");
            println("    ██║╚██╗██║██╔══╝  ██╔══██║   ██║   ██║██╔══╝    ╚██╔╝  ");
            println("    ██║ ╚████║███████╗██║  ██║   ██║   ██║██║        ██║   ");
            println("    ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝╚═╝        ╚═╝   ");
            println();
            println("    " + appInfo.description() + " - v" + appInfo.version());
            println("    ════════════════════════════════════════════════════════");
            println();
        } else {
            // ASCII fallback
            println(center(appInfo.name() + " - v" + appInfo.version()));
            println(center(appInfo.description()));
            println();
        }
        println("    -- " + signature() + " --");
        println();
    }

    private static String signature() {
        String env = System.getenv("NEATIFY_SIGNATURE");
        if (env != null && !env.isBlank()) {
            return env;
        }
        String user = System.getProperty("user.name", "neatify");
        return "by " + user;
    }

    // Note: removed deprecated printBanner(String) overload to keep API lean

    // ============ Tables and bars ============

    public static void printResultTable(int moved, int skipped, int errors) {
        int total = moved + skipped;

        println();
        println(line());
        println(center("OPERATION SUMMARY"));
        println(line());
        println(String.format("  Files processed    : %-15d", total));
        println(String.format("  Moved              : %-15d", moved));
        println(String.format("  Skipped            : %-15d", skipped));
        println(String.format("  Errors             : %-15d", errors));
        println("-".repeat(LINE_WIDTH));
    }

    // ============ User input ============

    public static String readInput(String prompt) {
        return readInput(prompt, null);
    }

    public static String readInput(String prompt, String defaultValue) {
        String fullPrompt = defaultValue != null && !defaultValue.isEmpty()
            ? prompt + " [" + defaultValue + "]: "
            : prompt + ": ";

        print(fullPrompt);
        String input = scanner.nextLine().trim();
        return input.isEmpty() && defaultValue != null ? defaultValue : input;
    }

    public static void waitForEnter() {
        print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // ============ Safer Banner (Unicode via \+ env override) ============

    /**
     * Unicode banner encoded with sequences, with ASCII fallback.
     * Respects env override NEATIFY_FORCE_UNICODE=true to force Unicode rendering.
     */
    public static void printBannerSafe(AppInfo appInfo) {
        println();
        if (io.neatify.cli.util.AsciiSymbols.useUnicode()) {
            String title = appInfo.name() + " - v" + appInfo.version();
            String desc = appInfo.description();
            int inner = Math.max(title.length(), desc.length());
            int pad = 2; // spaces padding left/right
            int width = inner + pad * 2;
            String h = "\u2550".repeat(width); // box drawing double horizontal
            println("  \u2554" + h + "\u2557"); // top left/right corners
            println("  \u2551" + padCenter(title, width) + "\u2551");
            println("  \u2551" + padCenter(desc, width) + "\u2551");
            println("  \u255A" + h + "\u255D"); // bottom left/right corners
        } else {
            println(center(appInfo.name() + " - v" + appInfo.version()));
            println(center(appInfo.description()));
        }
        println("    -- " + signature() + " --");
        println();
    }

    // Centers text inside a fixed width using spaces (no ANSI/unicode), returns exactly width chars
    private static String padCenter(String s, int width) {
        if (s == null) s = "";
        String t = s.trim();
        if (t.length() >= width) return t.substring(0, Math.min(width, t.length()));
        int left = (width - t.length()) / 2;
        int right = width - t.length() - left;
        return " ".repeat(left) + t + " ".repeat(right);
    }
}
