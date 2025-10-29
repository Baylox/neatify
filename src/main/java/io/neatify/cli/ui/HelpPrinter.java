package io.neatify.cli.ui;

import static io.neatify.cli.ui.Display.printLine;

/**
 * Prints application help.
 */
public final class HelpPrinter {

    private HelpPrinter() {
        // Utility class
    }

    public static void print() {
        System.out.println();
        printLine();
        System.out.println(io.neatify.cli.ui.Display.center("HELP - NEATIFY"));
        printLine();
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -jar neatify.jar [options]");
        System.out.println();
        System.out.println("MODES:");
        System.out.println("  No arguments                Start interactive mode");
        System.out.println("  --interactive, -i           Start interactive mode");
        System.out.println("  --undo                      Undo the last run (journal)");
        System.out.println("  --undo-list                 List journaled runs (.neatify/runs)");
        System.out.println("  --undo-run <timestamp>      Undo a specific run");
        System.out.println();
        System.out.println("OPTIONS (command mode):");
        System.out.println("  --source, -s <dir>          Directory to organize (required)");
        System.out.println("  --rules, -r <file>          Rules file (required)");
        System.out.println("  --use-default-rules         Use built-in default rules (no --rules)");
        System.out.println("  --apply, -a                 Apply changes (otherwise dry-run)");
        System.out.println("  --json                      JSON output (preview + result)");
        System.out.println("  --on-collision <mode>       Collision: rename (default), skip, overwrite");
        System.out.println("  --include <glob>            Include (repeatable), e.g. **/*.pdf");
        System.out.println("  --exclude <glob>            Exclude (repeatable), e.g. **/node_modules/**");
        System.out.println("  --help, -h                  Show this help");
        System.out.println("  --version, -v               Show version");
        System.out.println();
        System.out.println("DISPLAY OPTIONS:");
        System.out.println("  --no-color                  Disable ANSI colors");
        System.out.println("  --ascii                     Use ASCII symbols instead of Unicode");
        System.out.println("  --per-folder-preview <n>    Files per folder to display (default: 5)");
        System.out.println("  --sort <mode>               File sort: alpha, ext or size (default: alpha)");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  # Interactive mode");
        System.out.println("  java -jar neatify.jar");
        System.out.println();
        System.out.println("  # Simulation (dry-run)");
        System.out.println("  java -jar neatify.jar --source ~/Downloads --rules rules.properties");
        System.out.println();
        System.out.println("  # Real apply");
        System.out.println("  java -jar neatify.jar --source ~/Downloads --rules rules.properties --apply");
        System.out.println();
    }
}
