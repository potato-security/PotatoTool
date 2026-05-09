package com.potato.potatotool.utils.browser;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BrowserRuntimeConfigTest {

    @Test
    public void applyBrowserSettingsShouldWriteEnvPathAndClearLegacyHeadlessPath() {
        JsonObject rootConfig = new JsonObject();
        JsonObject vulnScan = new JsonObject();
        JsonObject headless = new JsonObject();
        headless.addProperty("browserPath", "/legacy/chrome");
        vulnScan.add("headless", headless);
        rootConfig.add(ConfigConstants.VULNSCAN, vulnScan);

        BrowserRuntimeConfig.applyBrowserSettings(rootConfig, "  /Applications/Google Chrome.app/Contents/MacOS/Google Chrome  ");

        JsonObject envPath = rootConfig.getAsJsonObject(ConfigConstants.ENV_PATH);
        assertNotNull(envPath);
        assertEquals("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                envPath.get(ConfigConstants.ENV_PATH_BROWSER).getAsString());
        assertEquals("", envPath.get(ConfigConstants.ENV_PATH_PYTHON).getAsString());
        assertFalse(rootConfig.has(ConfigConstants.VULNSCAN));
    }
}
