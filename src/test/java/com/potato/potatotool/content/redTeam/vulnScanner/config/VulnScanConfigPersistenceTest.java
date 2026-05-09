package com.potato.potatotool.content.redTeam.vulnScanner.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VulnScan 配置持久化测试")
class VulnScanConfigPersistenceTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    @DisplayName("保存单个变量字典时应保留 customDicts 对象")
    void shouldPreserveCustomDictsObjectWhenSavingSingleDictionary(@TempDir Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("config.json");
        Path userDict = tempHome.resolve("users.txt");
        Files.write(userDict, "admin\nroot\n".getBytes(StandardCharsets.UTF_8));

        String json = "{\n" +
                "  \"VulnScan\": {\n" +
                "    \"variables\": {\n" +
                "      \"customDicts\": {\n" +
                "        \"tenant\": \"/tmp/tenant.txt\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;

        boolean saved = VulnScanConfig.getInstance().saveUserDictPath(userDict.toString());
        assertTrue(saved);

        JsonObject rootConfig = JsonParser.parseString(
                new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8)
        ).getAsJsonObject();
        JsonObject variables = rootConfig
                .getAsJsonObject(ConfigConstants.VULNSCAN)
                .getAsJsonObject(ConfigConstants.VULNSCAN_VARIABLES);

        assertEquals(userDict.toString(), variables.get(ConfigConstants.VULNSCAN_VAR_USER_DICT).getAsString());
        assertEquals(
                "/tmp/tenant.txt",
                variables.getAsJsonObject(ConfigConstants.VULNSCAN_VAR_CUSTOM_DICTS).get("tenant").getAsString()
        );
    }
}
