package com.potato.potatotool.utils.browser;

import com.potato.potatotool.utils.core.EnvPathConfig;

/**
 * Shared browser runtime configuration for all Chromium-dependent features.
 */
public final class BrowserRuntimeConfig {
    private static final int DEFAULT_EXISTING_SESSION_PORT = 9223;
    private static final boolean DEFAULT_REUSE_EXISTING_SESSION = true;

    private BrowserRuntimeConfig() {
    }

    public static synchronized void initializeAtStartup() {
        preloadResolvedBrowserPath();
    }

    public static synchronized String getBrowserPath() {
        return resolveBrowserPath();
    }

    public static synchronized boolean isReuseExistingSessionEnabled() {
        return DEFAULT_REUSE_EXISTING_SESSION;
    }

    public static synchronized int getExistingSessionPort() {
        return DEFAULT_EXISTING_SESSION_PORT;
    }

    public static synchronized String resolveBrowserPath() {
        String configuredPath = BrowserRuntimeResolver.resolveConfiguredBrowserPath(EnvPathConfig.getBrowserPath());
        if (configuredPath != null) {
            return configuredPath;
        }

        String detectedPath = BrowserRuntimeResolver.autoDetectBrowserPath();
        if (detectedPath == null || detectedPath.trim().isEmpty()) {
            return null;
        }

        EnvPathConfig.saveBrowserPath(detectedPath);
        return detectedPath;
    }

    public static synchronized void preloadResolvedBrowserPath() {
        String configuredPath = BrowserRuntimeResolver.resolveConfiguredBrowserPath(EnvPathConfig.getBrowserPath());
        if (configuredPath != null) {
            return;
        }
        String detectedPath = BrowserRuntimeResolver.autoDetectBrowserPath();
        if (detectedPath != null && !detectedPath.trim().isEmpty()) {
            EnvPathConfig.saveBrowserPath(detectedPath);
        }
    }

    public static synchronized void applyBrowserSettings(com.google.gson.JsonObject rootConfig,
                                                         String browserPath) {
        EnvPathConfig.applyBrowserPath(rootConfig, browserPath);
    }
}
