package io.neatify.core.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.neatify.TestHelper;
import io.neatify.core.LocalFileMover;
import io.neatify.core.contract.FileMover;

import static org.junit.jupiter.api.Assertions.*;

public abstract class FileMoverSecurityTestBase extends TestHelper {

    protected final LocalFileMover mover = new LocalFileMover();

    protected void assertActionExists(List<FileMover.Action> actions, String filename) {
        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals(filename)),
            "Action should exist for file: " + filename);
    }

    protected void assertActionNotExists(List<FileMover.Action> actions, String filename, String message) {
        assertFalse(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals(filename)), message);
    }

    protected void assertMaliciousRuleBlockedForFile(Path tempDir, String filename, String extension,
                                                     String maliciousTarget) throws IOException {
        createTestFile(tempDir, filename);
        Map<String, String> maliciousRules = Map.of(extension, maliciousTarget);
        List<FileMover.Action> actions = mover.plan(tempDir, maliciousRules, 100_000, List.of(), List.of(), true);
        assertEquals(0, actions.size(), "Rules with path traversal should not generate any actions");
    }

    protected void setupCollisionScenario(Path tempDir, String baseFilename, String... existingContents)
            throws IOException {
        Path targetDir = tempDir.resolve("Documents");
        Files.createDirectories(targetDir);

        Files.writeString(targetDir.resolve(baseFilename), existingContents[0]);
        for (int i = 1; i < existingContents.length; i++) {
            String filename = baseFilename.replaceFirst("\\.", "_" + i + ".");
            Files.writeString(targetDir.resolve(filename), existingContents[i]);
        }
    }
}
