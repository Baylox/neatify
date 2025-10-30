package io.neatify.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;


/**
 * Plans and executes file moves according to rules.
 */
public final class FileMover {

    private static final int DEFAULT_MAX_FILES = 100_000; // Anti-DoS

    public enum CollisionStrategy {
        RENAME {
            @Override
            Path move(Path source, Path target) throws IOException {
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
                throw new IOException("Could not find a unique name after " + MAX_RETRIES + " attempts");
            }
        },
        SKIP {
            @Override
            Path move(Path source, Path target) throws IOException {
                try {
                    Files.move(source, target);
                    return target;
                } catch (FileAlreadyExistsException e) {
                    return null;
                }
            }
        },
        OVERWRITE {
            @Override
            Path move(Path source, Path target) throws IOException {
                try {
                    return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        };

        abstract Path move(Path source, Path target) throws IOException;
    }

    private FileMover() { }

    public record Action(Path source, Path target, String reason) {}
    public record Result(int moved, int skipped, List<String> errors) {}

    public static List<Action> plan(Path sourceRoot, Map<String, String> rules) throws IOException {
        return FilePlanner.plan(sourceRoot, rules, DEFAULT_MAX_FILES, List.of(), List.of());
    }

    public static List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles) throws IOException {
        return FilePlanner.plan(sourceRoot, rules, maxFiles, List.of(), List.of());
    }

    /** Plans with include/exclude glob filters on relative paths. */
    public static List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles,
                                    List<String> includes, List<String> excludes) throws IOException {
        return FilePlanner.plan(sourceRoot, rules, maxFiles, includes, excludes);
    }

    // Planning helpers moved to FilePlanner

    public static Result execute(List<Action> actions, boolean dryRun) {
        return FileExecutor.execute(actions, dryRun, CollisionStrategy.RENAME, null);
    }

    public static Result execute(List<Action> actions, boolean dryRun, CollisionStrategy strategy) {
        return FileExecutor.execute(actions, dryRun, strategy, null);
    }

    @FunctionalInterface
    public interface MoveListener { void onMoved(Path source, Path finalTarget); }

    public static Result execute(List<Action> actions, boolean dryRun, CollisionStrategy strategy, MoveListener listener) {
        return FileExecutor.execute(actions, dryRun, strategy, listener);
    }

    // Move logic moved to FileExecutor
}
