package io.neatify.core.security;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.neatify.core.contract.FileMover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FileMoverPathTraversalTest extends FileMoverSecurityTestBase {

    @Test
    void testPathTraversal_SecondLevelProtection(@TempDir Path tempDir) throws IOException {
        assertMaliciousRuleBlockedForFile(tempDir, "test.jpg", "jpg", "ValidFolder/../../../etc");
    }

    @Test
    void testResolvedPath_StaysInSourceRoot(@TempDir Path tempDir) throws IOException {
        assertMaliciousRuleBlockedForFile(tempDir, "document.pdf", "pdf", "../../../Windows");
    }

    @Test
    void testValidNestedPath_Works(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "photo.jpg");

        Map<String, String> validRules = Map.of("jpg", "Media/Photos/Vacation");
        List<FileMover.Action> actions = mover.plan(tempDir, validRules, 100_000, List.of(), List.of(), true);

        assertEquals(1, actions.size());

        FileMover.Action action = actions.get(0);
        Path resolvedTarget = action.target().normalize();
        Path normalizedSource = tempDir.normalize();

        assertTrue(resolvedTarget.startsWith(normalizedSource),
            "The resolved path should stay within the source folder");
        assertTrue(resolvedTarget.toString().contains("Media"));
        assertTrue(resolvedTarget.toString().contains("Photos"));
        assertTrue(resolvedTarget.toString().contains("Vacation"));
    }

    @Test
    void testAbsolutePath_Blocked(@TempDir Path tempDir) throws IOException {
        assertMaliciousRuleBlockedForFile(tempDir, "script.exe", "exe", "/etc/malicious");
    }

    @Test
    void testMixedRules_OnlyValidProcessed(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "image.jpg");
        createTestFile(tempDir, "document.pdf");
        createTestFile(tempDir, "script.exe");

        Map<String, String> mixedRules = Map.of(
            "jpg", "Images",
            "pdf", "../../../etc",
            "exe", "Applications/Tools"
        );

        List<FileMover.Action> actions = mover.plan(tempDir, mixedRules, 100_000, List.of(), List.of(), true);

        assertEquals(2, actions.size(), "Only valid rules should generate actions");
        assertActionExists(actions, "image.jpg");
        assertActionExists(actions, "script.exe");
        assertActionNotExists(actions, "document.pdf", "document.pdf should NOT be processed (malicious rule)");
    }

    @Test
    void testPathNormalization(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.txt");

        Map<String, String> rules = Map.of("txt", "Documents/./Subfolder");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        assertEquals(1, actions.size());

        Path targetPath = actions.get(0).target().normalize();
        assertFalse(targetPath.toString().contains("/./"),
            "The path should not contain './' after normalization");
    }
}
