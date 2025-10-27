package io.neatify.core;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Responsable de la planification et de l'exécution des déplacements de fichiers.
 * Principe : plan() calcule les actions, execute() les applique (ou simule en dry run).
 */
public final class FileMover {

    private FileMover() {
        // Classe utilitaire
    }

    /**
     * Représente une action de déplacement de fichier.
     *
     * @param source le fichier source
     * @param target le fichier cible (chemin complet avec nom)
     * @param reason la raison du déplacement (ex: "extension : jpg -> Images")
     */
    public record Action(Path source, Path target, String reason) {}

    /**
     * Résultat de l'exécution.
     *
     * @param moved nombre de fichiers déplacés
     * @param skipped nombre de fichiers ignorés (erreurs, collisions non résolues, etc.)
     * @param errors liste des erreurs rencontrées
     */
    public record Result(int moved, int skipped, List<String> errors) {}

    /**
     * Planifie les déplacements de fichiers selon les règles données.
     *
     * @param sourceRoot dossier racine à analyser
     * @param rules map [extension -> dossier cible]
     * @return liste des actions planifiées
     * @throws IOException si le dossier source n'est pas accessible
     */
    public static List<Action> plan(Path sourceRoot, Map<String, String> rules) throws IOException {
        Objects.requireNonNull(sourceRoot, "Le dossier source ne peut pas être null");
        Objects.requireNonNull(rules, "Les règles ne peuvent pas être null");

        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("Le chemin source doit être un dossier : " + sourceRoot);
        }

        List<Action> actions = new ArrayList<>();

        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // Ignorer les fichiers cachés par défaut
                if (file.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    FileMetadata metadata = FileMetadata.from(file);

                    if (metadata.hasNoExtension()) {
                        // Pas de règle pour les fichiers sans extension
                        return FileVisitResult.CONTINUE;
                    }

                    String targetFolder = Rules.getTargetFolder(rules, metadata.extension());

                    if (targetFolder != null) {
                        Path targetDir = sourceRoot.resolve(targetFolder);
                        Path targetFile = resolveUniqueTarget(targetDir, metadata.fileName());

                        String reason = String.format("extension: %s -> %s", metadata.extension(), targetFolder);
                        actions.add(new Action(file, targetFile, reason));
                    }

                } catch (IOException e) {
                    System.err.println("Erreur lors de la lecture de " + file + " : " + e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return actions;
    }

    /**
     * Exécute les actions planifiées (ou simule si dryRun = true).
     *
     * @param actions liste des actions à exécuter
     * @param dryRun si true, simule sans déplacer (affiche juste les actions).
     * @return le résultat de l'exécution
     */
    public static Result execute(List<Action> actions, boolean dryRun) {
        Objects.requireNonNull(actions, "La liste d'actions ne peut pas être null");

        int moved = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Action action : actions) {
            if (dryRun) {
                System.out.printf("[DRY-RUN] %s -> %s (%s)%n",
                    action.source(),
                    action.target(),
                    action.reason()
                );
                moved++;
            } else {
                try {
                    // Créer le dossier cible si nécessaire
                    Files.createDirectories(action.target().getParent());

                    // Déplacer le fichier (ATOMIC_MOVE si même volume, sinon REPLACE_EXISTING)
                    try {
                        Files.move(action.source(), action.target(), StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        // Fallback : déplacement non atomique
                        Files.move(action.source(), action.target());
                    }

                    System.out.printf("[MOVED] %s -> %s%n", action.source().getFileName(), action.target());
                    moved++;

                } catch (IOException e) {
                    String errorMsg = String.format("Echec du deplacement de %s: %s",
                        action.source(),
                        e.getMessage()
                    );
                    errors.add(errorMsg);
                    System.err.println("[ERROR] " + errorMsg);
                    skipped++;
                }
            }
        }

        return new Result(moved, skipped, errors);
    }

    /**
     * Résout un chemin cible unique en ajoutant un suffixe _n si le fichier existe déjà.
     *
     * @param targetDir dossier cible
     * @param fileName nom du fichier
     * @return un chemin unique (ajoute _1, _2, etc. si collision).
     */
    private static Path resolveUniqueTarget(Path targetDir, String fileName) {
        Path target = targetDir.resolve(fileName);

        if (!Files.exists(target)) {
            return target;
        }

        // Collision : ajouter un suffixe _n
        String nameWithoutExt = fileName;
        String extension = "";

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        while (Files.exists(target)) {
            String newName = nameWithoutExt + "_" + counter + extension;
            target = targetDir.resolve(newName);
            counter++;
        }

        return target;
    }
}
