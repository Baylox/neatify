package io.neatify.core;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the built-in default rules.
 */
class DefaultRulesTest {

    private final Map<String, String> rules = Rules.getDefaults();

    @Test
    void defaults_areNotEmpty() {
        assertNotNull(rules);
        assertFalse(rules.isEmpty(), "Default rules must not be empty");
    }

    @Test
    void defaults_imageExtensions_mapToImages() {
        assertEquals("Images", rules.get("jpg"));
        assertEquals("Images", rules.get("jpeg"));
        assertEquals("Images", rules.get("png"));
        assertEquals("Images", rules.get("gif"));
        assertEquals("Images", rules.get("svg"));
        assertEquals("Images", rules.get("webp"));
    }

    @Test
    void defaults_documentExtensions_mapToDocuments() {
        assertEquals("Documents", rules.get("pdf"));
        assertEquals("Documents", rules.get("doc"));
        assertEquals("Documents", rules.get("docx"));
        assertEquals("Documents", rules.get("txt"));
        assertEquals("Documents", rules.get("md"));
    }

    @Test
    void defaults_spreadsheetExtensions_mapToSubfolder() {
        assertEquals("Documents/Spreadsheets", rules.get("xls"));
        assertEquals("Documents/Spreadsheets", rules.get("xlsx"));
        assertEquals("Documents/Spreadsheets", rules.get("csv"));
    }

    @Test
    void defaults_presentationExtensions_mapToSubfolder() {
        assertEquals("Documents/Presentations", rules.get("ppt"));
        assertEquals("Documents/Presentations", rules.get("pptx"));
    }

    @Test
    void defaults_archiveExtensions_mapToArchives() {
        assertEquals("Archives", rules.get("zip"));
        assertEquals("Archives", rules.get("rar"));
        assertEquals("Archives", rules.get("7z"));
        assertEquals("Archives", rules.get("tar"));
    }

    @Test
    void defaults_videoExtensions_mapToVideos() {
        assertEquals("Videos", rules.get("mp4"));
        assertEquals("Videos", rules.get("mkv"));
        assertEquals("Videos", rules.get("avi"));
    }

    @Test
    void defaults_musicExtensions_mapToMusic() {
        assertEquals("Music", rules.get("mp3"));
        assertEquals("Music", rules.get("wav"));
        assertEquals("Music", rules.get("flac"));
    }

    @Test
    void defaults_codeExtensions_mapToCode() {
        assertEquals("Code", rules.get("java"));
        assertEquals("Code", rules.get("py"));
        assertEquals("Code", rules.get("js"));
        assertEquals("Code", rules.get("ts"));
        assertEquals("Code", rules.get("go"));
    }

    @Test
    void defaults_mapIsImmutable() {
        assertThrows(UnsupportedOperationException.class,
            () -> rules.put("xyz", "Test"),
            "Default rules map must be immutable");
    }

    @Test
    void defaults_unknownExtension_returnsNull() {
        assertNull(Rules.getTargetFolder(rules, "unknownxyz"));
    }
}
