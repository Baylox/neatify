package io.neatify.cli.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentParserNewFlagsTest {

    private final ArgumentParser parser = new ArgumentParser();

    @Test
    void testUseDefaultRules_RemovesRulesRequirement() {
        String[] args = {"--source", "/tmp/source", "--use-default-rules"};
        CLIConfig config = parser.parse(args);
        assertTrue(config.isUseDefaultRules());
        assertNotNull(config.getSourceDir());
        assertNull(config.getRulesFile());
    }

    @Test
    void testUndoListFlag() {
        String[] args = {"--undo", "--undo-list", "--source", "/tmp"};
        CLIConfig config = parser.parse(args);
        assertTrue(config.isUndo());
        assertTrue(config.isUndoList());
    }

    @Test
    void testUndoRunFlag() {
        String[] args = {"--undo", "--undo-run", "1719930000000", "--source", "/tmp"};
        CLIConfig config = parser.parse(args);
        assertTrue(config.isUndo());
        assertEquals("1719930000000", config.getUndoRun());
    }
}
