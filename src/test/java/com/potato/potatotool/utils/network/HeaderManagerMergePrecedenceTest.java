package com.potato.potatotool.utils.network;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("HeaderManager 请求头合并优先级测试")
class HeaderManagerMergePrecedenceTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    @DisplayName("应按 global < config < poc 的顺序覆盖请求头")
    void shouldMergeHeadersInExpectedPrecedence(@TempDir Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("config.json");

        String json = "{\n" +
                "  \"" + ConfigConstants.HTTP_HEADERS + "\": {\n" +
                "    \"" + ConfigConstants.HTTP_HEADERS_GLOBAL + "\": {\n" +
                "      \"User-Agent\": \"global-ua\",\n" +
                "      \"X-Global\": \"global-value\",\n" +
                "      \"X-Shared\": \"global-shared\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": false,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {}\n" +
                "  }\n" +
                "}";
        Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;

        Map<String, String> configHeaders = new LinkedHashMap<String, String>();
        configHeaders.put("X-Config", "config-value");
        configHeaders.put("X-Shared", "config-shared");

        Map<String, String> pocHeaders = new LinkedHashMap<String, String>();
        pocHeaders.put("X-Poc", "poc-value");
        pocHeaders.put("X-Shared", "poc-shared");

        Map<String, String> merged = HeaderManager.getInstance().mergeHeaders(pocHeaders, configHeaders);

        assertEquals("global-ua", merged.get("User-Agent"));
        assertEquals("global-value", merged.get("X-Global"));
        assertEquals("config-value", merged.get("X-Config"));
        assertEquals("poc-value", merged.get("X-Poc"));
        assertEquals("poc-shared", merged.get("X-Shared"));
    }
}
