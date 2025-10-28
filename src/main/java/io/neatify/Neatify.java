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
import java.util.HashMap;
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
        ArgumentParser parser = new ArgumentParser();
        return parser.parse(args);
    }

    /**
     * Classe interne pour parser les arguments de ligne de commande.
     * Utilise un Map de handlers pour éviter la complexité cyclomatique d'un gros switch.
     */
    private static class ArgumentParser {
        private final Map<String, ArgumentHandler> handlers;
        private Config config;
        private String[] args;
        private int index;

        ArgumentParser() {
            this.handlers = createHandlers();
        }

        Config parse(String[] arguments) {
            this.config = new Config();
            this.args = arguments;

            for (index = 0; index < args.length; index++) {
                String arg = args[index];
                ArgumentHandler handler = handlers.get(arg);

                if (handler == null) {
                    throw new IllegalArgumentException("Argument inconnu : " + arg);
                }

                index = handler.handle(index);
            }

            validateRequiredArguments();
            return config;
        }

        private Map<String, ArgumentHandler> createHandlers() {
            Map<String, ArgumentHandler> map = new HashMap<>();

            // Arguments avec chemins
            map.put("--source", i -> parsePathArgument(i, "--source", path -> config.sourceDir = path));
            map.put("-s", map.get("--source"));
            map.put("--rules", i -> parsePathArgument(i, "--rules", path -> config.rulesFile = path));
            map.put("-r", map.get("--rules"));

            // Flags booléens simples
            map.put("--apply", i -> { config.apply = true; return i; });
            map.put("-a", map.get("--apply"));
            map.put("--help", i -> { config.showHelp = true; return i; });
            map.put("-h", map.get("--help"));
            map.put("--version", i -> { config.showVersion = true; return i; });
            map.put("-v", map.get("--version"));
            map.put("--interactive", i -> { config.interactive = true; return i; });
            map.put("-i", map.get("--interactive"));
            map.put("--no-color", i -> { config.noColor = true; return i; });
            map.put("--ascii", i -> { config.ascii = true; return i; });

            // Arguments avec valeurs complexes
            map.put("--per-folder-preview", this::parsePerFolderPreview);
            map.put("--sort", this::parseSort);

            return map;
        }

        private int parsePathArgument(int i, String argName, PathConsumer consumer) {
            requireNextArgument(i, argName);
            consumer.accept(Paths.get(args[i + 1]));
            return i + 1;
        }

        private int parsePerFolderPreview(int i) {
            requireNextArgument(i, "--per-folder-preview");
            try {
                int value = Integer.parseInt(args[i + 1]);
                if (value <= 0) {
                    throw new IllegalArgumentException("--per-folder-preview doit etre positif");
                }
                config.perFolderPreview = value;
                return i + 1;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("--per-folder-preview necessite un nombre");
            }
        }

        private int parseSort(int i) {
            requireNextArgument(i, "--sort");
            String sort = args[i + 1].toLowerCase();
            if (!sort.matches("alpha|ext|size")) {
                throw new IllegalArgumentException("--sort doit etre: alpha, ext ou size");
            }
            config.sortMode = sort;
            return i + 1;
        }

        private void requireNextArgument(int i, String argName) {
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException(argName + " necessite un argument");
            }
        }

        private void validateRequiredArguments() {
            if (requiresSourceAndRules()) {
                if (config.sourceDir == null) {
                    throw new IllegalArgumentException("--source est obligatoire");
                }
                if (config.rulesFile == null) {
                    throw new IllegalArgumentException("--rules est obligatoire");
                }
            }
        }

        private boolean requiresSourceAndRules() {
            return !config.showHelp && !config.showVersion && !config.interactive;
        }
    }

    @FunctionalInterface
    private interface ArgumentHandler {
        int handle(int index);
    }

    @FunctionalInterface
    private interface PathConsumer {
        void accept(Path path);
    }

    private static PreviewRenderer.SortMode parseSortMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "ext" -> PreviewRenderer.SortMode.EXT;
            case "size" -> PreviewRenderer.SortMode.SIZE;
            default -> PreviewRenderer.SortMode.ALPHA;
        };
    }

    private static void validatePaths(Config config) {
        validateSourceDir(config.sourceDir);
        validateRulesFile(config.rulesFile);
        validateSourceDirSecurity(config.sourceDir);
    }

    private static void validateSourceDir(Path sourceDir) {
        if (!Files.exists(sourceDir)) {
            throw new IllegalArgumentException("Dossier inexistant: " + sourceDir);
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("--source doit etre un dossier: " + sourceDir);
        }
    }

    private static void validateRulesFile(Path rulesFile) {
        if (!Files.exists(rulesFile)) {
            throw new IllegalArgumentException("Fichier inexistant: " + rulesFile);
        }
        if (!Files.isRegularFile(rulesFile)) {
            throw new IllegalArgumentException("--rules doit etre un fichier: " + rulesFile);
        }
    }

    private static void validateSourceDirSecurity(Path sourceDir) {
        try {
            PathSecurity.validateSourceDir(sourceDir);
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
