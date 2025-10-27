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
                "╔════════════════════════════════════════════╗\n" +
                String.format("║            %s v%-8s             ║\n", appInfo.name(), appInfo.version()) +
                String.format("║   %-39s║\n", appInfo.description()) +
                "╚════════════════════════════════════════════╝\n" +
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
        return "\nAppuyez sur Entrée pour continuer...";
    }
}
