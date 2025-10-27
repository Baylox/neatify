package io.neatify;

import io.neatify.cli.BannerRenderer;
import io.neatify.cli.InteractiveCLI;
import io.neatify.core.FileMover;
import io.neatify.core.Rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.neatify.cli.ConsoleUI.*;

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
                printHelp();
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
            printError("Erreur: " + e.getMessage());
            System.err.println("Utilisez --help pour voir l'aide.");
            System.exit(1);
        } catch (IOException e) {
            printError("Erreur I/O: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            printError("Erreur inattendue: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void executeOrganization(Config config) throws IOException {
        validatePaths(config);

        // Chargement des regles
        printInfo("Chargement des regles depuis: " + config.rulesFile);
        Map<String, String> rules = Rules.load(config.rulesFile);
        printSuccess(rules.size() + " regle(s) chargee(s)");
        System.out.println();

        // Planification
        printInfo("Analyse du dossier: " + config.sourceDir);
        List<FileMover.Action> actions = FileMover.plan(config.sourceDir, rules);

        if (actions.isEmpty()) {
            printWarning("Aucun fichier a deplacer.");
            return;
        }

        printSuccess(actions.size() + " fichier(s) a deplacer");
        System.out.println();

        // Exécution
        if (config.apply) {
            printInfo("Application des changements...");
        } else {
            printInfo("Mode DRY-RUN - Utilisez --apply pour appliquer");
        }
        System.out.println();

        FileMover.Result result = FileMover.execute(actions, !config.apply);

        // Resume
        System.out.println(BannerRenderer.renderResultTable(
            result.moved(),
            result.skipped(),
            result.errors().size()
        ));

        if (!result.errors().isEmpty()) {
            printError("Details des erreurs:");
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
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--source necessite un argument");
                    config.sourceDir = Paths.get(args[++i]);
                }
                case "--rules", "-r" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--rules necessite un argument");
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
            throw new IllegalArgumentException("Dossier inexistant: " + config.sourceDir);
        }
        if (!Files.isDirectory(config.sourceDir)) {
            throw new IllegalArgumentException("--source doit etre un dossier: " + config.sourceDir);
        }
        if (!Files.exists(config.rulesFile)) {
            throw new IllegalArgumentException("Fichier inexistant: " + config.rulesFile);
        }
        if (!Files.isRegularFile(config.rulesFile)) {
            throw new IllegalArgumentException("--rules doit etre un fichier: " + config.rulesFile);
        }
    }

    private static void printHelp() {
        System.out.println();
        printLine();
        System.out.println("AIDE - NEATIFY");
        printLine();
        System.out.println();
        System.out.println("UTILISATION :");
        System.out.println("  java -jar neatify.jar [options]");
        System.out.println();
        System.out.println("MODES :");
        System.out.println("  Sans arguments              Lance le mode interactif");
        System.out.println("  --interactive, -i           Lance le mode interactif");
        System.out.println();
        System.out.println("OPTIONS (mode ligne de commande):");
        System.out.println("  --source, -s <dossier>      Dossier a ranger (obligatoire)");
        System.out.println("  --rules, -r <fichier>       Fichier de regles (obligatoire)");
        System.out.println("  --apply, -a                 Applique les changements (sinon dry-run)");
        System.out.println("  --help, -h                  Affiche cette aide");
        System.out.println("  --version, -v               Affiche la version");
        System.out.println();
        System.out.println("EXEMPLES :");
        System.out.println("  # Mode interactif");
        System.out.println("  java -jar neatify.jar");
        System.out.println();
        System.out.println("  # Simulation (dry-run)");
        System.out.println("  java -jar neatify.jar --source ~/Downloads --rules rules.properties");
        System.out.println();
        System.out.println("  # Application reelle");
        System.out.println("  java -jar neatify.jar --source ~/Downloads --rules rules.properties --apply");
        System.out.println();
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
