package io.neatify.cli.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import io.neatify.core.PathSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Handles journaling and undoing operations. */
public final class UndoExecutor {

    private static final Logger logger = LoggerFactory.getLogger(UndoExecutor.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private UndoExecutor() {}

    public record Move(java.nio.file.Path from, java.nio.file.Path to) {}
    public record UndoResult(int restored, int skipped, List<String> errors) {}
    public record RunMeta(long time, String onCollision, int movesCount, Path file) {}

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

    private static Path neatifyDir(Path sourceRoot) { return sourceRoot.resolve(".neatify"); }
    private static Path runsDir(Path sourceRoot) { return neatifyDir(sourceRoot).resolve("runs"); }
    private static Path gitignore(Path sourceRoot) { return neatifyDir(sourceRoot).resolve(".gitignore"); }
    private static Path manifestPath(Path sourceRoot) { return neatifyDir(sourceRoot).resolve("manifest.json"); }

    public static Path appendRun(Path sourceRoot, String onCollision, List<Move> moves) throws IOException {
        if (moves.isEmpty()) return null;
        Path dir = runsDir(sourceRoot);
        Files.createDirectories(dir);
        ensureGitignore(sourceRoot);

        long now = System.currentTimeMillis();
        Path runFile = dir.resolve(now + ".json");

        // Convert Move records to DTOs
        List<MoveDto> moveDtos = moves.stream()
            .map(m -> new MoveDto(
                m.from.toAbsolutePath().toString(),
                m.to.toAbsolutePath().toString()))
            .collect(Collectors.toList());

        RunDoc runDoc = new RunDoc(now, onCollision, moveDtos);
        String json = gson.toJson(runDoc);

        Files.writeString(runFile, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return runFile;
    }

    private static void ensureGitignore(Path sourceRoot) {
        try {
            Path gi = gitignore(sourceRoot);
            if (!Files.exists(gi)) {
                Files.createDirectories(gi.getParent());
                Files.writeString(gi, "*\n!.gitignore\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            logger.warn("Failed to create .gitignore file in .neatify directory: {}", e.getMessage());
        }
    }

    // ===== New per-run storage (.neatify/runs/<timestamp>.json) =====

    public static UndoResult undoLast(Path sourceRoot) throws IOException {
        UndoResult v2 = undoLastV2(sourceRoot);
        if (v2 != null) return v2;
        // Fallback to legacy manifest.json (compatibility)
        return undoLastFromLegacyManifest(sourceRoot);
    }

    public static UndoResult undoLastV2(Path sourceRoot) throws IOException {
        Path dir = runsDir(sourceRoot);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return null;
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            Path latest = s.filter(p -> p.getFileName().toString().endsWith(".json"))
                .max((a,b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                .orElse(null);
            if (latest == null) return null;
            return undoRunFile(sourceRoot, latest);
        }
    }

    public static java.util.List<RunMeta> listRuns(Path sourceRoot) throws IOException {
        Path dir = runsDir(sourceRoot);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return java.util.List.of();
        java.util.List<RunMeta> metas = new java.util.ArrayList<>();
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".json"))
             .sorted((a,b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
             .forEach(p -> {
                 try {
                     String c = Files.readString(p, StandardCharsets.UTF_8);
                     RunDoc rd = gson.fromJson(c, RunDoc.class);
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

    public static UndoResult undoRun(Path sourceRoot, long timestamp) throws IOException {
        Path file = runsDir(sourceRoot).resolve(timestamp + ".json");
        if (!Files.exists(file)) return null;
        return undoRunFile(sourceRoot, file);
    }

    private static UndoResult undoRunFile(Path sourceRoot, Path runFile) throws IOException {
        String content = Files.readString(runFile, StandardCharsets.UTF_8).trim();
        RunDoc rd = gson.fromJson(content, RunDoc.class);
        if (rd == null || rd.moves == null) return null;

        // Convert DTOs back to Move records
        List<Move> moves = rd.moves.stream()
            .map(dto -> new Move(Paths.get(dto.from), Paths.get(dto.to)))
            .collect(Collectors.toList());

        int restored = 0, skipped = 0; List<String> errors = new ArrayList<>();
        Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        for (Move m : moves) {
            Path from = m.from.toAbsolutePath().normalize();
            Path to = m.to.toAbsolutePath().normalize();
            if (!from.startsWith(normalizedRoot) || !to.startsWith(normalizedRoot)) {
                skipped++; errors.add("Out of scope: " + from + " / " + to); continue;
            }
            if (!Files.exists(to)) { skipped++; errors.add("Absent: " + to); continue; }
            try {
                PathSecurity.assertNoSymlinkInAncestry(from);
                Files.createDirectories(from.getParent());
                if (Files.exists(from)) { skipped++; continue; }
                Files.move(to, from);
                restored++;
            } catch (IOException e) { skipped++; errors.add(e.getMessage()); }
        }

        try {
            Files.deleteIfExists(runFile);
        } catch (IOException e) {
            logger.warn("Failed to delete run file after undo {}: {}", runFile, e.getMessage());
        }
        return new UndoResult(restored, skipped, errors);
    }

    // ====== Legacy manifest.json fallback ======
    private static UndoResult undoLastFromLegacyManifest(Path sourceRoot) throws IOException {
        Path mf = manifestPath(sourceRoot);
        if (!Files.exists(mf)) return null;
        String content = Files.readString(mf, StandardCharsets.UTF_8).trim();
        int runsStart = content.indexOf("[", content.indexOf("\"runs\""));
        int runsEnd = content.lastIndexOf("]");
        if (runsStart < 0 || runsEnd < 0 || runsEnd <= runsStart) return null;
        String runs = content.substring(runsStart + 1, runsEnd);
        int lastObjStart = runs.lastIndexOf('{');
        int lastObjEnd = runs.lastIndexOf('}');
        if (lastObjStart < 0 || lastObjEnd <= lastObjStart) return null;
        String lastRun = runs.substring(lastObjStart, lastObjEnd + 1);

        int ms = lastRun.indexOf("\"moves\"");
        if (ms < 0) return null;
        int arrStart = lastRun.indexOf('[', ms);
        int arrEnd = lastRun.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return null;
        String movesArr = lastRun.substring(arrStart + 1, arrEnd).trim();
        List<Move> moves = parseMoves(movesArr);

        int restored = 0, skipped = 0; List<String> errors = new ArrayList<>();
        for (Move m : moves) {
            Path from = m.from; Path to = m.to;
            if (!Files.exists(to)) { skipped++; errors.add("Absent: " + to); continue; }
            try {
                Files.createDirectories(from.getParent());
                if (Files.exists(from)) { skipped++; continue; }
                Files.move(to, from);
                restored++;
            } catch (IOException e) { skipped++; errors.add(e.getMessage()); }
        }

        String newRuns = (lastObjStart > 0 ? runs.substring(0, lastObjStart - 1) : "").trim();
        String newContent = content.substring(0, runsStart + 1) + newRuns + content.substring(runsEnd);
        Files.writeString(mf, newContent, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

        return new UndoResult(restored, skipped, errors);
    }

    // Legacy manifest.json parsing helpers (still needed for backward compatibility)
    private static List<Move> parseMoves(String movesArr) {
        List<Move> list = new ArrayList<>();
        if (movesArr.isEmpty()) return list;
        String[] objs = movesArr.split("},\\{");
        for (String o : objs) {
            String obj = o;
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";
            String from = extract(obj, "from");
            String to = extract(obj, "to");
            if (from != null && to != null) {
                list.add(new Move(Paths.get(from), Paths.get(to)));
            }
        }
        return list;
    }

    private static String extract(String jsonObj, String key) {
        String k = "\"" + key + "\"";
        int p = jsonObj.indexOf(k);
        if (p < 0) return null;
        int c = jsonObj.indexOf(':', p);
        int q1 = jsonObj.indexOf('"', c + 1);
        int q2 = jsonObj.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        return jsonObj.substring(q1 + 1, q2).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
