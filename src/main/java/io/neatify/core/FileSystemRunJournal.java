package io.neatify.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.neatify.core.contract.RunJournal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filesystem-backed {@link RunJournal}.
 *
 * <p>Stores one JSON document per run under {@code <root>/.neatify/runs/<timestamp>.json}
 * and reverts moves with the same {@link PathSecurity} guards used during planning, so an
 * undo can never restore a file outside the source root (symlink / {@code ..} escapes are
 * rejected). A legacy {@code manifest.json} format is still read for backward compatibility.
 */
public final class FileSystemRunJournal implements RunJournal {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemRunJournal.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // JSON DTOs for Gson serialization
    private static final class MoveDto {
        String from;
        String to;

        MoveDto(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    private static final class RunDoc {
        long time;

        @SerializedName("onCollision")
        String onCollision;

        List<MoveDto> moves;

        RunDoc(long time, String onCollision, List<MoveDto> moves) {
            this.time = time;
            this.onCollision = onCollision;
            this.moves = moves;
        }
    }

    private static Path neatifyDir(Path sourceRoot) {
        return sourceRoot.resolve(".neatify");
    }

    private static Path runsDir(Path sourceRoot) {
        return neatifyDir(sourceRoot).resolve("runs");
    }

    private static Path gitignore(Path sourceRoot) {
        return neatifyDir(sourceRoot).resolve(".gitignore");
    }

    private static Path manifestPath(Path sourceRoot) {
        return neatifyDir(sourceRoot).resolve("manifest.json");
    }

    @Override
    public Path append(Path sourceRoot, String onCollision, List<Move> moves) throws IOException {
        if (moves.isEmpty()) {
            return null;
        }
        Path dir = runsDir(sourceRoot);
        Files.createDirectories(dir);
        ensureGitignore(sourceRoot);

        long now = System.currentTimeMillis();
        // Guard against two runs completing within the same millisecond (CREATE_NEW would fail)
        Path runFile = dir.resolve(now + ".json");
        int attempt = 1;
        while (Files.exists(runFile) && attempt <= 100) {
            runFile = dir.resolve(now + "_" + attempt + ".json");
            attempt++;
        }

        List<MoveDto> moveDtos = moves.stream()
            .map(m -> new MoveDto(
                m.from().toAbsolutePath().toString(),
                m.to().toAbsolutePath().toString()))
            .collect(Collectors.toList());

        RunDoc runDoc = new RunDoc(now, onCollision, moveDtos);
        String json = GSON.toJson(runDoc);

        Files.writeString(runFile, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return runFile;
    }

    private static void ensureGitignore(Path sourceRoot) {
        try {
            Path gi = gitignore(sourceRoot);
            if (!Files.exists(gi)) {
                Path parent = gi.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(gi, "*\n!.gitignore\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            logger.warn("Failed to create .gitignore file in .neatify directory: {}", e.getMessage());
        }
    }

    @Override
    public UndoResult undoLast(Path sourceRoot) throws IOException {
        UndoResult v2 = undoLastV2(sourceRoot);
        if (v2 != null) {
            return v2;
        }
        // Fallback to legacy manifest.json (compatibility)
        return undoLastFromLegacyManifest(sourceRoot);
    }

    private UndoResult undoLastV2(Path sourceRoot) throws IOException {
        Path dir = runsDir(sourceRoot);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return null;
        }
        try (Stream<Path> s = Files.list(dir)) {
            Path latest = s.filter(p -> p.getFileName().toString().endsWith(".json"))
                .max((a, b) -> {
                    // Compare numerically using the leading timestamp in the filename
                    // (handles both "<ts>.json" and "<ts>_N.json" from same-millisecond retries)
                    try {
                        long ta = Long.parseLong(a.getFileName().toString().split("[._]")[0]);
                        long tb = Long.parseLong(b.getFileName().toString().split("[._]")[0]);
                        int cmp = Long.compare(ta, tb);
                        if (cmp != 0) {
                            return cmp;
                        }
                    } catch (NumberFormatException ignored) {
                        // Fall through to lexicographic comparison below
                    }
                    return a.getFileName().toString().compareTo(b.getFileName().toString());
                })
                .orElse(null);
            if (latest == null) {
                return null;
            }
            return undoRunFile(sourceRoot, latest);
        }
    }

    @Override
    public List<RunMeta> list(Path sourceRoot) throws IOException {
        Path dir = runsDir(sourceRoot);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return List.of();
        }
        List<RunMeta> metas = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                .forEach(p -> {
                    try {
                        String c = Files.readString(p, StandardCharsets.UTF_8);
                        RunDoc rd = GSON.fromJson(c, RunDoc.class);
                        if (rd != null && rd.moves != null) {
                            metas.add(new RunMeta(rd.time, rd.onCollision, rd.moves.size(), p));
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to parse run file {}: {}", p, e.getMessage());
                    }
                });
        }
        return metas;
    }

    @Override
    public UndoResult undoRun(Path sourceRoot, long timestamp) throws IOException {
        Path dir = runsDir(sourceRoot);
        // Try exact match first
        Path file = dir.resolve(timestamp + ".json");
        if (Files.exists(file)) {
            return undoRunFile(sourceRoot, file);
        }

        // Search for suffixed files (e.g., timestamp_1.json, timestamp_2.json)
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return null;
        }
        String prefix = String.valueOf(timestamp);
        try (Stream<Path> s = Files.list(dir)) {
            Path match = s.filter(p -> {
                    String name = p.getFileName().toString();
                    if (!name.endsWith(".json")) {
                        return false;
                    }
                    String base = name.substring(0, name.length() - 5); // strip .json
                    // Match "timestamp" or "timestamp_N"
                    return base.equals(prefix) || base.startsWith(prefix + "_");
                })
                // Sort descending so the highest suffix (latest run) is selected first
                .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                .findFirst()
                .orElse(null);
            if (match == null) {
                return null;
            }
            return undoRunFile(sourceRoot, match);
        }
    }

    private UndoResult undoRunFile(Path sourceRoot, Path runFile) throws IOException {
        String content = Files.readString(runFile, StandardCharsets.UTF_8).trim();
        RunDoc rd = GSON.fromJson(content, RunDoc.class);
        if (rd == null || rd.moves == null) {
            return null;
        }

        List<Move> moves = rd.moves.stream()
            .map(dto -> new Move(Paths.get(dto.from), Paths.get(dto.to)))
            .collect(Collectors.toList());

        UndoResult result = restoreMoves(sourceRoot, moves);

        try {
            Files.deleteIfExists(runFile);
        } catch (IOException e) {
            logger.warn("Failed to delete run file after undo {}: {}", runFile, e.getMessage());
        }
        return result;
    }

    /**
     * Restores each move (moving {@code to} back to {@code from}) while enforcing that both
     * endpoints stay within {@code sourceRoot}. Out-of-scope or symlink-escaping moves are
     * skipped and reported.
     */
    private UndoResult restoreMoves(Path sourceRoot, List<Move> moves) {
        int restored = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        for (Move m : moves) {
            Path from = m.from().toAbsolutePath().normalize();
            Path to = m.to().toAbsolutePath().normalize();
            if (!from.startsWith(normalizedRoot) || !to.startsWith(normalizedRoot)) {
                skipped++;
                errors.add("Out of scope: " + from + " / " + to);
                continue;
            }
            if (!Files.exists(to)) {
                skipped++;
                errors.add("Absent: " + to);
                continue;
            }
            try {
                PathSecurity.assertResolvedWithin(sourceRoot, from);
                PathSecurity.assertResolvedWithin(sourceRoot, to); // block any symlink/.. escaping the source root
                Path fromParent = from.getParent();
                if (fromParent != null) {
                    Files.createDirectories(fromParent);
                }
                if (Files.exists(from)) {
                    skipped++;
                    continue;
                }
                Files.move(to, from);
                restored++;
            } catch (IOException | SecurityException e) {
                skipped++;
                errors.add(e.getMessage());
            }
        }
        return new UndoResult(restored, skipped, errors);
    }

    // ====== Legacy manifest.json fallback ======

    /** DTOs for the legacy manifest.json format: {"runs":[{"moves":[{"from":…,"to":…}]}]} */
    private static final class LegacyManifest {
        List<LegacyRun> runs;
    }

    private static final class LegacyRun {
        List<MoveDto> moves;
    }

    private UndoResult undoLastFromLegacyManifest(Path sourceRoot) throws IOException {
        Path mf = manifestPath(sourceRoot);
        if (!Files.exists(mf)) {
            return null;
        }
        String content = Files.readString(mf, StandardCharsets.UTF_8).trim();

        LegacyManifest manifest;
        try {
            manifest = GSON.fromJson(content, LegacyManifest.class);
        } catch (Exception e) {
            logger.warn("Cannot parse legacy manifest.json: {}", e.getMessage());
            return null;
        }
        if (manifest == null || manifest.runs == null || manifest.runs.isEmpty()) {
            return null;
        }

        // Take the last run and remove it from the list
        LegacyRun lastRun = manifest.runs.remove(manifest.runs.size() - 1);
        if (lastRun == null || lastRun.moves == null) {
            return null;
        }

        List<Move> moves = lastRun.moves.stream()
            .filter(dto -> dto.from != null && dto.to != null)
            .map(dto -> new Move(Paths.get(dto.from), Paths.get(dto.to)))
            .collect(Collectors.toList());

        UndoResult result = restoreMoves(sourceRoot, moves);

        // Write the manifest back with the last run removed
        Files.writeString(mf, GSON.toJson(manifest), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

        return result;
    }
}
