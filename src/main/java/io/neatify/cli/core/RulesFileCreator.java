package io.neatify.cli.core;

import io.neatify.core.PathSecurity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.neatify.cli.ui.Display.*;

/**
 * Handles creating rules files in interactive mode.
 */
public final class RulesFileCreator {

    private RulesFileCreator() {
        // Utility class
    }

    /**
     * Starts the full flow to create a rules file.
     */
    public static void create() throws IOException {
        printSection("CREATE A RULES FILE");

        String filename = readInput("File name", "custom-rules/my-rules.properties");
        Path rulesFile = Paths.get(filename).toAbsolutePath().normalize();

        if (!validateSecurity(rulesFile)) return;
        if (!confirmOverwriteIfExists(rulesFile)) return;

        String content = generateDefaultContent();

        createParentDirectoryIfNeeded(rulesFile);

        if (!writeSecurely(rulesFile, content)) return;

        printSuccess(rulesFile);
    }

    private static boolean validateSecurity(Path rulesFile) {
        Path safeDir = Paths.get("custom-rules").toAbsolutePath().normalize();
        Path target = rulesFile.toAbsolutePath().normalize();

        if (!target.startsWith(safeDir)) {
            printError("SECURITY: The file must be inside the custom-rules/ folder");
            waitForEnter();
            return false;
        }

        Path relativeTarget = safeDir.relativize(target);
        try {
            PathSecurity.validateRelativeSubpath(relativeTarget.toString());
        } catch (SecurityException | IllegalArgumentException e) {
            printError("SECURITY: " + e.getMessage());
            waitForEnter();
            return false;
        }

        return true;
    }

    private static boolean confirmOverwriteIfExists(Path rulesFile) {
        if (Files.exists(rulesFile)) {
            String overwrite = readInput("File already exists. Overwrite? (y/N)", "n");
            if (!overwrite.equalsIgnoreCase("y") && !overwrite.equalsIgnoreCase("yes")) {
                printWarning("Operation cancelled.");
                waitForEnter();
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

    private static boolean writeSecurely(Path rulesFile, String content) throws IOException {
        // SECURITY: check for symlinks in ancestry
        try {
            PathSecurity.assertNoSymlinkInAncestry(rulesFile);
        } catch (SecurityException e) {
            printError("SECURITY: " + e.getMessage());
            waitForEnter();
            return false;
        }

        // SECURITY: Atomic write via CREATE_NEW (anti-TOCTOU)
        try {
            Files.writeString(rulesFile, content,
                java.nio.file.StandardOpenOption.CREATE_NEW);
            return true;
        } catch (java.nio.file.FileAlreadyExistsException e) {
            // If we get here, another process created the file meanwhile (race condition)
            printError("SECURITY: File was created by another process");
            waitForEnter();
            return false;
        }
    }

    private static void printSuccess(Path rulesFile) {
        io.neatify.cli.ui.Display.printSuccess("File created: " + rulesFile.toAbsolutePath());
        printInfo("You can now edit it to customize rules.");
        printInfo("Note: This file will not be versioned by Git.");
        waitForEnter();
    }
}
