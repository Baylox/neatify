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

    // System directories that are forbidden as an organization source.
    // Note: /var and /tmp are intentionally NOT listed — they host legitimate
    // user temp areas (e.g. macOS /var/folders, /tmp) and are reached through
    // system symlinks (/var -> /private/var on macOS). The entries below cover
    // system binaries and configuration, which is what actually matters here.
    private static final List<String> FORBIDDEN_PATHS_UNIX = List.of(
        "/etc", "/bin", "/sbin", "/usr/bin", "/usr/sbin",
        "/sys", "/proc", "/dev", "/boot", "/root"
    );

    private static final List<String> FORBIDDEN_PATHS_WINDOWS = List.of(
        "C:\\Windows", "C:\\Program Files", "C:\\Program Files (x86)",
        "C:\\ProgramData", "C:\\Users\\All Users"
    );

    /**
     * Validates that a path is safe to use as the organization source.
     * Resolves symlinks first (so a link pointing at a system directory is
     * caught by its real target), then blocks forbidden system directories.
     *
     * @param sourcePath path to validate
     * @throws SecurityException if the path is not safe
     * @throws IOException on I/O error
     */
    public static void validateSourceDir(Path sourcePath) throws IOException {
        if (sourcePath == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        Path resolved = resolveRealPathOrNormalized(sourcePath);

        checkNotForbiddenPath(resolved, FORBIDDEN_PATHS_UNIX);
        checkNotForbiddenPath(resolved, FORBIDDEN_PATHS_WINDOWS);
    }

    /**
     * Verifies a (already symlink-resolved) path does not match a forbidden
     * directories list. Each forbidden entry is itself resolved to its real
     * path so that e.g. an usrmerge {@code /bin -> /usr/bin} still matches.
     */
    private static void checkNotForbiddenPath(Path resolved, List<String> forbiddenPaths) throws IOException {
        for (String forbidden : forbiddenPaths) {
            Path forbiddenPath;
            try {
                forbiddenPath = resolveRealPathOrNormalized(Paths.get(forbidden));
            } catch (java.nio.file.InvalidPathException ignored) {
                // Path not valid on this system (e.g., a Windows path on Unix) — skip it.
                continue;
            }
            if (resolved.equals(forbiddenPath) || resolved.startsWith(forbiddenPath)) {
                throw new SecurityException(
                    "Forbidden system directory: " + resolved + " (area: " + forbidden + ")"
                );
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
     * Checks whether the given path resides inside a Git repository.
     * Walks up the directory tree looking for a .git directory or file (worktree).
     *
     * @param start the directory to check
     * @return true if a .git ancestor is found
     */
    public static boolean isInsideGitRepository(Path start) {
        if (start == null) return false;
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            Path gitDir = current.resolve(".git");
            if (Files.exists(gitDir)) {
                // .git can be a directory or a file (worktree); both indicate a repo
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Asserts that the real (symlink-resolved) location of {@code candidate}
     * stays within the real location of {@code root}.
     *
     * <p>This blocks any symlink or {@code ..} segment that would make the path
     * escape the trusted area (the real attack: a symlink inside the work area
     * pointing somewhere else), while allowing legitimate system symlinks that
     * sit <em>above</em> root (e.g. macOS {@code /var -> /private/var}, or an
     * usrmerge {@code /bin -> /usr/bin}).
     *
     * @param root      trusted base directory (expected to exist)
     * @param candidate path to validate (may or may not exist yet)
     * @throws SecurityException if the resolved candidate escapes root
     * @throws IOException on I/O error resolving root
     */
    public static void assertResolvedWithin(Path root, Path candidate) throws IOException {
        if (root == null || candidate == null) {
            throw new IllegalArgumentException("root and candidate must not be null");
        }

        Path realRoot = resolveRealPathOrNormalized(root);
        Path realCandidate = resolveRealPathOrNormalized(candidate);

        if (!realCandidate.startsWith(realRoot)) {
            throw new SecurityException(
                "Resolved path escapes the trusted root: " + candidate
                    + " -> " + realCandidate + " (root: " + realRoot + ")"
            );
        }
    }

    /**
     * Resolves a path to its real (symlink-followed) absolute form. When the
     * path does not exist yet, resolves the closest existing ancestor to its
     * real path and re-attaches the remaining (normalized) segments. This makes
     * symlink resolution work for not-yet-created targets too — crucial so that
     * a {@code root/link/file} target is judged by where {@code link} really
     * points, even though {@code file} does not exist.
     */
    private static Path resolveRealPathOrNormalized(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        try {
            return absolute.toRealPath();
        } catch (IOException notFullyPresent) {
            // Walk up to the nearest existing ancestor, resolve it, then rebuild.
            Path existing = absolute;
            while (existing != null && !Files.exists(existing)) {
                existing = existing.getParent();
            }
            if (existing == null) {
                return absolute;
            }
            Path realExisting = existing.toRealPath();
            Path remainder = existing.relativize(absolute);
            return realExisting.resolve(remainder).normalize();
        }
    }
}
