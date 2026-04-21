package com.potato.potatotool.utils.core;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;

/**
 * Stores external runtime executable paths shared across modules.
 */
public final class EnvPathConfig {
    private EnvPathConfig() {
    }

    public static synchronized void migrateLegacyConfigIfNeeded() {
        JsonObject rootConfig = getRootConfigCopy();
        JsonObject envPathConfig = getOrCreateObject(rootConfig, ConfigConstants.ENV_PATH);
        boolean changed = false;

        if (!envPathConfig.has(ConfigConstants.ENV_PATH_BROWSER)) {
            envPathConfig.addProperty(ConfigConstants.ENV_PATH_BROWSER, "");
            changed = true;
        }
        if (!envPathConfig.has(ConfigConstants.ENV_PATH_PYTHON)) {
            envPathConfig.addProperty(ConfigConstants.ENV_PATH_PYTHON, "");
            changed = true;
        }

        String browserPath = readString(envPathConfig, ConfigConstants.ENV_PATH_BROWSER);
        if (browserPath.isEmpty()) {
            String legacyBrowserPath = readLegacyBrowserPath(rootConfig);
            if (!legacyBrowserPath.isEmpty()) {
                envPathConfig.addProperty(ConfigConstants.ENV_PATH_BROWSER, legacyBrowserPath);
                changed = true;
            }
        }

        String pythonPath = readString(envPathConfig, ConfigConstants.ENV_PATH_PYTHON);
        if (pythonPath.isEmpty()) {
            String legacyPythonPath = readLegacyPythonPath(rootConfig);
            if (!legacyPythonPath.isEmpty()) {
                envPathConfig.addProperty(ConfigConstants.ENV_PATH_PYTHON, legacyPythonPath);
                changed = true;
            }
        }

        if (clearLegacyBrowserPath(rootConfig)) {
            changed = true;
        }
        if (clearLegacyPythonPath(rootConfig)) {
            changed = true;
        }

        if (changed) {
            rootConfig.add(ConfigConstants.ENV_PATH, envPathConfig);
            Constants.saveConfig(rootConfig);
        }
    }

    public static synchronized String getBrowserPath() {
        migrateLegacyConfigIfNeeded();
        return readString(getEnvPathSnapshot(), ConfigConstants.ENV_PATH_BROWSER);
    }

    public static synchronized String getPythonPath() {
        migrateLegacyConfigIfNeeded();
        return readString(getEnvPathSnapshot(), ConfigConstants.ENV_PATH_PYTHON);
    }

    public static synchronized boolean saveBrowserPath(String browserPath) {
        JsonObject rootConfig = getRootConfigCopy();
        applyBrowserPath(rootConfig, browserPath);
        return Constants.saveConfig(rootConfig);
    }

    public static synchronized boolean savePythonPath(String pythonPath) {
        JsonObject rootConfig = getRootConfigCopy();
        applyPythonPath(rootConfig, pythonPath);
        return Constants.saveConfig(rootConfig);
    }

    public static synchronized void applyBrowserPath(JsonObject rootConfig, String browserPath) {
        JsonObject envPathConfig = getOrCreateObject(rootConfig, ConfigConstants.ENV_PATH);
        ensureDefaults(envPathConfig);
        envPathConfig.addProperty(ConfigConstants.ENV_PATH_BROWSER, safeTrim(browserPath));
        rootConfig.add(ConfigConstants.ENV_PATH, envPathConfig);
        clearLegacyBrowserPath(rootConfig);
    }

    public static synchronized void applyPythonPath(JsonObject rootConfig, String pythonPath) {
        JsonObject envPathConfig = getOrCreateObject(rootConfig, ConfigConstants.ENV_PATH);
        ensureDefaults(envPathConfig);
        envPathConfig.addProperty(ConfigConstants.ENV_PATH_PYTHON, safeTrim(pythonPath));
        rootConfig.add(ConfigConstants.ENV_PATH, envPathConfig);
        clearLegacyPythonPath(rootConfig);
    }

    private static void ensureDefaults(JsonObject envPathConfig) {
        if (!envPathConfig.has(ConfigConstants.ENV_PATH_BROWSER)) {
            envPathConfig.addProperty(ConfigConstants.ENV_PATH_BROWSER, "");
        }
        if (!envPathConfig.has(ConfigConstants.ENV_PATH_PYTHON)) {
            envPathConfig.addProperty(ConfigConstants.ENV_PATH_PYTHON, "");
        }
    }

