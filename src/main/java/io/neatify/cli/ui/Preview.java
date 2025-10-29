package io.neatify.cli.ui;

import io.neatify.cli.util.Ansi;
import io.neatify.cli.util.AsciiSymbols;
import io.neatify.core.FileMetadata;
import io.neatify.core.FileMover;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilitaire pour generer et afficher l'apercu des changements.
 */
public final class Preview {

    /** Options de tri pour l'affichage. */
    public enum SortMode {
        ALPHA,   // Alphabetique
        EXT,     // Par extension
        SIZE     // Par taille (necessite metadonnees supplementaires)
    }

    /** Configuration du rendu. */
    public static class Config {
        public int maxFilesPerFolder = 5;
        public SortMode sortMode = SortMode.ALPHA;
        public boolean showDuplicates = true;

        public Config maxFilesPerFolder(int value) { this.maxFilesPerFolder = value; return this; }
        public Config sortMode(SortMode mode) { this.sortMode = mode; return this; }
        public Config showDuplicates(boolean value) { this.showDuplicates = value; return this; }
    }

    /** Element de fichier avec metadonnees pour l'affichage. */
    private record FileEntry(String name, String extension, int count) {}

    /** Groupe de fichiers par dossier. */
    private record FolderGroup(String folderName, List<FileEntry> files) {}

    private Preview() { }

    // ============ API Publique ============

    /** Affiche l'apercu des actions avec configuration personnalisee. */
    public static void print(List<FileMover.Action> actions, Config config) {
        render(actions, config).forEach(System.out::println);
    }

    /**
     * Genere l'apercu formate des changements (pour tests ou usage avance).
     * @param actions liste des actions planifiees
     * @param config configuration du rendu
     * @return lignes a afficher
     */
    public static List<String> render(List<FileMover.Action> actions, Config config) {
        if (actions.isEmpty()) {
            return List.of();
        }

        // Grouper par dossier de destination
        Map<String, List<FileMover.Action>> byFolder = groupByFolder(actions);

        // Convertir en FolderGroup avec comptage des duplicatas
        List<FolderGroup> groups = byFolder.entrySet().stream()
            .map(entry -> createFolderGroup(entry.getKey(), entry.getValue(), config))
            .toList();

        // Generer les lignes de sortie
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(formatSection("APERCU DES CHANGEMENTS"));

        for (FolderGroup group : groups) {
            lines.addAll(renderFolderGroup(group, config));
        }

        lines.add("");
        lines.add(formatProgressBar(actions.size()));
        lines.add("");

        return lines;
    }

    // ============ Logique interne ============

    /** Groupe les actions par dossier de destination. */
    private static Map<String, List<FileMover.Action>> groupByFolder(List<FileMover.Action> actions) {
        Map<String, List<FileMover.Action>> grouped = new LinkedHashMap<>();
        for (FileMover.Action action : actions) {
            Path parent = action.target().getParent();
            String folderName = parent != null ? parent.getFileName().toString() : "(racine)";
            grouped.computeIfAbsent(folderName, k -> new ArrayList<>()).add(action);
        }
        return grouped;
    }

    /** Cree un FolderGroup avec comptage des duplicatas. */
    private static FolderGroup createFolderGroup(String folderName, List<FileMover.Action> actions, Config config) {
        Map<String, Long> counts = actions.stream()
            .map(a -> a.source().getFileName().toString())
            .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

        List<FileEntry> entries = counts.entrySet().stream()
            .map(e -> new FileEntry(
                e.getKey(),
                FileMetadata.extensionOf(e.getKey()),
                e.getValue().intValue()
            ))
            .toList();

        List<FileEntry> sorted = sortEntries(entries, config.sortMode);
        return new FolderGroup(folderName, sorted);
    }

    /** Trie les entrees selon le mode demande. */
    private static List<FileEntry> sortEntries(List<FileEntry> entries, SortMode mode) {
        return switch (mode) {
            case ALPHA -> entries.stream().sorted(Comparator.comparing(FileEntry::name)).toList();
            case EXT -> entries.stream().sorted(Comparator.comparing(FileEntry::extension).thenComparing(FileEntry::name)).toList();
            case SIZE -> entries; // SIZE necessiterait des metadonnees supplementaires
        };
    }

    /** Genere les lignes pour un groupe de dossier. */
    private static List<String> renderFolderGroup(FolderGroup group, Config config) {
        List<String> lines = new ArrayList<>();

        int totalFiles = group.files.stream().mapToInt(FileEntry::count).sum();
        String header = String.format("\n%s %s/  (%d fichier%s)",
            Ansi.cyan(AsciiSymbols.arrow()),
            Ansi.cyan(group.folderName),
            totalFiles,
            totalFiles > 1 ? "s" : ""
        );
        lines.add(header);

        int maxShow = Math.min(config.maxFilesPerFolder, group.files.size());
        for (int i = 0; i < maxShow; i++) {
            FileEntry entry = group.files.get(i);
            lines.add(formatFileEntry(entry, config.showDuplicates));
        }

        if (group.files.size() > maxShow) {
            int remaining = group.files.size() - maxShow;
            lines.add(Ansi.dim(String.format("  %s %d autre(s)...", AsciiSymbols.plus(), remaining)));
        }

        return lines;
    }

    /** Formate une entree de fichier. */
    private static String formatFileEntry(FileEntry entry, boolean showDuplicates) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(Ansi.dim(AsciiSymbols.bullet())).append(" ");
        sb.append(entry.name);
        if (showDuplicates && entry.count > 1) {
            sb.append(" ").append(Ansi.yellow(AsciiSymbols.times() + entry.count));
        }
        return sb.toString();
    }

    /** Formate la section header. */
    private static String formatSection(String title) {
        String line = "================================================";
        return line + "\n" + title + "\n" + line;
    }

    /** Formate la barre de progression (100% apres planification). */
    private static String formatProgressBar(int fileCount) {
        int percentage = 100;
        int width = 40;
        int filled = width;
        StringBuilder bar = new StringBuilder("[");
        bar.append("#".repeat(Math.max(0, filled)));
        bar.append("] ").append(percentage).append("% (")
            .append(fileCount).append("/").append(fileCount).append(")");
        return bar.toString();
    }
}

