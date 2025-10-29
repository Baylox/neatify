package io.neatify.cli.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests essentiels pour ArgumentParser - Focus sur parsing et validation critique.
 */
class ArgumentParserTest {

    private final ArgumentParser parser = new ArgumentParser();

    @Test
    void testParse_ValidCompleteArguments() {
        String[] args = {"--source", "/tmp/source", "--rules", "/tmp/rules.properties", "--apply"};
        CLIConfig config = parser.parse(args);

        assertTrue(config.getSourceDir().toString().contains("source"));
        assertTrue(config.getRulesFile().toString().contains("rules.properties"));
        assertTrue(config.isApply());
    }

    @Test
    void testParse_MissingRequiredArguments() {
        String[] args = {"--source", "/tmp/source"}; // Missing --rules

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> parser.parse(args));

        assertTrue(exception.getMessage().contains("--rules is required"));
    }

    @Test
    void testParse_HelpMode_NoRequiredArgs() {
        String[] args = {"--help"};
        CLIConfig config = parser.parse(args);

        assertTrue(config.isShowHelp());
        assertFalse(config.requiresSourceAndRules());
    }

    @Test
    void testParse_UnknownArgument() {
        String[] args = {"--unknown-flag"};

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> parser.parse(args));

        assertTrue(exception.getMessage().contains("Unknown argument"));
    }

    @Test
    void testParse_InvalidPerFolderPreview() {
        String[] args = {"--source", "/tmp", "--rules", "/tmp/r", "--per-folder-preview", "abc"};

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> parser.parse(args));

        assertTrue(exception.getMessage().contains("requires a number"));
    }

    @Test
    void testParse_InvalidSortMode() {
        String[] args = {"--source", "/tmp", "--rules", "/tmp/r", "--sort", "invalid"};

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> parser.parse(args));

        assertTrue(exception.getMessage().contains("alpha, ext or size"));
    }

    @Test
    void testParse_JsonFlag_AndCollision() {
        String[] args = {"--source", "/tmp", "--rules", "/tmp/r", "--json", "--on-collision", "overwrite"};
        CLIConfig config = parser.parse(args);

        assertTrue(config.isJson());
        assertEquals("overwrite", config.getOnCollision());
    }

    @Test
    void testParse_IncludeExclude_Multiple() {
        String[] args = {"--source", "/tmp", "--rules", "/tmp/r",
                "--include", "**/*.pdf", "--include", "**/*.jpg",
                "--exclude", "**/node_modules/**"};
        CLIConfig config = parser.parse(args);

        assertTrue(config.getIncludes().contains("**/*.pdf"));
        assertTrue(config.getIncludes().contains("**/*.jpg"));
        assertTrue(config.getExcludes().contains("**/node_modules/**"));
    }
}
