package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class HeadlessCompatibilityTest {

    @Test
    public void testBuildCompatibilityErrorMessageContainsSettingHint() {
        HeadlessHandler.HeadlessCompatibilityResult result = new HeadlessHandler.HeadlessCompatibilityResult();
        result.setStatus(HeadlessHandler.CompatibilityStatus.NOT_FOUND);
        result.setMessage("browser not found");

        String message = HeadlessHandler.buildCompatibilityErrorMessage(result);

        assertNotNull(message);
        assertTrue(message.contains("设置入口"));
        assertTrue(message.contains("Headless 浏览器路径"));
    }

    @Test
    public void testCheckCompatibilityWithNonExecutableFileShouldReturnNotFound() throws Exception {
        File tempFile = File.createTempFile("headless-browser", ".tmp");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("not a browser");
        }

        HeadlessHandler.HeadlessCompatibilityResult result = HeadlessHandler.checkCompatibility(tempFile.getAbsolutePath());

        assertNotNull(result);
        assertEquals(HeadlessHandler.CompatibilityStatus.NOT_FOUND, result.getStatus());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("浏览器版本解析失败"));
    }

    @Test
    public void testParseMajorVersionByReflection() throws Exception {
        Method method = HeadlessHandler.class.getDeclaredMethod("parseMajorVersion", String.class);
        method.setAccessible(true);

        Integer major = (Integer) method.invoke(null, "Google Chrome 134.0.6998.89");
        Integer invalid = (Integer) method.invoke(null, "unknown-version");
        Integer empty = (Integer) method.invoke(null, "");

        assertEquals(Integer.valueOf(134), major);
        assertNull(invalid);
        assertNull(empty);
    }
}
