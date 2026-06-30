package io.neatify.cli.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.neatify.cli.ui.Console;
import io.neatify.cli.ui.Preview;
import io.neatify.cli.ui.Theme;
import io.neatify.cli.util.ResultPrinter;
import io.neatify.core.OrganizationService;
import io.neatify.core.PathSecurity;
import io.neatify.core.contract.FileMover;
import io.neatify.core.contract.RulesProvider;

import static io.neatify.cli.ui.Display.*;

public final class FileOrganizer {

    private static final int MAX_FILES = 100_000;

    private final RulesProvider rulesProvider;
    private final OrganizationService organizationService;
    private final Console console;
    private final Theme theme;

    public FileOrganizer(RulesProvider rulesProvider, OrganizationService organizationService,
            Console console, Theme theme) {
        this.rulesProvider = rulesProvider;
        this.organizationService = organizationService;
        this.console = console;
        this.theme = theme;
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

        executeIfConfirmed(actions, sourceDir, rules.get(), filters);
    }

    private Path promptAndValidateSourceDir() throws IOException {
        String sourcePath = console.readInput("Folder to organize (full path)");
        Path sourceDir = Paths.get(sourcePath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            printError("Invalid folder: " + sourcePath);
            console.waitForEnter();
            return null;
        }

        try {
            PathSecurity.validateSourceDir(sourceDir);
        } catch (SecurityException e) {
            printError("SECURITY: " + e.getMessage());
            console.waitForEnter();
            return null;
        }

        if (PathSecurity.isInsideGitRepository(sourceDir)) {
            printError("BLOCKED: This folder is inside a Git repository.");
            printWarning("Organizing may move versioned files.");
            String confirm = console.readInput("Type FORCE to override this protection, or press Enter to cancel", "");
            if (!"FORCE".equals(confirm)) {
                printWarning("Operation cancelled. Use a non-Git directory or type FORCE to proceed.");
                console.waitForEnter();
                return null;
            }
            printWarning("Protection overridden. Proceeding inside Git repository.");
        }

        return sourceDir;
    }

    private Optional<Map<String, String>> promptAndLoadRules() throws IOException {
        String rulesPath = console.readInput("Rules file (.properties) [Enter = default rules]", "");

        if (rulesPath.isBlank()) {
            printInfo("Using built-in default rules...");
            Map<String, String> rules = rulesProvider.getDefaults();
            printSuccess(rules.size() + " default rule(s) loaded");
            return Optional.of(rules);
        } else {
            Path rulesFile = Paths.get(rulesPath);
            if (!Files.exists(rulesFile)) {
                printError("File does not exist: " + rulesPath);
                console.waitForEnter();
                return Optional.empty();
            }
            printInfo("Loading rules from file...");
            Map<String, String> rules = rulesProvider.load(rulesFile);
            printSuccess(rules.size() + " rule(s) loaded");
            return Optional.of(rules);
        }
    }

    private OrganizationService.Request request(Path sourceDir, Map<String, String> rules,
            Filters filters, FileMover.CollisionStrategy strategy) {
        return new OrganizationService.Request(
            sourceDir, rules, MAX_FILES, filters.includes(), filters.excludes(), strategy, true);
    }

    private List<FileMover.Action> planActions(Path sourceDir, Map<String, String> rules, Filters filters)
            throws IOException {
        printInfo("Scanning folder...");
        // Strategy is irrelevant for planning; the chosen one is supplied at apply time.
        List<FileMover.Action> actions = organizationService.plan(
            request(sourceDir, rules, filters, FileMover.CollisionStrategy.RENAME));

        if (actions.isEmpty()) {
            printWarning("No files to move.");
            console.waitForEnter();
            return List.of();
        }

        printSuccess(actions.size() + " file(s) to move");
        Preview.print(actions, new Preview.Config()
            .maxFilesPerFolder(5).sortMode(Preview.SortMode.ALPHA).showDuplicates(true).theme(theme));
        return actions;
    }

    private void executeIfConfirmed(List<FileMover.Action> actions, Path sourceDir,
            Map<String, String> rules, Filters filters) throws IOException {
        String confirm = console.readInput("Apply these changes? (y/N)", "n");

        if (!"y".equalsIgnoreCase(confirm) && !"yes".equalsIgnoreCase(confirm)) {
            printWarning("Operation cancelled.");
            console.waitForEnter();
            return;
        }

        FileMover.CollisionStrategy strategy = promptCollisionStrategy();
        printInfo("Applying changes...");
        OrganizationService.Outcome outcome = organizationService.apply(
            request(sourceDir, rules, filters, strategy), actions);
        if (outcome.journalError() != null) {
            printErr("Undo journal not written: " + outcome.journalError());
        } else if (outcome.journalPath() != null) {
            printInfo("Journal written: " + outcome.journalPath().toAbsolutePath());
        }

        System.out.println();
        ResultPrinter.print(outcome.result());
        console.waitForEnter();
    }

    private record Filters(List<String> includes, List<String> excludes) {}

    private Filters promptFilters() {
        String inc = console.readInput("Include (glob, comma-separated) [Enter = none]", "");
        String exc = console.readInput("Exclude (glob, comma-separated) [Enter = none]", "");
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
        String s = console.readInput("Collision strategy [rename|skip|overwrite]", "rename");
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "skip" -> FileMover.CollisionStrategy.SKIP;
            case "overwrite" -> FileMover.CollisionStrategy.OVERWRITE;
            default -> FileMover.CollisionStrategy.RENAME;
        };
    }
}
