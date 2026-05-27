package com.potato.potatotool;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ToStartTest {
    @Test
    public void startupPage_shouldExposePortScanDirectLaunchMapping() {
        ToStart.StartupPage page = ToStart.StartupPage.RED_PORT_SCAN;

        assertFalse(page.isBlueMode());
        assertEquals(11, page.getNavIndex());
        assertEquals("port", page.getAlias());
    }

    @Test
    public void parseStartupPage_shouldResolvePortAliasAndEnumName() throws Exception {
        Method method = ToStart.class.getDeclaredMethod("parseStartupPage", String.class);
        method.setAccessible(true);

        assertSame(ToStart.StartupPage.RED_PORT_SCAN, method.invoke(null, "port"));
        assertSame(ToStart.StartupPage.RED_PORT_SCAN, method.invoke(null, "RED_PORT_SCAN"));
    }
}
