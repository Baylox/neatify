package io.neatify.cli.core;

import io.neatify.core.PathSecurity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.neatify.cli.ui.Display.*;

/**
 * Gère la création de fichiers de règles en mode interactif.
 */
public final class RulesFileCreator {

    private RulesFileCreator() {
        // Classe utilitaire
    }

    /**
     * Lance le processus complet de création d'un fichier de règles.
     */
    public static void create() throws IOException {
        printSection("CREER UN FICHIER DE REGLES");

        String filename = readInput("Nom du fichier", "custom-rules/my-rules.properties");
        Path rulesFile = Paths.get(filename).toAbsolutePath().normalize();

        if (!validateSecurity(rulesFile)) return;
        if (!confirmOverwriteIfExists(rulesFile)) return;

        String content = generateDefaultContent();

        createParentDirectoryIfNeeded(rulesFile);

        if (!writeSecurely(rulesFile, content)) return;

        printSuccess(rulesFile);
    }

    private static boolean validateSecurity(Path rulesFile) {
        Path safeDir = Paths.get("custom-rules").toAbsolutePath().normalize();
        Path target = rulesFile.toAbsolutePath().normalize();

        if (!target.startsWith(safeDir)) {
            printError("SECURITE : Le fichier doit etre dans le dossier custom-rules/");
            waitForEnter();
            return false;
        }

        Path relativeTarget = safeDir.relativize(target);
        try {
            PathSecurity.validateRelativeSubpath(relativeTarget.toString());
        } catch (SecurityException | IllegalArgumentException e) {
            printError("SECURITE : " + e.getMessage());
            waitForEnter();
            return false;
        }

        return true;
    }

    private static boolean confirmOverwriteIfExists(Path rulesFile) {
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

    private static String generateDefaultContent() {
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

    private static void createParentDirectoryIfNeeded(Path rulesFile) throws IOException {
        Path parentDir = rulesFile.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            printInfo("Dossier créé : " + parentDir);
        }
    }

    private static boolean writeSecurely(Path rulesFile, String content) throws IOException {
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

    private static void printSuccess(Path rulesFile) {
        io.neatify.cli.ui.Display.printSuccess("Fichier cree: " + rulesFile.toAbsolutePath());
        printInfo("Vous pouvez maintenant l'editer pour personnaliser les regles.");
        printInfo("Note: Ce fichier ne sera pas versionne par Git.");
        waitForEnter();
    }
}
