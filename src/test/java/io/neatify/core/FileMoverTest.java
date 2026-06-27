package io.neatify.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.neatify.TestHelper;
import io.neatify.core.contract.FileMover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FileMoverTest extends TestHelper {

    private final LocalFileMover mover = new LocalFileMover();

    @Test
    void testPlan_BasicFunctionality(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "image.jpg", "test");
        createTestFile(tempDir, "document.pdf", "test");

        Map<String, String> rules = Map.of("jpg", "Images", "pdf", "Documents");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        assertEquals(2, actions.size());
        assertTrue(actions.stream().anyMatch(a -> a.source().getFileName().toString().equals("image.jpg")));
        assertTrue(actions.stream().anyMatch(a -> a.source().getFileName().toString().equals("document.pdf")));
    }

    @Test
    void testPlan_IgnoresHiddenFiles(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, ".hidden.jpg", "test");

        Map<String, String> rules = Map.of("jpg", "Images");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        assertEquals(0, actions.size());
    }

    @Test
    void testPlan_WithNestedFolders(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("subfolder");
        Files.createDirectory(subDir);
        createTestFile(subDir, "nested.jpg", "test");

        Map<String, String> rules = Map.of("jpg", "Images");
        List<FileMover.Action> actions = mover.plan(tempDir, rules, 100_000, List.of(), List.of(), true);

        assertEquals(1, actions.size());
        assertTrue(actions.get(0).source().toString().contains("nested.jpg"));
    }

    @Test
    void testExecute_DryRun(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.jpg", "content");
        Path source = tempDir.resolve("test.jpg");
        Path target = tempDir.resolve("Images").resolve("test.jpg");

        FileMover.Result result = mover.execute(List.of(createAction(source, target)), true, FileMover.CollisionStrategy.RENAME, null);

        assertTrue(Files.exists(source));
        assertFalse(Files.exists(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_RealMove(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.jpg", "content");
        Path source = tempDir.resolve("test.jpg");
        Path target = tempDir.resolve("Images").resolve("test.jpg");

        FileMover.Result result = mover.execute(List.of(createAction(source, target)), false, FileMover.CollisionStrategy.RENAME, null);

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

        Map<String, String> rules = Map.of("pdf", "Docs", "jpg", "Images", "txt", "Texts");

        var actions = mover.plan(tempDir, rules, 100_000,
            List.of("**/*.pdf", "**/*.txt"),
            List.of("**/sub/*.txt"),
            true
        );

        assertEquals(1, actions.stream().filter(a -> a.source().getFileName().toString().equals("a.pdf")).count());
        assertEquals(0, actions.stream().filter(a -> a.source().getFileName().toString().equals("b.jpg")).count());
        assertEquals(0, actions.stream().filter(a -> a.source().getFileName().toString().equals("c.txt")).count());
    }

    @Test
    void testExecute_CollisionStrategies(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "x.txt", "one");
        Path targetDir = tempDir.resolve("Dest");
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve("x.txt");
        Files.writeString(target, "existing");

        // SKIP
        FileMover.Result resSkip = mover.execute(
            List.of(createAction(tempDir.resolve("x.txt"), target)), false, FileMover.CollisionStrategy.SKIP, null);
        assertTrue(Files.exists(target));
        assertTrue(Files.exists(tempDir.resolve("x.txt")));
        assertEquals(1, resSkip.skipped());

        // OVERWRITE
        Files.writeString(tempDir.resolve("x.txt"), "two");
        FileMover.Result resOv = mover.execute(
            List.of(createAction(tempDir.resolve("x.txt"), target)), false, FileMover.CollisionStrategy.OVERWRITE, null);
        assertFalse(Files.exists(tempDir.resolve("x.txt")));
        assertEquals("two", Files.readString(target));
        assertEquals(1, resOv.moved());

        // RENAME
        Files.writeString(tempDir.resolve("x.txt"), "three");
        FileMover.Result resRn = mover.execute(
            List.of(createAction(tempDir.resolve("x.txt"), target)), false, FileMover.CollisionStrategy.RENAME, null);
        assertFalse(Files.exists(tempDir.resolve("x.txt")));
        assertTrue(Files.exists(target));
        assertTrue(Files.list(targetDir).anyMatch(p -> p.getFileName().toString().matches("x_\\d+\\.txt")));
        assertEquals(1, resRn.moved());
    }

    @Test
    void testExecute_CreatesTargetDirectory(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "test.jpg", "content");
        Path source = tempDir.resolve("test.jpg");
        Path target = tempDir.resolve("NewFolder").resolve("Images").resolve("test.jpg");

        FileMover.Result result = mover.execute(List.of(createAction(source, target)), false, FileMover.CollisionStrategy.RENAME, null);

        assertTrue(Files.exists(target.getParent()));
        assertTrue(Files.exists(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_MultipleFiles(@TempDir Path tempDir) throws IOException {
        createTestFile(tempDir, "image1.jpg", "image1");
        createTestFile(tempDir, "image2.jpg", "image2");
        createTestFile(tempDir, "doc.pdf", "document");

        List<FileMover.Action> actions = List.of(
            createAction(tempDir.resolve("image1.jpg"), tempDir.resolve("Images/image1.jpg")),
            createAction(tempDir.resolve("image2.jpg"), tempDir.resolve("Images/image2.jpg")),
            createAction(tempDir.resolve("doc.pdf"), tempDir.resolve("Docs/doc.pdf"))
        );

        FileMover.Result result = mover.execute(actions, false, FileMover.CollisionStrategy.RENAME, null);

        assertEquals(3, result.moved());
        assertTrue(Files.exists(tempDir.resolve("Images/image1.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Images/image2.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Docs/doc.pdf")));
    }
}
