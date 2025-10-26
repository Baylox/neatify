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
 * Tests unitaires pour FileMover.
 */
class FileMoverTest {

    @Test
    void testPlan_WithMatchingRules(@TempDir Path tempDir) throws IOException {
        // Créer des fichiers de test
        Files.writeString(tempDir.resolve("image.jpg"), "test");
        Files.writeString(tempDir.resolve("document.pdf"), "test");

        Map<String, String> rules = Map.of(
            "jpg", "Images",
            "pdf", "Documents"
        );

        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(2, actions.size());

        // Vérifier les actions planifiées
        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals("image.jpg") &&
            a.target().toString().contains("Images")
        ));

        assertTrue(actions.stream().anyMatch(a ->
            a.source().getFileName().toString().equals("document.pdf") &&
            a.target().toString().contains("Documents")
        ));
    }

    @Test
    void testPlan_WithNoMatchingRules(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("file.unknown"), "test");

        Map<String, String> rules = Map.of("jpg", "Images");

        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(0, actions.size());
    }

    @Test
    void testPlan_WithFilesWithoutExtension(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("README"), "test");

        Map<String, String> rules = Map.of("jpg", "Images");

        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(0, actions.size());
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
        // Créer structure avec sous-dossiers
        Path subDir = tempDir.resolve("subfolder");
        Files.createDirectory(subDir);
        Files.writeString(subDir.resolve("nested.jpg"), "test");

        Map<String, String> rules = Map.of("jpg", "Images");

        List<FileMover.Action> actions = FileMover.plan(tempDir, rules);

        assertEquals(1, actions.size());
        assertTrue(actions.get(0).source().toString().contains("nested.jpg"));
    }

    @Test
    void testPlan_WithNullSourceRoot() {
        Map<String, String> rules = Map.of("jpg", "Images");

        assertThrows(NullPointerException.class,
            () -> FileMover.plan(null, rules));
    }

    @Test
    void testPlan_WithNullRules(@TempDir Path tempDir) {
        assertThrows(NullPointerException.class,
            () -> FileMover.plan(tempDir, null));
    }

    @Test
    void testPlan_WithNonExistentDirectory(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("nonexistent");
        Map<String, String> rules = Map.of("jpg", "Images");

        assertThrows(IllegalArgumentException.class,
            () -> FileMover.plan(nonExistent, rules));
    }

    @Test
    void testPlan_WithFileInsteadOfDirectory(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "test");

        Map<String, String> rules = Map.of("jpg", "Images");

        assertThrows(IllegalArgumentException.class,
            () -> FileMover.plan(file, rules));
    }

    @Test
    void testExecute_DryRun(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("test.jpg");
        Files.writeString(source, "content");

        Path target = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action = new FileMover.Action(source, target, "test");

        FileMover.Result result = FileMover.execute(List.of(action), true);

        // En dry-run, le fichier ne doit pas être déplacé
        assertTrue(Files.exists(source));
        assertFalse(Files.exists(target));
        assertEquals(1, result.moved());
        assertEquals(0, result.skipped());
    }

    @Test
    void testExecute_RealMove(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("test.jpg");
        Files.writeString(source, "content");

        Path target = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action = new FileMover.Action(source, target, "test");

        FileMover.Result result = FileMover.execute(List.of(action), false);

        // En mode réel, le fichier doit être déplacé
        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
        assertEquals("content", Files.readString(target));
        assertEquals(1, result.moved());
        assertEquals(0, result.skipped());
    }

    @Test
    void testExecute_WithCollision(@TempDir Path tempDir) throws IOException {
        // Créer deux fichiers avec le même nom
        Path source1 = tempDir.resolve("test.jpg");
        Path source2 = tempDir.resolve("test2.jpg");
        Files.writeString(source1, "content1");
        Files.writeString(source2, "content2");

        // Le premier déplacement
        Path target1 = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action1 = new FileMover.Action(source1, target1, "test");
        FileMover.execute(List.of(action1), false);

        // Le deuxième devrait créer test_1.jpg
        Path target2 = tempDir.resolve("Images").resolve("test.jpg");
        FileMover.Action action2 = new FileMover.Action(source2, target2, "test");

        // Simuler la résolution de collision en créant un nouveau nom
        Path resolvedTarget = tempDir.resolve("Images").resolve("test_1.jpg");
        FileMover.Action resolvedAction = new FileMover.Action(source2, resolvedTarget, "test");

        FileMover.Result result = FileMover.execute(List.of(resolvedAction), false);

        assertTrue(Files.exists(target1));
        assertTrue(Files.exists(resolvedTarget));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_CreatesTargetDirectory(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("test.jpg");
        Files.writeString(source, "content");

        Path targetDir = tempDir.resolve("NewFolder").resolve("Images");
        Path target = targetDir.resolve("test.jpg");

        FileMover.Action action = new FileMover.Action(source, target, "test");

        FileMover.Result result = FileMover.execute(List.of(action), false);

        assertTrue(Files.exists(targetDir));
        assertTrue(Files.exists(target));
        assertEquals(1, result.moved());
    }

    @Test
    void testExecute_WithNullActions() {
        assertThrows(NullPointerException.class,
            () -> FileMover.execute(null, false));
    }

    @Test
    void testExecute_WithEmptyActionsList(@TempDir Path tempDir) {
        FileMover.Result result = FileMover.execute(List.of(), false);

        assertEquals(0, result.moved());
        assertEquals(0, result.skipped());
        assertTrue(result.errors().isEmpty());
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
        assertEquals(0, result.skipped());
        assertTrue(Files.exists(tempDir.resolve("Images/image1.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Images/image2.jpg")));
        assertTrue(Files.exists(tempDir.resolve("Docs/doc.pdf")));
    }

    @Test
    void testAction_Record() {
        Path source = Path.of("source.txt");
        Path target = Path.of("target.txt");
        String reason = "test reason";

        FileMover.Action action = new FileMover.Action(source, target, reason);

        assertEquals(source, action.source());
        assertEquals(target, action.target());
        assertEquals(reason, action.reason());
    }

    @Test
    void testResult_Record() {
        List<String> errors = List.of("error1", "error2");
        FileMover.Result result = new FileMover.Result(10, 2, errors);

        assertEquals(10, result.moved());
        assertEquals(2, result.skipped());
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().contains("error1"));
        assertTrue(result.errors().contains("error2"));
    }
}
