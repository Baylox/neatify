package io.neatify.cli.ui;

import io.neatify.cli.util.Ansi;
import io.neatify.cli.util.AsciiSymbols;
import io.neatify.core.FileMetadata;
import io.neatify.core.FileMover;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to generate and display a preview of planned changes.
 */
public final class Preview {

    /** Sorting options for display. */
    public enum SortMode {
        ALPHA,   // Alphabetical
        EXT,     // By extension
        SIZE     // By size (best-effort metadata)
    }

    /** Rendering configuration. */
    public static class Config {
        public int maxFilesPerFolder = 5;
        public SortMode sortMode = SortMode.ALPHA;
        public boolean showDuplicates = true;

        public Config maxFilesPerFolder(int value) { this.maxFilesPerFolder = value; return this; }
        public Config sortMode(SortMode mode) { this.sortMode = mode; return this; }
        public Config showDuplicates(boolean value) { this.showDuplicates = value; return this; }
    }

    /** File entry with metadata for display. */
    private record FileEntry(String name, String extension, int count, long totalBytes) {}

    /** Group of files per folder. */
    private record FolderGroup(String folderName, List<FileEntry> files) {}

    private Preview() { }

    // ============ Public API ============

    /** Prints the preview for actions with a custom configuration. */
    public static void print(List<FileMover.Action> actions, Config config) {
        render(actions, config).forEach(System.out::println);
    }

    /**
     * Generates a formatted preview of changes (for tests or advanced usage).
     * @param actions planned actions
     * @param config rendering configuration
     * @return lines to display
     */
    public static List<String> render(List<FileMover.Action> actions, Config config) {
        if (actions.isEmpty()) {
            return List.of();
        }

        // Group by destination folder
        Map<String, List<FileMover.Action>> byFolder = groupByFolder(actions);

        // Convert to FolderGroup with duplicate counting
        List<FolderGroup> groups = byFolder.entrySet().stream()
            .map(entry -> createFolderGroup(entry.getKey(), entry.getValue(), config))
            .toList();

        // Generate output lines
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(formatSection("CHANGES PREVIEW"));

        for (FolderGroup group : groups) {
            lines.addAll(renderFolderGroup(group, config));
        }

        lines.add("");
        lines.add(formatProgressBar(actions.size()));
        lines.add("");

        return lines;
    }

    // ============ Internal logic ============

    /** Groups actions by destination folder. */
    private static Map<String, List<FileMover.Action>> groupByFolder(List<FileMover.Action> actions) {
        Map<String, List<FileMover.Action>> grouped = new LinkedHashMap<>();
        for (FileMover.Action action : actions) {
            Path parent = action.target().getParent();
            String folderName = parent != null ? parent.getFileName().toString() : "(root)";
            grouped.computeIfAbsent(folderName, k -> new ArrayList<>()).add(action);
        }
        return grouped;
    }

    /** Creates a FolderGroup with duplicate counting. */
    private static FolderGroup createFolderGroup(String folderName, List<FileMover.Action> actions, Config config) {
        // group by file name
        Map<String, List<FileMover.Action>> byName = actions.stream()
            .collect(Collectors.groupingBy(a -> a.source().getFileName().toString(), LinkedHashMap::new, Collectors.toList()));

        List<FileEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<FileMover.Action>> e : byName.entrySet()) {
            String name = e.getKey();
            List<FileMover.Action> group = e.getValue();
            int count = group.size();
            long size = 0L;
            for (FileMover.Action a : group) {
                try { size += Files.size(a.source()); } catch (Exception ignore) { /* best-effort */ }
            }
            entries.add(new FileEntry(name, FileMetadata.extensionOf(name), count, size));
        }

        List<FileEntry> sorted = sortEntries(entries, config.sortMode);
        return new FolderGroup(folderName, sorted);
    }

    /** Sorts entries according to requested mode. */
    private static List<FileEntry> sortEntries(List<FileEntry> entries, SortMode mode) {
        return switch (mode) {
            case ALPHA -> entries.stream()
                .sorted(Comparator.comparing(FileEntry::name))
                .toList();
            case EXT -> entries.stream()
                .sorted(Comparator.comparing(FileEntry::extension).thenComparing(FileEntry::name))
                .toList();
            case SIZE -> entries.stream()
                .sorted(Comparator.comparingLong(FileEntry::totalBytes).reversed().thenComparing(FileEntry::name))
                .toList();
        };
    }

    /** Generates lines for a folder group. */
    private static List<String> renderFolderGroup(FolderGroup group, Config config) {
        List<String> lines = new ArrayList<>();

        int totalFiles = group.files.stream().mapToInt(FileEntry::count).sum();
        String header = String.format("\n%s %s/  (%d file%s)",
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
            lines.add(Ansi.dim(String.format("  %s %d more...", AsciiSymbols.plus(), remaining)));
        }

        return lines;
    }

    /** Formats a file entry. */
    private static String formatFileEntry(FileEntry entry, boolean showDuplicates) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(Ansi.dim(AsciiSymbols.bullet())).append(" ");
        sb.append(entry.name);
        if (showDuplicates && entry.count > 1) {
            sb.append(" ").append(Ansi.yellow(AsciiSymbols.times() + entry.count));
        }
        return sb.toString();
    }

    /** Formats the section header. */
    private static String formatSection(String title) {
        String line = Display.line();
        return line + "\n" + Display.center(title) + "\n" + line;
    }

    /** Formats the progress bar (100% after planning). */
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
