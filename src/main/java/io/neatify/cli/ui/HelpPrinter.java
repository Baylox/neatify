package io.neatify.cli.ui;

import io.neatify.cli.args.CliOption;

import static io.neatify.cli.ui.Display.printLine;

/**
 * Prints application help.
 *
 * <p>The option list is derived from {@link CliOption} (the single source of
 * truth), so the help output, the parser and the generated CLI reference stay
 * in sync automatically.
 */
public final class HelpPrinter {

    private HelpPrinter() {
        // Utility class
    }

    public static void print() {
        System.out.println();
        printLine();
        System.out.println(Display.center("HELP - NEATIFY"));
        printLine();
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  neatify [options]");
        System.out.println("  (no arguments starts interactive mode)");

        for (CliOption.Group group : CliOption.Group.values()) {
            System.out.println();
            System.out.println(group.title() + ":");
            for (CliOption option : CliOption.values()) {
                if (option.group() == group) {
                    System.out.println("  " + formatInvocation(option) + describe(option));
                }
            }
        }

        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  # Interactive mode");
        System.out.println("  neatify");
        System.out.println();
        System.out.println("  # Simulation (dry-run)");
        System.out.println("  neatify --source ~/Downloads --rules rules.properties");
        System.out.println();
        System.out.println("  # Real apply");
        System.out.println("  neatify --source ~/Downloads --rules rules.properties --apply");
        System.out.println();
    }

    /** Left column: "--flag, -alias <arg>" padded to a fixed width. */
    private static String formatInvocation(CliOption option) {
        StringBuilder sb = new StringBuilder(option.flag());
        if (option.alias() != null) {
            sb.append(", ").append(option.alias());
        }
        if (option.argMeta() != null) {
            sb.append(' ').append(option.argMeta());
        }
        return sb.toString();
    }

    private static String describe(CliOption option) {
        String left = formatInvocation(option);
        int pad = Math.max(1, 28 - left.length());
        return " ".repeat(pad) + option.description();
    }
}
