package io.neatify.cli;

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
 * Gère le mode interactif de Neatify.
 */
public final class InteractiveCLI {

    private final AppInfo appInfo;

    public InteractiveCLI(String version) {
        this.appInfo = AppInfo.neatify(version);
    }

    public void run() throws IOException {
        printBanner(appInfo);

        while (true) {
            printMenu();
            String choice = readInput("Votre choix");

            switch (choice) {
                case "1" -> organizeFiles();
                case "2" -> createRulesFile();
                case "3" -> {
                    printHelp();
                    waitForEnter();
                }
                case "4" -> {
                    System.out.println(appInfo.name() + " version " + appInfo.version());
                    System.out.println(appInfo.description());
                    waitForEnter();
                }
                case "5" -> {
                    printSuccess("Au revoir !");
                    return;
                }
                default -> printWarning("Choix invalide. Veuillez réessayer.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        printLine();
        System.out.println("MENU PRINCIPAL");
        printLine();
        System.out.println("  1. Organiser des fichiers");
        System.out.println("  2. Creer un fichier de regles");
        System.out.println("  3. Afficher l'aide");
        System.out.println("  4. Afficher la version");
        System.out.println("  5. Quitter");
        printLine();
    }

    private void organizeFiles() throws IOException {
        printSection("ORGANISATION DE FICHIERS");

        // Demander le dossier source
        String sourcePath = readInput("Dossier a organiser (chemin complet)");
        Path sourceDir = Paths.get(sourcePath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            printError("Dossier invalide : " + sourcePath);
            waitForEnter();
            return;
        }

        // Demander le fichier de regles
        String rulesPath = readInput("Fichier de regles (.properties) [Entree = regles par defaut]", "");

        Map<String, String> rules;

        // Si vide, utiliser les regles par defaut
        if (rulesPath.isBlank()) {
            printInfo("Utilisation des regles par defaut integrees...");
            rules = Rules.getDefaults();
            printSuccess(rules.size() + " regle(s) par defaut chargee(s)");
        } else {
            // Sinon, charger le fichier spécifié
            Path rulesFile = Paths.get(rulesPath);

            if (!Files.exists(rulesFile)) {
                printError("Fichier inexistant : " + rulesPath);
                waitForEnter();
                return;
            }

            printInfo("Chargement des regles depuis le fichier...");
            rules = Rules.load(rulesFile);
            printSuccess(rules.size() + " regle(s) chargee(s)");
        }

        printInfo("Analyse du dossier...");
        List<FileMover.Action> actions = FileMover.plan(sourceDir, rules);

        if (actions.isEmpty()) {
            printWarning("Aucun fichier à déplacer.");
            waitForEnter();
            return;
        }

        printSuccess(actions.size() + " fichier(s) a deplacer");
        showPreview(actions);

        // Confirmation
        String confirm = readInput("Appliquer ces changements? (o/N)", "n");
        if (!confirm.equalsIgnoreCase("o") && !confirm.equalsIgnoreCase("oui")) {
            printWarning("Operation annulee.");
            waitForEnter();
            return;
        }

        // Exécution
        printInfo("Application des changements...");
        FileMover.Result result = FileMover.execute(actions, false);

        // Resume
        showSummary(result);
        waitForEnter();
    }

    private void showPreview(List<FileMover.Action> actions) {
        System.out.println();
        printSection("APERCU DES CHANGEMENTS");

        // Barre de progression visuelle
        System.out.println(BannerRenderer.renderProgressBar(actions.size(), actions.size(), 40));
        System.out.println();

        // Apercu des fichiers
        int preview = Math.min(10, actions.size());
        for (int i = 0; i < preview; i++) {
            FileMover.Action action = actions.get(i);
            System.out.printf("  [%d] %s\n      -> %s\n",
                i + 1,
                action.source().getFileName(),
                action.target().getParent().getFileName() + "/" + action.target().getFileName()
            );
        }
        if (actions.size() > preview) {
            System.out.println("  ... et " + (actions.size() - preview) + " autre(s)");
        }
        System.out.println();
    }

    private void showSummary(FileMover.Result result) {
        System.out.println();
        System.out.println(BannerRenderer.renderResultTable(
            result.moved(),
            result.skipped(),
            result.errors().size()
        ));

        if (!result.errors().isEmpty()) {
            printError("Details des erreurs:");
            result.errors().forEach(err -> System.out.println("  - " + err));
        }
    }

    private void createRulesFile() throws IOException {
        printSection("CREER UN FICHIER DE REGLES");

        String filename = readInput("Nom du fichier", "custom-rules/my-rules.properties");
        Path rulesFile = Paths.get(filename);

        if (Files.exists(rulesFile)) {
            String overwrite = readInput("Le fichier existe. Ecraser? (o/N)", "n");
            if (!overwrite.equalsIgnoreCase("o") && !overwrite.equalsIgnoreCase("oui")) {
                printWarning("Operation annulee.");
                waitForEnter();
                return;
            }
        }

        String content = """
            # Regles de rangement Neatify
            # Format: extension=DossierCible

            # Images
            jpg=Images
            png=Images
            gif=Images

            # Documents
            pdf=Documents
            docx=Documents
            txt=Documents

            # Archives
            zip=Archives
            rar=Archives

            # Videos
            mp4=Videos
            avi=Videos

            # Code
            java=Code
            py=Code
            js=Code
            """;

        // Créer le dossier parent s'il n'existe pas
        Path parentDir = rulesFile.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            printInfo("Dossier créé : " + parentDir);
        }

        Files.writeString(rulesFile, content);
        printSuccess("Fichier cree: " + rulesFile.toAbsolutePath());
        printInfo("Vous pouvez maintenant l'editer pour personnaliser les regles.");
        printInfo("Note: Ce fichier ne sera pas versionne par Git.");
        waitForEnter();
    }

    private void printHelp() {
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
}
