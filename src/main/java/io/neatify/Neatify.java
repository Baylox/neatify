package io.neatify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.neatify.ConsoleUI.*;

/**
 * Point d'entrée principal de Neatify.
 * Supporte deux modes : interactif (par défaut) et ligne de commande.
 */
public final class Neatify {

    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        try {
            // Mode interactif si aucun argument
            if (args.length == 0) {
                new InteractiveCLI(VERSION).run();
                return;
            }

            // Mode ligne de commande
            Config config = parseArguments(args);

            if (config.showHelp) {
                new InteractiveCLI(VERSION).run(); // Utilise l'aide de InteractiveCLI
                return;
            }

            if (config.showVersion) {
                System.out.println("Neatify version " + VERSION);
                return;
            }

            if (config.interactive) {
                new InteractiveCLI(VERSION).run();
                return;
            }

            // Exécution normale
            executeOrganization(config);

        } catch (IllegalArgumentException e) {
            printError("Erreur : " + e.getMessage());
            System.err.println("Utilisez --help pour voir l'aide.");
            System.exit(1);
        } catch (IOException e) {
            printError("Erreur I/O : " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            printError("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void executeOrganization(Config config) throws IOException {
        validatePaths(config);

        // Chargement des règles
        printInfo("Chargement des règles depuis : " + config.rulesFile);
        Map<String, String> rules = Rules.load(config.rulesFile);
        printSuccess(rules.size() + " règle(s) chargée(s)");
        System.out.println();

        // Planification
        printInfo("Analyse du dossier : " + config.sourceDir);
        List<FileMover.Action> actions = FileMover.plan(config.sourceDir, rules);

        if (actions.isEmpty()) {
            printWarning("Aucun fichier à déplacer.");
            return;
        }

        printSuccess(actions.size() + " fichier(s) à déplacer");
        System.out.println();

        // Exécution
        if (config.apply) {
            printInfo("Application des changements...");
        } else {
            printInfo("Mode DRY-RUN - Utilisez --apply pour appliquer");
        }
        System.out.println();

        FileMover.Result result = FileMover.execute(actions, !config.apply);

        // Résumé
        System.out.println();
        printLine();
        System.out.println("RÉSUMÉ");
        printLine();
        printSuccess("Fichiers " + (config.apply ? "déplacés" : "à déplacer") + " : " + result.moved());

        if (result.skipped() > 0) {
            printWarning("Fichiers ignorés : " + result.skipped());
        }

        if (!result.errors().isEmpty()) {
            printError("Erreurs :");
            result.errors().forEach(err -> System.out.println("  - " + err));
        }

        if (!config.apply && result.moved() > 0) {
            System.out.println();
            printInfo("Relancez avec --apply pour appliquer");
        }
    }

    private static Config parseArguments(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--source", "-s" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--source nécessite un argument");
                    config.sourceDir = Paths.get(args[++i]);
                }
                case "--rules", "-r" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--rules nécessite un argument");
                    config.rulesFile = Paths.get(args[++i]);
                }
                case "--apply", "-a" -> config.apply = true;
                case "--help", "-h" -> config.showHelp = true;
                case "--version", "-v" -> config.showVersion = true;
                case "--interactive", "-i" -> config.interactive = true;
                default -> throw new IllegalArgumentException("Argument inconnu : " + args[i]);
            }
        }

        if (!config.showHelp && !config.showVersion && !config.interactive) {
            if (config.sourceDir == null) throw new IllegalArgumentException("--source est obligatoire");
            if (config.rulesFile == null) throw new IllegalArgumentException("--rules est obligatoire");
        }

        return config;
    }

    private static void validatePaths(Config config) {
        if (!Files.exists(config.sourceDir)) {
            throw new IllegalArgumentException("Dossier inexistant : " + config.sourceDir);
        }
        if (!Files.isDirectory(config.sourceDir)) {
            throw new IllegalArgumentException("--source doit être un dossier : " + config.sourceDir);
        }
        if (!Files.exists(config.rulesFile)) {
            throw new IllegalArgumentException("Fichier inexistant : " + config.rulesFile);
        }
        if (!Files.isRegularFile(config.rulesFile)) {
            throw new IllegalArgumentException("--rules doit être un fichier : " + config.rulesFile);
        }
    }

    private static class Config {
        Path sourceDir;
        Path rulesFile;
        boolean apply = false;
        boolean showHelp = false;
        boolean showVersion = false;
        boolean interactive = false;
    }
}
