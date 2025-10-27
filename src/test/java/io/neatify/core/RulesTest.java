package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests essentiels pour Rules.
 */
class RulesTest {

    @Test
    void testLoad_BasicFunctionality(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=Images
            png=Images
            pdf=Documents
            """);

        Map<String, String> rules = Rules.load(rulesFile);

        assertEquals(3, rules.size());
        assertEquals("Images", rules.get("jpg"));
        assertEquals("Documents", rules.get("pdf"));
    }

    @Test
    void testLoad_NormalizesExtensions(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            JPG=Images
            .png=Images
            PDF=Documents
            """);

        Map<String, String> rules = Rules.load(rulesFile);

        assertEquals("Images", rules.get("jpg"));
        assertEquals("Images", rules.get("png"));
        assertEquals("Documents", rules.get("pdf"));
    }

    @Test
    void testLoad_SanitizesFolderNames(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=My:Images*
            pdf=Docs<>
            """);

        Map<String, String> rules = Rules.load(rulesFile);

        assertEquals("My_Images_", rules.get("jpg"));
        assertEquals("Docs__", rules.get("pdf"));
    }

    @Test
    void testLoad_WithNestedFolders(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            csv=Documents/Spreadsheets
            xlsx=Documents/Spreadsheets
            """);

        Map<String, String> rules = Rules.load(rulesFile);

        assertEquals("Documents/Spreadsheets", rules.get("csv"));
    }

    @Test
    void testLoad_RejectsEmptyFile(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "");

        assertThrows(IllegalArgumentException.class, () -> Rules.load(rulesFile));
    }

    @Test
    void testGetTargetFolder_CaseInsensitive() {
        Map<String, String> rules = Map.of("jpg", "Images");

        assertEquals("Images", Rules.getTargetFolder(rules, "JPG"));
        assertEquals("Images", Rules.getTargetFolder(rules, "jpg"));
    }
}
