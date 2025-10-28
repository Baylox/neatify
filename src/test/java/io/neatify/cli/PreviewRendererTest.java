package io.neatify.cli;

import io.neatify.TestHelper;
import io.neatify.cli.util.Ansi;
import io.neatify.cli.util.AsciiSymbols;
import io.neatify.cli.util.PreviewRenderer;
import io.neatify.core.FileMover;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests essentiels pour PreviewRenderer - Focus sur les cas critiques uniquement.
 */
class PreviewRendererTest extends TestHelper {

    @BeforeEach
    void setUp() {
        Ansi.setEnabled(false);
        AsciiSymbols.setUseUnicode(false);
    }

    @Test
    void testRender_EmptyActions() {
        List<FileMover.Action> actions = List.of();
        PreviewRenderer.Config config = new PreviewRenderer.Config();

        List<String> lines = PreviewRenderer.render(actions, config);

        assertTrue(lines.isEmpty());
    }

    @Test
    void testRender_SingleFile() {
        Path source = Paths.get("/tmp/test.txt");
        Path target = Paths.get("/tmp/Documents/test.txt");
        FileMover.Action action = createAction(source, target);

        List<FileMover.Action> actions = List.of(action);
        PreviewRenderer.Config config = new PreviewRenderer.Config();

        List<String> lines = PreviewRenderer.render(actions, config);

        assertFalse(lines.isEmpty());
        String output = String.join("\n", lines);
        assertTrue(output.contains("Documents"));
        assertTrue(output.contains("test.txt"));
    }

    @Test
    void testRender_MultipleFolders() {
        List<FileMover.Action> actions = List.of(
            createAction(
                Paths.get("/tmp/doc.pdf"),
                Paths.get("/tmp/Documents/doc.pdf")
            ),
            createAction(
                Paths.get("/tmp/photo.jpg"),
                Paths.get("/tmp/Images/photo.jpg")
            )
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config();
        List<String> lines = PreviewRenderer.render(actions, config);

        String output = String.join("\n", lines);
        assertTrue(output.contains("Documents"));
        assertTrue(output.contains("Images"));
        assertTrue(output.contains("doc.pdf"));
        assertTrue(output.contains("photo.jpg"));
    }

    @Test
    void testRender_DuplicateCounting() {
        List<FileMover.Action> actions = List.of(
            createAction(Paths.get("/tmp/1/photo.jpg"), Paths.get("/tmp/Images/photo.jpg")),
            createAction(Paths.get("/tmp/2/photo.jpg"), Paths.get("/tmp/Images/photo.jpg")),
            createAction(Paths.get("/tmp/3/photo.jpg"), Paths.get("/tmp/Images/photo.jpg"))
        );

        PreviewRenderer.Config config = new PreviewRenderer.Config().showDuplicates(true);
        List<String> lines = PreviewRenderer.render(actions, config);

        String output = String.join("\n", lines);
        assertTrue(output.contains("photo.jpg"));
        assertTrue(output.contains("x3"));
    }
}
