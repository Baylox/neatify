package io.neatify.cli.args;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Command-line arguments parser for Neatify.
 * Builds an immutable configuration via simple handlers.
 */
public class ArgumentParser {
    private final Map<String, ArgumentHandler> handlers;
    private CLIConfig config;
    private String[] args;
    private int index;

    public ArgumentParser() {
        this.handlers = createHandlers();
    }

    /**
     * Parses arguments and returns a configuration.
     * @param arguments CLI arguments
     * @return parsed configuration
     */
    public CLIConfig parse(String[] arguments) {
        this.config = new CLIConfig();
        this.args = arguments;

        for (index = 0; index < args.length; index++) {
            String arg = args[index];
            ArgumentHandler handler = handlers.get(arg);
            if (handler == null) {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
            index = handler.handle(index);
        }

        validateRequiredArguments();
        return config;
    }

    private Map<String, ArgumentHandler> createHandlers() {
        Map<String, ArgumentHandler> map = new HashMap<>();

        // Path-based arguments
        map.put("--source", i -> parsePathArgument(i, "--source", config::setSourceDir));
        map.put("-s", map.get("--source"));
        map.put("--rules", i -> parsePathArgument(i, "--rules", config::setRulesFile));
        map.put("-r", map.get("--rules"));
        map.put("--use-default-rules", i -> { config.setUseDefaultRules(true); return i; });

        // Simple boolean flags
        map.put("--apply", i -> { config.setApply(true); return i; });
        map.put("-a", map.get("--apply"));
        map.put("--help", i -> { config.setShowHelp(true); return i; });
        map.put("-h", map.get("--help"));
        map.put("--version", i -> { config.setShowVersion(true); return i; });
        map.put("-v", map.get("--version"));
        map.put("--interactive", i -> { config.setInteractive(true); return i; });
        map.put("-i", map.get("--interactive"));
        map.put("--undo", i -> { config.setUndo(true); return i; });
        map.put("--undo-list", i -> { config.setUndo(true); config.setUndoList(true); return i; });
        map.put("--undo-run", i -> { requireNextArgument(i, "--undo-run"); config.setUndo(true); config.setUndoRun(args[i+1]); return i + 1; });
        map.put("--no-color", i -> { config.setNoColor(true); return i; });
        map.put("--ascii", i -> { config.setAscii(true); return i; });
        map.put("--json", i -> { config.setJson(true); return i; });

        // Logging levels
        map.put("--quiet", i -> { config.setQuiet(true); return i; });
        map.put("-q", map.get("--quiet"));
        map.put("--verbose", i -> { config.setVerbose(true); return i; });
        map.put("--debug", i -> { config.setDebug(true); return i; });

        // Arguments with values
        map.put("--per-folder-preview", this::parsePerFolderPreview);
        map.put("--sort", this::parseSort);
        map.put("--on-collision", this::parseCollision);
        map.put("--include", this::parseInclude);
        map.put("--exclude", this::parseExclude);
        map.put("--max-files", this::parseMaxFiles);

        return map;
    }

    private int parsePathArgument(int i, String argName, PathConsumer consumer) {
        requireNextArgument(i, argName);
        consumer.accept(Paths.get(args[i + 1]));
        return i + 1;
    }

    private int parsePerFolderPreview(int i) {
        requireNextArgument(i, "--per-folder-preview");
        try {
            int value = Integer.parseInt(args[i + 1]);
            if (value <= 0) {
                throw new IllegalArgumentException("--per-folder-preview must be positive");
            }
            config.setPerFolderPreview(value);
            return i + 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--per-folder-preview requires a number");
        }
    }

    private int parseMaxFiles(int i) {
        requireNextArgument(i, "--max-files");
        try {
            int value = Integer.parseInt(args[i + 1]);
            if (value <= 0) throw new IllegalArgumentException("--max-files must be positive");
            config.setMaxFiles(value);
            return i + 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--max-files requires a number");
        }
    }

    private int parseSort(int i) {
        requireNextArgument(i, "--sort");
        String sort = args[i + 1].toLowerCase();
        if (!sort.matches("alpha|ext|size")) {
            throw new IllegalArgumentException("--sort must be one of: alpha, ext or size");
        }
        config.setSortMode(sort);
        return i + 1;
    }

    private int parseCollision(int i) {
        requireNextArgument(i, "--on-collision");
        String strategy = args[i + 1].toLowerCase();
        if (!strategy.matches("rename|skip|overwrite")) {
            throw new IllegalArgumentException("--on-collision must be one of: rename, skip or overwrite");
        }
        config.setOnCollision(strategy);
        return i + 1;
    }

    private int parseInclude(int i) {
        requireNextArgument(i, "--include");
        config.addInclude(args[i + 1]);
        return i + 1;
    }

    private int parseExclude(int i) {
        requireNextArgument(i, "--exclude");
        config.addExclude(args[i + 1]);
        return i + 1;
    }

    private void requireNextArgument(int i, String argName) {
        if (i + 1 >= args.length) {
            throw new IllegalArgumentException(argName + " requires a value");
        }
    }

    private void validateRequiredArguments() {
        boolean needsSource = !config.isShowHelp() && !config.isShowVersion() && !config.isInteractive();

        if (needsSource && config.getSourceDir() == null) {
            throw new IllegalArgumentException("--source is required");
        }

        boolean needsRules = needsSource && !config.isUndo() && !config.isUseDefaultRules();
        if (needsRules && config.getRulesFile() == null) {
            throw new IllegalArgumentException("--rules is required");
        }
    }

    @FunctionalInterface
    private interface ArgumentHandler { int handle(int index); }

    @FunctionalInterface
    private interface PathConsumer { void accept(Path path); }
}
