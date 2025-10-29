package io.neatify.cli.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Gestion du journal et de l'annulation des operations. */
public final class UndoExecutor {

    private UndoExecutor() {}

    public record Move(java.nio.file.Path from, java.nio.file.Path to) {}
    public record UndoResult(int restored, int skipped, List<String> errors) {}

    private static Path manifestPath(Path sourceRoot) {
        return sourceRoot.resolve(".neatify").resolve("manifest.json");
    }

    public static void appendRun(Path sourceRoot, String onCollision, List<Move> moves) throws IOException {
        if (moves.isEmpty()) return;
        Path dir = sourceRoot.resolve(".neatify");
        Files.createDirectories(dir);
        Path mf = manifestPath(sourceRoot);
        String existing = Files.exists(mf) ? Files.readString(mf, StandardCharsets.UTF_8).trim() : "";
        StringBuilder sb = new StringBuilder();
        long now = System.currentTimeMillis();

        if (existing.isEmpty()) {
            sb.append("{\"runs\":[");
        } else {
            // retirer le dernier ']}', si present
            int idx = existing.lastIndexOf("]}");
            if (idx > 0) {
                sb.append(existing, 0, idx);
                sb.append(',');
            } else {
                sb.append("{\"runs\":[");
            }
        }

        sb.append('{')
          .append("\"time\":").append(now).append(',')
          .append("\"onCollision\":\"").append(escape(onCollision)).append("\",")
          .append("\"moves\":[");
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            sb.append('{')
              .append("\"from\":\"").append(escape(m.from.toAbsolutePath().toString())).append("\",")
              .append("\"to\":\"").append(escape(m.to.toAbsolutePath().toString())).append("\"")
              .append('}');
            if (i < moves.size() - 1) sb.append(',');
        }
        sb.append("]}");
        sb.append("]}");

        Files.writeString(mf, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static UndoResult undoLast(Path sourceRoot) throws IOException {
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

        // Extraire moves minimalement (sans lib JSON): rechercher "moves":[...]
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

        // Réécrire manifest sans le dernier run
        String newRuns = (lastObjStart > 0 ? runs.substring(0, lastObjStart - 1) : "").trim();
        String newContent = content.substring(0, runsStart + 1) + newRuns + content.substring(runsEnd);
        Files.writeString(mf, newContent, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

        return new UndoResult(restored, skipped, errors);
    }

    private static List<Move> parseMoves(String movesArr) {
        List<Move> list = new ArrayList<>();
        if (movesArr.isEmpty()) return list;
        // Split naïf sur '},{' (suppose pas de '}, {' dans chemins)
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

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

