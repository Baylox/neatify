package io.neatify.cli.args;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps {@link CliOption} (the documented source of truth) and
 * {@link ArgumentParser} (the parsing behaviour) in sync: every declared flag
 * must be recognized by the parser, and an undeclared flag must be rejected.
 */
class CliOptionTest {

    private static final String VALID_ARG = "x";

    @Test
    void everyFlagIsRecognizedByTheParser() {
        for (CliOption option : CliOption.values()) {
            for (String flag : option.flags()) {
                String[] args = option.takesArgument()
                    ? new String[]{flag, VALID_ARG}
                    : new String[]{flag};
                // A recognized flag must not raise "Unknown argument".
                try {
                    new ArgumentParser().parse(args);
                } catch (IllegalArgumentException e) {
                    assertFalse(e.getMessage().startsWith("Unknown argument"),
                        "Flag declared in CliOption but unknown to the parser: " + flag);
                    // Other validation errors (e.g. "--source is required") are fine here.
                }
            }
        }
    }

    @Test
    void unknownFlagIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ArgumentParser().parse(new String[]{"--definitely-not-a-flag"}));
        assertTrue(e.getMessage().startsWith("Unknown argument"));
    }

    @Test
    void flagsAreUniqueAcrossOptions() {
        Set<String> seen = new HashSet<>();
        for (String flag : CliOption.allFlags()) {
            assertTrue(seen.add(flag), "Duplicate flag spelling declared: " + flag);
        }
    }

    @Test
    void everyOptionHasFlagAndDescription() {
        for (CliOption option : CliOption.values()) {
            assertNotNull(option.flag(), "flag must not be null");
            assertTrue(option.flag().startsWith("--"), "primary flag must start with --: " + option.flag());
            assertNotNull(option.description(), "description must not be null for " + option.flag());
            assertFalse(option.description().isBlank(), "description must not be blank for " + option.flag());
            assertNotNull(option.group(), "group must not be null for " + option.flag());
        }
    }

    @Test
    void parserAcceptsArgumentTakingOptionsWithAValue() {
        // Sanity: a value-taking option must consume the next token. Use a fully
        // valid command (source + default rules) so required-argument validation
        // does not get in the way.
        assertDoesNotThrow(() ->
            new ArgumentParser().parse(new String[]{"--source", "/tmp", "--use-default-rules"}));
    }
}
