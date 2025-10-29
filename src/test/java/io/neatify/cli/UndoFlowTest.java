package io.neatify.cli;

import io.neatify.cli.args.ArgumentParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UndoFlowTest {

    @Test
    void applyThenUndo_WritesRunAndRestoresFiles() throws IOException {
        Path src = Files.createTempDirectory("neatify-undo-");
        try {
            // Create a few files matching default rules
            Path f1 = Files.writeString(src.resolve("doc1.pdf"), "hello");
            Path f2 = Files.writeString(src.resolve("img1.jpg"), "img");
            Path f3 = Files.writeString(src.resolve("song.mp3"), "music");

            // Execute CLI flow with default rules and apply
            var cfg = new ArgumentParser().parse(new String[]{
                "--source", src.toString(),
                "--use-default-rules",
                "--apply"
            });
            FileOrganizationExecutor exec = new FileOrganizationExecutor();
            exec.execute(cfg);

            // Verify files moved under expected folders
            assertFalse(Files.exists(f1));
            assertTrue(Files.exists(src.resolve("Documents").resolve("doc1.pdf")));
            assertTrue(Files.exists(src.resolve("Images").resolve("img1.jpg"))
                    || Files.exists(src.resolve("Images").resolve("img1.JPG")));
            assertTrue(Files.exists(src.resolve("Music").resolve("song.mp3")));

            // There should be at least one run file
            Path runs = src.resolve(".neatify").resolve("runs");
            assertTrue(Files.exists(runs));
            try (var s = Files.list(runs)) {
                assertTrue(s.anyMatch(p -> p.getFileName().toString().endsWith(".json")));
            }

            // Undo last run
            var result = io.neatify.cli.core.UndoExecutor.undoLast(src);
            assertNotNull(result);
            assertTrue(result.restored() >= 3);

            // Files restored to original locations
            assertTrue(Files.exists(f1));
            assertTrue(Files.exists(f2));
            assertTrue(Files.exists(f3));
        } finally {
            // cleanup
            try (var s = Files.walk(src)) {
                s.sorted((a,b)->b.compareTo(a)).forEach(p -> { try { Files.deleteIfExists(p);} catch (IOException ignore) {} });
            }
        }
    }
}
