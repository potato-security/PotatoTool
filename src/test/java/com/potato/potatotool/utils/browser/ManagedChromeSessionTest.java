package com.potato.potatotool.utils.browser;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManagedChromeSessionTest {

    @SuppressWarnings("unchecked")
    private List<String> buildCommand(boolean directMode, boolean headlessMode) throws Exception {
        Method method = ManagedChromeSession.class.getDeclaredMethod(
                "buildLaunchCommand",
                String.class,
                String.class,
                boolean.class,
                boolean.class,
                int.class,
                java.io.File.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(
                null,
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "https://example.com",
                directMode,
                headlessMode,
                9222,
                new java.io.File("/tmp/potatotool-browser-test"));
    }

    @Test
    public void headlessLaunchShouldAppendHeadlessFlags() throws Exception {
        List<String> command = buildCommand(false, true);

        assertTrue(command.contains("--headless=new"));
        assertTrue(command.contains("--disable-gpu"));
    }

    @Test
    public void visibleLaunchShouldNotAppendHeadlessFlags() throws Exception {
        List<String> command = buildCommand(true, false);

        assertFalse(command.contains("--headless=new"));
        assertFalse(command.contains("--disable-gpu"));
        assertTrue(command.contains("--no-proxy-server"));
    }
}
