package io.neatify.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.neatify.core.contract.FileMover;
import io.neatify.core.contract.RunJournal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seam tests for {@link OrganizationService}: with fake {@link FileMover} and {@link RunJournal}
 * implementations we can assert the plan &rarr; apply &rarr; journal wiring without touching disk.
 */
class OrganizationServiceTest {

    private static OrganizationService.Request request(FileMover.CollisionStrategy strategy) {
        return new OrganizationService.Request(
            Paths.get("/src"), Map.of("txt", "Docs"), 100, List.of(), List.of(), strategy, true);
    }

    @Test
    void plan_delegatesToFileMoverWithRequestValues() throws IOException {
        RecordingFileMover mover = new RecordingFileMover(List.of(
            new FileMover.Action(Paths.get("/src/a.txt"), Paths.get("/src/Docs/a.txt"), "rule")));
        OrganizationService service = new OrganizationService(mover, new RecordingJournal());

        List<FileMover.Action> actions = service.plan(request(FileMover.CollisionStrategy.RENAME));

        assertEquals(1, actions.size());
        assertEquals(100, mover.lastMaxFiles);
        assertTrue(mover.lastSkipGitRepos);
    }

    @Test
    void apply_executesThenJournalsCollectedMoves() {
        FileMover.Action a = new FileMover.Action(Paths.get("/src/a.txt"), Paths.get("/src/Docs/a.txt"), "rule");
        RecordingFileMover mover = new RecordingFileMover(List.of(a));
        RecordingJournal journal = new RecordingJournal();
        OrganizationService service = new OrganizationService(mover, journal);

        OrganizationService.Outcome outcome = service.apply(request(FileMover.CollisionStrategy.SKIP), List.of(a));

        assertFalse(mover.lastDryRun, "apply must execute for real (dryRun=false)");
        assertEquals(FileMover.CollisionStrategy.SKIP, mover.lastStrategy);
        assertEquals(1, journal.appendedMoves.size(), "the moved file must be journaled");
        assertEquals("skip", journal.lastOnCollision, "collision strategy name is recorded");
        assertNull(outcome.journalError());
        assertNotNull(outcome.journalPath());
    }

    @Test
    void apply_journalFailureIsNonFatal() {
        FileMover.Action a = new FileMover.Action(Paths.get("/src/a.txt"), Paths.get("/src/Docs/a.txt"), "rule");
        RecordingFileMover mover = new RecordingFileMover(List.of(a));
        OrganizationService service = new OrganizationService(mover, new ThrowingJournal());

        OrganizationService.Outcome outcome = service.apply(request(FileMover.CollisionStrategy.RENAME), List.of(a));

        assertNotNull(outcome.result(), "the move result is still returned when journaling fails");
        assertNull(outcome.journalPath());
        assertEquals("disk full", outcome.journalError());
    }

    @Test
    void dryRun_doesNotJournal() {
        FileMover.Action a = new FileMover.Action(Paths.get("/src/a.txt"), Paths.get("/src/Docs/a.txt"), "rule");
        RecordingFileMover mover = new RecordingFileMover(List.of(a));
        RecordingJournal journal = new RecordingJournal();
        OrganizationService service = new OrganizationService(mover, journal);

        service.dryRun(request(FileMover.CollisionStrategy.RENAME), List.of(a));

        assertTrue(mover.lastDryRun, "dryRun must simulate (dryRun=true)");
        assertEquals(0, journal.appendCalls, "dry-run must never write to the journal");
    }

    // ===== Fakes =====

    private static final class RecordingFileMover implements FileMover {
        private final List<Action> planned;
        int lastMaxFiles;
        boolean lastSkipGitRepos;
        boolean lastDryRun;
        CollisionStrategy lastStrategy;

        RecordingFileMover(List<Action> planned) {
            this.planned = planned;
        }

        @Override
        public List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles,
                List<String> includes, List<String> excludes, boolean skipGitRepos) {
            this.lastMaxFiles = maxFiles;
            this.lastSkipGitRepos = skipGitRepos;
            return planned;
        }

        @Override
        public Result execute(List<Action> actions, boolean dryRun, CollisionStrategy strategy, MoveListener listener) {
            this.lastDryRun = dryRun;
            this.lastStrategy = strategy;
            if (!dryRun && listener != null) {
                for (Action action : actions) {
                    listener.onMoved(action.source(), action.target());
                }
            }
            return new Result(actions.size(), 0, List.of());
        }
    }

    private static final class RecordingJournal implements RunJournal {
        final List<Move> appendedMoves = new ArrayList<>();
        String lastOnCollision;
        int appendCalls;

        @Override
        public Path append(Path root, String onCollision, List<Move> moves) {
            appendCalls++;
            lastOnCollision = onCollision;
            appendedMoves.addAll(moves);
            return root.resolve(".neatify/runs/1.json");
        }

        @Override
        public UndoResult undoLast(Path root) {
            return null;
        }

        @Override
        public UndoResult undoRun(Path root, long timestamp) {
            return null;
        }

        @Override
        public List<RunMeta> list(Path root) {
            return List.of();
        }
    }

    private static final class ThrowingJournal implements RunJournal {
        @Override
        public Path append(Path root, String onCollision, List<Move> moves) throws IOException {
            throw new IOException("disk full");
        }

        @Override
        public UndoResult undoLast(Path root) {
            return null;
        }

        @Override
        public UndoResult undoRun(Path root, long timestamp) {
            return null;
        }

        @Override
        public List<RunMeta> list(Path root) {
            return List.of();
        }
    }
}
