package io.neatify.cli;

import java.io.PrintWriter;

/**
 * Abstraction de la sortie console.
 * Permet d'injecter différentes destinations (System.out, fichier, logger, etc.).
 * Améliore la testabilité en évitant le couplage direct avec System.out.
 */
public final class ConsoleOutput {

    private final PrintWriter writer;
    private final PrintWriter errorWriter;

    /**
     * Crée une sortie console avec des writers personnalisés.
     *
     * @param writer le writer pour la sortie standard
     * @param errorWriter le writer pour la sortie d'erreur
     */
    public ConsoleOutput(PrintWriter writer, PrintWriter errorWriter) {
        this.writer = writer;
        this.errorWriter = errorWriter;
    }

    /**
     * Crée une sortie console avec System.out et System.err.
     *
     * @return une instance utilisant les sorties standard
     */
    public static ConsoleOutput system() {
        return new ConsoleOutput(
            new PrintWriter(System.out, true),
            new PrintWriter(System.err, true)
        );
    }

    /**
     * Écrit une ligne sur la sortie standard.
     *
     * @param line la ligne à écrire
     */
    public void println(String line) {
        writer.println(line);
    }

    /**
     * Écrit du texte sur la sortie standard sans retour à la ligne.
     *
     * @param text le texte à écrire
     */
    public void print(String text) {
        writer.print(text);
    }

    /**
     * Écrit une ligne vide sur la sortie standard.
     */
    public void println() {
        writer.println();
    }

    /**
     * Écrit une ligne sur la sortie d'erreur.
     *
     * @param line la ligne à écrire
     */
    public void printlnError(String line) {
        errorWriter.println(line);
    }

    /**
     * Flush les buffers des writers.
     */
    public void flush() {
        writer.flush();
        errorWriter.flush();
    }

    /**
     * Affiche le banner de l'application.
     *
     * @param appInfo les informations de l'application
     */
    public void printBanner(AppInfo appInfo) {
        print(BannerRenderer.renderBanner(appInfo));
    }

    /**
     * Affiche une section avec titre.
     *
     * @param title le titre de la section
     */
    public void printSection(String title) {
        print(BannerRenderer.renderSection(title));
    }

    /**
     * Affiche une ligne de séparation.
     */
    public void printLine() {
        println(BannerRenderer.renderLine());
    }

    /**
     * Affiche un message de succès.
     *
     * @param message le message
     */
    public void printSuccess(String message) {
        println(BannerRenderer.renderSuccess(message));
    }

    /**
     * Affiche un message d'information.
     *
     * @param message le message
     */
    public void printInfo(String message) {
        println(BannerRenderer.renderInfo(message));
    }

    /**
     * Affiche un message d'avertissement.
     *
     * @param message le message
     */
    public void printWarning(String message) {
        println(BannerRenderer.renderWarning(message));
    }

    /**
     * Affiche un message d'erreur.
     *
     * @param message le message
     */
    public void printError(String message) {
        printlnError(BannerRenderer.renderError(message));
    }
}
