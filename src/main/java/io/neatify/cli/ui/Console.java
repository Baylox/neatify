package io.neatify.cli.ui;

/**
 * Abstraction over interactive console input.
 *
 * <p>Introduced so the interactive flows ({@code InteractiveCLI}, {@code FileOrganizer},
 * {@code RulesFileCreator}) no longer depend on a process-wide static {@link java.util.Scanner}.
 * The production implementation is {@link SystemConsole}; tests can supply a fake that scripts
 * answers, making the interactive paths unit-testable.
 */
public interface Console {

    /** Prints {@code prompt} then returns the trimmed line read from input. */
    String readInput(String prompt);

    /**
     * Prints {@code prompt} (annotated with {@code defaultValue}) then returns the trimmed line,
     * substituting {@code defaultValue} when the user enters nothing.
     */
    String readInput(String prompt, String defaultValue);

    /** Blocks until the user presses Enter. */
    void waitForEnter();
}
