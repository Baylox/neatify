package io.neatify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the invariants of the cross-platform launch scripts (neatify,
 * neatify.cmd) and their .gitattributes rules. These files are plain
 * text/binary assets that no other test exercises, yet a wrong line ending
 * silently breaks them (a CR in the POSIX script makes the shell fail with
 * "bad interpreter" on Linux/macOS).
 *
 * <p>The tests read the files straight from the project root so they run
 * inside {@code mvnw verify}. Surefire runs with the module directory as the
 * working directory, so {@code user.dir} points at the repo root.
 */
class LauncherScriptsTest {

    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String name) throws IOException {
        Path p = ROOT.resolve(name);
        assertTrue(Files.isRegularFile(p), name + " must exist at the project root: " + p);
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    void posixLauncher_usesLfOnly_noCarriageReturn() throws IOException {
        // A single CR would break the shebang/interpreter on *nix.
        String script = read("neatify");
        assertFalse(script.contains("\r"),
            "neatify (POSIX) must use LF line endings only — found a carriage return");
    }

    @Test
    void posixLauncher_startsWithShebang() throws IOException {
        assertTrue(read("neatify").startsWith("#!/usr/bin/env sh"),
            "neatify must start with the portable shebang #!/usr/bin/env sh");
    }

    @Test
    void posixLauncher_runsTheBuiltJar() throws IOException {
        // Core behavioural invariant: it must exec the shaded jar.
        assertTrue(read("neatify").contains("target/neatify.jar"),
            "neatify must launch target/neatify.jar");
    }

    @Test
    void windowsLauncher_usesCrlf() throws IOException {
        // Batch files want CRLF; assert every line break is a CRLF (no lone LF).
        String script = read("neatify.cmd");
        assertTrue(script.contains("\r\n"), "neatify.cmd must use CRLF line endings");
        assertFalse(script.replace("\r\n", "").contains("\n"),
            "neatify.cmd must not contain any lone LF — every newline must be CRLF");
    }

    @Test
    void windowsLauncher_runsTheBuiltJar() throws IOException {
        assertTrue(read("neatify.cmd").contains("target\\neatify.jar"),
            "neatify.cmd must launch target\\neatify.jar");
    }

    @Test
    void gitattributes_pinsScriptLineEndings() throws IOException {
        String attrs = read(".gitattributes");
        assertTrue(attrs.contains("neatify text eol=lf"),
            ".gitattributes must pin the POSIX launcher to LF");
        assertTrue(attrs.contains("*.cmd") && attrs.contains("eol=crlf"),
            ".gitattributes must pin .cmd files to CRLF");
    }

    // Note: the executable bit (git mode 100755) is an invariant too, but it
    // lives in the git index, not in file content — checking the filesystem bit
    // is unreliable depending on the checkout (e.g. network shares drop it).
    // It is verified in CI instead, via `git ls-files --stage neatify`.
}
