package io.neatify.cli;

import java.util.Scanner;

/**
 * Utilitaires pour l'interface console.
 * Méthodes statiques de compatibilité qui délèguent à ConsoleOutput et BannerRenderer.
 *
 * Architecture :
 * - BannerRenderer : logique pure de rendu (String → String)
 * - ConsoleOutput : abstraction d'IO (évite System.out en dur)
 * - ConsoleUI : façade statique pour la compatibilité
 */
public final class ConsoleUI {

    private static final Scanner scanner = new Scanner(System.in);
    private static final ConsoleOutput output = ConsoleOutput.system();

    private ConsoleUI() {
        // Classe utilitaire
    }

    /**
     * Affiche le banner de l'application.
     *
     * @param version la version de l'application
     * @deprecated Utilisez printBanner(AppInfo) pour une meilleure séparation des responsabilités
     */
    @Deprecated
    public static void printBanner(String version) {
        output.printBanner(AppInfo.neatify(version));
    }

    /**
     * Affiche le banner de l'application.
     *
     * @param appInfo les informations de l'application
     */
    public static void printBanner(AppInfo appInfo) {
        output.printBanner(appInfo);
    }

    /**
     * Affiche une section avec titre.
     *
     * @param title le titre de la section
     */
    public static void printSection(String title) {
        output.printSection(title);
    }

    /**
     * Affiche une ligne de séparation.
     */
    public static void printLine() {
        output.printLine();
    }

    /**
     * Affiche un message de succès.
     *
     * @param message le message
     */
    public static void printSuccess(String message) {
        output.printSuccess(message);
    }

    /**
     * Affiche un message d'information.
     *
     * @param message le message
     */
    public static void printInfo(String message) {
        output.printInfo(message);
    }

    /**
     * Affiche un message d'avertissement.
     *
     * @param message le message
     */
    public static void printWarning(String message) {
        output.printWarning(message);
    }

    /**
     * Affiche un message d'erreur.
     *
     * @param message le message
     */
    public static void printError(String message) {
        output.printError(message);
    }

    /**
     * Lit une entrée utilisateur avec un prompt.
     *
     * @param prompt le texte du prompt
     * @return l'entrée utilisateur
     */
    public static String readInput(String prompt) {
        return readInput(prompt, null);
    }

    /**
     * Lit une entrée utilisateur avec un prompt et une valeur par défaut.
     *
     * @param prompt le texte du prompt
     * @param defaultValue la valeur par défaut (peut être null)
     * @return l'entrée utilisateur ou la valeur par défaut si vide
     */
    public static String readInput(String prompt, String defaultValue) {
        output.print(BannerRenderer.renderPrompt(prompt, defaultValue));
        output.flush(); // Force l'affichage immédiat du prompt
        String input = scanner.nextLine().trim();
        return input.isEmpty() && defaultValue != null ? defaultValue : input;
    }

    /**
     * Attend que l'utilisateur appuie sur Entrée.
     */
    public static void waitForEnter() {
        output.print(BannerRenderer.renderWaitForEnter());
        output.flush(); // Force l'affichage immédiat du message
        scanner.nextLine();
    }
}
