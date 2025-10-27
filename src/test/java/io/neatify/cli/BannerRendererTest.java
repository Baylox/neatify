package io.neatify.cli;

import io.neatify.cli.ui.BannerRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests essentiels pour BannerRenderer.
 */
class BannerRendererTest {

    @Test
    void testRenderBanner_ContainsAppInfo() {
        AppInfo appInfo = new AppInfo("TestApp", "1.0.0", "Test description");
        String banner = BannerRenderer.renderBanner(appInfo);

        assertTrue(banner.contains("1.0.0"));
        assertTrue(banner.contains("Test description"));
    }

    @Test
    void testRenderProgressBar_BasicFunctionality() {
        String bar = BannerRenderer.renderProgressBar(50, 100, 20);

        assertTrue(bar.contains("50%"));
        assertTrue(bar.contains("50/100"));
        assertTrue(bar.contains("["));
        assertTrue(bar.contains("]"));
    }

    @Test
    void testRenderProgressBar_HandlesZeroTotal() {
        String bar = BannerRenderer.renderProgressBar(0, 0, 20);

        assertEquals("", bar);
    }
}
