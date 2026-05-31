package io.neatify.cli.ui;

import io.neatify.core.FileMover;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PreviewSizeSortTest {

    @Test
    void testSortBySizeDescending(@TempDir Path tmp) throws IOException {
        Path fSmall  = tmp.resolve("a.txt");
        Path fMedium = tmp.resolve("b.txt");
        Path fLarge  = tmp.resolve("c.txt");
        Files.writeString(fSmall,  "12345");           // 5 bytes
        Files.writeString(fMedium, "1".repeat(200));   // 200 bytes
        Files.writeString(fLarge,  "1".repeat(1000));  // 1000 bytes

        Path targetDir = tmp.resolve("Test");
        List<FileMover.Action> actions = List.of(
            new FileMover.Action(fSmall,  targetDir.resolve("a.txt"), "test"),
            new FileMover.Action(fMedium, targetDir.resolve("b.txt"), "test"),
            new FileMover.Action(fLarge,  targetDir.resolve("c.txt"), "test")
        );

        Preview.Config cfg = new Preview.Config()
            .maxFilesPerFolder(10)
            .sortMode(Preview.SortMode.SIZE)
            .showDuplicates(false);

        List<String> lines = Preview.render(actions, cfg);

        // Find the "Test/" group header (strip ANSI codes before matching)
        int idxTestHeader = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).replaceAll("\u001B\\[[;\\d]*m", "").contains("Test/")) {
                idxTestHeader = i; break;
            }
        }
        assertTrue(idxTestHeader >= 0, "Folder header not found in preview");

        // The three file entry lines immediately follow the header
        String l1 = lines.get(idxTestHeader + 1);
        String l2 = lines.get(idxTestHeader + 2);
        String l3 = lines.get(idxTestHeader + 3);

        // Expect c.txt (1000 B) first, then b.txt (200 B), then a.txt (5 B)
        assertTrue(l1.contains("c.txt"), "First should be largest file (c.txt): " + l1);
        assertTrue(l2.contains("b.txt"), "Second should be medium file (b.txt): " + l2);
        assertTrue(l3.contains("a.txt"), "Third should be smallest file (a.txt): " + l3);
    }
}
