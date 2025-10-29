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
    void testUndoListAndUndoRunParsing() {
        String[] a1 = {"--undo", "--undo-list", "--source", "/tmp"};
        CLIConfig c1 = parser.parse(a1);
        assertTrue(c1.isUndo());
        assertTrue(c1.isUndoList());

        String[] a2 = {"--undo", "--undo-run", "1719930000000", "--source", "/tmp"};
        CLIConfig c2 = parser.parse(a2);
        assertTrue(c2.isUndo());
        assertEquals("1719930000000", c2.getUndoRun());
    }
}
