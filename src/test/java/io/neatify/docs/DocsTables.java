package io.neatify.docs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.neatify.cli.args.CliOption;
import io.neatify.core.DefaultRulesView;

/**
 * Renders Markdown tables from the code's single sources of truth
 * ({@link CliOption} and the default rules), so the documentation can be both
 * generated and checked for drift. Test-only helper.
 */
public final class DocsTables {

    private DocsTables() {
    }

    /** Markdown table of every CLI option, grouped by section. */
    public static String cliOptionsTable() {
        StringBuilder sb = new StringBuilder();
        for (CliOption.Group group : CliOption.Group.values()) {
            List<CliOption> inGroup = java.util.Arrays.stream(CliOption.values())
                .filter(o -> o.group() == group)
                .toList();
            if (inGroup.isEmpty()) {
                continue;
            }
            sb.append("### ").append(titleCase(group.title())).append("\n\n");
            sb.append("| Option | Argument | Description |\n");
            sb.append("|--------|----------|-------------|\n");
            for (CliOption o : inGroup) {
                String flags = o.alias() == null ? code(o.flag()) : code(o.flag()) + ", " + code(o.alias());
                String arg = o.argMeta() == null ? "" : code(o.argMeta());
                sb.append("| ").append(flags).append(" | ").append(arg).append(" | ")
                    .append(o.description()).append(" |\n");
            }
            sb.append("\n");
        }
        return sb.toString().stripTrailing() + "\n";
    }

    /** Markdown table of the built-in default rules, one row per destination folder. */
    public static String defaultRulesTable() {
        // extension -> folder, from the single source of truth.
        Map<String, String> rules = DefaultRulesView.defaults();

        // folder -> sorted extensions.
        Map<String, java.util.List<String>> byFolder = new TreeMap<>();
        for (Map.Entry<String, String> e : rules.entrySet()) {
            byFolder.computeIfAbsent(e.getValue(), k -> new java.util.ArrayList<>()).add(e.getKey());
        }

        // Stable, readable ordering: by folder name.
        Map<String, java.util.List<String>> ordered = new LinkedHashMap<>(byFolder);

        StringBuilder sb = new StringBuilder();
        sb.append("| Destination folder | Extensions |\n");
        sb.append("|--------------------|------------|\n");
        for (Map.Entry<String, java.util.List<String>> e : ordered.entrySet()) {
            java.util.List<String> exts = e.getValue();
            java.util.Collections.sort(exts);
            String cell = exts.stream().map(DocsTables::code).collect(Collectors.joining(", "));
            sb.append("| ").append(code(e.getKey())).append(" | ").append(cell).append(" |\n");
        }
        return sb.toString().stripTrailing() + "\n";
    }

    private static String code(String s) {
        return "`" + s + "`";
    }

    private static String titleCase(String upper) {
        String lower = upper.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
