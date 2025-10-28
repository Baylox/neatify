package io.neatify.core;

import io.neatify.TestHelper;
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
class FileMoverTest extends TestHelper {

    @Test
    void testPlan_BasicFunctionality(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "image.jpg", "test");
        createTestFile(tempDir, "document.pdf", "test");

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
        createTestFile(tempDir, ".hidden.jpg", "test");

        Map<String, String> rules = Map.of("jpg", "Images");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(0, actions.size());
    }

    @Test
    void testPlan_WithNestedFolders(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("subfolder");
        Files.createDirectory(subDir);
        createTestFile(subDir, "nested.jpg", "test");

        Map<String, String> rules = Map.of("jpg", "Images");
        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(1, actions.size());
        assertTrue(actions.get(0).source().toString().contains("nested.jpg"));
    }

    @Test
    void testExecute_DryRun(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.jpg", "content");
        Path source = tempDir.resolve("test.jpg");
        Path target = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action = createAction(source, target);

        FileMover.Result result = FileMover.execute(List.of(action), true);

        assertTrue(Files.exists(source));
        assertFalse(Files.exists(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_RealMove(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.jpg", "content");
        Path source = tempDir.resolve("test.jpg");
        Path target = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action = createAction(source, target);

        FileMover.Result result = FileMover.execute(List.of(action), false);

        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
        assertEquals("content", Files.readString(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_CreatesTargetDirectory(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.jpg", "content");
        Path source = tempDir.resolve("test.jpg");
        Path targetDir = tempDir.resolve("NewFolder").resolve("Images");
        Path target = targetDir.resolve("test.jpg");
        FileMover.Action action = createAction(source, target);

        FileMover.Result result = FileMover.execute(List.of(action), false);

        assertTrue(Files.exists(targetDir));
        assertTrue(Files.exists(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_MultipleFiles(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "image1.jpg", "image1");
        createTestFile(tempDir, "image2.jpg", "image2");
        createTestFile(tempDir, "doc.pdf", "document");

        Path file1 = tempDir.resolve("image1.jpg");
        Path file2 = tempDir.resolve("image2.jpg");
        Path file3 = tempDir.resolve("doc.pdf");

        List<FileMover.Action> actions = List.of(
            createAction(file1, tempDir.resolve("Images/image1.jpg")),
            createAction(file2, tempDir.resolve("Images/image2.jpg")),
            createAction(file3, tempDir.resolve("Docs/doc.pdf"))
        );

        FileMover.Result result = FileMover.execute(actions, false);

        assertEquals(3, result.moved());
        assertTrue(Files.exists(tempDir.resolve("Images/image1.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Images/image2.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Docs/doc.pdf")));
    }
}
