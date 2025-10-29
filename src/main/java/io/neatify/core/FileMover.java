package io.neatify.core;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Planifie et exécute des déplacements de fichiers selon des règles.
 */
public final class FileMover {

    private static final int DEFAULT_MAX_FILES = 100_000; // Anti-DoS

    public enum CollisionStrategy { RENAME, SKIP, OVERWRITE }

    private FileMover() { }

    public record Action(Path source, Path target, String reason) {}
    public record Result(int moved, int skipped, List<String> errors) {}

    public static List<Action> plan(Path sourceRoot, Map<String, String> rules) throws IOException {
        return plan(sourceRoot, rules, DEFAULT_MAX_FILES);
    }

    public static List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles) throws IOException {
        return plan(sourceRoot, rules, maxFiles, List.of(), List.of());
    }

    /** Planifie en appliquant des filtres glob include/exclude sur chemins relatifs. */
    public static List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles,
                                    List<String> includes, List<String> excludes) throws IOException {
        Objects.requireNonNull(sourceRoot, "Le dossier source ne peut pas être null");
        Objects.requireNonNull(rules, "Les règles ne peuvent pas être null");

        if (maxFiles <= 0) throw new IllegalArgumentException("Le quota doit être positif : " + maxFiles);
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("Le chemin source doit être un dossier : " + sourceRoot);
        }

        List<Action> actions = new ArrayList<>();
        int[] fileCount = {0};

        List<PathMatcher> includeMatchers = compileMatchers(sourceRoot, includes);
        List<PathMatcher> excludeMatchers = compileMatchers(sourceRoot, excludes);

        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (++fileCount[0] > maxFiles) {
                    throw new IllegalStateException("Quota de fichiers dépassé : " + maxFiles);
                }
                return processFile(file, sourceRoot, rules, actions, includeMatchers, excludeMatchers);
            }
        });

        return actions;
    }

    private static FileVisitResult processFile(Path file, Path sourceRoot,
                                               Map<String, String> rules,
                                               List<Action> actions,
                                               List<PathMatcher> includes,
                                               List<PathMatcher> excludes) {
        String baseName = file.getFileName().toString();
        if (baseName.startsWith(".")) return FileVisitResult.CONTINUE; // ignore hidden

        try {
            // Filtres
            Path rel = sourceRoot.relativize(file);
            if (!includes.isEmpty()) {
                boolean ok = false;
                for (PathMatcher m : includes) { if (m.matches(rel)) { ok = true; break; } }
                if (!ok) return FileVisitResult.CONTINUE;
            }
            for (PathMatcher m : excludes) { if (m.matches(rel)) return FileVisitResult.CONTINUE; }

            FileMetadata metadata = FileMetadata.from(file);
            if (metadata.hasNoExtension()) return FileVisitResult.CONTINUE;

            String targetFolder = Rules.getTargetFolder(rules, metadata.extension());
            if (targetFolder == null) return FileVisitResult.CONTINUE;

            Path targetDir;
            try {
                targetDir = PathSecurity.safeResolveWithin(sourceRoot, targetFolder);
            } catch (SecurityException se) {
                System.err.println("[SECURITE] " + se.getMessage());
                return FileVisitResult.CONTINUE;
            }
            if (!targetDir.startsWith(sourceRoot.normalize())) return FileVisitResult.CONTINUE;

            Path targetFile = targetDir.resolve(metadata.fileName());
            String reason = String.format("extension: %s -> %s", metadata.extension(), targetFolder);
            actions.add(new Action(file, targetFile, reason));

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture de " + file + " : " + e.getMessage());
        }
        return FileVisitResult.CONTINUE;
    }

    private static List<PathMatcher> compileMatchers(Path base, List<String> patterns) {
        List<PathMatcher> list = new ArrayList<>();
        if (patterns == null) return list;
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            // principal
            list.add(base.getFileSystem().getPathMatcher("glob:" + p));
            // compat: **/X doit aussi matcher X (zéro dossier)
            if (p.startsWith("**/")) {
                String tail = p.substring(3);
                if (!tail.isBlank()) {
                    list.add(base.getFileSystem().getPathMatcher("glob:" + tail));
                }
            }
        }
        return list;
    }

    public static Result execute(List<Action> actions, boolean dryRun) {
        return execute(actions, dryRun, CollisionStrategy.RENAME);
    }

    public static Result execute(List<Action> actions, boolean dryRun, CollisionStrategy strategy) {
        Objects.requireNonNull(actions, "La liste d'actions ne peut pas être null");

        int moved = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Action action : actions) {
            if (dryRun) {
                System.out.printf("[DRY-RUN] %s -> %s (%s)%n", action.source(), action.target(), action.reason());
                moved++;
                continue;
            }
            try {
                Files.createDirectories(action.target().getParent());
                Path finalTarget = switch (strategy) {
                    case RENAME -> atomicMoveWithRetry(action.source(), action.target());
                    case SKIP -> moveSkipOnExist(action.source(), action.target());
                    case OVERWRITE -> moveOverwrite(action.source(), action.target());
                };
                if (finalTarget == null) {
                    // Skipped due to existing target
                    System.out.printf("[SKIPPED] %s (cible existe)%n", action.source().getFileName());
                    skipped++;
                } else {
                    System.out.printf("[MOVED] %s -> %s%n", action.source().getFileName(), finalTarget);
                    moved++;
                }
            } catch (IOException e) {
                String msg = String.format("Echec du deplacement de %s: %s", action.source(), e.getMessage());
                errors.add(msg);
                System.err.println("[ERROR] " + msg);
                skipped++;
            }
        }
        return new Result(moved, skipped, errors);
    }

    @FunctionalInterface
    public interface MoveListener { void onMoved(Path source, Path finalTarget); }

    public static Result execute(List<Action> actions, boolean dryRun, CollisionStrategy strategy, MoveListener listener) {
        Objects.requireNonNull(actions, "La liste d'actions ne peut pas être null");

        int moved = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Action action : actions) {
            if (dryRun) {
                System.out.printf("[DRY-RUN] %s -> %s (%s)%n", action.source(), action.target(), action.reason());
                moved++;
                continue;
            }
            try {
                Files.createDirectories(action.target().getParent());
                Path finalTarget = switch (strategy) {
                    case RENAME -> atomicMoveWithRetry(action.source(), action.target());
                    case SKIP -> moveSkipOnExist(action.source(), action.target());
                    case OVERWRITE -> moveOverwrite(action.source(), action.target());
                };
                if (finalTarget == null) {
                    System.out.printf("[SKIPPED] %s (cible existe)%n", action.source().getFileName());
                    skipped++;
                } else {
                    System.out.printf("[MOVED] %s -> %s%n", action.source().getFileName(), finalTarget);
                    moved++;
                    if (listener != null) listener.onMoved(action.source(), finalTarget);
                }
            } catch (IOException e) {
                String msg = String.format("Echec du deplacement de %s: %s", action.source(), e.getMessage());
                errors.add(msg);
                System.err.println("[ERROR] " + msg);
                skipped++;
            }
        }
        return new Result(moved, skipped, errors);
    }

    private static Path moveSkipOnExist(Path source, Path target) throws IOException {
        try {
            Files.move(source, target);
            return target;
        } catch (FileAlreadyExistsException e) {
            // Do not move, signal skip by returning null
            return null;
        }
    }

    private static Path moveOverwrite(Path source, Path target) throws IOException {
        try {
            return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path atomicMoveWithRetry(Path source, Path target) throws IOException {
        Path currentTarget = target;
        int counter = 1;
        final int MAX_RETRIES = 1000;
        while (counter <= MAX_RETRIES) {
            try {
                Files.move(source, currentTarget);
                return currentTarget;
            } catch (FileAlreadyExistsException e) {
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
