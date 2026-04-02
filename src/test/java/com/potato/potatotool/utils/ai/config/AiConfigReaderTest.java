package com.potato.potatotool.utils.ai.config;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiConfigReader 测试")
class AiConfigReaderTest {

    private final AiConfigReader reader = new AiConfigReader();

    @Test
    @DisplayName("读取结构化配置成功")
    void readStructuredConfigSuccess() {
        JsonObject thinking = new JsonObject();
        thinking.addProperty(ConfigConstants.AI_THINKING_ENABLED, true);
        thinking.addProperty(ConfigConstants.AI_THINKING_BUDGET_TOKENS, 2048);

        JsonObject ai = new JsonObject();
        ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
        ai.addProperty(ConfigConstants.AI_BASE_URL, "https://open.bigmodel.cn/api/paas/v4/chat/completions");
        ai.addProperty(ConfigConstants.AI_API_KEY, "test-key");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "glm-4.6");
        ai.addProperty(ConfigConstants.AI_TIMEOUT_MS, 30000);
        ai.add(ConfigConstants.AI_THINKING, thinking);

        AiRuntimeConfig config = reader.read(ai, true);

        assertEquals("https://open.bigmodel.cn/api/paas/v4/chat/completions", config.getBaseUrl());
        assertEquals("test-key", config.getApiKey());
        assertEquals("glm-4.6", config.getModelName());
        assertEquals(30000, config.getTimeoutMs());
        assertTrue(config.isUseProxy());
        assertTrue(config.getThinkingConfig().isEnabled());
        assertEquals(2048, config.getThinkingConfig().getBudgetTokens());
    }

    @Test
    @DisplayName("缺少必填字段时抛出异常")
    void readMissingRequiredFieldThrows() {
        JsonObject ai = new JsonObject();
        ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
        ai.addProperty(ConfigConstants.AI_BASE_URL, "");
        ai.addProperty(ConfigConstants.AI_API_KEY, "");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> reader.read(ai, false));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("legacy 字段为明文时可回退读取")
    void readFallbackToLegacyPlainFields() {
        JsonObject ai = new JsonObject();
        ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
        ai.addProperty(ConfigConstants.AI_BASE_URL, "");
        ai.addProperty(ConfigConstants.AI_API_KEY, "");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "");
        ai.addProperty(ConfigConstants.AI_LOCAL_BASE_URL, "");
        ai.addProperty(ConfigConstants.AI_LOCAL_API_KEY, "");
        ai.addProperty(ConfigConstants.AI_LOCAL_MODEL_NAME, "");
        ai.addProperty("AI_API_Base", "http://127.0.0.1:50003/stream");
        ai.addProperty("AI_API_Key", "legacy-key");
        ai.addProperty("AI_Model", "legacy-model");

        AiRuntimeConfig config = reader.read(ai, false);

        assertEquals("http://127.0.0.1:50003/stream", config.getBaseUrl());
        assertEquals("legacy-key", config.getApiKey());
        assertEquals("legacy-model", config.getModelName());
    }

    @Test
    @DisplayName("结构化明文配置时不标记为内置 AI")
    void readPlainConfigIsNotBuiltinAi() {
        JsonObject ai = new JsonObject();
        ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
        ai.addProperty(ConfigConstants.AI_BASE_URL, "https://api.example.com/v1/chat/completions");
        ai.addProperty(ConfigConstants.AI_API_KEY, "plain-key");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "plain-model");

        AiRuntimeConfig config = reader.read(ai, false);

        assertFalse(config.isBuiltinAi());
    }

    @Test
    @DisplayName("明文为空回退到本地/历史字段时标记为内置 AI")
    void readFallbackConfigIsBuiltinAi() {
        JsonObject ai = new JsonObject();
        ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
        ai.addProperty(ConfigConstants.AI_BASE_URL, "");
        ai.addProperty(ConfigConstants.AI_API_KEY, "");
        ai.addProperty(ConfigConstants.AI_MODEL_NAME, "");
        ai.addProperty(ConfigConstants.AI_LOCAL_BASE_URL, "");
        ai.addProperty(ConfigConstants.AI_LOCAL_API_KEY, "");
        ai.addProperty(ConfigConstants.AI_LOCAL_MODEL_NAME, "");
        ai.addProperty("AI_API_Base", "http://127.0.0.1:50003/stream");
        ai.addProperty("AI_API_Key", "legacy-key");
        ai.addProperty("AI_Model", "legacy-model");

        AiRuntimeConfig config = reader.read(ai, false);

        assertTrue(config.isBuiltinAi());
    }
}
