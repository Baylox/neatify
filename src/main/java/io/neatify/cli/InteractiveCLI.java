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

    private final String version;

    public InteractiveCLI(String version) {
        this.version = version;
    }

    public void run() throws IOException {
        printBanner(version);

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
                    System.out.println("Neatify version " + version);
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
        System.out.println("  2. Créer un fichier de règles");
        System.out.println("  3. Afficher l'aide");
        System.out.println("  4. Afficher la version");
        System.out.println("  5. Quitter");
        printLine();
    }

    private void organizeFiles() throws IOException {
        printSection("ORGANISATION DE FICHIERS");

        // Demander le dossier source
        String sourcePath = readInput("Dossier à organiser (chemin complet)");
        Path sourceDir = Paths.get(sourcePath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            printError("Dossier invalide : " + sourcePath);
            waitForEnter();
            return;
        }

        // Demander le fichier de règles
        String rulesPath = readInput("Fichier de règles (.properties) [Entrée = règles par défaut]", "");

        Map<String, String> rules;

        // Si vide, utiliser les règles par défaut
        if (rulesPath.isBlank()) {
            printInfo("Utilisation des règles par défaut intégrées...");
            rules = Rules.getDefaults();
            printSuccess(rules.size() + " règle(s) par défaut chargée(s)");
        } else {
            // Sinon, charger le fichier spécifié
            Path rulesFile = Paths.get(rulesPath);

            if (!Files.exists(rulesFile)) {
                printError("Fichier inexistant : " + rulesPath);
                waitForEnter();
                return;
            }

            printInfo("Chargement des règles depuis le fichier...");
            rules = Rules.load(rulesFile);
            printSuccess(rules.size() + " règle(s) chargée(s)");
        }

        printInfo("Analyse du dossier...");
        List<FileMover.Action> actions = FileMover.plan(sourceDir, rules);

        if (actions.isEmpty()) {
            printWarning("Aucun fichier à déplacer.");
            waitForEnter();
            return;
        }

        printSuccess(actions.size() + " fichier(s) à déplacer");
        showPreview(actions);

        // Confirmation
        String confirm = readInput("Appliquer ces changements ? (o/N)", "n");
        if (!confirm.equalsIgnoreCase("o") && !confirm.equalsIgnoreCase("oui")) {
            printWarning("Opération annulée.");
            waitForEnter();
            return;
        }

        // Exécution
        printInfo("Application des changements...");
        FileMover.Result result = FileMover.execute(actions, false);

        // Résumé
        showSummary(result);
        waitForEnter();
    }

    private void showPreview(List<FileMover.Action> actions) {
        System.out.println();
        printSection("APERÇU DES CHANGEMENTS");
        int preview = Math.min(10, actions.size());
        for (int i = 0; i < preview; i++) {
            FileMover.Action action = actions.get(i);
            System.out.printf("  %s -> %s%n",
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
        printSection("RÉSUMÉ");
        printSuccess("Fichiers déplacés : " + result.moved());
        if (result.skipped() > 0) {
            printWarning("Fichiers ignorés : " + result.skipped());
        }
        if (!result.errors().isEmpty()) {
            printError("Erreurs : " + result.errors().size());
            result.errors().forEach(err -> System.out.println("  - " + err));
        }
    }

    private void createRulesFile() throws IOException {
        printSection("CRÉER UN FICHIER DE RÈGLES");

        String filename = readInput("Nom du fichier", "my-rules.properties");
        Path rulesFile = Paths.get(filename);

        if (Files.exists(rulesFile)) {
            String overwrite = readInput("Le fichier existe. Écraser ? (o/N)", "n");
            if (!overwrite.equalsIgnoreCase("o") && !overwrite.equalsIgnoreCase("oui")) {
                printWarning("Opération annulée.");
                waitForEnter();
                return;
            }
        }

        String content = """
            # Règles de rangement Neatify
            # Format : extension=DossierCible

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

            # Vidéos
            mp4=Videos
            avi=Videos

            # Code
            java=Code
            py=Code
            js=Code
            """;

        Files.writeString(rulesFile, content);
        printSuccess("Fichier créé : " + rulesFile.toAbsolutePath());
        printInfo("Vous pouvez maintenant l'éditer pour personnaliser les règles.");
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
        System.out.println("OPTIONS (mode ligne de commande) :");
        System.out.println("  --source, -s <dossier>      Dossier à ranger (obligatoire)");
        System.out.println("  --rules, -r <fichier>       Fichier de règles (obligatoire)");
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
        System.out.println("  # Application réelle");
        System.out.println("  java -jar neatify.jar --source ~/Downloads --rules rules.properties --apply");
        System.out.println();
    }
}
