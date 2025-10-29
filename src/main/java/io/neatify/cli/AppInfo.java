package io.neatify.cli;

/**
 * Application information.
 * Immutable record carrying application metadata.
 *
 * @param name application name
 * @param version application version
 * @param description short application description
 */
public record AppInfo(String name, String version, String description) {

    /**
     * Compact constructor with validation.
     */
    public AppInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Application name cannot be empty");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version cannot be empty");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
    }

    /**
     * Creates an AppInfo instance for Neatify.
     *
     * @param version application version
     * @return AppInfo instance for Neatify
     */
    public static AppInfo neatify(String version) {
        return new AppInfo(
            "NEATIFY",
            version,
            "Automatic organization tool"
        );
    }
}
