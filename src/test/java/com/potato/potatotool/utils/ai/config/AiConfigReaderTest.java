package com.potato.potatotool.utils.ai.config;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiConfigReader 测试")
class AiConfigReaderTest {

    @Test
    @DisplayName("相对 base_url 会在读取配置时被直接拦截")
    void rejectRelativeBaseUrl() {
        AiConfigReader reader = new TestableAiConfigReader();
        JsonObject ai = baseConfig();
        ai.addProperty(ConfigConstants.AI_BASE_URL, "/v1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ((TestableAiConfigReader) reader).readForTest(ai, false)
        );

        assertTrue(exception.getMessage().contains("完整的 HTTP/HTTPS 地址"));
    }

    @Test
    @DisplayName("完整的 absolute base_url 仍可正常读取")
    void acceptAbsoluteBaseUrl() {
        AiConfigReader reader = new TestableAiConfigReader();
        JsonObject ai = baseConfig();
        ai.addProperty(ConfigConstants.AI_BASE_URL, "https://api.deepseek.com");

        AiRuntimeConfig config = ((TestableAiConfigReader) reader).readForTest(ai, false);

        assertEquals("https://api.deepseek.com", config.getBaseUrl());
    }

    private JsonObject baseConfig() {
        JsonObject ai = new JsonObject();
        ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
        ai.addProperty(ConfigConstants.AI_API_KEY, "test-key");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "deepseek-chat");
        return ai;
    }

    private static class TestableAiConfigReader extends AiConfigReader {
        private AiRuntimeConfig readForTest(JsonObject aiConfig, boolean useProxy) {
            return read(aiConfig, useProxy);
        }
    }
}
