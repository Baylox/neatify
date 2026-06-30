package io.neatify.core.contract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Records every applied organization run and restores them on demand.
 *
 * <p>Each run is a list of {@link Move} (source &rarr; final target). The journal
 * persists them so that a run can later be reverted ({@link #undoLast},
 * {@link #undoRun}) or inspected ({@link #list}). Implementations are free to
 * choose the storage medium; {@link io.neatify.core.FileSystemRunJournal} writes
 * one JSON document per run under {@code .neatify/runs/}.
 */
public interface RunJournal {

    /** A single file relocation: {@code from} (original) &rarr; {@code to} (applied target). */
    record Move(Path from, Path to) {}

    /** Outcome of an undo operation. */
    record UndoResult(int restored, int skipped, List<String> errors) {
        public UndoResult {
            errors = List.copyOf(errors);
        }
    }

    /** Lightweight metadata describing a persisted run. */
    record RunMeta(long time, String onCollision, int movesCount, Path file) {}

    /**
     * Appends a run to the journal.
     *
     * @return the path of the written run, or {@code null} when {@code moves} is empty.
     */
    Path append(Path root, String onCollision, List<Move> moves) throws IOException;

    /** Reverts the most recent run. Returns {@code null} when there is nothing to undo. */
    UndoResult undoLast(Path root) throws IOException;

    /** Reverts the run identified by {@code timestamp}. Returns {@code null} when not found. */
    UndoResult undoRun(Path root, long timestamp) throws IOException;

    /** Lists persisted runs, most recent first. */
    List<RunMeta> list(Path root) throws IOException;
}
