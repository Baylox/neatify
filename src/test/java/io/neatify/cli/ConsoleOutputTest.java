package io.neatify.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ConsoleOutput.
 * Utilise StringWriter pour capturer la sortie sans System.out.
 */
class ConsoleOutputTest {

    private StringWriter outWriter;
    private StringWriter errWriter;
    private ConsoleOutput output;

    @BeforeEach
    void setUp() {
        outWriter = new StringWriter();
        errWriter = new StringWriter();
        output = new ConsoleOutput(
            new PrintWriter(outWriter, true),
            new PrintWriter(errWriter, true)
        );
    }

    @Test
    void testPrintln_WritesToOutput() {
        output.println("Test message");

        assertEquals("Test message" + System.lineSeparator(), outWriter.toString());
        assertEquals("", errWriter.toString());
    }

    @Test
    void testPrint_WritesToOutputWithoutNewline() {
        output.print("Test");

        assertEquals("Test", outWriter.toString());
        assertEquals("", errWriter.toString());
    }

    @Test
    void testPrintlnEmpty_WritesNewline() {
        output.println();

        assertEquals(System.lineSeparator(), outWriter.toString());
    }

    @Test
    void testPrintlnError_WritesToErrorOutput() {
        output.printlnError("Error message");

        assertEquals("", outWriter.toString());
        assertEquals("Error message" + System.lineSeparator(), errWriter.toString());
    }

    @Test
    void testPrintBanner_ContainsAppInfo() {
        AppInfo appInfo = new AppInfo("TestApp", "1.0.0", "Description");
        output.printBanner(appInfo);

        String out = outWriter.toString();
        assertTrue(out.contains("TestApp"));
        assertTrue(out.contains("1.0.0"));
        assertTrue(out.contains("Description"));
    }

    @Test
    void testPrintSection_ContainsTitleAndLines() {
        output.printSection("TEST SECTION");

        String out = outWriter.toString();
        assertTrue(out.contains("TEST SECTION"));
        assertTrue(out.contains("="));
    }

    @Test
    void testPrintLine_ContainsSeparator() {
        output.printLine();

        String out = outWriter.toString();
        assertTrue(out.contains("="));
        assertTrue(out.endsWith(System.lineSeparator()));
        // La ligne fait 48 caractères + le séparateur de ligne (plateforme dépendante)
        assertEquals(48 + System.lineSeparator().length(), out.length());
    }

    @Test
    void testPrintSuccess_ContainsCheckmark() {
        output.printSuccess("Success message");

        String out = outWriter.toString();
        assertTrue(out.contains("✓"));
        assertTrue(out.contains("Success message"));
    }

    @Test
    void testPrintInfo_ContainsInfoIcon() {
        output.printInfo("Info message");

        String out = outWriter.toString();
        assertTrue(out.contains("[i]"));
        assertTrue(out.contains("Info message"));
    }

    @Test
    void testPrintWarning_ContainsWarningIcon() {
        output.printWarning("Warning message");

        String out = outWriter.toString();
        assertTrue(out.contains("[!]"));
        assertTrue(out.contains("Warning message"));
    }

    @Test
    void testPrintError_WritesToErrorStream() {
        output.printError("Error message");

        assertEquals("", outWriter.toString());
        String err = errWriter.toString();
        assertTrue(err.contains("✗"));
        assertTrue(err.contains("Error message"));
    }

    @Test
    void testMultiplePrints_AppendCorrectly() {
        output.print("Hello");
        output.print(" ");
        output.print("World");

        assertEquals("Hello World", outWriter.toString());
    }

    @Test
    void testFlush_DoesNotThrow() {
        output.println("Test");
        assertDoesNotThrow(() -> output.flush());
    }

    @Test
    void testSystem_ReturnsNonNullInstance() {
        ConsoleOutput systemOutput = ConsoleOutput.system();

        assertNotNull(systemOutput);
    }

    @Test
    void testSystem_CanPrintWithoutException() {
        ConsoleOutput systemOutput = ConsoleOutput.system();

        assertDoesNotThrow(() -> systemOutput.println("Test"));
        assertDoesNotThrow(() -> systemOutput.printError("Error"));
    }

    @Test
    void testPrintOperations_DoNotInterfere() {
        output.println("Standard output");
        output.printlnError("Error output");

        assertTrue(outWriter.toString().contains("Standard output"));
        assertTrue(errWriter.toString().contains("Error output"));
        assertFalse(outWriter.toString().contains("Error output"));
        assertFalse(errWriter.toString().contains("Standard output"));
    }
}
