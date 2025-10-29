package io.neatify.cli.core;

import io.neatify.cli.ui.Preview;
import io.neatify.cli.util.ResultPrinter;
import io.neatify.core.FileMover;
import io.neatify.core.PathSecurity;
import io.neatify.core.Rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.neatify.cli.ui.Display.*;

/**
 * Handles file organization flow in interactive mode.
 */
public final class FileOrganizer {

    private FileOrganizer() {
        // Classe utilitaire
    }

    /**
     * Starts the full interactive organization process.
     */
    public static void organize() throws IOException {
        printSection("FILE ORGANIZATION");

        Path sourceDir = promptAndValidateSourceDir();
        if (sourceDir == null) return;

        Map<String, String> rules = promptAndLoadRules();
        if (rules == null) return;

        Filters filters = promptFilters();

        List<FileMover.Action> actions = planActions(sourceDir, rules, filters);
        if (actions == null) return;

        executeIfConfirmed(actions, sourceDir);
    }

    private static Path promptAndValidateSourceDir() throws IOException {
        String sourcePath = readInput("Folder to organize (full path)");
        Path sourceDir = Paths.get(sourcePath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            printError("Invalid folder: " + sourcePath);
            waitForEnter();
            return null;
        }

        try {
            PathSecurity.validateSourceDir(sourceDir);
            return sourceDir;
        } catch (SecurityException e) {
            printError("SECURITY: " + e.getMessage());
            waitForEnter();
            return null;
        }
    }

    private static Map<String, String> promptAndLoadRules() throws IOException {
        String rulesPath = readInput("Rules file (.properties) [Enter = default rules]", "");

        if (rulesPath.isBlank()) {
            return loadDefaultRules();
        } else {
            return loadCustomRules(rulesPath);
        }
    }

    private static Map<String, String> loadDefaultRules() {
        printInfo("Using built-in default rules...");
        Map<String, String> rules = Rules.getDefaults();
        printSuccess(rules.size() + " default rule(s) loaded");
        return rules;
    }

    private static Map<String, String> loadCustomRules(String rulesPath) throws IOException {
        Path rulesFile = Paths.get(rulesPath);

        if (!Files.exists(rulesFile)) {
            printError("File does not exist: " + rulesPath);
            waitForEnter();
            return null;
        }

        printInfo("Loading rules from file...");
        Map<String, String> rules = Rules.load(rulesFile);
        printSuccess(rules.size() + " rule(s) loaded");
        return rules;
    }

    private static List<FileMover.Action> planActions(Path sourceDir, Map<String, String> rules, Filters filters) throws IOException {
        printInfo("Scanning folder...");
        List<FileMover.Action> actions = FileMover.plan(
            sourceDir,
            rules,
            100_000,
            filters.includes,
            filters.excludes
        );

        if (actions.isEmpty()) {
            printWarning("No files to move.");
            waitForEnter();
            return null;
        }

        printSuccess(actions.size() + " file(s) to move");
        showPreview(actions);
        return actions;
    }

    private static void showPreview(List<FileMover.Action> actions) {
        Preview.Config config = new Preview.Config()
            .maxFilesPerFolder(5)
            .sortMode(Preview.SortMode.ALPHA)
            .showDuplicates(true);

        Preview.print(actions, config);
    }

    private static void executeIfConfirmed(List<FileMover.Action> actions, Path sourceDir) throws IOException {
        String confirm = readInput("Apply these changes? (y/N)", "n");

        if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
            printWarning("Operation cancelled.");
            waitForEnter();
            return;
        }

        FileMover.CollisionStrategy strategy = promptCollisionStrategy();
        printInfo("Applying changes...");
        java.util.List<io.neatify.cli.core.UndoExecutor.Move> moves = new java.util.ArrayList<>();
        FileMover.Result result = FileMover.execute(actions, false, strategy, (src, dst) -> {
            moves.add(new io.neatify.cli.core.UndoExecutor.Move(src, dst));
        });
        try {
            java.nio.file.Path runPath = io.neatify.cli.core.UndoExecutor.appendRun(sourceDir, strategy.name().toLowerCase(), moves);
            if (runPath != null) {
                printInfo("Journal written: " + runPath.toAbsolutePath());
            }
        } catch (IOException e) {
            printErr("Undo journal not written: " + e.getMessage());
        }

        showSummary(result);
        waitForEnter();
    }

    private static void showSummary(FileMover.Result result) {
        System.out.println();
        ResultPrinter.print(result);
    }

    // ======= Options interactives =======
    private record Filters(java.util.List<String> includes, java.util.List<String> excludes) {}

    private static Filters promptFilters() {
        String inc = readInput("Include (glob, comma-separated) [Enter = none]", "");
        String exc = readInput("Exclude (glob, comma-separated) [Enter = none]", "");
        java.util.List<String> includes = new java.util.ArrayList<>();
        java.util.List<String> excludes = new java.util.ArrayList<>();
        if (!inc.isBlank()) for (String p : inc.split(",")) { String s=p.trim(); if(!s.isEmpty()) includes.add(s); }
        if (!exc.isBlank()) for (String p : exc.split(",")) { String s=p.trim(); if(!s.isEmpty()) excludes.add(s); }
        return new Filters(includes, excludes);
    }

    private static FileMover.CollisionStrategy promptCollisionStrategy() {
        String s = readInput("Collision strategy [rename|skip|overwrite]", "rename");
        return switch (s.toLowerCase()) {
            case "skip" -> FileMover.CollisionStrategy.SKIP;
            case "overwrite" -> FileMover.CollisionStrategy.OVERWRITE;
            default -> FileMover.CollisionStrategy.RENAME;
        };
    }
}