    private static JsonObject getEnvPathSnapshot() {
        JsonObject rootConfig = getRootConfigCopy();
        JsonObject envPathConfig = getObject(rootConfig, ConfigConstants.ENV_PATH);
        return envPathConfig == null ? new JsonObject() : envPathConfig;
    }

    private static String readLegacyBrowserPath(JsonObject rootConfig) {
        JsonObject browserConfig = getObject(rootConfig, ConfigConstants.BROWSER);
        String browserPath = readString(browserConfig, "manualPath");
        if (!browserPath.isEmpty()) {
            return browserPath;
        }
        browserPath = readString(browserConfig, "autoDetectedPath");
        if (!browserPath.isEmpty()) {
            return browserPath;
        }
        JsonObject vulnScanConfig = getObject(rootConfig, ConfigConstants.VULNSCAN);
        JsonObject headlessConfig = getObject(vulnScanConfig, ConfigConstants.VULNSCAN_HEADLESS);
        return readString(headlessConfig, ConfigConstants.VULNSCAN_HEADLESS_BROWSER_PATH);
    }

    private static String readLegacyPythonPath(JsonObject rootConfig) {
        JsonObject vulnScanConfig = getObject(rootConfig, ConfigConstants.VULNSCAN);
        return readString(vulnScanConfig, ConfigConstants.VULNSCAN_PYTHON_PATH);
    }

    private static boolean clearLegacyBrowserPath(JsonObject rootConfig) {
        boolean changed = false;

        JsonObject browserConfig = getObject(rootConfig, ConfigConstants.BROWSER);
        if (browserConfig != null) {
            if (browserConfig.has("manualPath")) {
                browserConfig.remove("manualPath");
                changed = true;
            }
            if (browserConfig.has("autoDetectedPath")) {
                browserConfig.remove("autoDetectedPath");
                changed = true;
            }
            if (browserConfig.has("autoDetectedAt")) {
                browserConfig.remove("autoDetectedAt");
                changed = true;
            }
            rootConfig.add(ConfigConstants.BROWSER, browserConfig);
        }

        JsonObject vulnScanConfig = getObject(rootConfig, ConfigConstants.VULNSCAN);
        JsonObject headlessConfig = getObject(vulnScanConfig, ConfigConstants.VULNSCAN_HEADLESS);
        if (headlessConfig != null && headlessConfig.has(ConfigConstants.VULNSCAN_HEADLESS_BROWSER_PATH)) {
            headlessConfig.remove(ConfigConstants.VULNSCAN_HEADLESS_BROWSER_PATH);
            changed = true;
            if (headlessConfig.entrySet().isEmpty()) {
                vulnScanConfig.remove(ConfigConstants.VULNSCAN_HEADLESS);
            } else {
                vulnScanConfig.add(ConfigConstants.VULNSCAN_HEADLESS, headlessConfig);
            }
            if (vulnScanConfig.entrySet().isEmpty()) {
                rootConfig.remove(ConfigConstants.VULNSCAN);
            } else {
                rootConfig.add(ConfigConstants.VULNSCAN, vulnScanConfig);
            }
        }

        return changed;
    }

    private static boolean clearLegacyPythonPath(JsonObject rootConfig) {
        JsonObject vulnScanConfig = getObject(rootConfig, ConfigConstants.VULNSCAN);
        if (vulnScanConfig == null || !vulnScanConfig.has(ConfigConstants.VULNSCAN_PYTHON_PATH)) {
            return false;
        }
        vulnScanConfig.remove(ConfigConstants.VULNSCAN_PYTHON_PATH);
        if (vulnScanConfig.entrySet().isEmpty()) {
            rootConfig.remove(ConfigConstants.VULNSCAN);
        } else {
            rootConfig.add(ConfigConstants.VULNSCAN, vulnScanConfig);
        }
        return true;
    }

    private static JsonObject getRootConfigCopy() {
        Object configObject = Constants.getOutsideConfig(null);
        if (configObject instanceof JsonObject) {
            return ((JsonObject) configObject).deepCopy();
        }
        return new JsonObject();
    }

    private static JsonObject getObject(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            return null;
        }
        return parent.getAsJsonObject(key);
    }

    private static JsonObject getOrCreateObject(JsonObject parent, String key) {
        JsonObject existing = getObject(parent, key);
        return existing == null ? new JsonObject() : existing;
    }

    private static String readString(JsonObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        try {
            String value = jsonObject.get(key).getAsString();
            return value == null ? "" : value.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
