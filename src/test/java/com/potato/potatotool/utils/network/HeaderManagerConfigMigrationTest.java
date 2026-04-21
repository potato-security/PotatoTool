package com.potato.potatotool.utils.network;

import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Global HTTP Headers 配置迁移测试")
class HeaderManagerConfigMigrationTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    @DisplayName("RequestObj 和 HeaderManager 只读取顶层 HttpHeaders 配置")
    void shouldReadOnlyTopLevelHttpHeadersConfig(@TempDir Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("config.json");

        String json = "{\n" +
                "  \"HttpHeaders\": {\n" +
                "    \"globalHeaders\": {\n" +
                "      \"User-Agent\": \"TopLevel-UA\",\n" +
                "      \"X-Top-Level\": \"enabled\"\n" +
                "    },\n" +
                "    \"templates\": {\n" +
                "      \"Probe\": {\n" +
                "        \"X-Template\": \"probe\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"Proxy\": {\n" +
                "    \"enable\": false,\n" +
                "    \"address\": \"\",\n" +
                "    \"services\": {}\n" +
                "  },\n" +
                "  \"VulnScan\": {\n" +
                "    \"customHeaders\": {\n" +
                "      \"User-Agent\": \"Legacy-UA\",\n" +
                "      \"X-Legacy\": \"legacy\"\n" +
                "    },\n" +
                "    \"headerTemplates\": {\n" +
                "      \"Legacy\": {\n" +
                "        \"X-Template\": \"legacy\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;

        HeaderManager headerManager = HeaderManager.getInstance();
        Map<String, String> customHeaders = headerManager.getCustomHeaders();

        assertEquals("TopLevel-UA", customHeaders.get("User-Agent"));
        assertEquals("enabled", customHeaders.get("X-Top-Level"));
        assertFalse(customHeaders.containsKey("X-Legacy"));

        assertTrue(headerManager.getTemplateNames().contains("Probe"));
        assertFalse(headerManager.getTemplateNames().contains("Legacy"));
        assertEquals("probe", headerManager.getTemplate("Probe").get("X-Template"));

        RequestObj requestObj = new RequestObj();
        assertEquals("TopLevel-UA", requestObj.getHeaders().get("User-Agent"));
        assertEquals("enabled", requestObj.getHeaders().get("X-Top-Level"));
        assertFalse(requestObj.getHeaders().containsKey("X-Legacy"));
    }
}
