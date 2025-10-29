package io.neatify.cli.ui;

import io.neatify.cli.AppInfo;

import java.util.Scanner;

/**
 * Utilitaire simplifié pour l'affichage console.
 * Fusionne l'ancien BannerRenderer, ConsoleOutput et ConsoleUI.
 */
public final class Display {

    private static final Scanner scanner = new Scanner(System.in);

    private Display() {
        // Classe utilitaire
    }

    // ============ Affichage de base ============

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

    // ============ Messages formatés ============

    public static void printSuccess(String message) {
        println("[✓] " + message);
    }

    public static void printInfo(String message) {
        println("[i] " + message);
    }

    public static void printWarning(String message) {
        println("[!] " + message);
    }

    public static void printErr(String message) {
        printError("[✗] " + message);
    }

    // ============ Structure ============

    public static void printLine() {
        println("================================================");
    }

    public static void printSection(String title) {
        println();
        printLine();
        println(title);
        printLine();
    }

    // ============ Banner ============

    public static void printBanner(AppInfo appInfo) {
        print("\n" +
                "    ███╗   ██╗███████╗ █████╗ ████████╗██╗███████╗██╗   ██╗\n" +
                "    ████╗  ██║██╔════╝██╔══██╗╚══██╔══╝██║██╔════╝╚██╗ ██╔╝\n" +
                "    ██╔██╗ ██║█████╗  ███████║   ██║   ██║█████╗   ╚████╔╝ \n" +
                "    ██║╚██╗██║██╔══╝  ██╔══██║   ██║   ██║██╔══╝    ╚██╔╝  \n" +
                "    ██║ ╚████║███████╗██║  ██║   ██║   ██║██║        ██║   \n" +
                "    ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝╚═╝        ╚═╝   \n" +
                "\n" +
                "    " + appInfo.description() + " - v" + appInfo.version() + "\n" +
                "    ════════════════════════════════════════════════════════\n" +
                "\n");
    }

    /**
     * Affiche le banner (version dépréciée pour compatibilité).
     * @deprecated Utilisez printBanner(AppInfo) à la place
     */
    @Deprecated
    public static void printBanner(String version) {
        printBanner(AppInfo.neatify(version));
    }

    // ============ Tableaux et barres ============

    public static void printResultTable(int moved, int skipped, int errors) {
        int total = moved + skipped;

        println();
        println("    ┌─────────────────────────────────────────┐");
        println("    │           RESUME DE L'OPERATION         │");
        println("    ├─────────────────────────────────────────┤");
        println(String.format("    │  Fichiers traites    │  %-15d │", total));
        println(String.format("    │  Deplaces            │  %-15d │", moved));
        println(String.format("    │  Ignores             │  %-15d │", skipped));
        println(String.format("    │  Erreurs             │  %-15d │", errors));
        println("    └─────────────────────────────────────────┘");
    }

    public static void printProgressBar(int current, int total, int width) {
        if (total == 0) return;

        int percentage = (current * 100) / total;
        int filled = (current * width) / total;
        int empty = width - filled;

        StringBuilder bar = new StringBuilder("[");
        bar.append("█".repeat(filled));
        bar.append("░".repeat(empty));
        bar.append("] ").append(percentage).append("% (")
            .append(current).append("/").append(total).append(")");

        println(bar.toString());
    }

    // ============ Input utilisateur ============

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
        print("\nAppuyez sur Entree pour continuer...");
        scanner.nextLine();
    }
}
