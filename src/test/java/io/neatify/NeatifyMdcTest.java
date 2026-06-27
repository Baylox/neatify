package io.neatify;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that Neatify.main() always cleans up the MDC "jsonMode" key,
 * even after a normal exit, so subsequent tests running in the same thread
 * are not affected by a stale MDC entry.
 *
 * Regression for: MDC.put("jsonMode","true") was never followed by MDC.remove().
 */
class NeatifyMdcTest {

    @Test
    void main_jsonMode_mdcIsCleanedUpAfterRun(@TempDir Path tempDir) {
        // Pre-condition: MDC should not have the key before the test
        assertNull(MDC.get("jsonMode"), "Pre-condition: jsonMode must be absent before test");

        String[] args = {
            "--source", tempDir.toString(),
            "--use-default-rules",
            "--json"
        };

        Neatify.main(args);

        // Post-condition: MDC key must have been removed by the finally block
        assertNull(MDC.get("jsonMode"),
            "jsonMode MDC key must be removed after main() returns");
    }

    @Test
    void main_noJsonMode_mdcRemainsAbsent(@TempDir Path tempDir) {
        assertNull(MDC.get("jsonMode"));

        // Run without --json (writes to stderr via warning, not JSON)
        String[] args = {
            "--source", tempDir.toString(),
            "--use-default-rules"
        };
        Neatify.main(args);

        assertNull(MDC.get("jsonMode"),
            "jsonMode MDC key must remain absent when --json is not used");
    }
}
