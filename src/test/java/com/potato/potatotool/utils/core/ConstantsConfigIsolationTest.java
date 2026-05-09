package com.potato.potatotool.utils.core;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Constants 配置读写隔离测试")
class ConstantsConfigIsolationTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    @DisplayName("getOutsideConfig 返回值修改不应污染缓存")
    void shouldReturnDeepCopyFromOutsideConfig(@TempDir Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("config.json");

        String json = "{\n" +
                "  \"Proxy\": {\n" +
                "    \"enable\": false,\n" +
                "    \"address\": \"http://127.0.0.1:7890\",\n" +
                "    \"services\": {\n" +
                "      \"VulnScan\": true\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;

        JsonObject firstRead = (JsonObject) Constants.getOutsideConfig(null);
        assertNotNull(firstRead);
        firstRead.getAsJsonObject(ConfigConstants.PROXY).addProperty(ConfigConstants.PROXY_ENABLE, true);
        firstRead.getAsJsonObject(ConfigConstants.PROXY)
                .getAsJsonObject(ConfigConstants.PROXY_SERVICES)
                .addProperty(ConfigConstants.VULNSCAN_SERVICE, false);

        JsonObject secondRead = (JsonObject) Constants.getOutsideConfig(null);
        assertNotNull(secondRead);
        JsonObject proxyConfig = secondRead.getAsJsonObject(ConfigConstants.PROXY);
        assertEquals(false, proxyConfig.get(ConfigConstants.PROXY_ENABLE).getAsBoolean());
        assertEquals(
                true,
                proxyConfig.getAsJsonObject(ConfigConstants.PROXY_SERVICES)
                        .get(ConfigConstants.VULNSCAN_SERVICE)
                        .getAsBoolean()
        );
    }
}
