package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.utils.browser.BrowserRuntimeResolver;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeadlessCompatibilityTest {

    @Test
    public void testBuildCompatibilityErrorMessageContainsSettingHint() {
        HeadlessHandler.HeadlessCompatibilityResult result = new HeadlessHandler.HeadlessCompatibilityResult();
        result.setStatus(HeadlessHandler.CompatibilityStatus.NOT_FOUND);
        result.setMessage("browser not found");

        String message = HeadlessHandler.buildCompatibilityErrorMessage(result);

        assertNotNull(message);
        assertTrue(message.contains("设置入口"));
        assertTrue(message.contains("浏览器运行时"));
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
    public void testParseMajorVersion() {
        Integer major = BrowserRuntimeResolver.parseMajorVersion("Google Chrome 134.0.6998.89");
        Integer invalid = BrowserRuntimeResolver.parseMajorVersion("unknown-version");
        Integer empty = BrowserRuntimeResolver.parseMajorVersion("");

        assertEquals(Integer.valueOf(134), major);
        assertNull(invalid);
        assertNull(empty);
    }

    @Test
    public void testSupportPolicySummaryShouldDescribeDriverlessCdpRuntime() {
        String summary = HeadlessHandler.getSupportPolicySummary();

        assertTrue(summary.contains("DevTools"));
        assertTrue(summary.contains("无需额外驱动"));
    }

    @Test
    public void testBuildDisplayMessageShouldUseCdpDefaults() {
        HeadlessHandler.HeadlessCompatibilityResult result = new HeadlessHandler.HeadlessCompatibilityResult();
        result.setStatus(HeadlessHandler.CompatibilityStatus.COMPATIBLE);
        result.setBrowserVersion("Google Chrome 135.0.1");
        result.setMessage("浏览器运行时可用");

        String message = result.buildDisplayMessage();

        assertTrue(message.contains("引擎=cdp"));
        assertTrue(message.contains("Chrome/Chromium 浏览器（内置 DevTools 连接）"));
        assertTrue(message.contains("策略:"));
        assertTrue(message.contains("浏览器运行时可用"));
    }

    @Test
    public void testCheckCompatibilityWithoutConfiguredPathShouldReturnNotFound() {
        HeadlessHandler.HeadlessCompatibilityResult result = HeadlessHandler.checkCompatibility((String) null);

        assertNotNull(result);
        assertEquals(HeadlessHandler.CompatibilityStatus.NOT_FOUND, result.getStatus());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("未检测到 Chrome/Chromium 浏览器"));
    }

    @Test
    public void testCheckCompatibilityWithInvalidConfiguredBrowserPathShouldReturnNotFound() throws Exception {
        File tempFile = File.createTempFile("invalid-browser-runtime", ".txt");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("not-a-browser-runtime");
        }

        HeadlessHandler.HeadlessCompatibilityResult compatibility = HeadlessHandler.checkCompatibility(tempFile.getAbsolutePath());
        assertEquals(HeadlessHandler.CompatibilityStatus.NOT_FOUND, compatibility.getStatus());
        assertTrue(compatibility.getMessage().contains("浏览器版本解析失败"));
    }
}
