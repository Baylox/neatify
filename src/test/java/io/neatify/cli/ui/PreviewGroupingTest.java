package io.neatify.cli.ui;

import io.neatify.core.FileMover;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that Preview.render() groups actions by their full destination path,
 * preventing collisions between distinct folders that share the same last component.
 *
 * Regression for: groupByFolder used only the last path component as map key, causing
 * e.g. "Work/Images" and "Personal/Images" to be merged into a single "Images" group.
 */
class PreviewGroupingTest {

    private FileMover.Action action(Path source, Path target) {
        return new FileMover.Action(source, target, "test");
    }

    @Test
    void render_distinctFoldersWithSameLastComponent_areKeptSeparate(@TempDir Path root) {
        // Two files going to different folders that share the last component "Images"
        Path src1 = root.resolve("photo.jpg");
        Path src2 = root.resolve("avatar.png");
        Path tgt1 = root.resolve("Work").resolve("Images").resolve("photo.jpg");
        Path tgt2 = root.resolve("Personal").resolve("Images").resolve("avatar.png");

        List<FileMover.Action> actions = List.of(action(src1, tgt1), action(src2, tgt2));
        Preview.Config cfg = new Preview.Config().maxFilesPerFolder(10);

        List<String> lines = Preview.render(actions, cfg);

        // The rendered output must contain two separate folder headers
        long arrowLines = lines.stream()
            .filter(l -> l.contains("Images"))
            .count();
        // Both "Work/Images" and "Personal/Images" contribute a header mentioning "Images"
        assertTrue(arrowLines >= 2,
            "Expected 2 folder headers containing 'Images' but found " + arrowLines +
            ". Lines: " + lines);
    }

    @Test
    void render_singleFolder_showsCorrectFileCount(@TempDir Path root) {
        Path src1 = root.resolve("a.jpg");
        Path src2 = root.resolve("b.jpg");
        Path tgt1 = root.resolve("Images").resolve("a.jpg");
        Path tgt2 = root.resolve("Images").resolve("b.jpg");

        List<FileMover.Action> actions = List.of(action(src1, tgt1), action(src2, tgt2));
        Preview.Config cfg = new Preview.Config().maxFilesPerFolder(10);

        List<String> lines = Preview.render(actions, cfg);

        assertTrue(lines.stream().anyMatch(l -> l.contains("2 files")),
            "Expected '2 files' in the header. Lines: " + lines);
    }

}
