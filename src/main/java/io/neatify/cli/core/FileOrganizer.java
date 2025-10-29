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
 * Gère la logique d'organisation des fichiers en mode interactif.
 */
public final class FileOrganizer {

    private FileOrganizer() {
        // Classe utilitaire
    }

    /**
     * Lance le processus complet d'organisation de fichiers.
     */
    public static void organize() throws IOException {
        printSection("ORGANISATION DE FICHIERS");

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
        String sourcePath = readInput("Dossier a organiser (chemin complet)");
        Path sourceDir = Paths.get(sourcePath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            printError("Dossier invalide : " + sourcePath);
            waitForEnter();
            return null;
        }

        try {
            PathSecurity.validateSourceDir(sourceDir);
            return sourceDir;
        } catch (SecurityException e) {
            printError("SECURITE : " + e.getMessage());
            waitForEnter();
            return null;
        }
    }

    private static Map<String, String> promptAndLoadRules() throws IOException {
        String rulesPath = readInput("Fichier de regles (.properties) [Entree = regles par defaut]", "");

        if (rulesPath.isBlank()) {
            return loadDefaultRules();
        } else {
            return loadCustomRules(rulesPath);
        }
    }

    private static Map<String, String> loadDefaultRules() {
        printInfo("Utilisation des regles par defaut integrees...");
        Map<String, String> rules = Rules.getDefaults();
        printSuccess(rules.size() + " regle(s) par defaut chargee(s)");
        return rules;
    }

    private static Map<String, String> loadCustomRules(String rulesPath) throws IOException {
        Path rulesFile = Paths.get(rulesPath);

        if (!Files.exists(rulesFile)) {
            printError("Fichier inexistant : " + rulesPath);
            waitForEnter();
            return null;
        }

        printInfo("Chargement des regles depuis le fichier...");
        Map<String, String> rules = Rules.load(rulesFile);
        printSuccess(rules.size() + " regle(s) chargee(s)");
        return rules;
    }

    private static List<FileMover.Action> planActions(Path sourceDir, Map<String, String> rules, Filters filters) throws IOException {
        printInfo("Analyse du dossier...");
        List<FileMover.Action> actions = FileMover.plan(
            sourceDir,
            rules,
            100_000,
            filters.includes,
            filters.excludes
        );

        if (actions.isEmpty()) {
            printWarning("Aucun fichier à déplacer.");
            waitForEnter();
            return null;
        }

        printSuccess(actions.size() + " fichier(s) a deplacer");
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
        String confirm = readInput("Appliquer ces changements? (o/N)", "n");

        if (!confirm.equalsIgnoreCase("o") && !confirm.equalsIgnoreCase("oui")) {
            printWarning("Operation annulee.");
            waitForEnter();
            return;
        }

        FileMover.CollisionStrategy strategy = promptCollisionStrategy();
        printInfo("Application des changements...");
        java.util.List<io.neatify.cli.core.UndoExecutor.Move> moves = new java.util.ArrayList<>();
        FileMover.Result result = FileMover.execute(actions, false, strategy, (src, dst) -> {
            moves.add(new io.neatify.cli.core.UndoExecutor.Move(src, dst));
        });
        try {
            io.neatify.cli.core.UndoExecutor.appendRun(sourceDir, strategy.name().toLowerCase(), moves);
        } catch (IOException e) {
            printErr("Journal d'undo non ecrit: " + e.getMessage());
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
        String inc = readInput("Inclure (glob, separes par ,) [Entree = aucun]", "");
        String exc = readInput("Exclure (glob, separes par ,) [Entree = aucun]", "");
        java.util.List<String> includes = new java.util.ArrayList<>();
        java.util.List<String> excludes = new java.util.ArrayList<>();
        if (!inc.isBlank()) for (String p : inc.split(",")) { String s=p.trim(); if(!s.isEmpty()) includes.add(s); }
        if (!exc.isBlank()) for (String p : exc.split(",")) { String s=p.trim(); if(!s.isEmpty()) excludes.add(s); }
        return new Filters(includes, excludes);
    }

    private static FileMover.CollisionStrategy promptCollisionStrategy() {
        String s = readInput("Strategie de collision [rename|skip|overwrite]", "rename");
        return switch (s.toLowerCase()) {
            case "skip" -> FileMover.CollisionStrategy.SKIP;
            case "overwrite" -> FileMover.CollisionStrategy.OVERWRITE;
            default -> FileMover.CollisionStrategy.RENAME;
        };
    }
}
