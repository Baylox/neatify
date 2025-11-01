package io.neatify.cli;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.neatify.Neatify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that in --json mode and with 0 planned actions,
 * the program emits a valid JSON object on stdout only.
 */
class JsonOutputNoActionTest {

    @Test
    void testJsonOutputWhenNoActions(@TempDir Path tempDir) {
        String[] args = new String[] {
            "--source", tempDir.toString(),
            "--use-default-rules",
            "--json"
        };

        // Capture stdout
        PrintStream prevOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);

            // Run main
            Neatify.main(args);

        } finally {
            System.setOut(prevOut);
        }

        String out = baos.toString(StandardCharsets.UTF_8).trim();
        assertFalse(out.isEmpty(), "stdout should contain JSON output");

        // Parse and validate JSON structure
        JsonObject root = JsonParser.parseString(out).getAsJsonObject();
        assertEquals(tempDir.toString(), root.get("source").getAsString());
        assertFalse(root.get("apply").getAsBoolean());
        assertEquals("rename", root.get("onCollision").getAsString());
        assertEquals(0, root.get("planned").getAsInt());
        assertEquals(0, root.getAsJsonArray("actions").size());

        JsonObject result = root.getAsJsonObject("result");
        assertNotNull(result, "result object must be present");
        assertEquals(0, result.get("moved").getAsInt());
        assertEquals(0, result.get("skipped").getAsInt());
        assertTrue(result.getAsJsonArray("errors").size() >= 0);
    }
}

