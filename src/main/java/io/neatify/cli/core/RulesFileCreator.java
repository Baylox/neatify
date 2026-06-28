package io.neatify.cli.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.neatify.cli.ui.Console;
import io.neatify.core.PathSecurity;

import static io.neatify.cli.ui.Display.*;

/**
 * Handles creating rules files in interactive mode.
 */
public final class RulesFileCreator {

    private final Console console;

    public RulesFileCreator(Console console) {
        this.console = console;
    }

    /**
     * Starts the full flow to create a rules file.
     */
    public void create() throws IOException {
        printSection("CREATE A RULES FILE");

        String filename = console.readInput("File name", "custom-rules/my-rules.properties");
        Path rulesFile = Paths.get(filename).toAbsolutePath().normalize();

        if (!validateSecurity(rulesFile)) return;
        boolean fileExists = Files.exists(rulesFile);
        if (!confirmOverwriteIfExists(rulesFile)) return;

        String content = generateDefaultContent();

        createParentDirectoryIfNeeded(rulesFile);

        if (!writeSecurely(rulesFile, content, fileExists)) return;

        printCreated(rulesFile);
    }

    private boolean validateSecurity(Path rulesFile) {
        Path safeDir = Paths.get("custom-rules").toAbsolutePath().normalize();
        Path target = rulesFile.toAbsolutePath().normalize();

        if (!target.startsWith(safeDir)) {
            printError("SECURITY: The file must be inside the custom-rules/ folder");
            console.waitForEnter();
            return false;
        }

        Path relativeTarget = safeDir.relativize(target);
        try {
            PathSecurity.validateRelativeSubpath(relativeTarget.toString());
        } catch (SecurityException | IllegalArgumentException e) {
            printError("SECURITY: " + e.getMessage());
            console.waitForEnter();
            return false;
        }

        return true;
    }

    private boolean confirmOverwriteIfExists(Path rulesFile) {
        if (Files.exists(rulesFile)) {
            String overwrite = console.readInput("File already exists. Overwrite? (y/N)", "n");
            if (!"y".equalsIgnoreCase(overwrite) && !"yes".equalsIgnoreCase(overwrite)) {
                printWarning("Operation cancelled.");
                console.waitForEnter();
                return false;
            }
        }
        return true;
    }

    private static String generateDefaultContent() {
        return """
            # Neatify organization rules
            # Format: extension=TargetFolder

            # Images
            jpg=Images
            png=Images
            gif=Images

            # Documents
            pdf=Documents
            docx=Documents
            txt=Documents

            # Archives
            zip=Archives
            rar=Archives

            # Videos
            mp4=Videos
            avi=Videos

            # Code
            java=Code
            py=Code
            js=Code
            """;
    }

    private static void createParentDirectoryIfNeeded(Path rulesFile) throws IOException {
        Path parentDir = rulesFile.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            printInfo("Directory created: " + parentDir);
        }
    }

    private boolean writeSecurely(Path rulesFile, String content, boolean overwrite) throws IOException {
        // SECURITY: the real (symlink-resolved) target must stay inside custom-rules/
        try {
            Path safeDir = Paths.get("custom-rules").toAbsolutePath().normalize();
            PathSecurity.assertResolvedWithin(safeDir, rulesFile);
        } catch (SecurityException e) {
            printError("SECURITY: " + e.getMessage());
            console.waitForEnter();
            return false;
        }

        // If overwrite was confirmed, delete existing file before atomic CREATE_NEW
        if (overwrite) {
            Files.deleteIfExists(rulesFile);
        }

        // SECURITY: Atomic write via CREATE_NEW (anti-TOCTOU)
        try {
            Files.writeString(rulesFile, content,
                java.nio.file.StandardOpenOption.CREATE_NEW);
            return true;
        } catch (java.nio.file.FileAlreadyExistsException e) {
            // If we get here, another process created the file meanwhile (race condition)
            printError("SECURITY: File was created by another process");
            console.waitForEnter();
            return false;
        }
    }

    private void printCreated(Path rulesFile) {
        printSuccess("File created: " + rulesFile.toAbsolutePath());
        printInfo("You can now edit it to customize rules.");
        printInfo("Note: This file will not be versioned by Git.");
        console.waitForEnter();
    }
}
