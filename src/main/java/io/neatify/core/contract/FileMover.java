package io.neatify.core.contract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface FileMover {

    List<Action> plan(Path sourceRoot, Map<String, String> rules, int maxFiles,
                      List<String> includes, List<String> excludes,
                      boolean skipGitRepos) throws IOException;

    Result execute(List<Action> actions, boolean dryRun, CollisionStrategy strategy, MoveListener listener);

    enum CollisionStrategy { RENAME, SKIP, OVERWRITE }

    @FunctionalInterface
    interface MoveListener { void onMoved(Path source, Path finalTarget); }

    record Action(Path source, Path target, String reason) {}

    record Result(int moved, int skipped, List<String> errors) {
        public Result {
            errors = List.copyOf(errors);
        }
    }
}
