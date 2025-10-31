package io.neatify.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

final class FilePlanner {

    private static final Logger logger = LoggerFactory.getLogger(FilePlanner.class);
    private static final Marker SECURITY_MARKER = MarkerFactory.getMarker("SECURITY");

    private FilePlanner() { }

    static List<FileMover.Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles,
                                       List<String> includes, List<String> excludes,
                                       boolean skipGitRepos) throws IOException {
        Objects.requireNonNull(sourceRoot, "Source directory cannot be null");
        Objects.requireNonNull(rules, "Rules cannot be null");

        if (maxFiles <= 0) throw new IllegalArgumentException("Max files quota must be positive: " + maxFiles);
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("Source path must be a directory: " + sourceRoot);
        }

        List<FileMover.Action> actions = new ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger fileCount = new java.util.concurrent.atomic.AtomicInteger(0);

        List<PathMatcher> includeMatchers = compileMatchers(sourceRoot, includes);
        List<PathMatcher> excludeMatchers = compileMatchers(sourceRoot, excludes);

        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Skip internal journal directory to avoid moving undo files
                Path name = dir.getFileName();
                if (name != null && name.toString().equals(".neatify")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                // Skip Git repositories (dir contains a .git entry or is the .git folder)
                if (skipGitRepos) {
                    try {
                        if (name != null && name.toString().equals(".git")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        Path gitEntry = dir.resolve(".git");
                        if (Files.exists(gitEntry)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    } catch (Exception ignored) { }
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // Never process files under the internal .neatify directory
                Path relToRoot = sourceRoot.relativize(file);
                if (relToRoot.getNameCount() > 0 &&
                    relToRoot.getName(0).toString().equals(".neatify")) {
                    return FileVisitResult.CONTINUE;
                }
                if (fileCount.incrementAndGet() > maxFiles) {
                    throw new IllegalStateException("File quota exceeded: " + maxFiles);
                }
                Optional<FileMover.Action> planned = planFor(file, sourceRoot, rules, includeMatchers, excludeMatchers);
                planned.ifPresent(actions::add);
                return FileVisitResult.CONTINUE;
            }
        });

        return actions;
    }

    private static Optional<FileMover.Action> planFor(Path file, Path sourceRoot,
                                                      Map<String, String> rules,
                                                      List<PathMatcher> includes,
                                                      List<PathMatcher> excludes) {
        String baseName = file.getFileName().toString();
        if (baseName.startsWith(".")) return Optional.empty(); // ignore hidden

        try {
            Path rel = sourceRoot.relativize(file);
            if (!includes.isEmpty() && !matchesIncludes(rel, includes)) return Optional.empty();
            if (matchesExcludes(rel, excludes)) return Optional.empty();

            FileMetadata metadata = FileMetadata.from(file);
            if (metadata.hasNoExtension()) return Optional.empty();

            String targetFolder = Rules.getTargetFolder(rules, metadata.extension());
            if (targetFolder == null) return Optional.empty();

            Path targetDir;
            try {
                targetDir = PathSecurity.safeResolveWithin(sourceRoot, targetFolder);
            } catch (SecurityException se) {
                logger.warn(SECURITY_MARKER, "Security violation detected: {}", se.getMessage());
                return Optional.empty();
            }
            if (!targetDir.startsWith(sourceRoot.normalize())) return Optional.empty();

            Path targetFile = targetDir.resolve(metadata.fileName());

            // Avoid planning a no-op move (already in the right place)
            if (file.toAbsolutePath().normalize().equals(targetFile.toAbsolutePath().normalize())) {
                return Optional.empty();
            }
            String reason = String.format("extension: %s -> %s", metadata.extension(), targetFolder);
            return Optional.of(new FileMover.Action(file, targetFile, reason));

        } catch (IOException e) {
            logger.error("Error while reading file {}: {}", file, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static boolean matchesIncludes(Path rel, List<PathMatcher> includes) {
        if (includes == null || includes.isEmpty()) return true;
        for (PathMatcher m : includes) if (m.matches(rel)) return true;
        return false;
    }

    private static boolean matchesExcludes(Path rel, List<PathMatcher> excludes) {
        if (excludes == null || excludes.isEmpty()) return false;
        for (PathMatcher m : excludes) if (m.matches(rel)) return true;
        return false;
    }

    private static List<PathMatcher> compileMatchers(Path base, List<String> patterns) {
        List<PathMatcher> list = new ArrayList<>();
        if (patterns == null) return list;
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            list.add(base.getFileSystem().getPathMatcher("glob:" + p));
            if (p.startsWith("**/")) {
                String tail = p.substring(3);
                if (!tail.isBlank()) {
                    list.add(base.getFileSystem().getPathMatcher("glob:" + tail));
                }
            }
        }
        return list;
    }
}
