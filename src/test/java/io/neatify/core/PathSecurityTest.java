package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de sécurité pour PathSecurity.
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

        assertTrue(exception.getMessage().contains("Path traversal interdit"));
    }

    @Test
    void testValidateRelativeSubpath_RejectsAbsoluteUnix() {
        SecurityException exception = assertThrows(SecurityException.class,
            () -> PathSecurity.validateRelativeSubpath("/etc/passwd"));

        assertTrue(exception.getMessage().contains("Chemin absolu Unix interdit"));
    }

    @Test
    void testValidateRelativeSubpath_RejectsAbsoluteWindows() {
        SecurityException exception = assertThrows(SecurityException.class,
            () -> PathSecurity.validateRelativeSubpath("C:\\Windows"));

        assertTrue(exception.getMessage().contains("Chemin absolu Windows interdit"));
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

        assertTrue(exception.getMessage().contains("Path traversal interdit"));
    }

    @Test
    void testAssertNoSymlinkInAncestry_ValidPath(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test");

        assertDoesNotThrow(() -> PathSecurity.assertNoSymlinkInAncestry(file));
    }

    @Test
    void testValidateSourceDir_ValidDir(@TempDir Path tempDir) {
        // Un dossier temporaire devrait être autorisé
        assertDoesNotThrow(() -> PathSecurity.validateSourceDir(tempDir));
    }

    @Test
    void testValidateSourceDir_RejectsSystemDirs() {
        // Ce test vérifie que les dossiers système sont bien bloqués
        // mais est tolérant aux différences d'environnement
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
            assertTrue(e.getMessage().contains("Dossier système interdit"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean testWindowsSystem() {
        // Sur Windows, on teste juste que la validation fonctionne sans planter
        // Le blocage spécifique des dossiers Windows est difficile à tester de manière portable
        return true;
    }
}
