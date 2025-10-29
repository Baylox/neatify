package io.neatify.cli;

import io.neatify.cli.args.CLIConfig;
import io.neatify.cli.ui.Preview;
import io.neatify.cli.util.Ansi;
import io.neatify.cli.util.AsciiSymbols;
import io.neatify.cli.util.ResultPrinter;
import io.neatify.core.FileMover;
import io.neatify.core.PathSecurity;
import io.neatify.core.Rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.neatify.cli.ui.Display.*;

/**
 * Exécuteur du workflow d'organisation de fichiers en mode CLI.
 * Encapsule toute la logique d'exécution : validation, planification, preview, exécution.
 */
public class FileOrganizationExecutor {

    /**
     * Exécute le workflow complet d'organisation de fichiers.
     *
     * @param config configuration CLI
     * @throws IOException si erreur I/O durant l'exécution
     */
    public void execute(CLIConfig config) throws IOException {
        validatePaths(config);
        applyDisplayOptions(config);

        if (config.isUndo()) {
            performUndo(config);
            return;
        }

        Map<String, String> rules = loadRules(config);
        List<FileMover.Action> actions = planActions(config, rules);

        if (actions.isEmpty()) {
            printWarning("Aucun fichier a deplacer.");
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
            throw new IllegalArgumentException("Dossier inexistant: " + sourceDir);
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("--source doit etre un dossier: " + sourceDir);
        }
    }

    private void validateRulesFile(Path rulesFile) {
        if (!Files.exists(rulesFile)) {
            throw new IllegalArgumentException("Fichier inexistant: " + rulesFile);
        }
        if (!Files.isRegularFile(rulesFile)) {
            throw new IllegalArgumentException("--rules doit etre un fichier: " + rulesFile);
        }
    }

    private void validateSourceDirSecurity(Path sourceDir) {
        try {
            PathSecurity.validateSourceDir(sourceDir);
        } catch (IOException e) {
            throw new IllegalArgumentException("Erreur lors de la validation: " + e.getMessage(), e);
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
            printInfo("Utilisation des regles par defaut integrees...");
            Map<String, String> rules = Rules.getDefaults();
            printSuccess(rules.size() + " regle(s) par defaut chargee(s)");
            System.out.println();
            return rules;
        } else {
            printInfo("Chargement des regles depuis: " + config.getRulesFile());
            Map<String, String> rules = Rules.load(config.getRulesFile());
            printSuccess(rules.size() + " regle(s) chargee(s)");
            System.out.println();
            return rules;
        }
    }

    private List<FileMover.Action> planActions(CLIConfig config, Map<String, String> rules) throws IOException {
        printInfo("Analyse du dossier: " + config.getSourceDir());
        List<FileMover.Action> actions = FileMover.plan(
            config.getSourceDir(),
            rules,
            100_000,
            config.getIncludes(),
            config.getExcludes()
        );
        printSuccess(actions.size() + " fichier(s) a deplacer");
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
            printInfo("Application des changements...");
        } else {
            printInfo("Mode DRY-RUN - Utilisez --apply pour appliquer");
        }
        System.out.println();

        FileMover.CollisionStrategy strategy = parseCollision(config.getOnCollision());
        if (config.isApply()) {
            java.util.List<io.neatify.cli.core.UndoExecutor.Move> moves = new java.util.ArrayList<>();
            FileMover.Result res = FileMover.execute(actions, false, strategy, (src, dst) -> {
                moves.add(new io.neatify.cli.core.UndoExecutor.Move(src, dst));
            });
            try {
                io.neatify.cli.core.UndoExecutor.appendRun(config.getSourceDir(), config.getOnCollision(), moves);
            } catch (java.io.IOException e) {
                printErr("Impossible d'ecrire le journal d'undo: " + e.getMessage());
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
            printInfo("Relancez avec --apply pour appliquer");
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
        printInfo("Annulation de la derniere execution...");
        io.neatify.cli.core.UndoExecutor.UndoResult r = io.neatify.cli.core.UndoExecutor.undoLast(config.getSourceDir());
        if (r == null) {
            printWarning("Aucune execution precedente dans le journal.");
            return;
        }
        printSuccess("Restaures: " + r.restored() + ", ignores: " + r.skipped() + ", erreurs: " + r.errors().size());
        if (!r.errors().isEmpty()) {
            printErr("Erreurs pendant l'undo:");
            r.errors().forEach(e -> println("  - " + e));
        }
    }
}
