package io.neatify.cli.ui;

import io.neatify.cli.AppInfo;
import io.neatify.cli.core.FileOrganizer;
import io.neatify.cli.core.RulesFileCreator;
import io.neatify.cli.core.UndoExecutor;

import java.io.IOException;

import static io.neatify.cli.ui.Display.*;

/**
 * Handles Neatify interactive mode – main menu and coordination.
 */
public final class InteractiveCLI {

    private final AppInfo appInfo;

    public InteractiveCLI(String version) {
        this.appInfo = AppInfo.neatify(version);
    }

    public void run() throws IOException {
        // Use safer banner that supports and env override
        Display.printBannerSafe(appInfo);

        while (true) {
            printMenu();
            String choice = readInput("Your choice");

            switch (choice) {
                case "1" -> FileOrganizer.organize();
                case "2" -> RulesFileCreator.create();
                case "3" -> { performUndo(); waitForEnter(); }
                case "4" -> { HelpPrinter.print(); waitForEnter(); }
                case "5" -> { printVersion(); waitForEnter(); }
                case "6", "q", "Q" -> { printSuccess("Goodbye!"); return; }
                default -> printWarning("Invalid choice. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println(center("MAIN MENU"));
        printLine();
        System.out.println("  1. Organize files");
        System.out.println("  2. Create a rules file");
        System.out.println("  3. Undo last run");
        System.out.println("  4. Show help");
        System.out.println("  5. Show version");
        System.out.println("  6. Quit       (or 'q')");
        printLine();
    }

    private void printVersion() {
        System.out.println(appInfo.name() + " version " + appInfo.version());
        System.out.println(appInfo.description());
    }

    private void performUndo() throws java.io.IOException {
        printSection("UNDO LAST RUN");
        String sourcePath = readInput("Source folder (used during organization)");
        java.nio.file.Path sourceDir = java.nio.file.Paths.get(sourcePath);
        UndoExecutor.UndoResult r = UndoExecutor.undoLastV2(sourceDir);
        if (r == null) {
            printWarning("No journal found. Nothing to undo.");
            return;
        }
        printSuccess("Restored: " + r.restored() + ", skipped: " + r.skipped() + ", errors: " + r.errors().size());
        if (!r.errors().isEmpty()) {
            printErr("Errors:");
            r.errors().forEach(e -> println("  - " + e));
        }
    }
}
