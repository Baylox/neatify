package io.neatify;

import io.neatify.cli.FileOrganizationExecutor;
import io.neatify.cli.args.ArgumentParser;
import io.neatify.cli.args.CLIConfig;
import io.neatify.cli.ui.HelpPrinter;
import io.neatify.cli.ui.InteractiveCLI;

import java.io.IOException;

import static io.neatify.cli.ui.Display.printErr;

/**
 * Neatify main entry point.
 * Supports two modes: interactive (default) and CLI.
 */
public final class Neatify {

    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        try {
            // Interactive mode when no arguments
            if (args.length == 0) {
                new InteractiveCLI(VERSION).run();
                return;
            }

            // CLI mode
            CLIConfig config = parseArguments(args);

            if (config.isShowHelp()) {
                HelpPrinter.print();
                return;
            }

            if (config.isShowVersion()) {
                System.out.println("Neatify version " + VERSION);
                return;
            }

            if (config.isInteractive()) {
                new InteractiveCLI(VERSION).run();
                return;
            }

            // Normal execution
            new FileOrganizationExecutor().execute(config);

        } catch (IllegalArgumentException e) {
            printErr("Error: " + e.getMessage());
            System.err.println("Use --help to see usage.");
            System.exit(1);
        } catch (IOException e) {
            printErr("I/O Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            printErr("Unexpected error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static CLIConfig parseArguments(String[] args) {
        return new ArgumentParser().parse(args);
    }
}
