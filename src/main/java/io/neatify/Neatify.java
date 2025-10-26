package io.neatify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Point d'entrée CLI de Neatify.
 * Utilisation : java -jar neatify.jar --source <dossier> --rules <fichier.properties> [--apply]
 */
public final class Neatify {

    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        try {
            Config config = parseArguments(args);

            if (config.showHelp) {
                printHelp();
                return;
            }

            if (config.showVersion) {
                System.out.println("Neatify version " + VERSION);
                return;
            }

            // Validation des chemins
            validatePaths(config);

            // Chargement des règles
            System.out.println("📋 Chargement des règles depuis : " + config.rulesFile);
            Map<String, String> rules = Rules.load(config.rulesFile);
            System.out.println("✅ " + rules.size() + " règle(s) chargée(s)\n");

            // Planification
            System.out.println("🔍 Analyse du dossier : " + config.sourceDir);
            List<FileMover.Action> actions = FileMover.plan(config.sourceDir, rules);

            if (actions.isEmpty()) {
                System.out.println("ℹ️  Aucun fichier à déplacer selon les règles définies.");
                return;
            }

            System.out.println("📦 " + actions.size() + " fichier(s) à déplacer\n");

            // Exécution
            if (config.apply) {
                System.out.println("🚀 Application des changements...\n");
            } else {
                System.out.println("🔍 Mode DRY-RUN (simulation) - Utilisez --apply pour appliquer les changements\n");
            }

            FileMover.Result result = FileMover.execute(actions, !config.apply);

            // Résumé
            System.out.println("\n" + "=".repeat(50));
            System.out.println("📊 RÉSUMÉ");
            System.out.println("=".repeat(50));
            System.out.printf("✅ Fichiers %s : %d%n", config.apply ? "déplacés" : "à déplacer", result.moved());

            if (result.skipped() > 0) {
                System.out.printf("⚠️  Fichiers ignorés : %d%n", result.skipped());
            }

            if (!result.errors().isEmpty()) {
                System.out.println("\n❌ Erreurs rencontrées :");
                result.errors().forEach(err -> System.out.println("  - " + err));
            }

            if (!config.apply && result.moved() > 0) {
                System.out.println("\n💡 Pour appliquer ces changements, relancez avec --apply");
            }

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Erreur de configuration : " + e.getMessage());
            System.err.println("Utilisez --help pour voir l'utilisation correcte.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("❌ Erreur I/O : " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Parse les arguments de la ligne de commande.
     */
    private static Config parseArguments(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--source", "-s" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--source nécessite un argument");
                    }
                    config.sourceDir = Paths.get(args[++i]);
                }
                case "--rules", "-r" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--rules nécessite un argument");
                    }
                    config.rulesFile = Paths.get(args[++i]);
                }
                case "--apply", "-a" -> config.apply = true;
                case "--help", "-h" -> config.showHelp = true;
                case "--version", "-v" -> config.showVersion = true;
                default -> throw new IllegalArgumentException("Argument inconnu : " + args[i]);
            }
        }

        // Validation : --source et --rules sont obligatoires (sauf si --help ou --version)
        if (!config.showHelp && !config.showVersion) {
            if (config.sourceDir == null) {
                throw new IllegalArgumentException("L'argument --source est obligatoire");
            }
            if (config.rulesFile == null) {
                throw new IllegalArgumentException("L'argument --rules est obligatoire");
            }
        }

        return config;
    }

    /**
     * Valide que les chemins existent et sont valides.
     */
    private static void validatePaths(Config config) {
        if (!Files.exists(config.sourceDir)) {
            throw new IllegalArgumentException("Le dossier source n'existe pas : " + config.sourceDir);
        }

        if (!Files.isDirectory(config.sourceDir)) {
            throw new IllegalArgumentException("--source doit pointer vers un dossier : " + config.sourceDir);
        }

        if (!Files.exists(config.rulesFile)) {
            throw new IllegalArgumentException("Le fichier de règles n'existe pas : " + config.rulesFile);
        }

        if (!Files.isRegularFile(config.rulesFile)) {
            throw new IllegalArgumentException("--rules doit pointer vers un fichier : " + config.rulesFile);
        }
    }

    /**
     * Affiche l'aide.
     */
    private static void printHelp() {
        System.out.println("""

            📦 Neatify - Outil de rangement automatique de fichiers

            UTILISATION :
              java -jar neatify.jar --source <dossier> --rules <fichier.properties> [options]

            OPTIONS OBLIGATOIRES :
              --source, -s <dossier>        Dossier à ranger
              --rules, -r <fichier>         Fichier de règles (.properties)

            OPTIONS :
              --apply, -a                   Applique réellement les changements (sinon dry-run)
              --help, -h                    Affiche cette aide
              --version, -v                 Affiche la version

            EXEMPLES :
              # Simulation (dry-run)
              java -jar neatify.jar --source ~/Downloads --rules rules.properties

              # Application réelle
              java -jar neatify.jar --source ~/Downloads --rules rules.properties --apply

            FORMAT DU FICHIER DE RÈGLES :
              jpg=Images
              png=Images
              pdf=Documents
              mp4=Videos

            """);
    }

    /**
     * Configuration interne.
     */
    private static class Config {
        Path sourceDir;
        Path rulesFile;
        boolean apply = false;
        boolean showHelp = false;
        boolean showVersion = false;
    }
}
