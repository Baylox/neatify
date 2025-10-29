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

        List<FileMover.Action> actions = planActions(sourceDir, rules);
        if (actions == null) return;

        executeIfConfirmed(actions);
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

    private static List<FileMover.Action> planActions(Path sourceDir, Map<String, String> rules) throws IOException {
        printInfo("Analyse du dossier...");
        List<FileMover.Action> actions = FileMover.plan(sourceDir, rules);

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

    private static void executeIfConfirmed(List<FileMover.Action> actions) throws IOException {
        String confirm = readInput("Appliquer ces changements? (o/N)", "n");

        if (!confirm.equalsIgnoreCase("o") && !confirm.equalsIgnoreCase("oui")) {
            printWarning("Operation annulee.");
            waitForEnter();
            return;
        }

        printInfo("Application des changements...");
        FileMover.Result result = FileMover.execute(actions, false);

        showSummary(result);
        waitForEnter();
    }

    private static void showSummary(FileMover.Result result) {
        System.out.println();
        ResultPrinter.print(result);
    }
}
