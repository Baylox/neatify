package io.neatify.cli;

import io.neatify.cli.args.ArgumentParser;
import io.neatify.cli.args.CLIConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileOrganizationExecutor.
 */
class FileOrganizationExecutorTest {

    private final FileOrganizationExecutor executor = new FileOrganizationExecutor();

    @Test
    void execute_emptySource_doesNotThrow(@TempDir Path tempDir) {
        CLIConfig cfg = new ArgumentParser().parse(new String[]{
            "--source", tempDir.toString(),
            "--use-default-rules"
        });
        assertDoesNotThrow(() -> executor.execute(cfg));
    }

    @Test
    void execute_nonExistentSource_throwsIllegalArgument() {
        Path missing = Path.of(System.getProperty("java.io.tmpdir"), "neatify_no_such_dir_99999");
        CLIConfig cfg = new ArgumentParser().parse(new String[]{
            "--source", missing.toString(),
            "--use-default-rules"
        });
        assertThrows(IllegalArgumentException.class, () -> executor.execute(cfg));
    }

    @Test
    void execute_dryRun_doesNotMoveFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("report.pdf"), "data");

        CLIConfig cfg = new ArgumentParser().parse(new String[]{
            "--source", tempDir.toString(),
            "--use-default-rules"
            // no --apply → dry-run
        });
        executor.execute(cfg);

        // File must still be in its original location
        assertTrue(Files.exists(tempDir.resolve("report.pdf")),
            "Dry-run must not move files");
    }

    @Test
    void execute_applyMoves_filesRelocated(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("photo.jpg"), "img");

        CLIConfig cfg = new ArgumentParser().parse(new String[]{
            "--source", tempDir.toString(),
            "--use-default-rules",
            "--apply"
        });
        executor.execute(cfg);

        assertFalse(Files.exists(tempDir.resolve("photo.jpg")),
            "File should have been moved");
        assertTrue(Files.exists(tempDir.resolve("Images").resolve("photo.jpg")),
            "File should now be under Images/");
    }

    @Test
    void execute_jsonMode_outputsValidJson(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("song.mp3"), "audio");

        CLIConfig cfg = new ArgumentParser().parse(new String[]{
            "--source", tempDir.toString(),
            "--use-default-rules",
            "--json"
        });

        PrintStream prevOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            executor.execute(cfg);
        } finally {
            System.setOut(prevOut);
        }

        String json = baos.toString(StandardCharsets.UTF_8).trim();
        assertFalse(json.isEmpty(), "JSON output must not be empty");
        assertTrue(json.startsWith("{"), "JSON output must start with {");
        assertTrue(json.contains("\"planned\""), "JSON must contain 'planned' field");
        assertTrue(json.contains("\"actions\""), "JSON must contain 'actions' field");
        assertTrue(json.contains("\"result\""),  "JSON must contain 'result' field");
    }

    @Test
    void execute_insideGitRepo_withApply_withoutOverride_throws(@TempDir Path tempDir) throws IOException {
        // Simulate a git repo by creating a .git directory inside the source
        Files.createDirectory(tempDir.resolve(".git"));
        Files.writeString(tempDir.resolve("file.jpg"), "img");

        CLIConfig cfg = new ArgumentParser().parse(new String[]{
            "--source", tempDir.toString(),
            "--use-default-rules",
            "--apply"
            // no --allow-inside-git
        });
        assertThrows(IllegalArgumentException.class, () -> executor.execute(cfg),
            "--apply inside git repo should be blocked by default");
    }
}
