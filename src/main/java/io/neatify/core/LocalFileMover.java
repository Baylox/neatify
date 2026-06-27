package io.neatify.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import io.neatify.core.contract.FileMover;

/**
 * {@link FileMover} implementation backed by the local filesystem. Planning is
 * delegated to {@link FilePlanner} and execution to {@link FileExecutor}.
 */
public final class LocalFileMover implements FileMover {

    private static final int DEFAULT_MAX_FILES = 100_000;

    @Override
    public List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles,
                             List<String> includes, List<String> excludes,
                             boolean skipGitRepos) throws IOException {
        return FilePlanner.plan(sourceRoot, rules, maxFiles, includes, excludes, skipGitRepos);
    }

    public List<Action> plan(Path sourceRoot, Map<String, String> rules) throws IOException {
        return plan(sourceRoot, rules, DEFAULT_MAX_FILES, List.of(), List.of(), true);
    }

    @Override
    public Result execute(List<Action> actions, boolean dryRun, CollisionStrategy strategy, MoveListener listener) {
        return FileExecutor.execute(actions, dryRun, resolveHandler(strategy), listener);
    }

    public Result execute(List<Action> actions, boolean dryRun) {
        return execute(actions, dryRun, CollisionStrategy.RENAME, null);
    }

    private FileExecutor.CollisionHandler resolveHandler(CollisionStrategy strategy) {
        return switch (strategy) {
            case RENAME -> this::moveWithRename;
            case SKIP -> this::moveWithSkip;
            case OVERWRITE -> this::moveWithOverwrite;
        };
    }

    private Path moveWithRename(Path source, Path target) throws IOException {
        Path current = target;
        int counter = 1;
        final int MAX_RETRIES = 1000;
        while (counter <= MAX_RETRIES) {
            try {
                Files.move(source, current);
                return current;
            } catch (FileAlreadyExistsException e) {
                String fileName = target.getFileName().toString();
                String nameWithoutExt = fileName;
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    nameWithoutExt = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }
                current = target.getParent().resolve(nameWithoutExt + "_" + counter + extension);
                counter++;
            }
        }
        throw new IOException("Could not find a unique name after " + MAX_RETRIES + " attempts");
    }

    private Path moveWithSkip(Path source, Path target) throws IOException {
        try {
            Files.move(source, target);
            return target;
        } catch (FileAlreadyExistsException e) {
            return null;
        }
    }

    private Path moveWithOverwrite(Path source, Path target) throws IOException {
        try {
            return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
