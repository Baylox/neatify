package io.neatify.cli.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.neatify.cli.ui.Preview;
import io.neatify.cli.util.ResultPrinter;
import io.neatify.core.LocalFileMover;
import io.neatify.core.PathSecurity;
import io.neatify.core.PropertiesRulesProvider;
import io.neatify.core.contract.FileMover;
import io.neatify.core.contract.RulesProvider;

import static io.neatify.cli.ui.Display.*;

public final class FileOrganizer {

    private final FileMover fileMover;
    private final RulesProvider rulesProvider;

    public FileOrganizer() {
        this(new LocalFileMover(), new PropertiesRulesProvider());
    }

    public FileOrganizer(FileMover fileMover, RulesProvider rulesProvider) {
        this.fileMover = fileMover;
        this.rulesProvider = rulesProvider;
    }

    public void organize() throws IOException {
        printSection("FILE ORGANIZATION");

        Path sourceDir = promptAndValidateSourceDir();
        if (sourceDir == null) return;

        Optional<Map<String, String>> rules = promptAndLoadRules();
        if (rules.isEmpty()) return;

        Filters filters = promptFilters();

        List<FileMover.Action> actions = planActions(sourceDir, rules.get(), filters);
        if (actions.isEmpty()) return;

        executeIfConfirmed(actions, sourceDir);
    }

    private Path promptAndValidateSourceDir() throws IOException {
        String sourcePath = readInput("Folder to organize (full path)");
        Path sourceDir = Paths.get(sourcePath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            printError("Invalid folder: " + sourcePath);
            waitForEnter();
            return null;
        }

        try {
            PathSecurity.validateSourceDir(sourceDir);
        } catch (SecurityException e) {
            printError("SECURITY: " + e.getMessage());
            waitForEnter();
            return null;
        }

        if (PathSecurity.isInsideGitRepository(sourceDir)) {
            printError("BLOCKED: This folder is inside a Git repository.");
            printWarning("Organizing may move versioned files.");
            String confirm = readInput("Type FORCE to override this protection, or press Enter to cancel", "");
            if (!"FORCE".equals(confirm)) {
                printWarning("Operation cancelled. Use a non-Git directory or type FORCE to proceed.");
                waitForEnter();
                return null;
            }
            printWarning("Protection overridden. Proceeding inside Git repository.");
        }

        return sourceDir;
    }

    private Optional<Map<String, String>> promptAndLoadRules() throws IOException {
        String rulesPath = readInput("Rules file (.properties) [Enter = default rules]", "");

        if (rulesPath.isBlank()) {
            printInfo("Using built-in default rules...");
            Map<String, String> rules = rulesProvider.getDefaults();
            printSuccess(rules.size() + " default rule(s) loaded");
            return Optional.of(rules);
        } else {
            Path rulesFile = Paths.get(rulesPath);
            if (!Files.exists(rulesFile)) {
                printError("File does not exist: " + rulesPath);
                waitForEnter();
                return Optional.empty();
            }
            printInfo("Loading rules from file...");
            Map<String, String> rules = rulesProvider.load(rulesFile);
            printSuccess(rules.size() + " rule(s) loaded");
            return Optional.of(rules);
        }
    }

    private List<FileMover.Action> planActions(Path sourceDir, Map<String, String> rules, Filters filters) throws IOException {
        printInfo("Scanning folder...");
        List<FileMover.Action> actions = fileMover.plan(
            sourceDir, rules, 100_000, filters.includes(), filters.excludes(), true
        );

        if (actions.isEmpty()) {
            printWarning("No files to move.");
            waitForEnter();
            return List.of();
        }

        printSuccess(actions.size() + " file(s) to move");
        Preview.print(actions, new Preview.Config().maxFilesPerFolder(5).sortMode(Preview.SortMode.ALPHA).showDuplicates(true));
        return actions;
    }

    private void executeIfConfirmed(List<FileMover.Action> actions, Path sourceDir) throws IOException {
        String confirm = readInput("Apply these changes? (y/N)", "n");

        if (!"y".equalsIgnoreCase(confirm) && !"yes".equalsIgnoreCase(confirm)) {
            printWarning("Operation cancelled.");
            waitForEnter();
            return;
        }

        FileMover.CollisionStrategy strategy = promptCollisionStrategy();
        printInfo("Applying changes...");
        List<UndoExecutor.Move> moves = new java.util.ArrayList<>();
        FileMover.Result result = fileMover.execute(actions, false, strategy, (src, dst) ->
            moves.add(new UndoExecutor.Move(src, dst))
        );
        try {
            Path runPath = UndoExecutor.appendRun(sourceDir, strategy.name().toLowerCase(Locale.ROOT), moves);
            if (runPath != null) printInfo("Journal written: " + runPath.toAbsolutePath());
        } catch (IOException e) {
            printErr("Undo journal not written: " + e.getMessage());
        }

        System.out.println();
        ResultPrinter.print(result);
        waitForEnter();
    }

    private record Filters(List<String> includes, List<String> excludes) {}

    private Filters promptFilters() {
        String inc = readInput("Include (glob, comma-separated) [Enter = none]", "");
        String exc = readInput("Exclude (glob, comma-separated) [Enter = none]", "");
        return new Filters(parsePatterns(inc), parsePatterns(exc));
    }

    private List<String> parsePatterns(String input) {
        if (input == null || input.isBlank()) return List.of();
        List<String> out = new java.util.ArrayList<>();
        for (String part : input.split(",")) {
            String s = part.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private FileMover.CollisionStrategy promptCollisionStrategy() {
        String s = readInput("Collision strategy [rename|skip|overwrite]", "rename");
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "skip" -> FileMover.CollisionStrategy.SKIP;
            case "overwrite" -> FileMover.CollisionStrategy.OVERWRITE;
            default -> FileMover.CollisionStrategy.RENAME;
        };
    }
}
