package io.neatify.cli.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.neatify.cli.AppContext;
import io.neatify.cli.AppInfo;
import io.neatify.cli.core.FileOrganizer;
import io.neatify.cli.core.RulesFileCreator;
import io.neatify.core.contract.RunJournal;

import static io.neatify.cli.ui.Display.*;

/**
 * Handles Neatify interactive mode – main menu and coordination.
 */
public final class InteractiveCLI {

    private final AppInfo appInfo;
    private final AppContext context;
    private final Console console;
    private final Theme theme;

    public InteractiveCLI(String version, AppContext context) {
        this.appInfo = AppInfo.neatify(version);
        this.context = context;
        this.console = context.console();
        this.theme = new Theme(context.displayOptions());
    }

    public void run() throws IOException {
        // Use safer banner that supports and env override
        Display.printBannerSafe(appInfo, theme);

        while (true) {
            printMenu();
            String choice = console.readInput("Your choice");

            switch (choice) {
                case "1" -> new FileOrganizer(
                    context.rulesProvider(), context.organizationService(), console, theme).organize();
                case "2" -> new RulesFileCreator(console).create();
                case "3" -> { performUndo(); console.waitForEnter(); }
                case "4" -> { HelpPrinter.print(); console.waitForEnter(); }
                case "5" -> { printVersion(); console.waitForEnter(); }
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

    private void performUndo() throws IOException {
        printSection("UNDO LAST RUN");
        String sourcePath = console.readInput("Source folder (used during organization)");
        Path sourceDir = Paths.get(sourcePath);
        RunJournal.UndoResult r = context.runJournal().undoLast(sourceDir);
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
