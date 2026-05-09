package com.potato.potatotool.controller.publicPane;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.browser.BrowserRuntimeConfig;
import com.potato.potatotool.utils.core.EnvPathConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("PaneSetting 运行时路径配置测试")
class PaneSettingRuntimePathConfigTest {

    @Test
    @DisplayName("浏览器和 Python 路径应写入共享 EnvPath 配置并清理旧键")
    void shouldWriteRuntimePathsToSharedEnvPathConfig() {
        JsonObject rootConfig = new JsonObject();

        JsonObject browserConfig = new JsonObject();
        browserConfig.addProperty("manualPath", "/legacy/browser");
        rootConfig.add("Browser", browserConfig);

        JsonObject vulnScan = new JsonObject();
        vulnScan.addProperty("pythonPath", "/legacy/python");
        rootConfig.add(ConfigConstants.VULNSCAN, vulnScan);

        BrowserRuntimeConfig.applyBrowserSettings(rootConfig, "  /custom/browser  ");
        EnvPathConfig.applyPythonPath(rootConfig, "  /custom/python  ");

        JsonObject envPath = rootConfig.getAsJsonObject(ConfigConstants.ENV_PATH);
        assertNotNull(envPath);
        assertEquals("/custom/browser", envPath.get(ConfigConstants.ENV_PATH_BROWSER).getAsString());
        assertEquals("/custom/python", envPath.get(ConfigConstants.ENV_PATH_PYTHON).getAsString());
        assertFalse(rootConfig.has("Browser"));
        assertFalse(rootConfig.has(ConfigConstants.VULNSCAN));
    }
}
