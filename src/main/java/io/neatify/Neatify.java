package io.neatify;

import io.neatify.cli.ui.BannerRenderer;
import io.neatify.cli.ui.HelpPrinter;
import io.neatify.cli.ui.InteractiveCLI;
import io.neatify.cli.util.Ansi;
import io.neatify.cli.util.AsciiSymbols;
import io.neatify.cli.util.PreviewRenderer;
import io.neatify.core.FileMover;
import io.neatify.core.PathSecurity;
import io.neatify.core.Rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.neatify.cli.ui.ConsoleUI.*;

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
                HelpPrinter.print();
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

        // Appliquer les options de rendu
        if (config.noColor) {
            Ansi.setEnabled(false);
        }
        if (config.ascii) {
            AsciiSymbols.setUseUnicode(false);
        }

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

        // Afficher l'aperçu avec le nouveau renderer
        PreviewRenderer.Config rendererConfig = new PreviewRenderer.Config()
            .maxFilesPerFolder(config.perFolderPreview)
            .sortMode(parseSortMode(config.sortMode))
            .showDuplicates(true);

        List<String> previewLines = PreviewRenderer.render(actions, rendererConfig);
        previewLines.forEach(System.out::println);

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
                case "--no-color" -> config.noColor = true;
                case "--ascii" -> config.ascii = true;
                case "--per-folder-preview" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--per-folder-preview necessite un argument");
                    try {
                        config.perFolderPreview = Integer.parseInt(args[++i]);
                        if (config.perFolderPreview <= 0) {
                            throw new IllegalArgumentException("--per-folder-preview doit etre positif");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("--per-folder-preview necessite un nombre");
                    }
                }
                case "--sort" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--sort necessite un argument");
                    String sort = args[++i].toLowerCase();
                    if (!sort.matches("alpha|ext|size")) {
                        throw new IllegalArgumentException("--sort doit etre: alpha, ext ou size");
                    }
                    config.sortMode = sort;
                }
                default -> throw new IllegalArgumentException("Argument inconnu : " + args[i]);
            }
        }

        if (!config.showHelp && !config.showVersion && !config.interactive) {
            if (config.sourceDir == null) throw new IllegalArgumentException("--source est obligatoire");
            if (config.rulesFile == null) throw new IllegalArgumentException("--rules est obligatoire");
        }

        return config;
    }

    private static PreviewRenderer.SortMode parseSortMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "ext" -> PreviewRenderer.SortMode.EXT;
            case "size" -> PreviewRenderer.SortMode.SIZE;
            default -> PreviewRenderer.SortMode.ALPHA;
        };
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

        // SÉCURITÉ : Valider que le dossier source est sûr
        try {
            PathSecurity.validateSourceDir(config.sourceDir);
        } catch (IOException e) {
            throw new IllegalArgumentException("Erreur lors de la validation: " + e.getMessage(), e);
        }
    }


    private static class Config {
        Path sourceDir;
        Path rulesFile;
        boolean apply = false;
        boolean showHelp = false;
        boolean showVersion = false;
        boolean interactive = false;

        // Options de preview
        boolean noColor = false;
        boolean ascii = false;
        int perFolderPreview = 5;
        String sortMode = "alpha";
    }
}
