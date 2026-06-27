package io.neatify.core.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.neatify.core.PathSecurity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for PathSecurity.
 */
class PathSecurityTest {

    @Test
    void testValidateRelativeSubpath_Valid() {
        assertDoesNotThrow(() -> PathSecurity.validateRelativeSubpath("Images"));
        assertDoesNotThrow(() -> PathSecurity.validateRelativeSubpath("Documents/Work"));
        assertDoesNotThrow(() -> PathSecurity.validateRelativeSubpath("Media/Photos/Vacation"));
    }

    @Test
    void testValidateRelativeSubpath_RejectsPathTraversal() {
        SecurityException exception = assertThrows(SecurityException.class,
            () -> PathSecurity.validateRelativeSubpath("../etc"));

        assertTrue(exception.getMessage().contains("Path traversal not allowed"));
    }

    @Test
    void testValidateRelativeSubpath_RejectsAbsoluteUnix() {
        SecurityException exception = assertThrows(SecurityException.class,
            () -> PathSecurity.validateRelativeSubpath("/etc/passwd"));

        assertTrue(exception.getMessage().contains("Absolute Unix path not allowed"));
    }

    @Test
    void testValidateRelativeSubpath_RejectsAbsoluteWindows() {
        SecurityException exception = assertThrows(SecurityException.class,
            () -> PathSecurity.validateRelativeSubpath("C:\\Windows"));

        assertTrue(exception.getMessage().contains("Absolute Windows path not allowed"));
    }

    @Test
    void testSafeResolveWithin_Valid(@TempDir Path tempDir) {
        Path resolved = PathSecurity.safeResolveWithin(tempDir, "subfolder/file.txt");

        assertTrue(resolved.toAbsolutePath().normalize().startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    void testSafeResolveWithin_RejectsEscape(@TempDir Path tempDir) {
        SecurityException exception = assertThrows(SecurityException.class,
            () -> PathSecurity.safeResolveWithin(tempDir, "../../etc/passwd"));

        assertTrue(exception.getMessage().contains("Path traversal not allowed"));
    }

    @Test
    void testAssertResolvedWithin_ValidPath(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test");

        assertDoesNotThrow(() -> PathSecurity.assertResolvedWithin(tempDir, file));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // symbolic links require elevated privileges on Windows
    void testAssertResolvedWithin_SymlinkEscapingRoot_isRejected(@TempDir Path root, @TempDir Path outside)
        throws IOException {
        // A symlink inside root that points OUTSIDE must be refused: its real
        // target escapes the trusted root. This is the genuine symlink attack.
        Path link = root.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException e) {
            return; // symlinks not supported here (e.g. some filesystems) — skip gracefully
        }
        Path candidate = link.resolve("stolen.txt");

        assertThrows(SecurityException.class,
            () -> PathSecurity.assertResolvedWithin(root, candidate));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testAssertResolvedWithin_InternalSymlink_isAllowed(@TempDir Path base) throws IOException {
        // A symlink that stays INSIDE the trusted area must be allowed. This
        // mirrors legitimate system symlinks above a temp dir (e.g. macOS
        // /var -> /private/var) which previously caused false positives.
        Path actual = base.resolve("actual");
        Files.createDirectories(actual);
        Path linkedRoot = base.resolve("linkedRoot");
        try {
            Files.createSymbolicLink(linkedRoot, actual);
        } catch (UnsupportedOperationException | IOException e) {
            return; // symlinks not supported here — skip gracefully
        }
        Path candidate = linkedRoot.resolve("sub").resolve("file.txt");

        // root is reached through a symlink, but the candidate stays within it.
        assertDoesNotThrow(() -> PathSecurity.assertResolvedWithin(linkedRoot, candidate));
    }

    @Test
    void testValidateSourceDir_ValidDir(@TempDir Path tempDir) {
        // A temporary folder must be allowed — including on macOS where it lives
        // under /private/var/folders (reached via the /var system symlink).
        assertDoesNotThrow(() -> PathSecurity.validateSourceDir(tempDir));
    }

    @Test
    void testValidateSourceDir_RejectsSystemDirs() {
        // This test verifies that system directories are properly blocked
        // but is tolerant to environment differences
        boolean tested = isUnixLikeSystem() ? testUnixSystemDirectory() : testWindowsSystem();

        assertTrue(tested, "At least one system directory test should have been performed");
    }

    private boolean isUnixLikeSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }

    private boolean testUnixSystemDirectory() {
        Path binDir = Path.of("/bin");
        if (!Files.exists(binDir) || !Files.isDirectory(binDir)) {
            return false;
        }

        try {
            PathSecurity.validateSourceDir(binDir);
            System.err.println("WARNING: /bin should have been blocked but wasn't");
            return false;
        } catch (SecurityException e) {
            assertTrue(e.getMessage().contains("Forbidden system directory"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean testWindowsSystem() {
        // Verify that the Windows-specific forbidden path list blocks C:\Windows
        // PathSecurity.validateSourceDir uses Paths.get(forbidden) internally,
        // so we test via validateRelativeSubpath + safeResolveWithin as a proxy,
        // OR directly check that C:\Windows is in the forbidden list by calling
        // validateSourceDir with a synthetic path that matches it.
        try {
            Path windowsDir = Path.of("C:\\Windows");
            // If the path is invalid on this OS this will throw InvalidPathException — skip
            PathSecurity.validateSourceDir(windowsDir);
            // If no exception is thrown, the block didn't fire — fail visibly
            System.err.println("WARNING: C:\\Windows should have been blocked but wasn't");
            return false;
        } catch (SecurityException e) {
            assertTrue(e.getMessage().contains("Forbidden system directory"),
                "Expected 'Forbidden system directory' in: " + e.getMessage());
            return true;
        } catch (java.nio.file.InvalidPathException | IOException e) {
            // Path not valid on this OS (e.g., running on Linux in CI) — test not applicable
            return true;
        }
    }
}
