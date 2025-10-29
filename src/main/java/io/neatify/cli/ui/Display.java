package io.neatify.cli.ui;

import io.neatify.cli.AppInfo;

import java.util.Scanner;

/**
 * Utilitaire simplifie pour l'affichage console.
 * Regroupe banniere, sorties formatees et saisie utilisateur.
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

    // ============ Messages formates ============

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

    public static void printLine() {
        println("================================================");
    }

    public static void printSection(String title) {
        println();
        printLine();
        println(title);
        printLine();
    }

    // ============ Banniere ============

    public static void printBanner(AppInfo appInfo) {
        println();
        printLine();
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
            println(appInfo.name() + " - v" + appInfo.version());
            println(appInfo.description());
        }
        println("-- " + signature() + " --");
        printLine();
    }

    private static String signature() {
        String env = System.getenv("NEATIFY_SIGNATURE");
        if (env != null && !env.isBlank()) {
            return env;
        }
        String user = System.getProperty("user.name", "neatify");
        return "by " + user;
    }

    /**
     * Version de compatibilite (depreciee).
     */
    @Deprecated
    public static void printBanner(String version) {
        printBanner(AppInfo.neatify(version));
    }

    // ============ Tableaux et barres ============

    public static void printResultTable(int moved, int skipped, int errors) {
        int total = moved + skipped;

        println();
        println("================================================");
        println("RESUME DE L'OPERATION");
        println("================================================");
        println(String.format("  Fichiers traites   : %-15d", total));
        println(String.format("  Deplaces           : %-15d", moved));
        println(String.format("  Ignores            : %-15d", skipped));
        println(String.format("  Erreurs            : %-15d", errors));
        println("------------------------------------------------");
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
