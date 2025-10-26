package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour Rules.
 */
class RulesTest {

    @Test
    void testLoad_ValidRules(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=Images
            png=Images
            pdf=Documents
            txt=Documents
            """);

        Map<String, String> rules = Rules.load(rulesFile);

        assertEquals(4, rules.size());
        assertEquals("Images", rules.get("jpg"));
        assertEquals("Images", rules.get("png"));
        assertEquals("Documents", rules.get("pdf"));
        assertEquals("Documents", rules.get("txt"));
    }

    @Test
    void testLoad_WithComments(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            # Images
            jpg=Images
            png=Images

            # Documents
            pdf=Documents
            """);

        Map<String, String> rules = Rules.load(rulesFile);

        assertEquals(3, rules.size());
    }

    @Test
    void testLoad_WithWhitespace(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
              jpg  =  Images
            png=  Pictures
            """);

        Map<String, String> rules = Rules.load(rulesFile);

        assertEquals("Images", rules.get("jpg"));
        assertEquals("Pictures", rules.get("png"));
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
        assertEquals("Documents/Spreadsheets", rules.get("xlsx"));
    }

    @Test
    void testLoad_FileNotFound(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("nonexistent.properties");

        assertThrows(IOException.class, () -> Rules.load(nonExistent));
    }

    @Test
    void testLoad_WithNullPath() {
        assertThrows(NullPointerException.class, () -> Rules.load(null));
    }

    @Test
    void testLoad_WithDirectory(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> Rules.load(tempDir));
    }

    @Test
    void testLoad_EmptyFile(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "");

        assertThrows(IllegalArgumentException.class, () -> Rules.load(rulesFile));
    }

    @Test
    void testLoad_OnlyComments(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            # Only comments
            # No rules
            """);

        assertThrows(IllegalArgumentException.class, () -> Rules.load(rulesFile));
    }

    @Test
    void testLoad_EmptyExtension(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            =Images
            """);

        assertThrows(IllegalArgumentException.class, () -> Rules.load(rulesFile));
    }

    @Test
    void testLoad_EmptyFolder(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=
            """);

        assertThrows(IllegalArgumentException.class, () -> Rules.load(rulesFile));
    }

    @Test
    void testLoad_WhitespaceFolder(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, """
            jpg=
            """);

        assertThrows(IllegalArgumentException.class, () -> Rules.load(rulesFile));
    }

    @Test
    void testGetTargetFolder_ValidExtension() {
        Map<String, String> rules = Map.of(
            "jpg", "Images",
            "pdf", "Documents"
        );

        assertEquals("Images", Rules.getTargetFolder(rules, "jpg"));
        assertEquals("Documents", Rules.getTargetFolder(rules, "pdf"));
    }

    @Test
    void testGetTargetFolder_UnknownExtension() {
        Map<String, String> rules = Map.of("jpg", "Images");

        assertNull(Rules.getTargetFolder(rules, "unknown"));
    }

    @Test
    void testGetTargetFolder_CaseInsensitive() {
        Map<String, String> rules = Map.of("jpg", "Images");

        assertEquals("Images", Rules.getTargetFolder(rules, "JPG"));
        assertEquals("Images", Rules.getTargetFolder(rules, "JpG"));
    }

    @Test
    void testGetTargetFolder_WithNullExtension() {
        Map<String, String> rules = Map.of("jpg", "Images");

        assertNull(Rules.getTargetFolder(rules, null));
    }

    @Test
    void testGetTargetFolder_WithEmptyExtension() {
        Map<String, String> rules = Map.of("jpg", "Images");

        assertNull(Rules.getTargetFolder(rules, ""));
    }

    @Test
    void testGetTargetFolder_WithNullRules() {
        assertThrows(NullPointerException.class,
            () -> Rules.getTargetFolder(null, "jpg"));
    }

    @Test
    void testLoad_ReturnsImmutableMap(@TempDir Path tempDir) throws IOException {
        Path rulesFile = tempDir.resolve("rules.properties");
        Files.writeString(rulesFile, "jpg=Images");

        Map<String, String> rules = Rules.load(rulesFile);

        assertThrows(UnsupportedOperationException.class,
            () -> rules.put("new", "value"));
    }
}
