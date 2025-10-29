package io.neatify.cli;

import io.neatify.cli.args.CLIConfig;
import io.neatify.cli.ui.Preview;
import io.neatify.cli.util.Ansi;
import io.neatify.cli.util.AsciiSymbols;
import io.neatify.cli.util.ResultPrinter;
import io.neatify.core.FileMover;
import io.neatify.core.PathSecurity;
import io.neatify.core.Rules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.neatify.cli.ui.Display.*;

/**
 * Executes the CLI file-organization workflow.
 * Encapsulates validation, planning, preview, and execution.
 */
public class FileOrganizationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FileOrganizationExecutor.class);

    /**
     * Executes the full file-organization workflow.
     *
     * @param config CLI configuration
     * @throws IOException on I/O errors during execution
     */
    public void execute(CLIConfig config) throws IOException {
        // Set run ID for correlation in logs
        String runId = String.valueOf(System.currentTimeMillis());
        MDC.put("runId", runId);
        logger.debug("Starting execution with runId: {}", runId);

        try {
            validatePaths(config);
            applyDisplayOptions(config);

            if (config.isUndo()) {
                performUndo(config);
                return;
            }

            Map<String, String> rules = loadRules(config);
            List<FileMover.Action> actions = planActions(config, rules);

            if (actions.isEmpty()) {
                printWarning("No files to move.");
                return;
            }

            if (config.isJson()) {
                FileMover.Result result = executeActions(config, actions);
                printJson(config, actions, result);
            } else {
                showPreview(config, actions);
                FileMover.Result result = executeActions(config, actions);
                showSummary(config, result);
            }
        } finally {
            MDC.remove("runId");
            logger.debug("Execution completed, runId cleared");
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
        if (config.isNoColor()) {
            Ansi.setEnabled(false);
        }
        if (config.isAscii()) {
            AsciiSymbols.setUseUnicode(false);
        }
    }

    private Map<String, String> loadRules(CLIConfig config) throws IOException {
        if (config.isUseDefaultRules()) {
            printInfo("Using built-in default rules...");
            Map<String, String> rules = Rules.getDefaults();
            printSuccess(rules.size() + " default rule(s) loaded");
            System.out.println();
            return rules;
        } else {
            printInfo("Loading rules from: " + config.getRulesFile());
            Map<String, String> rules = Rules.load(config.getRulesFile());
            printSuccess(rules.size() + " rule(s) loaded");
            System.out.println();
            return rules;
        }
    }

    private List<FileMover.Action> planActions(CLIConfig config, Map<String, String> rules) throws IOException {
        printInfo("Scanning folder: " + config.getSourceDir());
        List<FileMover.Action> actions = FileMover.plan(
            config.getSourceDir(),
            rules,
            config.getMaxFiles(),
            config.getIncludes(),
            config.getExcludes()
        );
        printSuccess(actions.size() + " file(s) to move");
        return actions;
    }

    private void showPreview(CLIConfig config, List<FileMover.Action> actions) {
        Preview.Config rendererConfig = new Preview.Config()
            .maxFilesPerFolder(config.getPerFolderPreview())
            .sortMode(parseSortMode(config.getSortMode()))
            .showDuplicates(true);

        Preview.print(actions, rendererConfig);
    }

    private FileMover.Result executeActions(CLIConfig config, List<FileMover.Action> actions) {
        if (config.isApply()) {
            printInfo("Applying changes...");
        } else {
            printInfo("DRY-RUN mode - Use --apply to apply");
        }
        System.out.println();

        FileMover.CollisionStrategy strategy = parseCollision(config.getOnCollision());
        if (config.isApply()) {
            java.util.List<io.neatify.cli.core.UndoExecutor.Move> moves = new java.util.ArrayList<>();
            FileMover.Result res = FileMover.execute(actions, false, strategy, (src, dst) -> {
                moves.add(new io.neatify.cli.core.UndoExecutor.Move(src, dst));
            });
            try {
                java.nio.file.Path runPath = io.neatify.cli.core.UndoExecutor.appendRun(config.getSourceDir(), config.getOnCollision(), moves);
                if (runPath != null) {
                    printInfo("Journal written: " + runPath.toAbsolutePath());
                }
            } catch (java.io.IOException e) {
                logger.error("Failed to write undo journal: {}", e.getMessage(), e);
                printErr("Unable to write undo journal: " + e.getMessage());
            }
            return res;
        } else {
            return FileMover.execute(actions, true, strategy);
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
        return switch (mode.toLowerCase()) {
            case "ext" -> Preview.SortMode.EXT;
            case "size" -> Preview.SortMode.SIZE;
            default -> Preview.SortMode.ALPHA;
        };
    }

    private FileMover.CollisionStrategy parseCollision(String s) {
        return switch (s.toLowerCase()) {
            case "skip" -> FileMover.CollisionStrategy.SKIP;
            case "overwrite" -> FileMover.CollisionStrategy.OVERWRITE;
            default -> FileMover.CollisionStrategy.RENAME;
        };
    }

    private void printJson(CLIConfig config, List<FileMover.Action> actions, FileMover.Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"source\":\"").append(escape(config.getSourceDir().toString())).append("\",");
        sb.append("\"apply\":").append(config.isApply()).append(',');
        sb.append("\"onCollision\":\"").append(escape(config.getOnCollision())).append("\",");
        sb.append("\"planned\":").append(actions.size()).append(',');
        sb.append("\"actions\":[");
        for (int i = 0; i < actions.size(); i++) {
            var a = actions.get(i);
            sb.append('{')
              .append("\"source\":\"").append(escape(a.source().toString())).append("\",")
              .append("\"target\":\"").append(escape(a.target().toString())).append("\",")
              .append("\"reason\":\"").append(escape(a.reason())).append("\"")
              .append('}');
            if (i < actions.size() - 1) sb.append(',');
        }
        sb.append(']');
        if (result != null) {
            sb.append(',').append("\"result\":{")
              .append("\"moved\":").append(result.moved()).append(',')
              .append("\"skipped\":").append(result.skipped()).append(',')
              .append("\"errors\":[");
            for (int i = 0; i < result.errors().size(); i++) {
                String e = result.errors().get(i);
                sb.append("\"").append(escape(e)).append("\"");
                if (i < result.errors().size() - 1) sb.append(',');
            }
            sb.append("]}");
        }
        sb.append('}');
        System.out.println(sb.toString());
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void performUndo(CLIConfig config) throws IOException {
        if (config.isUndoList()) {
            var runs = io.neatify.cli.core.UndoExecutor.listRuns(config.getSourceDir());
            if (runs.isEmpty()) {
                printWarning("No previous runs.");
            } else {
                printSection("AVAILABLE JOURNALS (.neatify/runs)");
                for (var m : runs) {
                    println("  - " + m.time() + " (" + m.movesCount() + " moves, collision=" + m.onCollision() + ")");
                }
            }
            return;
        }

        if (config.getUndoRun() != null) {
            try {
                long ts = Long.parseLong(config.getUndoRun());
                var r = io.neatify.cli.core.UndoExecutor.undoRun(config.getSourceDir(), ts);
                if (r == null) { printWarning("Run not found: " + ts); return; }
                printSuccess("Restored: " + r.restored() + ", skipped: " + r.skipped() + ", errors: " + r.errors().size());
                if (!r.errors().isEmpty()) { printErr("Errors during undo:"); r.errors().forEach(e -> println("  - " + e)); }
                return;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("--undo-run requires a numeric timestamp");
            }
        }

        printInfo("Undoing last run...");
        var r = io.neatify.cli.core.UndoExecutor.undoLast(config.getSourceDir());
        if (r == null) {
            printWarning("No previous run found in the journal.");
            return;
        }
        printSuccess("Restored: " + r.restored() + ", skipped: " + r.skipped() + ", errors: " + r.errors().size());
        if (!r.errors().isEmpty()) {
            printErr("Errors during undo:");
            r.errors().forEach(e -> println("  - " + e));
        }
    }
}
