package io.neatify.docs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Keeps the generated documentation tables in sync with the code.
 *
 * <p>Each target file contains a block delimited by
 * {@code <!-- AUTOGEN:<id> START -->} / {@code <!-- AUTOGEN:<id> END -->}. This
 * test regenerates the block content from the code's sources of truth
 * ({@link io.neatify.cli.args.CliOption}, the default rules) and fails if the
 * committed file differs — so docs cannot drift. Run with
 * {@code -Dneatify.docs.update=true} to rewrite the blocks on purpose.
 */
class DocsGenerationTest {

    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final boolean UPDATE = Boolean.getBoolean("neatify.docs.update");

    @Test
    void cliReferenceTableIsUpToDate() throws IOException {
        check("docs/reference/cli.md", "flags", DocsTables.cliOptionsTable());
    }

    @Test
    void defaultRulesTableIsUpToDate() throws IOException {
        check("docs/guides/rules.md", "rules", DocsTables.defaultRulesTable());
    }

    private static void check(String relativePath, String blockId, String generated) throws IOException {
        Path file = ROOT.resolve(relativePath);
        assertTrue(Files.isRegularFile(file),
            "Documentation target is missing: " + file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        String startMarker = "<!-- AUTOGEN:" + blockId + " START -->";
        String endMarker = "<!-- AUTOGEN:" + blockId + " END -->";

        Pattern block = Pattern.compile(
            Pattern.quote(startMarker) + "(.*?)" + Pattern.quote(endMarker),
            Pattern.DOTALL);
        Matcher m = block.matcher(content);
        assertTrue(m.find(),
            "Missing AUTOGEN markers (" + blockId + ") in " + relativePath);

        String desired = startMarker + "\n\n" + generated.stripTrailing() + "\n\n" + endMarker;
        String actualBlock = content.substring(m.start(), m.end());

        if (actualBlock.equals(desired)) {
            return; // up to date
        }

        if (UPDATE) {
            String updated = content.substring(0, m.start()) + desired + content.substring(m.end());
            Files.writeString(file, updated, StandardCharsets.UTF_8);
            return;
        }

        fail("Generated block '" + blockId + "' in " + relativePath
            + " is out of date. Re-run with -Dneatify.docs.update=true to refresh.\n"
            + "---- expected ----\n" + desired + "\n---- found ----\n" + actualBlock);
    }
}
