package io.neatify.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.neatify.core.contract.FileMover;
import io.neatify.core.contract.RunJournal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared organization flow: plan &rarr; (dry-run | apply) &rarr; journal.
 *
 * <p>This is the single place that wires {@link FileMover} and {@link RunJournal} together.
 * Both CLI front-ends (the flag-driven {@code FileOrganizationExecutor} and the interactive
 * {@code FileOrganizer}) delegate here, so the move-and-journal sequence lives in exactly one
 * spot. The class has no presentation concerns and depends only on core contracts.
 */
public final class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);

    private final FileMover fileMover;
    private final RunJournal journal;

    public OrganizationService(FileMover fileMover, RunJournal journal) {
        this.fileMover = fileMover;
        this.journal = journal;
    }

    /** Inputs for a single organization run. */
    public record Request(
        Path sourceDir,
        Map<String, String> rules,
        int maxFiles,
        List<String> includes,
        List<String> excludes,
        FileMover.CollisionStrategy strategy,
        boolean skipGitRepos) {
        public Request {
            rules = Map.copyOf(rules);
            includes = List.copyOf(includes);
            excludes = List.copyOf(excludes);
        }
    }

    /**
     * Result of an {@link #apply} call. {@code journalPath} is {@code null} when nothing was
     * journaled (no moves) or when journaling failed; in the failure case {@code journalError}
     * carries the message so the caller can surface it without the service doing any I/O.
     */
    public record Outcome(FileMover.Result result, Path journalPath, String journalError) {}

    /** Computes the planned moves without touching the filesystem. */
    public List<FileMover.Action> plan(Request request) throws IOException {
        return fileMover.plan(
            request.sourceDir(), request.rules(), request.maxFiles(),
            request.includes(), request.excludes(), request.skipGitRepos());
    }

    /** Simulates the moves (no filesystem changes, no journal). */
    public FileMover.Result dryRun(Request request, List<FileMover.Action> actions) {
        return fileMover.execute(actions, true, request.strategy(), null);
    }

    /**
     * Applies the moves and records them in the journal. Journaling failures are non-fatal:
     * the files are already moved, so the {@link FileMover.Result} is returned regardless and
     * the error (if any) is reported through {@link Outcome#journalError()}.
     */
    public Outcome apply(Request request, List<FileMover.Action> actions) {
        List<RunJournal.Move> moves = new ArrayList<>();
        FileMover.Result result = fileMover.execute(
            actions, false, request.strategy(),
            (src, dst) -> moves.add(new RunJournal.Move(src, dst)));

        String onCollision = request.strategy().name().toLowerCase(Locale.ROOT);
        Path journalPath = null;
        String journalError = null;
        try {
            journalPath = journal.append(request.sourceDir(), onCollision, moves);
        } catch (IOException e) {
            journalError = e.getMessage();
            logger.error("Failed to write undo journal: {}", e.getMessage(), e);
        }
        return new Outcome(result, journalPath, journalError);
    }
}
