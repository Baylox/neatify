package io.neatify.cli.args;

import java.nio.file.Path;

/**
 * Configuration for command-line execution.
 * Holds all parameters parsed from CLI arguments.
 */
public class CLIConfig {
    private Path sourceDir;
    private Path rulesFile;
    private boolean useDefaultRules = false;
    private boolean apply = false;
    private boolean showHelp = false;
    private boolean showVersion = false;
    private boolean interactive = false;
    private boolean undo = false;
    private boolean undoList = false;
    private String undoRun = null;

    // Preview options
    private boolean noColor = false;
    private boolean ascii = false;
    private int perFolderPreview = 5;
    private String sortMode = "alpha";

    // Output and execution
    private boolean json = false;
    private String onCollision = "rename"; // rename | skip | overwrite
    private int maxFiles = 100_000;

    // Logging options
    private boolean quiet = false;    // WARN+ only
    private boolean verbose = false;  // INFO level (default)
    private boolean debug = false;    // DEBUG level

    // Filters
    private java.util.List<String> includes = new java.util.ArrayList<>();
    private java.util.List<String> excludes = new java.util.ArrayList<>();

    // Getters
    public Path getSourceDir() { return sourceDir; }
    public Path getRulesFile() { return rulesFile; }
    public boolean isUseDefaultRules() { return useDefaultRules; }
    public boolean isApply() { return apply; }
    public boolean isShowHelp() { return showHelp; }
    public boolean isShowVersion() { return showVersion; }
    public boolean isInteractive() { return interactive; }
    public boolean isUndo() { return undo; }
    public boolean isUndoList() { return undoList; }
    public String getUndoRun() { return undoRun; }
    public boolean isNoColor() { return noColor; }
    public boolean isAscii() { return ascii; }
    public int getPerFolderPreview() { return perFolderPreview; }
    public String getSortMode() { return sortMode; }
    public boolean isJson() { return json; }
    public String getOnCollision() { return onCollision; }
    public int getMaxFiles() { return maxFiles; }
    public boolean isQuiet() { return quiet; }
    public boolean isVerbose() { return verbose; }
    public boolean isDebug() { return debug; }
    public java.util.List<String> getIncludes() { return includes; }
    public java.util.List<String> getExcludes() { return excludes; }

    // Setters (package-private, intended for ArgumentParser only)
    void setSourceDir(Path sourceDir) { this.sourceDir = sourceDir; }
    void setRulesFile(Path rulesFile) { this.rulesFile = rulesFile; }
    void setUseDefaultRules(boolean useDefaultRules) { this.useDefaultRules = useDefaultRules; }
    void setApply(boolean apply) { this.apply = apply; }
    void setShowHelp(boolean showHelp) { this.showHelp = showHelp; }
    void setShowVersion(boolean showVersion) { this.showVersion = showVersion; }
    void setInteractive(boolean interactive) { this.interactive = interactive; }
    void setUndo(boolean undo) { this.undo = undo; }
    void setUndoList(boolean undoList) { this.undoList = undoList; }
    void setUndoRun(String undoRun) { this.undoRun = undoRun; }
    void setNoColor(boolean noColor) { this.noColor = noColor; }
    void setAscii(boolean ascii) { this.ascii = ascii; }
    void setPerFolderPreview(int perFolderPreview) { this.perFolderPreview = perFolderPreview; }
    void setSortMode(String sortMode) { this.sortMode = sortMode; }
    void setJson(boolean json) { this.json = json; }
    void setOnCollision(String onCollision) { this.onCollision = onCollision; }
    void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
    void setQuiet(boolean quiet) { this.quiet = quiet; }
    void setVerbose(boolean verbose) { this.verbose = verbose; }
    void setDebug(boolean debug) { this.debug = debug; }
    void addInclude(String pattern) { this.includes.add(pattern); }
    void addExclude(String pattern) { this.excludes.add(pattern); }

    /**
     * Indicates if both sourceDir and rulesFile are required
     * (i.e. CLI command mode, not help/version/interactive).
     */
    public boolean requiresSourceAndRules() {
        // True only when the CLI requires both --source and --rules.
        // With --use-default-rules, the rules file is not required.
        return !showHelp && !showVersion && !interactive && !undo && !useDefaultRules;
    }
}
