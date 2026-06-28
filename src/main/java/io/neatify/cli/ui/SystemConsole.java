package io.neatify.cli.ui;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * {@link Console} backed by {@link System#in} / {@link System#out}.
 *
 * <p>Owns the single {@link Scanner} over standard input (previously a static field on
 * {@code Display}). The scanner deliberately stays open for the whole process: closing it would
 * also close {@code System.in}.
 */
public final class SystemConsole implements Console {

    private final Scanner scanner;
    private final PrintStream out;

    public SystemConsole() {
        this(new Scanner(System.in, StandardCharsets.UTF_8), System.out);
    }

    SystemConsole(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    @Override
    public String readInput(String prompt) {
        return readInput(prompt, null);
    }

    @Override
    public String readInput(String prompt, String defaultValue) {
        String fullPrompt = defaultValue != null && !defaultValue.isEmpty()
            ? prompt + " [" + defaultValue + "]: "
            : prompt + ": ";

        out.print(fullPrompt);
        String input = scanner.nextLine().trim();
        return input.isEmpty() && defaultValue != null ? defaultValue : input;
    }

    @Override
    public void waitForEnter() {
        out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
