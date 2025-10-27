package io.neatify.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour AppInfo.
 */
class AppInfoTest {

    @Test
    void testCreateAppInfo_ValidData() {
        AppInfo appInfo = new AppInfo("TestApp", "1.0.0", "A test application");

        assertEquals("TestApp", appInfo.name());
        assertEquals("1.0.0", appInfo.version());
        assertEquals("A test application", appInfo.description());
    }

    @Test
    void testNeatifyFactory_ValidVersion() {
        AppInfo appInfo = AppInfo.neatify("2.5.1");

        assertEquals("NEATIFY", appInfo.name());
        assertEquals("2.5.1", appInfo.version());
        assertEquals("Outil de rangement automatique", appInfo.description());
    }

    @Test
    void testCreateAppInfo_NullName_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AppInfo(null, "1.0.0", "Description")
        );
        assertTrue(exception.getMessage().contains("nom"));
    }

    @Test
    void testCreateAppInfo_BlankName_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AppInfo("   ", "1.0.0", "Description")
        );
        assertTrue(exception.getMessage().contains("nom"));
    }

    @Test
    void testCreateAppInfo_NullVersion_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AppInfo("TestApp", null, "Description")
        );
        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void testCreateAppInfo_BlankVersion_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AppInfo("TestApp", "", "Description")
        );
        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void testCreateAppInfo_NullDescription_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AppInfo("TestApp", "1.0.0", null)
        );
        assertTrue(exception.getMessage().contains("description"));
    }

    @Test
    void testCreateAppInfo_BlankDescription_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AppInfo("TestApp", "1.0.0", "  ")
        );
        assertTrue(exception.getMessage().contains("description"));
    }

    @Test
    void testAppInfo_RecordEquality() {
        AppInfo appInfo1 = new AppInfo("App", "1.0", "Desc");
        AppInfo appInfo2 = new AppInfo("App", "1.0", "Desc");
        AppInfo appInfo3 = new AppInfo("App", "2.0", "Desc");

        assertEquals(appInfo1, appInfo2);
        assertNotEquals(appInfo1, appInfo3);
    }

    @Test
    void testAppInfo_ToString() {
        AppInfo appInfo = new AppInfo("MyApp", "1.2.3", "My description");
        String toString = appInfo.toString();

        assertTrue(toString.contains("MyApp"));
        assertTrue(toString.contains("1.2.3"));
        assertTrue(toString.contains("My description"));
    }
}
