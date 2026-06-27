package io.neatify.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.neatify.core.contract.FileMover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FileExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FileExecutor.class);

    private FileExecutor() { }

    static FileMover.Result execute(List<FileMover.Action> actions, boolean dryRun,
                                    CollisionHandler handler,
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
                Path targetParent = action.target().getParent();
                if (targetParent != null) {
                    Files.createDirectories(targetParent);
                }
                Path finalTarget = handler.move(action.source(), action.target());
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
            }
        }
        return new FileMover.Result(moved, skipped, errors);
    }

    @FunctionalInterface
    interface CollisionHandler {
        Path move(Path source, Path target) throws IOException;
    }
}
