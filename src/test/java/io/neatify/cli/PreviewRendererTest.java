package io.neatify.cli;

import java.nio.file.Path;
import java.util.List;

import io.neatify.TestHelper;
import io.neatify.cli.ui.Preview;
import io.neatify.cli.ui.Theme;
import io.neatify.core.contract.FileMover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Essential tests for Preview - Focus on critical cases only.
 */
class PreviewRendererTest extends TestHelper {

    /** Deterministic plain output (no ANSI color, ASCII symbols). */
    private Preview.Config plainConfig() {
        return new Preview.Config().theme(Theme.plain());
    }

    @Test
    void testRender_EmptyActions() {
        List<FileMover.Action> actions = List.of();
        Preview.Config config = plainConfig();

        List<String> lines = Preview.render(actions, config);

        assertTrue(lines.isEmpty());
    }

    @Test
    void testRender_SingleFile(@TempDir Path tempDir) {
        Path source = tempDir.resolve("test.txt");
        Path target = tempDir.resolve("Documents").resolve("test.txt");
        FileMover.Action action = createAction(source, target);

        List<FileMover.Action> actions = List.of(action);
        Preview.Config config = plainConfig();

        List<String> lines = Preview.render(actions, config);

        assertFalse(lines.isEmpty());
        String output = String.join("\n", lines);
        assertTrue(output.contains("Documents"));
        assertTrue(output.contains("test.txt"));
    }

    @Test
    void testRender_MultipleFolders(@TempDir Path tempDir) {
        List<FileMover.Action> actions = List.of(
            createAction(
                tempDir.resolve("doc.pdf"),
                tempDir.resolve("Documents").resolve("doc.pdf")
            ),
            createAction(
                tempDir.resolve("photo.jpg"),
                tempDir.resolve("Images").resolve("photo.jpg")
            )
        );

        Preview.Config config = plainConfig();
        List<String> lines = Preview.render(actions, config);

        String output = String.join("\n", lines);
        assertTrue(output.contains("Documents"));
        assertTrue(output.contains("Images"));
        assertTrue(output.contains("doc.pdf"));
        assertTrue(output.contains("photo.jpg"));
    }

    @Test
    void testRender_DuplicateCounting(@TempDir Path tempDir) {
        // Three source files with the same name targeting the same folder
        Path sub1 = tempDir.resolve("sub1");
        Path sub2 = tempDir.resolve("sub2");
        Path sub3 = tempDir.resolve("sub3");
        Path imagesDir = tempDir.resolve("Images");

        List<FileMover.Action> actions = List.of(
            createAction(sub1.resolve("photo.jpg"), imagesDir.resolve("photo.jpg")),
            createAction(sub2.resolve("photo.jpg"), imagesDir.resolve("photo.jpg")),
            createAction(sub3.resolve("photo.jpg"), imagesDir.resolve("photo.jpg"))
        );

        Preview.Config config = plainConfig().showDuplicates(true);
        List<String> lines = Preview.render(actions, config);

        String output = String.join("\n", lines);
        assertTrue(output.contains("photo.jpg"));
        assertTrue(output.contains("x3"));
    }
}
