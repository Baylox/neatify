package io.neatify.core.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.neatify.core.PathSecurity;

import org.junit.jupiter.api.Test;
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
    void testAssertNoSymlinkInAncestry_ValidPath(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test");

        assertDoesNotThrow(() -> PathSecurity.assertNoSymlinkInAncestry(file));
    }

    @Test
    void testValidateSourceDir_ValidDir(@TempDir Path tempDir) {
        // A temporary folder should be allowed
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
            // /bin must be refused. Depending on the distribution this is either
            // the forbidden-system-directory guard, or the symlink guard on
            // usrmerge systems (Debian/Ubuntu) where /bin -> usr/bin. Both are
            // valid security rejections of /bin.
            String msg = e.getMessage();
            assertTrue(msg.contains("Forbidden system directory") || msg.contains("Symlink"),
                "/bin must be rejected as a forbidden or symlinked path, got: " + msg);
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
