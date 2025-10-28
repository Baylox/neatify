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

    // Quota par défaut : 100 000 fichiers (protection DoS)
    private static final int DEFAULT_MAX_FILES = 100_000;

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
     * Planifie les déplacements de fichiers selon les règles données (quota par défaut).
     *
     * @param sourceRoot dossier racine à analyser
     * @param rules map [extension -> dossier cible]
     * @return liste des actions planifiées
     * @throws IOException si le dossier source n'est pas accessible
     */
    public static List<Action> plan(Path sourceRoot, Map<String, String> rules) throws IOException {
        return plan(sourceRoot, rules, DEFAULT_MAX_FILES);
    }

    /**
     * Planifie les déplacements de fichiers selon les règles données, avec quota personnalisé.
     *
     * @param sourceRoot dossier racine à analyser
     * @param rules map [extension -> dossier cible]
     * @param maxFiles nombre maximum de fichiers à traiter (protection DoS)
     * @return liste des actions planifiées
     * @throws IOException si le dossier source n'est pas accessible
     * @throws IllegalStateException si le quota de fichiers est dépassé
     */
    public static List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles) throws IOException {
        Objects.requireNonNull(sourceRoot, "Le dossier source ne peut pas être null");
        Objects.requireNonNull(rules, "Les règles ne peuvent pas être null");

        if (maxFiles <= 0) {
            throw new IllegalArgumentException("Le quota doit être positif : " + maxFiles);
        }

        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("Le chemin source doit être un dossier : " + sourceRoot);
        }

        List<Action> actions = new ArrayList<>();
        int[] fileCount = {0};

        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (++fileCount[0] > maxFiles) {
                    throw new IllegalStateException(
                        "Quota de fichiers dépassé : " + maxFiles + " fichiers maximum. " +
                        "Utilisez un dossier plus petit ou augmentez la limite."
                    );
                }
                return processFile(file, sourceRoot, rules, actions);
            }
        });

        return actions;
    }

    /**
     * Traite un fichier individuel et ajoute une action si une règle correspond.
     */
    private static FileVisitResult processFile(Path file, Path sourceRoot,
                                               Map<String, String> rules,
                                               List<Action> actions) {
        if (file.getFileName().toString().startsWith(".")) {
            return FileVisitResult.CONTINUE;
        }

        try {
            FileMetadata metadata = FileMetadata.from(file);

            if (metadata.hasNoExtension()) {
                return FileVisitResult.CONTINUE;
            }

            String targetFolder = Rules.getTargetFolder(rules, metadata.extension());

            if (targetFolder != null) {
                Path targetDir = sourceRoot.resolve(targetFolder).normalize();
                try {
                    targetDir = PathSecurity.safeResolveWithin(sourceRoot, targetFolder);
                } catch (SecurityException se) {
                    System.err.println("[SECURITE] " + se.getMessage());
                    return FileVisitResult.CONTINUE;
                }

                if (!targetDir.startsWith(sourceRoot.normalize())) {
                    System.err.println("[SECURITE] Tentative de path traversal bloquée : " + targetFolder);
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = targetDir.resolve(metadata.fileName());
                String reason = String.format("extension: %s -> %s", metadata.extension(), targetFolder);
                actions.add(new Action(file, targetFile, reason));
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture de " + file + " : " + e.getMessage());
        }

        return FileVisitResult.CONTINUE;
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

                    // SÉCURITÉ : Déplacement atomique avec gestion des collisions (anti-TOCTOU)
                    Path finalTarget = atomicMoveWithRetry(action.source(), action.target());

                    System.out.printf("[MOVED] %s -> %s%n", action.source().getFileName(), finalTarget);
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
     * Déplace un fichier de manière atomique, avec retry automatique si collision.
     * Protection anti-TOCTOU : pas de "check puis use", on essaie directement.
     *
     * @param source fichier source
     * @param target cible initiale
     * @return le chemin final (peut avoir un suffixe si collision)
     * @throws IOException si erreur I/O
     */
    private static Path atomicMoveWithRetry(Path source, Path target) throws IOException {
        Path currentTarget = target;
        int counter = 1;
        final int MAX_RETRIES = 1000;

        while (counter <= MAX_RETRIES) {
            try {
                // Tentative de déplacement SANS écraser (pas de REPLACE_EXISTING)
                // Note : pas d'ATOMIC_MOVE car il peut écraser sur certains systèmes
                Files.move(source, currentTarget);
                return currentTarget;  // Succès !
            } catch (FileAlreadyExistsException e) {
                // Collision : générer un nouveau nom avec suffixe
                String fileName = target.getFileName().toString();
                String nameWithoutExt = fileName;
                String extension = "";

                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    nameWithoutExt = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }

                String newName = nameWithoutExt + "_" + counter + extension;
                currentTarget = target.getParent().resolve(newName);
                counter++;
            }
        }

        throw new IOException("Impossible de trouver un nom unique après " + MAX_RETRIES + " tentatives");
    }
}
