package io.neatify.cli;

/**
 * Rendu pur du banner de l'application.
 * Classe pure (sans effets de bord) qui génère des représentations textuelles.
 * Facilite les tests unitaires et la réutilisation.
 */
public final class BannerRenderer {

    private BannerRenderer() {
        // Classe utilitaire
    }

    /**
     * Génère le banner de l'application sous forme de String.
     * Fonction pure : même entrée → même sortie, sans effets de bord.
     *
     * @param appInfo les informations de l'application
     * @return le banner formaté
     */
    public static String renderBanner(AppInfo appInfo) {
        return "\n" +
                "    ███╗   ██╗███████╗ █████╗ ████████╗██╗███████╗██╗   ██╗\n" +
                "    ████╗  ██║██╔════╝██╔══██╗╚══██╔══╝██║██╔════╝╚██╗ ██╔╝\n" +
                "    ██╔██╗ ██║█████╗  ███████║   ██║   ██║█████╗   ╚████╔╝ \n" +
                "    ██║╚██╗██║██╔══╝  ██╔══██║   ██║   ██║██╔══╝    ╚██╔╝  \n" +
                "    ██║ ╚████║███████╗██║  ██║   ██║   ██║██║        ██║   \n" +
                "    ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝╚═╝        ╚═╝   \n" +
                "\n" +
                "    " + appInfo.description() + " - v" + appInfo.version() + "\n" +
                "    ════════════════════════════════════════════════════════\n" +
                "\n";
    }

    /**
     * Génère une ligne de séparation.
     *
     * @return la ligne de séparation
     */
    public static String renderLine() {
        return "================================================";
    }

    /**
     * Génère une section avec titre.
     *
     * @param title le titre de la section
     * @return la section formatée
     */
    public static String renderSection(String title) {
        return "\n" +
                renderLine() + "\n" +
                title + "\n" +
                renderLine() + "\n";
    }

    /**
     * Formate un message de succès.
     *
     * @param message le message
     * @return le message formaté
     */
    public static String renderSuccess(String message) {
        return "[✓] " + message;
    }

    /**
     * Formate un message d'information.
     *
     * @param message le message
     * @return le message formaté
     */
    public static String renderInfo(String message) {
        return "[i] " + message;
    }

    /**
     * Formate un message d'avertissement.
     *
     * @param message le message
     * @return le message formaté
     */
    public static String renderWarning(String message) {
        return "[!] " + message;
    }

    /**
     * Formate un message d'erreur.
     *
     * @param message le message
     * @return le message formaté
     */
    public static String renderError(String message) {
        return "[✗] " + message;
    }

    /**
     * Formate un prompt avec valeur par défaut optionnelle.
     *
     * @param prompt le texte du prompt
     * @param defaultValue la valeur par défaut (peut être null).
     * @return le prompt formaté
     */
    public static String renderPrompt(String prompt, String defaultValue) {
        if (defaultValue != null && !defaultValue.isEmpty()) {
            return prompt + " [" + defaultValue + "]: ";
        }
        return prompt + ": ";
    }

    /**
     * Génère le message "Appuyez sur Entrée pour continuer...".
     *
     * @return le message formaté
     */
    public static String renderWaitForEnter() {
        return "\nAppuyez sur Entree pour continuer...";
    }

    /**
     * Genere une barre de progression.
     *
     * @param current valeur actuelle
     * @param total valeur totale
     * @param width largeur de la barre
     * @return la barre de progression formatee
     */
    public static String renderProgressBar(int current, int total, int width) {
        if (total == 0) return "";

        int percentage = (current * 100) / total;
        int filled = (current * width) / total;
        int empty = width - filled;

        StringBuilder bar = new StringBuilder("[");
        bar.append("█".repeat(filled));
        bar.append("░".repeat(empty));
        bar.append("] ").append(percentage).append("% (").append(current).append("/").append(total).append(")");

        return bar.toString();
    }

    /**
     * Genere un tableau de resultats.
     *
     * @param moved nombre de fichiers deplaces
     * @param skipped nombre de fichiers ignores
     * @param errors nombre d'erreurs
     * @return le tableau formate
     */
    public static String renderResultTable(int moved, int skipped, int errors) {
        int total = moved + skipped;

        return "\n" +
                "    ┌─────────────────────────────────────────┐\n" +
                "    │           RESUME DE L'OPERATION         │\n" +
                "    ├─────────────────────────────────────────┤\n" +
                String.format("    │  Fichiers traites    │  %-15d │\n", total) +
                String.format("    │  Deplaces            │  %-15d │\n", moved) +
                String.format("    │  Ignores             │  %-15d │\n", skipped) +
                String.format("    │  Erreurs             │  %-15d │\n", errors) +
                "    └─────────────────────────────────────────┘\n";
    }

    /**
     * Genere un spinner d'animation (pour les operations longues).
     *
     * @param frame numero de frame (0-3)
     * @param message message a afficher
     * @return le spinner formate
     */
    public static String renderSpinner(int frame, String message) {
        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        return frames[frame % frames.length] + " " + message;
    }
}
