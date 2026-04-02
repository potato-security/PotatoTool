package com.potato.potatotool.controller.publicPane;

import com.potato.potatotool.content.classObj.ConfigConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneSetting AI配置映射测试")
class PaneSettingAiConfigTest {

    @Test
    @DisplayName("填充 AI 配置映射包含 provider timeout thinking")
    void fillAiConfigMapIncludeProviderTimeoutThinking() {
        Map<String, Object> aiMap = new LinkedHashMap<String, Object>();

        PaneSetting.fillAiConfigMap(
                aiMap,
                "OPENAI_COMPATIBLE",
                "https://api.example.com/v1/chat/completions",
                "test-key",
                "glm-4.6",
                45000,
                true,
                2048
        );

        assertEquals("OPENAI_COMPATIBLE", aiMap.get(ConfigConstants.AI_PROVIDER));
        assertEquals("https://api.example.com/v1/chat/completions", aiMap.get(ConfigConstants.AI_BASE_URL));
        assertEquals("test-key", aiMap.get(ConfigConstants.AI_API_KEY));
        assertEquals("glm-4.6", aiMap.get(ConfigConstants.AI_MODEL_NAME));
        assertEquals(45000, ((Number) aiMap.get(ConfigConstants.AI_TIMEOUT_MS)).intValue());

        assertTrue(aiMap.get(ConfigConstants.AI_THINKING) instanceof Map);
        Map<?, ?> thinkingMap = (Map<?, ?>) aiMap.get(ConfigConstants.AI_THINKING);
        assertEquals(true, thinkingMap.get(ConfigConstants.AI_THINKING_ENABLED));
        assertEquals(2048, ((Number) thinkingMap.get(ConfigConstants.AI_THINKING_BUDGET_TOKENS)).intValue());
    }
}
