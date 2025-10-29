package io.neatify.cli.args;

import java.nio.file.Path;

/**
 * Configuration pour l'exécution en ligne de commande.
 * Contient tous les paramètres parsés depuis les arguments CLI.
 */
public class CLIConfig {
    private Path sourceDir;
    private Path rulesFile;
    private boolean apply = false;
    private boolean showHelp = false;
    private boolean showVersion = false;
    private boolean interactive = false;

    // Options de preview
    private boolean noColor = false;
    private boolean ascii = false;
    private int perFolderPreview = 5;
    private String sortMode = "alpha";

    // Sorties et execution
    private boolean json = false;
    private String onCollision = "rename"; // rename | skip | overwrite

    // Filtres
    private java.util.List<String> includes = new java.util.ArrayList<>();
    private java.util.List<String> excludes = new java.util.ArrayList<>();

    // Getters
    public Path getSourceDir() { return sourceDir; }
    public Path getRulesFile() { return rulesFile; }
    public boolean isApply() { return apply; }
    public boolean isShowHelp() { return showHelp; }
    public boolean isShowVersion() { return showVersion; }
    public boolean isInteractive() { return interactive; }
    public boolean isNoColor() { return noColor; }
    public boolean isAscii() { return ascii; }
    public int getPerFolderPreview() { return perFolderPreview; }
    public String getSortMode() { return sortMode; }
    public boolean isJson() { return json; }
    public String getOnCollision() { return onCollision; }
    public java.util.List<String> getIncludes() { return includes; }
    public java.util.List<String> getExcludes() { return excludes; }

    // Setters (package-private pour être utilisés seulement par ArgumentParser)
    void setSourceDir(Path sourceDir) { this.sourceDir = sourceDir; }
    void setRulesFile(Path rulesFile) { this.rulesFile = rulesFile; }
    void setApply(boolean apply) { this.apply = apply; }
    void setShowHelp(boolean showHelp) { this.showHelp = showHelp; }
    void setShowVersion(boolean showVersion) { this.showVersion = showVersion; }
    void setInteractive(boolean interactive) { this.interactive = interactive; }
    void setNoColor(boolean noColor) { this.noColor = noColor; }
    void setAscii(boolean ascii) { this.ascii = ascii; }
    void setPerFolderPreview(int perFolderPreview) { this.perFolderPreview = perFolderPreview; }
    void setSortMode(String sortMode) { this.sortMode = sortMode; }
    void setJson(boolean json) { this.json = json; }
    void setOnCollision(String onCollision) { this.onCollision = onCollision; }
    void addInclude(String pattern) { this.includes.add(pattern); }
    void addExclude(String pattern) { this.excludes.add(pattern); }

    /**
     * Vérifie si la configuration nécessite sourceDir et rulesFile.
     * @return true si on est en mode commande (pas help/version/interactive)
     */
    public boolean requiresSourceAndRules() {
        return !showHelp && !showVersion && !interactive;
    }
}
