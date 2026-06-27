package io.neatify.cli.ui;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import io.neatify.cli.args.CliOption;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the help output is derived from {@link CliOption} (every flag shows
 * up) and uses the launcher invocation rather than the raw {@code java -jar}.
 */
class HelpPrinterTest {

    private static String captureHelp() {
        PrintStream prev = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(out, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            HelpPrinter.print();
        } finally {
            System.setOut(prev);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void helpListsEveryFlag() {
        String help = captureHelp();
        for (CliOption option : CliOption.values()) {
            assertTrue(help.contains(option.flag()),
                "Help output is missing flag: " + option.flag());
            if (option.alias() != null) {
                assertTrue(help.contains(option.alias()),
                    "Help output is missing alias: " + option.alias());
            }
        }
    }

    @Test
    void helpListsEveryGroupTitle() {
        String help = captureHelp();
        for (CliOption.Group group : CliOption.Group.values()) {
            assertTrue(help.contains(group.title()),
                "Help output is missing group: " + group.title());
        }
    }

    @Test
    void helpUsesLauncherNotRawJar() {
        String help = captureHelp();
        assertFalse(help.contains("java -jar"),
            "Help should reference the `neatify` launcher, not `java -jar`");
        assertTrue(help.contains("neatify"), "Help should mention the launcher name");
    }
}
