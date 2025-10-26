package io.neatify.cli;

import java.util.Scanner;

/**
 * Utilitaires pour l'interface console.
 */
public final class ConsoleUI {

    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleUI() {
        // Classe utilitaire
    }

    public static void printBanner(String version) {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║            NEATIFY v" + version + "                 ║");
        System.out.println("║   Outil de rangement automatique          ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
    }

    public static void printSection(String title) {
        System.out.println();
        printLine();
        System.out.println(title);
        printLine();
    }

    public static void printLine() {
        System.out.println("================================================");
    }

    public static void printSuccess(String message) {
        System.out.println("[✓] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[i] " + message);
    }

    public static void printWarning(String message) {
        System.out.println("[!] " + message);
    }

    public static void printError(String message) {
        System.err.println("[✗] " + message);
    }

    public static String readInput(String prompt) {
        return readInput(prompt, null);
    }

    public static String readInput(String prompt, String defaultValue) {
        if (defaultValue != null) {
            System.out.print(prompt + " [" + defaultValue + "]: ");
        } else {
            System.out.print(prompt + ": ");
        }

        String input = scanner.nextLine().trim();
        return input.isEmpty() && defaultValue != null ? defaultValue : input;
    }

    public static void waitForEnter() {
        System.out.println();
        System.out.print("Appuyez sur Entrée pour continuer...");
        scanner.nextLine();
    }
}
