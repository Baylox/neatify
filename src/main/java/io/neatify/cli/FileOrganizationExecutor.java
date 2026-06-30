package io.neatify.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.neatify.cli.args.CLIConfig;
import io.neatify.cli.ui.DisplayOptions;
import io.neatify.cli.ui.Preview;
import io.neatify.cli.ui.Theme;
import io.neatify.cli.util.ResultPrinter;
import io.neatify.core.OrganizationService;
import io.neatify.core.PathSecurity;
import io.neatify.core.contract.FileMover;
import io.neatify.core.contract.RulesProvider;
import io.neatify.core.contract.RunJournal;

import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static io.neatify.cli.ui.Display.*;

public class FileOrganizationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FileOrganizationExecutor.class);

    private final RulesProvider rulesProvider;
    private final OrganizationService organizationService;
    private final RunJournal runJournal;
    private final DisplayOptions baseDisplayOptions;
    private Theme theme;

    public FileOrganizationExecutor(
        RulesProvider rulesProvider,
        OrganizationService organizationService,
        RunJournal runJournal,
        DisplayOptions displayOptions) {
        this.rulesProvider = rulesProvider;
        this.organizationService = organizationService;
        this.runJournal = runJournal;
        this.baseDisplayOptions = displayOptions;
        this.theme = new Theme(displayOptions);
    }

    /** Builds an executor from the application context. */
    public static FileOrganizationExecutor from(AppContext ctx) {
        return new FileOrganizationExecutor(
            ctx.rulesProvider(), ctx.organizationService(), ctx.runJournal(), ctx.displayOptions());
    }

    public void execute(CLIConfig config) throws IOException {
        String runId = String.valueOf(System.currentTimeMillis());
        MDC.put("runId", runId);
        logger.debug("Starting execution with runId: {}", runId);

        try {
            validatePaths(config);
            enforceGitRepositoryPolicy(config);
            applyDisplayOptions(config);

            if (config.isUndo()) {
                performUndo(config);
                return;
            }

            Map<String, String> rules = loadRules(config);
            List<FileMover.Action> actions = planActions(config, rules);

            if (actions.isEmpty()) {
                if (config.isJson()) {
                    printJson(config, actions, new FileMover.Result(0, 0, List.of()));
                } else {
                    printWarning("No files to move.");
                }
                return;
            }

            OrganizationService.Request request = toRequest(config, rules);
            if (config.isJson()) {
                FileMover.Result result = executeActions(config, request, actions);
                printJson(config, actions, result);
            } else {
                showPreview(config, actions);
                FileMover.Result result = executeActions(config, request, actions);
                showSummary(config, result);
            }
        } finally {
            MDC.remove("runId");
            logger.debug("Execution completed, runId cleared");
        }
    }

    private void enforceGitRepositoryPolicy(CLIConfig config) {
        Path source = config.getSourceDir();
        boolean insideGit = PathSecurity.isInsideGitRepository(source);
        if (config.isApply() && insideGit && !config.isAllowInsideGit()) {
            throw new IllegalArgumentException(
                "--apply is blocked inside a Git repository by default. " +
                "Use --allow-inside-git to override, or run outside repos.");
        }
        if (insideGit && !config.isJson()) {
            if (config.isApply()) {
                printWarning("Applying inside a Git repository: " + source);
                printWarning("Proceeding because --allow-inside-git is set. Ensure you have backups.");
            } else {
                printWarning("Git repository detected: " + source + " (dry-run; --apply blocked unless --allow-inside-git)");
            }
        }
    }

    private void validatePaths(CLIConfig config) {
        validateSourceDir(config.getSourceDir());
        validateSourceDirSecurity(config.getSourceDir());
        if (!config.isUndo() && !config.isUseDefaultRules()) {
            validateRulesFile(config.getRulesFile());
        }
    }

    private void validateSourceDir(Path sourceDir) {
        if (!Files.exists(sourceDir)) {
            throw new IllegalArgumentException("Directory does not exist: " + sourceDir);
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("--source must be a directory: " + sourceDir);
        }
    }

    private void validateRulesFile(Path rulesFile) {
        if (!Files.exists(rulesFile)) {
            throw new IllegalArgumentException("File does not exist: " + rulesFile);
        }
        if (!Files.isRegularFile(rulesFile)) {
            throw new IllegalArgumentException("--rules must be a file: " + rulesFile);
        }
    }

    private void validateSourceDirSecurity(Path sourceDir) {
        try {
            PathSecurity.validateSourceDir(sourceDir);
        } catch (IOException e) {
            logger.error("Security validation failed for source directory: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Error during validation: " + e.getMessage(), e);
        }
    }

    private void applyDisplayOptions(CLIConfig config) {
        DisplayOptions options = baseDisplayOptions;
        if (config.isNoColor()) {
            options = options.withoutColor();
        }
        if (config.isAscii()) {
            options = options.asciiOnly();
        }
        this.theme = new Theme(options);
    }

    private Map<String, String> loadRules(CLIConfig config) throws IOException {
        if (config.isUseDefaultRules()) {
            if (!config.isJson()) printInfo("Using built-in default rules...");
            Map<String, String> rules = rulesProvider.getDefaults();
            if (!config.isJson()) { printSuccess(rules.size() + " default rule(s) loaded"); System.out.println(); }
            return rules;
        } else {
            if (!config.isJson()) printInfo("Loading rules from: " + config.getRulesFile());
            Map<String, String> rules = rulesProvider.load(config.getRulesFile());
            if (!config.isJson()) { printSuccess(rules.size() + " rule(s) loaded"); System.out.println(); }
            return rules;
        }
    }

    private OrganizationService.Request toRequest(CLIConfig config, Map<String, String> rules) {
        return new OrganizationService.Request(
            config.getSourceDir(), rules, config.getMaxFiles(),
            config.getIncludes(), config.getExcludes(),
            parseCollision(config.getOnCollision()), !config.isAllowInsideGit());
    }

    private List<FileMover.Action> planActions(CLIConfig config, Map<String, String> rules) throws IOException {
        if (!config.isJson()) printInfo("Scanning folder: " + config.getSourceDir());
        List<FileMover.Action> actions = organizationService.plan(toRequest(config, rules));
        if (!config.isJson()) printSuccess(actions.size() + " file(s) to move");
        return actions;
    }

    private void showPreview(CLIConfig config, List<FileMover.Action> actions) {
        Preview.Config rendererConfig = new Preview.Config()
            .maxFilesPerFolder(config.getPerFolderPreview())
            .sortMode(parseSortMode(config.getSortMode()))
            .showDuplicates(true)
            .theme(theme);
        Preview.print(actions, rendererConfig);
    }

    private FileMover.Result executeActions(CLIConfig config, OrganizationService.Request request,
            List<FileMover.Action> actions) {
        if (!config.isJson()) {
            if (config.isApply()) printInfo("Applying changes...");
            else printInfo("DRY-RUN mode - Use --apply to apply");
            System.out.println();
        }

        if (config.isApply()) {
            OrganizationService.Outcome outcome = organizationService.apply(request, actions);
            if (outcome.journalError() != null) {
                printErr("Unable to write undo journal: " + outcome.journalError());
            } else if (outcome.journalPath() != null && !config.isJson()) {
                printInfo("Journal written: " + outcome.journalPath().toAbsolutePath());
            }
            return outcome.result();
        } else {
            return organizationService.dryRun(request, actions);
        }
    }

    private void showSummary(CLIConfig config, FileMover.Result result) {
        ResultPrinter.print(result);
        if (!config.isApply() && result.moved() > 0) {
            System.out.println();
            printInfo("Re-run with --apply to apply");
        }
    }

    private Preview.SortMode parseSortMode(String mode) {
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "ext" -> Preview.SortMode.EXT;
            case "size" -> Preview.SortMode.SIZE;
            default -> Preview.SortMode.ALPHA;
        };
    }

    private FileMover.CollisionStrategy parseCollision(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "skip" -> FileMover.CollisionStrategy.SKIP;
            case "overwrite" -> FileMover.CollisionStrategy.OVERWRITE;
            default -> FileMover.CollisionStrategy.RENAME;
        };
    }

    private void printJson(CLIConfig config, List<FileMover.Action> actions, FileMover.Result result) {
        List<ActionDto> actionDtos = actions.stream()
            .map(a -> new ActionDto(a.source().toString(), a.target().toString(), a.reason()))
            .toList();
        ResultDto resultDto = result != null
            ? new ResultDto(result.moved(), result.skipped(), result.errors())
            : null;
        JsonOutput output = new JsonOutput(
            config.getSourceDir().toString(), config.isApply(), config.getOnCollision(),
            actions.size(), actionDtos, resultDto
        );
        System.out.println(new GsonBuilder().create().toJson(output));
    }

    private void performUndo(CLIConfig config) throws IOException {
        if (config.isUndoList()) {
            List<RunJournal.RunMeta> runs = runJournal.list(config.getSourceDir());
            if (runs.isEmpty()) {
                printWarning("No previous runs.");
            } else {
                printSection("AVAILABLE JOURNALS (.neatify/runs)");
                for (RunJournal.RunMeta m : runs) {
                    println("  - " + m.file().getFileName() + " (" + m.movesCount() + " moves, collision=" + m.onCollision() + ")");
                }
            }
            return;
        }

        if (config.getUndoRun() != null) {
            try {
                long ts = Long.parseLong(config.getUndoRun());
                RunJournal.UndoResult r = runJournal.undoRun(config.getSourceDir(), ts);
                if (r == null) { printWarning("Run not found: " + ts); return; }
                printSuccess("Restored: " + r.restored() + ", skipped: " + r.skipped() + ", errors: " + r.errors().size());
                if (!r.errors().isEmpty()) { printErr("Errors during undo:"); r.errors().forEach(e -> println("  - " + e)); }
                return;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("--undo-run requires a numeric timestamp", e);
            }
        }

        printInfo("Undoing last run...");
        RunJournal.UndoResult r = runJournal.undoLast(config.getSourceDir());
        if (r == null) { printWarning("No previous run found in the journal."); return; }
        printSuccess("Restored: " + r.restored() + ", skipped: " + r.skipped() + ", errors: " + r.errors().size());
        if (!r.errors().isEmpty()) { printErr("Errors during undo:"); r.errors().forEach(e -> println("  - " + e)); }
    }

    private record JsonOutput(String source, boolean apply, String onCollision, int planned,
                               List<ActionDto> actions, ResultDto result) {}
    private record ActionDto(String source, String target, String reason) {}
    private record ResultDto(int moved, int skipped, List<String> errors) {}
}
