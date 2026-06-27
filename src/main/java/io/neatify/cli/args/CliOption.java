package io.neatify.cli.args;

import java.util.Arrays;
import java.util.List;

/**
 * Single source of truth for the command-line options.
 *
 * <p>Each option declares its primary flag, optional short alias, optional
 * argument placeholder, help group and description in one place. Both the help
 * output ({@link io.neatify.cli.ui.HelpPrinter}) and the generated CLI
 * reference documentation derive from this enum, so they cannot drift apart.
 * {@link ArgumentParser} wires the parsing behaviour to these same flags.
 */
public enum CliOption {

    // Modes
    INTERACTIVE(Group.MODES, "--interactive", "-i", null, "Start interactive mode"),
    UNDO(Group.MODES, "--undo", null, null, "Undo the last run (journal)"),
    UNDO_LIST(Group.MODES, "--undo-list", null, null, "List journaled runs (.neatify/runs)"),
    UNDO_RUN(Group.MODES, "--undo-run", null, "<timestamp>", "Undo a specific run"),
    HELP(Group.MODES, "--help", "-h", null, "Show this help"),
    VERSION(Group.MODES, "--version", "-v", null, "Show version"),

    // Paths
    SOURCE(Group.PATHS, "--source", "-s", "<dir>", "Directory to organize (required)"),
    RULES(Group.PATHS, "--rules", "-r", "<file>", "Rules file (required unless --use-default-rules)"),
    USE_DEFAULT_RULES(Group.PATHS, "--use-default-rules", null, null, "Use built-in default rules (no --rules)"),

    // Execution
    APPLY(Group.EXECUTION, "--apply", "-a", null, "Apply changes (otherwise dry-run)"),
    JSON(Group.EXECUTION, "--json", null, null, "JSON output (preview + result)"),
    ON_COLLISION(Group.EXECUTION, "--on-collision", null, "<mode>", "Collision: rename (default), skip, overwrite"),
    MAX_FILES(Group.EXECUTION, "--max-files", null, "<n>", "Max files to scan (default: 100000)"),
    INCLUDE(Group.EXECUTION, "--include", null, "<glob>", "Include (repeatable), e.g. **/*.pdf"),
    EXCLUDE(Group.EXECUTION, "--exclude", null, "<glob>", "Exclude (repeatable), e.g. **/node_modules/**"),
    ALLOW_INSIDE_GIT(Group.EXECUTION, "--allow-inside-git", null, null, "Allow operating inside Git repositories (unsafe)"),

    // Display
    NO_COLOR(Group.DISPLAY, "--no-color", null, null, "Disable ANSI colors"),
    ASCII(Group.DISPLAY, "--ascii", null, null, "Use ASCII symbols instead of Unicode"),
    PER_FOLDER_PREVIEW(Group.DISPLAY, "--per-folder-preview", null, "<n>", "Files per folder to display (default: 5)"),
    SORT(Group.DISPLAY, "--sort", null, "<mode>", "File sort: alpha, ext or size (default: alpha)"),

    // Logging
    QUIET(Group.LOGGING, "--quiet", "-q", null, "Minimal output (WARN level)"),
    VERBOSE(Group.LOGGING, "--verbose", null, null, "Verbose output (INFO level, default)"),
    DEBUG(Group.LOGGING, "--debug", null, null, "Very verbose output (DEBUG level)");

    /** Logical grouping used to organize the help output and the docs. */
    public enum Group {
        MODES("MODES"),
        PATHS("PATHS"),
        EXECUTION("EXECUTION"),
        DISPLAY("DISPLAY"),
        LOGGING("LOGGING");

        private final String title;

        Group(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final Group group;
    private final String flag;
    private final String alias;
    private final String argMeta;
    private final String description;

    CliOption(Group group, String flag, String alias, String argMeta, String description) {
        this.group = group;
        this.flag = flag;
        this.alias = alias;
        this.argMeta = argMeta;
        this.description = description;
    }

    /** Primary flag, e.g. {@code --source}. */
    public String flag() {
        return flag;
    }

    /** Short alias (e.g. {@code -s}), or {@code null} when there is none. */
    public String alias() {
        return alias;
    }

    /** Argument placeholder (e.g. {@code <dir>}), or {@code null} for a boolean flag. */
    public String argMeta() {
        return argMeta;
    }

    /** Human-readable description. */
    public String description() {
        return description;
    }

    /** Group this option belongs to. */
    public Group group() {
        return group;
    }

    /** True when this option takes a value (i.e. it has an argument placeholder). */
    public boolean takesArgument() {
        return argMeta != null;
    }

    /** All flag spellings that should be recognized for this option (primary + alias). */
    public List<String> flags() {
        return alias == null ? List.of(flag) : List.of(flag, alias);
    }

    /** Every recognized flag spelling across all options (primary flags and aliases). */
    public static List<String> allFlags() {
        return Arrays.stream(values())
            .flatMap(o -> o.flags().stream())
            .toList();
    }
}
