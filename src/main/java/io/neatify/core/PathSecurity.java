package io.neatify.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Security utilities for validating paths.
 * Protects against: path traversal, symlink attacks, system access.
 */
public final class PathSecurity {

    private PathSecurity() {
        // Utility class
    }

    // System directories that are forbidden
    private static final List<String> FORBIDDEN_PATHS_UNIX = List.of(
        "/etc", "/bin", "/sbin", "/usr/bin", "/usr/sbin",
        "/var", "/sys", "/proc", "/dev", "/boot", "/root"
    );

    private static final List<String> FORBIDDEN_PATHS_WINDOWS = List.of(
        "C:\\Windows", "C:\\Program Files", "C:\\Program Files (x86)",
        "C:\\ProgramData", "C:\\Users\\All Users"
    );

    /**
     * Validates that a path is safe to use as the organization source.
     * Blocks system directories and symlinks.
     *
     * @param sourcePath path to validate
     * @throws SecurityException if the path is not safe
     * @throws IOException on I/O error
     */
    public static void validateSourceDir(Path sourcePath) throws IOException {
        if (sourcePath == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        Path normalized = sourcePath.toAbsolutePath().normalize();

        assertNoSymlinkInAncestry(normalized);
        checkNotForbiddenPath(normalized, FORBIDDEN_PATHS_UNIX);
        checkNotForbiddenPath(normalized, FORBIDDEN_PATHS_WINDOWS);
    }

    /**
     * Verifies a path does not match a forbidden directories list.
     */
    private static void checkNotForbiddenPath(Path normalized, List<String> forbiddenPaths) {
        for (String forbidden : forbiddenPaths) {
            try {
                Path forbiddenPath = Paths.get(forbidden).toAbsolutePath().normalize();
                if (normalized.equals(forbiddenPath) || normalized.startsWith(forbiddenPath)) {
                    throw new SecurityException(
                        "Forbidden system directory: " + normalized + " (area: " + forbidden + ")"
                    );
                }
            } catch (java.nio.file.InvalidPathException e) {
                // Ignore if path is invalid on this system (e.g., Windows path on Unix)
            }
        }
    }

    /**
     * Validates a relative subpath is safe (no .., not absolute).
     *
     * @param subpath subpath to validate
     * @throws SecurityException if it contains dangerous elements
     */
    public static void validateRelativeSubpath(String subpath) {
        if (subpath == null || subpath.isBlank()) {
            throw new IllegalArgumentException("Subpath cannot be empty");
        }

        checkNoPathTraversal(subpath);
        checkNotAbsolutePath(subpath);
    }

    /**
     * Verifies a path does not contain path traversal (..).
     */
    private static void checkNoPathTraversal(String subpath) {
        if (subpath.contains("..")) {
            throw new SecurityException("Path traversal not allowed (..): " + subpath);
        }
    }

    /**
     * Verifies a path is not absolute (Unix or Windows).
     */
    private static void checkNotAbsolutePath(String subpath) {
        if (subpath.startsWith("/")) {
            throw new SecurityException("Absolute Unix path not allowed: " + subpath);
        }
        if (subpath.matches("^[A-Za-z]:.*")) {
            throw new SecurityException("Absolute Windows path not allowed: " + subpath);
        }
    }

    /**
     * Safely resolves a subpath ensuring it remains within root.
     *
     * @param root root directory
     * @param subpath subpath to resolve
     * @return resolved path
     * @throws SecurityException if the path escapes root
     */
    public static Path safeResolveWithin(Path root, String subpath) {
        validateRelativeSubpath(subpath);

        Path resolved = root.resolve(subpath).normalize();
        Path normalizedRoot = root.toAbsolutePath().normalize();

        if (!resolved.toAbsolutePath().normalize().startsWith(normalizedRoot)) {
            throw new SecurityException(
                "Resolved path escapes the allowed area: " + subpath
            );
        }

        return resolved;
    }

    /**
     * Verifies no ancestor of the path is a symlink.
     * Protects against symlink attacks along the path.
     *
     * @param path path to check
     * @throws SecurityException if a symlink is detected
     * @throws IOException on I/O error
     */
    public static void assertNoSymlinkInAncestry(Path path) throws IOException {
        if (path == null) {
            return;
        }

        checkSymlinkSelf(path);
        checkSymlinkAncestors(path);
    }

    /**
     * Verifies that the path itself is not a symlink.
     */
    private static void checkSymlinkSelf(Path path) throws IOException {
        if (Files.exists(path) && Files.isSymbolicLink(path)) {
            throw new SecurityException("Symlink not allowed: " + path);
        }
    }

    /**
     * Verifies that no ancestor of the path is a symlink.
     */
    private static void checkSymlinkAncestors(Path path) throws IOException {
        Path current = path.getParent();
        while (current != null) {
            if (Files.exists(current) && Files.isSymbolicLink(current)) {
                throw new SecurityException(
                    "Symlink detected in parent path: " + current + " (full path: " + path + ")"
                );
            }
            current = current.getParent();
        }
    }
}
