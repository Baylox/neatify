package io.neatify.cli;

import io.neatify.cli.args.CLIConfig;
import io.neatify.cli.util.Ansi;
import io.neatify.cli.util.AsciiSymbols;
import io.neatify.cli.util.PreviewPrinter;
import io.neatify.cli.util.PreviewRenderer;
import io.neatify.cli.util.ResultPrinter;
import io.neatify.core.FileMover;
import io.neatify.core.PathSecurity;
import io.neatify.core.Rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.neatify.cli.ui.ConsoleUI.*;

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

        Map<String, String> rules = loadRules(config);
        List<FileMover.Action> actions = planActions(config, rules);

        if (actions.isEmpty()) {
            printWarning("Aucun fichier a deplacer.");
            return;
        }

        showPreview(config, actions);
        FileMover.Result result = executeActions(config, actions);
        showSummary(config, result);
    }

    private void validatePaths(CLIConfig config) {
        validateSourceDir(config.getSourceDir());
        validateRulesFile(config.getRulesFile());
        validateSourceDirSecurity(config.getSourceDir());
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
        printInfo("Chargement des regles depuis: " + config.getRulesFile());
        Map<String, String> rules = Rules.load(config.getRulesFile());
        printSuccess(rules.size() + " regle(s) chargee(s)");
        System.out.println();
        return rules;
    }

    private List<FileMover.Action> planActions(CLIConfig config, Map<String, String> rules) throws IOException {
        printInfo("Analyse du dossier: " + config.getSourceDir());
        List<FileMover.Action> actions = FileMover.plan(config.getSourceDir(), rules);
        printSuccess(actions.size() + " fichier(s) a deplacer");
        return actions;
    }

    private void showPreview(CLIConfig config, List<FileMover.Action> actions) {
        PreviewRenderer.Config rendererConfig = new PreviewRenderer.Config()
            .maxFilesPerFolder(config.getPerFolderPreview())
            .sortMode(parseSortMode(config.getSortMode()))
            .showDuplicates(true);

        PreviewPrinter.print(actions, rendererConfig);
    }

    private FileMover.Result executeActions(CLIConfig config, List<FileMover.Action> actions) {
        if (config.isApply()) {
            printInfo("Application des changements...");
        } else {
            printInfo("Mode DRY-RUN - Utilisez --apply pour appliquer");
        }
        System.out.println();

        return FileMover.execute(actions, !config.isApply());
    }

    private void showSummary(CLIConfig config, FileMover.Result result) {
        ResultPrinter.print(result);

        if (!config.isApply() && result.moved() > 0) {
            System.out.println();
            printInfo("Relancez avec --apply pour appliquer");
        }
    }

    private PreviewRenderer.SortMode parseSortMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "ext" -> PreviewRenderer.SortMode.EXT;
            case "size" -> PreviewRenderer.SortMode.SIZE;
            default -> PreviewRenderer.SortMode.ALPHA;
        };
    }
}
