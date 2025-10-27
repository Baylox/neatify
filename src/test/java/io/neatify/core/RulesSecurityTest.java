package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de sécurité pour Rules - Protection contre Path Traversal.
 * Ces tests garantissent que le code bloque les tentatives d'exploitation.
 */
class RulesSecurityTest {

    @Test
    void testPathTraversal_DoubleDot(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "jpg=../../etc");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Le chargement devrait échouer avec path traversal contenant '..'"
        );

        assertTrue(exception.getMessage().contains("Path traversal interdit"));
    }

    @Test
    void testPathTraversal_TripleDot(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "pdf=../../../Windows/System32");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile)
        );

        assertTrue(exception.getMessage().contains("Path traversal interdit"));
    }

    @Test
    void testPathTraversal_MixedWithValidPath(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "txt=Documents/../../../etc");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Le path traversal devrait être bloqué même mélangé avec un chemin valide"
        );

        assertTrue(exception.getMessage().contains("Path traversal interdit"));
    }

    @Test
    void testPathTraversal_AtEnd(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "txt=ValidFolder/..");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Le path traversal à la fin devrait être bloqué"
        );

        assertTrue(exception.getMessage().contains("Path traversal interdit"));
    }

    @Test
    void testAbsolutePath_Unix(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "jpg=/etc/passwd");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Les chemins absolus Unix devraient être bloqués"
        );

        assertTrue(exception.getMessage().contains("Chemin absolu Unix interdit"));
    }

    @Test
    void testAbsolutePath_UnixHome(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "pdf=/home/user/sensitive");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile)
        );

        assertTrue(exception.getMessage().contains("Chemin absolu Unix interdit"));
    }

    @Test
    void testAbsolutePath_Windows(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "exe=C:\\Windows\\System32");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Les chemins absolus Windows devraient être bloqués"
        );

        assertTrue(exception.getMessage().contains("Chemin absolu Windows interdit"));
    }

    @Test
    void testAbsolutePath_WindowsDrive(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "dll=D:\\Program Files\\Sensitive");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile)
        );

        assertTrue(exception.getMessage().contains("Chemin absolu Windows interdit"));
    }

    @Test
    void testValidNestedFolder_NoPathTraversal(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=Images/Photos/Vacation
            pdf=Documents/Work/Reports
            """);

        // Ces chemins sont valides et ne devraient PAS lancer d'exception
        Map<String, String> rules = assertDoesNotThrow(
            () -> Rules.load(rulesFile),
            "Les chemins relatifs imbriqués valides devraient être autorisés"
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

        // Un seul point dans un nom de dossier est valide
        Map<String, String> rules = assertDoesNotThrow(
            () -> Rules.load(rulesFile),
            "Un point simple dans un nom de dossier devrait être autorisé"
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

        // L'ensemble du chargement devrait échouer si une règle est malveillante
        assertThrows(
            IllegalArgumentException.class,
            () -> Rules.load(rulesFile),
            "Le chargement devrait échouer si au moins une règle est malveillante"
        );
    }
}
