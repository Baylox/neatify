package io.neatify;

import java.util.Scanner;

/**
 * Utilitaires pour l'interface console.
 */
final class ConsoleUI {

    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleUI() {
        // Classe utilitaire
    }

    static void printBanner(String version) {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║            NEATIFY v" + version + "                 ║");
        System.out.println("║   Outil de rangement automatique          ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
    }

    static void printSection(String title) {
        System.out.println();
        printLine();
        System.out.println(title);
        printLine();
    }

    static void printLine() {
        System.out.println("================================================");
    }

    static void printSuccess(String message) {
        System.out.println("[✓] " + message);
    }

    static void printInfo(String message) {
        System.out.println("[i] " + message);
    }

    static void printWarning(String message) {
        System.out.println("[!] " + message);
    }

    static void printError(String message) {
        System.err.println("[✗] " + message);
    }

    static String readInput(String prompt) {
        return readInput(prompt, null);
    }

    static String readInput(String prompt, String defaultValue) {
        if (defaultValue != null) {
            System.out.print(prompt + " [" + defaultValue + "]: ");
        } else {
            System.out.print(prompt + ": ");
        }

        String input = scanner.nextLine().trim();
        return input.isEmpty() && defaultValue != null ? defaultValue : input;
    }

    static void waitForEnter() {
        System.out.println();
        System.out.print("Appuyez sur Entrée pour continuer...");
        scanner.nextLine();
    }
}
