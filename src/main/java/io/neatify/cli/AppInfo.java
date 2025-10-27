package io.neatify.cli;

/**
 * Informations sur l'application.
 * Record immuable contenant les métadonnées de l'application.
 *
 * @param name le nom de l'application
 * @param version la version de l'application
 * @param description une brève description de l'application
 */
public record AppInfo(String name, String version, String description) {

    /**
     * Constructeur compact avec validation.
     */
    public AppInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'application ne peut pas être vide");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("La version ne peut pas être vide");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La description ne peut pas être vide");
        }
    }

    /**
     * Crée une instance pour Neatify.
     *
     * @param version la version de l'application
     * @return une instance d'AppInfo pour Neatify
     */
    public static AppInfo neatify(String version) {
        return new AppInfo(
            "NEATIFY",
            version,
            "Outil de rangement automatique"
        );
    }
}
