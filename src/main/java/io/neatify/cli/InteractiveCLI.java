package io.neatify.cli;

import io.neatify.core.FileMover;
import io.neatify.core.PathSecurity;
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

        Path sourceDir = promptAndValidateSourceDir();
        if (sourceDir == null) return;

        Map<String, String> rules = promptAndLoadRules();
        if (rules == null) return;

        List<FileMover.Action> actions = planActions(sourceDir, rules);
        if (actions == null) return;

        executeIfConfirmed(actions);
    }

    private Path promptAndValidateSourceDir() throws IOException {
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

    private Map<String, String> promptAndLoadRules() throws IOException {
        String rulesPath = readInput("Fichier de regles (.properties) [Entree = regles par defaut]", "");

        if (rulesPath.isBlank()) {
            return loadDefaultRules();
        } else {
            return loadCustomRules(rulesPath);
        }
    }

    private Map<String, String> loadDefaultRules() {
        printInfo("Utilisation des regles par defaut integrees...");
        Map<String, String> rules = Rules.getDefaults();
        printSuccess(rules.size() + " regle(s) par defaut chargee(s)");
        return rules;
    }

    private Map<String, String> loadCustomRules(String rulesPath) throws IOException {
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

    private List<FileMover.Action> planActions(Path sourceDir, Map<String, String> rules) throws IOException {
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

    private void executeIfConfirmed(List<FileMover.Action> actions) throws IOException {
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

    private void showPreview(List<FileMover.Action> actions) {
        // Utiliser le nouveau renderer
        PreviewRenderer.Config config = new PreviewRenderer.Config()
            .maxFilesPerFolder(5)
            .sortMode(PreviewRenderer.SortMode.ALPHA)
            .showDuplicates(true);

        List<String> lines = PreviewRenderer.render(actions, config);
        lines.forEach(System.out::println);
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
        Path rulesFile = Paths.get(filename).toAbsolutePath().normalize();

        if (!validateRulesFileSecurity(rulesFile, filename)) return;
        if (!confirmOverwriteIfExists(rulesFile)) return;

        String content = generateDefaultRulesContent();

        createParentDirectoryIfNeeded(rulesFile);

        if (!writeRulesFileSecurely(rulesFile, content)) return;

        printRulesFileCreationSuccess(rulesFile);
    }

    private boolean validateRulesFileSecurity(Path rulesFile, String filename) {
        // SÉCURITÉ : Vérifier que ce n'est pas un symlink
        if (Files.exists(rulesFile) && Files.isSymbolicLink(rulesFile)) {
            printError("SECURITE : Les liens symboliques sont interdits");
            waitForEnter();
            return false;
        }

        // SÉCURITÉ : Restreindre à la zone custom-rules/
        Path safeDir = Paths.get("custom-rules").toAbsolutePath().normalize();
        if (!rulesFile.startsWith(safeDir)) {
            printError("SECURITE : Le fichier doit etre dans le dossier custom-rules/");
            waitForEnter();
            return false;
        }

        // SÉCURITÉ : Bloquer path traversal
        if (filename.contains("..")) {
            printError("SECURITE : Path traversal interdit (..)");
            waitForEnter();
            return false;
        }

        return true;
    }

    private boolean confirmOverwriteIfExists(Path rulesFile) {
        if (Files.exists(rulesFile)) {
            String overwrite = readInput("Le fichier existe. Ecraser? (o/N)", "n");
            if (!overwrite.equalsIgnoreCase("o") && !overwrite.equalsIgnoreCase("oui")) {
                printWarning("Operation annulee.");
                waitForEnter();
                return false;
            }
        }
        return true;
    }

    private String generateDefaultRulesContent() {
        return """
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
    }

    private void createParentDirectoryIfNeeded(Path rulesFile) throws IOException {
        Path parentDir = rulesFile.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            printInfo("Dossier créé : " + parentDir);
        }
    }

    private boolean writeRulesFileSecurely(Path rulesFile, String content) throws IOException {
        // SÉCURITÉ : Vérifier les symlinks dans l'arborescence
        try {
            PathSecurity.assertNoSymlinkInAncestry(rulesFile);
        } catch (SecurityException e) {
            printError("SECURITE : " + e.getMessage());
            waitForEnter();
            return false;
        }

        // SÉCURITÉ : Écriture atomique avec CREATE_NEW (anti-TOCTOU)
        try {
            Files.writeString(rulesFile, content,
                java.nio.file.StandardOpenOption.CREATE_NEW);
            return true;
        } catch (java.nio.file.FileAlreadyExistsException e) {
            // Si on arrive ici, c'est que le fichier a été créé entre-temps (race condition)
            printError("SECURITE : Le fichier a ete cree par un autre processus");
            waitForEnter();
            return false;
        }
    }

    private void printRulesFileCreationSuccess(Path rulesFile) {
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
        System.out.println("OPTIONS D'AFFICHAGE :");
        System.out.println("  --no-color                  Desactive les couleurs ANSI");
        System.out.println("  --ascii                     Utilise des symboles ASCII au lieu d'Unicode");
        System.out.println("  --per-folder-preview <n>    Nombre de fichiers a afficher par dossier (defaut: 5)");
        System.out.println("  --sort <mode>               Tri des fichiers: alpha, ext ou size (defaut: alpha)");
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
