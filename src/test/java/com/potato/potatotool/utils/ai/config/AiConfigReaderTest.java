package com.potato.potatotool.utils.ai.config;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.crypto.SecurityInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiConfigReader 测试")
class AiConfigReaderTest {

    @BeforeAll
    static void initializeCryptoProvider() {
        SecurityInitializer.initializeSecurityProvider();
    }

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

    @Test
    @DisplayName("thinking budget 缺省值为统一默认值")
    void readThinkingBudgetUsesDefaultWhenMissing() {
        AiConfigReader reader = new TestableAiConfigReader();
        JsonObject ai = baseConfig();
        ai.addProperty(ConfigConstants.AI_BASE_URL, "https://api.deepseek.com");

        AiRuntimeConfig config = ((TestableAiConfigReader) reader).readForTest(ai, false);

        assertEquals(AiThinkingConfig.DEFAULT_BUDGET_TOKENS, config.getThinkingConfig().getBudgetTokens());
    }

    @Test
    @DisplayName("显式启用内置 AI 网关时忽略明文配置")
    void preferBuiltinGatewayWhenExplicitlyEnabled() {
        AiConfigReader reader = new TestableAiConfigReader();
        JsonObject ai = baseConfig();
        ai.addProperty(ConfigConstants.AI_USE_BUILTIN_GATEWAY, true);
        ai.addProperty(ConfigConstants.AI_BASE_URL, "https://custom.example.com/v1");
        ai.addProperty(ConfigConstants.AI_API_KEY, "custom-key");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "custom-model");
        ai.addProperty(ConfigConstants.AI_LOCAL_BASE_URL, encryptLocalConfig("https://builtin.example.com/v1"));
        ai.addProperty(ConfigConstants.AI_LOCAL_API_KEY, encryptLocalConfig("builtin-key"));
        ai.addProperty(ConfigConstants.AI_LOCAL_MODEL_NAME, encryptLocalConfig("builtin-model"));

        AiRuntimeConfig config = ((TestableAiConfigReader) reader).readForTest(ai, false);

        assertTrue(config.isBuiltinAi());
        assertEquals("https://builtin.example.com/v1", config.getBaseUrl());
        assertEquals("builtin-key", config.getApiKey());
        assertEquals("builtin-model", config.getModelName());
        assertEquals("OPENAI", config.getProviderType().name());
    }

    private JsonObject baseConfig() {
        JsonObject ai = new JsonObject();
        ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI");
        ai.addProperty(ConfigConstants.AI_API_KEY, "test-key");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "deepseek-chat");
        return ai;
    }

    private String encryptLocalConfig(String value) {
        String encrypted = AESUtils.encryptLocalConfig(value);
        assertNotNull(encrypted, "内置 AI 配置测试依赖 BC 加密 provider 初始化成功");
        return encrypted;
    }

    private static class TestableAiConfigReader extends AiConfigReader {
        private AiRuntimeConfig readForTest(JsonObject aiConfig, boolean useProxy) {
            return read(aiConfig, useProxy);
        }
    }
}
