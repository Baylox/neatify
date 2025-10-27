package io.neatify.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utilitaires de sécurité pour la validation des chemins.
 * Protège contre: path traversal, symlink attacks, accès système.
 */
public final class PathSecurity {

    private PathSecurity() {
        // Classe utilitaire
    }

    // Dossiers système interdits
    private static final List<String> FORBIDDEN_PATHS_UNIX = List.of(
        "/etc", "/bin", "/sbin", "/usr/bin", "/usr/sbin",
        "/var", "/sys", "/proc", "/dev", "/boot", "/root"
    );

    private static final List<String> FORBIDDEN_PATHS_WINDOWS = List.of(
        "C:\\Windows", "C:\\Program Files", "C:\\Program Files (x86)",
        "C:\\ProgramData", "C:\\Users\\All Users"
    );

    /**
     * Valide qu'un chemin est sûr pour être utilisé comme source d'organisation.
     * Bloque les dossiers système et les symlinks.
     *
     * @param sourcePath le chemin à valider
     * @throws SecurityException si le chemin n'est pas sûr
     * @throws IOException si erreur I/O
     */
    public static void validateSourceDir(Path sourcePath) throws IOException {
        if (sourcePath == null) {
            throw new IllegalArgumentException("Le chemin ne peut pas être null");
        }

        Path normalized = sourcePath.toAbsolutePath().normalize();

        assertNoSymlinkInAncestry(normalized);
        checkNotForbiddenPath(normalized, FORBIDDEN_PATHS_UNIX);
        checkNotForbiddenPath(normalized, FORBIDDEN_PATHS_WINDOWS);
    }

    /**
     * Vérifie qu'un chemin ne correspond pas à une liste de chemins interdits.
     */
    private static void checkNotForbiddenPath(Path normalized, List<String> forbiddenPaths) {
        for (String forbidden : forbiddenPaths) {
            try {
                Path forbiddenPath = Paths.get(forbidden).toAbsolutePath().normalize();
                if (normalized.equals(forbiddenPath) || normalized.startsWith(forbiddenPath)) {
                    throw new SecurityException(
                        "Dossier système interdit : " + normalized + " (zone: " + forbidden + ")"
                    );
                }
            } catch (java.nio.file.InvalidPathException e) {
                // Ignore si le chemin n'est pas valide sur ce système (ex: chemin Windows sur Unix)
            }
        }
    }

    /**
     * Valide qu'un sous-chemin relatif est sûr (pas de .., pas de chemin absolu).
     *
     * @param subpath le sous-chemin à valider
     * @throws SecurityException si le sous-chemin contient des éléments dangereux
     */
    public static void validateRelativeSubpath(String subpath) {
        if (subpath == null || subpath.isBlank()) {
            throw new IllegalArgumentException("Le sous-chemin ne peut pas être vide");
        }

        checkNoPathTraversal(subpath);
        checkNotAbsolutePath(subpath);
    }

    /**
     * Vérifie qu'un chemin ne contient pas de path traversal (..).
     */
    private static void checkNoPathTraversal(String subpath) {
        if (subpath.contains("..")) {
            throw new SecurityException("Path traversal interdit (..) : " + subpath);
        }
    }

    /**
     * Vérifie qu'un chemin n'est pas absolu (Unix ou Windows).
     */
    private static void checkNotAbsolutePath(String subpath) {
        if (subpath.startsWith("/")) {
            throw new SecurityException("Chemin absolu Unix interdit : " + subpath);
        }
        if (subpath.matches("^[A-Za-z]:.*")) {
            throw new SecurityException("Chemin absolu Windows interdit : " + subpath);
        }
    }

    /**
     * Résout un sous-chemin de manière sûre, en garantissant qu'il reste dans root.
     *
     * @param root le dossier racine
     * @param subpath le sous-chemin à résoudre
     * @return le chemin résolu
     * @throws SecurityException si le chemin sort de root
     */
    public static Path safeResolveWithin(Path root, String subpath) {
        validateRelativeSubpath(subpath);

        Path resolved = root.resolve(subpath).normalize();
        Path normalizedRoot = root.toAbsolutePath().normalize();

        if (!resolved.toAbsolutePath().normalize().startsWith(normalizedRoot)) {
            throw new SecurityException(
                "Le chemin résolu sort de la zone autorisée : " + subpath
            );
        }

        return resolved;
    }

    /**
     * Vérifie qu'aucun ancêtre du chemin n'est un symlink.
     * Protège contre les attaques par symlink dans le chemin.
     *
     * @param path le chemin à vérifier
     * @throws SecurityException si un symlink est détecté
     * @throws IOException si erreur I/O
     */
    public static void assertNoSymlinkInAncestry(Path path) throws IOException {
        if (path == null) {
            return;
        }

        checkSymlinkSelf(path);
        checkSymlinkAncestors(path);
    }

    /**
     * Vérifie que le chemin lui-même n'est pas un symlink.
     */
    private static void checkSymlinkSelf(Path path) throws IOException {
        if (Files.exists(path) && Files.isSymbolicLink(path)) {
            throw new SecurityException("Symlink interdit : " + path);
        }
    }

    /**
     * Vérifie qu'aucun ancêtre du chemin n'est un symlink.
     */
    private static void checkSymlinkAncestors(Path path) throws IOException {
        Path current = path.getParent();
        while (current != null) {
            if (Files.exists(current) && Files.isSymbolicLink(current)) {
                throw new SecurityException(
                    "Symlink détecté dans le chemin parent : " + current + " (chemin complet: " + path + ")"
                );
            }
            current = current.getParent();
        }
    }
}
