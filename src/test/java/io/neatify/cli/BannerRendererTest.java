package io.neatify.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour BannerRenderer.
 * Classe pure (sans effets de bord) donc facilement testable.
 */
class BannerRendererTest {

    @Test
    void testRenderBanner_ContainsAppInfo() {
        AppInfo appInfo = new AppInfo("TestApp", "1.0.0", "Test description");
        String banner = BannerRenderer.renderBanner(appInfo);

        assertNotNull(banner);
        assertTrue(banner.contains("TestApp"));
        assertTrue(banner.contains("1.0.0"));
        assertTrue(banner.contains("Test description"));
        assertTrue(banner.contains("╔"));
        assertTrue(banner.contains("╚"));
    }

    @Test
    void testRenderBanner_StartsAndEndsWithNewline() {
        AppInfo appInfo = new AppInfo("App", "1.0", "Desc");
        String banner = BannerRenderer.renderBanner(appInfo);

        assertTrue(banner.startsWith("\n"));
        assertTrue(banner.endsWith("\n"));
    }

    @Test
    void testRenderLine_ReturnsCorrectLength() {
        String line = BannerRenderer.renderLine();

        assertNotNull(line);
        assertEquals(48, line.length());
        assertTrue(line.matches("=+"));
    }

    @Test
    void testRenderSection_ContainsTitleAndLines() {
        String section = BannerRenderer.renderSection("TEST SECTION");

        assertNotNull(section);
        assertTrue(section.contains("TEST SECTION"));
        assertTrue(section.contains("="));
        assertTrue(section.startsWith("\n"));
    }

    @Test
    void testRenderSuccess_HasCheckmark() {
        String message = BannerRenderer.renderSuccess("Operation completed");

        assertNotNull(message);
        assertTrue(message.contains("✓"));
        assertTrue(message.contains("Operation completed"));
        assertEquals("[✓] Operation completed", message);
    }

    @Test
    void testRenderInfo_HasInfoIcon() {
        String message = BannerRenderer.renderInfo("Information message");

        assertNotNull(message);
        assertTrue(message.contains("i"));
        assertTrue(message.contains("Information message"));
        assertEquals("[i] Information message", message);
    }

    @Test
    void testRenderWarning_HasWarningIcon() {
        String message = BannerRenderer.renderWarning("Warning message");

        assertNotNull(message);
        assertTrue(message.contains("!"));
        assertTrue(message.contains("Warning message"));
        assertEquals("[!] Warning message", message);
    }

    @Test
    void testRenderError_HasErrorIcon() {
        String message = BannerRenderer.renderError("Error message");

        assertNotNull(message);
        assertTrue(message.contains("✗"));
        assertTrue(message.contains("Error message"));
        assertEquals("[✗] Error message", message);
    }

    @Test
    void testRenderPrompt_WithoutDefault() {
        String prompt = BannerRenderer.renderPrompt("Enter name", null);

        assertNotNull(prompt);
        assertEquals("Enter name: ", prompt);
        assertFalse(prompt.contains("["));
    }

    @Test
    void testRenderPrompt_WithDefault() {
        String prompt = BannerRenderer.renderPrompt("Enter name", "default");

        assertNotNull(prompt);
        assertEquals("Enter name [default]: ", prompt);
        assertTrue(prompt.contains("[default]"));
    }

    @Test
    void testRenderPrompt_WithEmptyDefault() {
        String prompt = BannerRenderer.renderPrompt("Enter name", "");

        assertNotNull(prompt);
        assertEquals("Enter name: ", prompt);
        assertFalse(prompt.contains("["));
    }

    @Test
    void testRenderWaitForEnter() {
        String message = BannerRenderer.renderWaitForEnter();

        assertNotNull(message);
        assertTrue(message.contains("Entrée"));
        assertTrue(message.startsWith("\n"));
    }

    @Test
    void testRenderBanner_IsDeterministic() {
        // Test que la fonction est pure (même entrée → même sortie)
        AppInfo appInfo = new AppInfo("App", "1.0", "Desc");
        String banner1 = BannerRenderer.renderBanner(appInfo);
        String banner2 = BannerRenderer.renderBanner(appInfo);

        assertEquals(banner1, banner2);
    }

    @Test
    void testRenderSuccess_HandlesEmptyString() {
        String message = BannerRenderer.renderSuccess("");

        assertEquals("[✓] ", message);
    }

    @Test
    void testRenderSuccess_HandlesSpecialCharacters() {
        String message = BannerRenderer.renderSuccess("Test with émojis 🎉 & symbols!");

        assertTrue(message.contains("émojis 🎉 & symbols!"));
    }
}
