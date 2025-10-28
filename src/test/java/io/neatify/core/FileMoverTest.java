package io.neatify.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests essentiels pour FileMover.
 */
class FileMoverTest {

    private Path createTestFile(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void testPlan_BasicFunctionality(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("image.jpg"), "test");
        Files.writeString(tempDir.resolve("document.pdf"), "test");

        Map<String, String> rules = Map.of(
            "jpg", "Images",
            "pdf", "Documents"
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(2, actions.size());
        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals("image.jpg")));
        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals("document.pdf")));
    }

    @Test
    void testPlan_IgnoresHiddenFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve(".hidden.jpg"), "test");

        Map<String, String> rules = Map.of("jpg", "Images");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(0, actions.size());
    }

    @Test
    void testPlan_WithNestedFolders(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("subfolder");
        Files.createDirectory(subDir);
        Files.writeString(subDir.resolve("nested.jpg"), "test");

        Map<String, String> rules = Map.of("jpg", "Images");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(1, actions.size());
        assertTrue(actions.get(0).source().toString().contains("nested.jpg"));
    }

    @Test
    void testExecute_DryRun(@TempDir Path tempDir) throws IOException {
        Path source = createTestFile(tempDir, "test.jpg", "content");
        Path target = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action = new FileMover.Action(source, target, "test");

        FileMover.Result result = FileMover.execute(List.of(action), true);

        assertTrue(Files.exists(source));
        assertFalse(Files.exists(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_RealMove(@TempDir Path tempDir) throws IOException {
        Path source = createTestFile(tempDir, "test.jpg", "content");
        Path target = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action = new FileMover.Action(source, target, "test");

        FileMover.Result result = FileMover.execute(List.of(action), false);

        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
        assertEquals("content", Files.readString(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_CreatesTargetDirectory(@TempDir Path tempDir) throws IOException {
        Path source = createTestFile(tempDir, "test.jpg", "content");
        Path targetDir = tempDir.resolve("NewFolder").resolve("Images");
        Path target = targetDir.resolve("test.jpg");
        FileMover.Action action = new FileMover.Action(source, target, "test");

        FileMover.Result result = FileMover.execute(List.of(action), false);

        assertTrue(Files.exists(targetDir));
        assertTrue(Files.exists(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_MultipleFiles(@TempDir Path tempDir) throws IOException {
        Path file1 = tempDir.resolve("image1.jpg");
        Path file2 = tempDir.resolve("image2.jpg");
        Path file3 = tempDir.resolve("doc.pdf");

        Files.writeString(file1, "image1");
        Files.writeString(file2, "image2");
        Files.writeString(file3, "document");

        List<FileMover.Action> actions = List.of(
            new FileMover.Action(file1, tempDir.resolve("Images/image1.jpg"), "test"),
            new FileMover.Action(file2, tempDir.resolve("Images/image2.jpg"), "test"),
            new FileMover.Action(file3, tempDir.resolve("Docs/doc.pdf"), "test")
        );

        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(3, result.moved());
        assertTrue(Files.exists(tempDir.resolve("Images/image1.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Images/image2.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Docs/doc.pdf")));
    }
}
