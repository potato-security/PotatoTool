package com.potato.potatotool.utils.core;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;

/**
 * Stores external runtime executable paths shared across modules.
 */
public final class EnvPathConfig {
    private static final String LEGACY_BROWSER_ROOT = "Browser";
    private static final String LEGACY_VULNSCAN_HEADLESS = "headless";
    private static final String LEGACY_VULNSCAN_PYTHON_PATH = "pythonPath";

    private EnvPathConfig() {
    }

    public static synchronized String getBrowserPath() {
        return readString(getEnvPathSnapshot(), ConfigConstants.ENV_PATH_BROWSER);
    }

    public static synchronized String getPythonPath() {
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
        JsonObject envPathConfig = getOrCreateEnvPathObject(rootConfig);
        envPathConfig.addProperty(ConfigConstants.ENV_PATH_BROWSER, safeTrim(browserPath));
        ensureDefaults(envPathConfig);
        rootConfig.add(ConfigConstants.ENV_PATH, envPathConfig);
        removeLegacyRuntimePathKeys(rootConfig);
    }

    public static synchronized void applyPythonPath(JsonObject rootConfig, String pythonPath) {
        JsonObject envPathConfig = getOrCreateEnvPathObject(rootConfig);
        envPathConfig.addProperty(ConfigConstants.ENV_PATH_PYTHON, safeTrim(pythonPath));
        ensureDefaults(envPathConfig);
        rootConfig.add(ConfigConstants.ENV_PATH, envPathConfig);
        removeLegacyRuntimePathKeys(rootConfig);
    }

    private static JsonObject getEnvPathSnapshot() {
        Object configObject = Constants.getOutsideConfig(ConfigConstants.ENV_PATH);
        if (configObject instanceof JsonObject) {
            JsonObject envPathConfig = (JsonObject) configObject;
            ensureDefaults(envPathConfig);
            return envPathConfig;
        }
        JsonObject envPathConfig = new JsonObject();
        ensureDefaults(envPathConfig);
        return envPathConfig;
    }

    private static JsonObject getRootConfigCopy() {
        Object configObject = Constants.getOutsideConfig(null);
        if (configObject instanceof JsonObject) {
            return (JsonObject) configObject;
        }
        return new JsonObject();
    }

    private static JsonObject getOrCreateEnvPathObject(JsonObject rootConfig) {
        if (rootConfig != null
                && rootConfig.has(ConfigConstants.ENV_PATH)
                && rootConfig.get(ConfigConstants.ENV_PATH).isJsonObject()) {
            return rootConfig.getAsJsonObject(ConfigConstants.ENV_PATH);
        }
        return new JsonObject();
    }

    private static void ensureDefaults(JsonObject envPathConfig) {
        if (!envPathConfig.has(ConfigConstants.ENV_PATH_BROWSER)) {
            envPathConfig.addProperty(ConfigConstants.ENV_PATH_BROWSER, "");
        }
        if (!envPathConfig.has(ConfigConstants.ENV_PATH_PYTHON)) {
            envPathConfig.addProperty(ConfigConstants.ENV_PATH_PYTHON, "");
        }
    }

    private static void removeLegacyRuntimePathKeys(JsonObject rootConfig) {
        if (rootConfig == null) {
            return;
        }

        rootConfig.remove(LEGACY_BROWSER_ROOT);

        if (!rootConfig.has(ConfigConstants.VULNSCAN)
                || !rootConfig.get(ConfigConstants.VULNSCAN).isJsonObject()) {
            return;
        }

        JsonObject vulnScanConfig = rootConfig.getAsJsonObject(ConfigConstants.VULNSCAN);
        vulnScanConfig.remove(LEGACY_VULNSCAN_HEADLESS);
        vulnScanConfig.remove(LEGACY_VULNSCAN_PYTHON_PATH);

        if (vulnScanConfig.entrySet().isEmpty()) {
            rootConfig.remove(ConfigConstants.VULNSCAN);
            return;
        }
        rootConfig.add(ConfigConstants.VULNSCAN, vulnScanConfig);
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
