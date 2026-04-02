package com.potato.potatotool.controller.blueTeam;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneAiAnswer thinking 配置联动测试")
class PaneAiAnswerThinkingConfigTest {

    @Test
    @DisplayName("每次读取最新 thinking 配置")
    void readLatestThinkingConfigEachTime() {
        ToggleConfigReader reader = new ToggleConfigReader();
        PaneAiAnswer paneAiAnswer = new PaneAiAnswer(null, reader);

        reader.setThinkingEnabled(false);
        assertFalse(paneAiAnswer.isThinkingEnabled());

        reader.setThinkingEnabled(true);
        assertTrue(paneAiAnswer.isThinkingEnabled());
    }

    private static class ToggleConfigReader extends AiConfigReader {
        private boolean thinkingEnabled;

        void setThinkingEnabled(boolean thinkingEnabled) {
            this.thinkingEnabled = thinkingEnabled;
        }

        @Override
        public AiRuntimeConfig read() {
            JsonObject thinking = new JsonObject();
            thinking.addProperty(ConfigConstants.AI_THINKING_ENABLED, thinkingEnabled);
            thinking.addProperty(ConfigConstants.AI_THINKING_BUDGET_TOKENS, 1024);

            JsonObject ai = new JsonObject();
            ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
            ai.addProperty(ConfigConstants.AI_BASE_URL, "https://api.example.com/v1/chat/completions");
            ai.addProperty(ConfigConstants.AI_API_KEY, "test-key");
            ai.addProperty(ConfigConstants.AI_MODEL_NAME, "test-model");
            ai.add(ConfigConstants.AI_THINKING, thinking);
            return read(ai, false);
        }
    }
}
