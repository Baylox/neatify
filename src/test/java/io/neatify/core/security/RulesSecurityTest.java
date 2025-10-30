package io.neatify.core.security;

import io.neatify.core.Rules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for Rules - Path Traversal Protection.
 * These tests ensure that the code blocks exploitation attempts.
 */
class RulesSecurityTest {

    @Test
    void testPathTraversal_DoubleDot(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "jpg=../../etc");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Loading should fail with path traversal containing '..'"
        );

        assertTrue(exception.getMessage().contains("Path traversal not allowed"));
    }

    @Test
    void testPathTraversal_TripleDot(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "pdf=../../../Windows/System32");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile)
        );

        assertTrue(exception.getMessage().contains("Path traversal not allowed"));
    }

    @Test
    void testPathTraversal_MixedWithValidPath(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "txt=Documents/../../../etc");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Path traversal should be blocked even when mixed with a valid path"
        );

        assertTrue(exception.getMessage().contains("Path traversal not allowed"));
    }

    @Test
    void testPathTraversal_AtEnd(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "txt=ValidFolder/..");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Path traversal at the end should be blocked"
        );

        assertTrue(exception.getMessage().contains("Path traversal not allowed"));
    }

    @Test
    void testAbsolutePath_Unix(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "jpg=/etc/passwd");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Absolute Unix paths should be blocked"
        );

        assertTrue(exception.getMessage().contains("Absolute Unix path not allowed"));
    }

    @Test
    void testAbsolutePath_UnixHome(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "pdf=/home/user/sensitive");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile)
        );

        assertTrue(exception.getMessage().contains("Absolute Unix path not allowed"));
    }

    @Test
    void testAbsolutePath_Windows(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "exe=C:\\Windows\\System32");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Absolute Windows paths should be blocked"
        );

        assertTrue(exception.getMessage().contains("Absolute Windows path not allowed"));
    }

    @Test
    void testAbsolutePath_WindowsDrive(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "dll=D:\\Program Files\\Sensitive");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile)
        );

        assertTrue(exception.getMessage().contains("Absolute Windows path not allowed"));
    }

    @Test
    void testValidNestedFolder_NoPathTraversal(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=Images/Photos/Vacation
            pdf=Documents/Work/Reports
            """);

        // These paths are valid and should NOT throw an exception
        Map<String, String> rules = assertDoesNotThrow(
            () -> Rules.load(rulesFile),
            "Valid nested relative paths should be allowed"
        );

        assertEquals("Images/Photos/Vacation", rules.get("jpg"));
        assertEquals("Documents/Work/Reports", rules.get("pdf"));
    }

    @Test
    void testDotInFolderName_Valid(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=Images.Backup
            pdf=Documents.v2
            """);

        // A single dot in a folder name is valid
        Map<String, String> rules = assertDoesNotThrow(
            () -> Rules.load(rulesFile),
            "A single dot in a folder name should be allowed"
        );

        assertEquals("Images.Backup", rules.get("jpg"));
        assertEquals("Documents.v2", rules.get("pdf"));
    }

    @Test
    void testMultipleRules_OneMalicious(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=Images
            pdf=Documents
            exe=../../../etc
            txt=TextFiles
            """);

        // The entire loading should fail if one rule is malicious
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Loading should fail if at least one rule is malicious"
        );

        assertNotNull(exception.getMessage(), "Exception should have a message");
    }
}
