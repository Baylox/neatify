package io.neatify.cli.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import io.neatify.cli.ui.Console;
import io.neatify.cli.ui.Theme;
import io.neatify.core.FileSystemRunJournal;
import io.neatify.core.LocalFileMover;
import io.neatify.core.OrganizationService;
import io.neatify.core.PropertiesRulesProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the interactive {@link FileOrganizer} end-to-end with a scripted {@link Console}.
 * This is only possible because console input is now an injectable seam rather than a static
 * {@code Scanner}.
 */
class FileOrganizerInteractiveTest {

    @Test
    void organize_scriptedConfirmation_movesFilesAndWritesJournal(@TempDir Path source) throws IOException {
        Files.writeString(source.resolve("photo.jpg"), "img");

        ScriptedConsole console = new ScriptedConsole(List.of(
            source.toString(), // Folder to organize
            "",                 // Rules file -> default rules
            "",                 // Include glob -> none
            "",                 // Exclude glob -> none
            "y",                // Apply these changes?
            "rename"            // Collision strategy
        ));

        OrganizationService service = new OrganizationService(new LocalFileMover(), new FileSystemRunJournal());
        FileOrganizer organizer = new FileOrganizer(
            new PropertiesRulesProvider(), service, console, Theme.plain());

        organizer.organize();

        assertFalse(Files.exists(source.resolve("photo.jpg")), "source file should have been moved");
        assertTrue(Files.exists(source.resolve("Images").resolve("photo.jpg")), "file should be under Images/");
        assertTrue(Files.isDirectory(source.resolve(".neatify").resolve("runs")), "a run journal should be written");
    }

    @Test
    void organize_declined_leavesFilesInPlace(@TempDir Path source) throws IOException {
        Files.writeString(source.resolve("photo.jpg"), "img");

        ScriptedConsole console = new ScriptedConsole(List.of(
            source.toString(),
            "",
            "",
            "",
            "n" // decline
        ));

        OrganizationService service = new OrganizationService(new LocalFileMover(), new FileSystemRunJournal());
        FileOrganizer organizer = new FileOrganizer(
            new PropertiesRulesProvider(), service, console, Theme.plain());

        organizer.organize();

        assertTrue(Files.exists(source.resolve("photo.jpg")), "declining must leave the file untouched");
    }

    /** A {@link Console} that returns scripted answers in order; output is ignored. */
    private static final class ScriptedConsole implements Console {
        private final Deque<String> answers;

        ScriptedConsole(List<String> answers) {
            this.answers = new ArrayDeque<>(answers);
        }

        @Override
        public String readInput(String prompt) {
            return readInput(prompt, null);
        }

        @Override
        public String readInput(String prompt, String defaultValue) {
            if (answers.isEmpty()) {
                return defaultValue != null ? defaultValue : "";
            }
            return answers.poll();
        }

        @Override
        public void waitForEnter() {
            // no-op for tests
        }
    }
}
