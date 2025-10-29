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
    void testPlan_WithIncludeExclude(@TempDir Path tempDir) throws IOException {
        Path sub = tempDir.resolve("sub");
        Files.createDirectory(sub);
        createTestFile(tempDir, "a.pdf", "a");
        createTestFile(tempDir, "b.jpg", "b");
        createTestFile(sub, "c.txt", "c");

        Map<String, String> rules = Map.of(
            "pdf", "Docs",
            "jpg", "Images",
            "txt", "Texts"
        );

        var actions = FileMover.plan(tempDir, rules, 100000,
            java.util.List.of("**/*.pdf", "**/*.txt"),
            java.util.List.of("**/sub/*.txt")
        );

        assertEquals(1, actions.stream().filter(a -> a.source().getFileName().toString().equals("a.pdf")).count());
        assertEquals(0, actions.stream().filter(a -> a.source().getFileName().toString().equals("b.jpg")).count());
        assertEquals(0, actions.stream().filter(a -> a.source().getFileName().toString().equals("c.txt")).count());
    }

    @Test
    void testExecute_CollisionStrategies(@TempDir Path tempDir) throws IOException {
        // Prepare files
        createTestFile(tempDir, "x.txt", "one");
        Path targetDir = tempDir.resolve("Dest");
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve("x.txt");
        Files.writeString(target, "existing");

        // SKIP: should not overwrite and should skip
        FileMover.Action actionSkip = createAction(tempDir.resolve("x.txt"), target);
        FileMover.Result resSkip = FileMover.execute(java.util.List.of(actionSkip), false, FileMover.CollisionStrategy.SKIP);
        assertTrue(Files.exists(target));
        assertTrue(Files.exists(tempDir.resolve("x.txt"))); // not moved
        assertEquals(1, resSkip.skipped());

        // OVERWRITE
        Files.writeString(tempDir.resolve("x.txt"), "two");
        FileMover.Action actionOv = createAction(tempDir.resolve("x.txt"), target);
        FileMover.Result resOv = FileMover.execute(java.util.List.of(actionOv), false, FileMover.CollisionStrategy.OVERWRITE);
        assertFalse(Files.exists(tempDir.resolve("x.txt")));
        assertEquals("two", Files.readString(target));
        assertEquals(1, resOv.moved());

        // RENAME
        // recreate source
        Files.writeString(tempDir.resolve("x.txt"), "three");
        FileMover.Action actionRn = createAction(tempDir.resolve("x.txt"), target);
        FileMover.Result resRn = FileMover.execute(java.util.List.of(actionRn), false, FileMover.CollisionStrategy.RENAME);
        assertFalse(Files.exists(tempDir.resolve("x.txt")));
        // original target remains, a new file with suffix exists
        assertTrue(Files.exists(target));
        assertTrue(Files.list(targetDir).anyMatch(p -> p.getFileName().toString().matches("x_\\d+\\.txt")));
        assertEquals(1, resRn.moved());
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
