package io.neatify.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class FileExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FileExecutor.class);

    private FileExecutor() { }

    static FileMover.Result execute(List<FileMover.Action> actions, boolean dryRun,
                                    FileMover.CollisionStrategy strategy,
                                    FileMover.MoveListener listener) {
        Objects.requireNonNull(actions, "Action list cannot be null");

        int moved = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (FileMover.Action action : actions) {
            if (dryRun) {
                logger.info("[DRY-RUN] {} -> {} ({})", action.source(), action.target(), action.reason());
                moved++;
                continue;
            }
            try {
                Files.createDirectories(action.target().getParent());
                Path finalTarget = strategy.move(action.source(), action.target());
                if (finalTarget == null) {
                    logger.info("[SKIPPED] {} (target exists)", action.source().getFileName());
                    skipped++;
                } else {
                    logger.info("[MOVED] {} -> {}", action.source().getFileName(), finalTarget);
                    moved++;
                    if (listener != null) listener.onMoved(action.source(), finalTarget);
                }
            } catch (IOException e) {
                String msg = String.format("Failed to move %s: %s", action.source(), e.getMessage());
                errors.add(msg);
                logger.error("Failed to move file: {}", msg, e);
                skipped++;
            }
        }
        return new FileMover.Result(moved, skipped, errors);
    }

    // Strategy-specific moving logic is implemented in FileMover.CollisionStrategy
}
