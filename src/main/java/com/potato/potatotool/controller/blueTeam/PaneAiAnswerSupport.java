package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;

final class PaneAiAnswerSupport {

    private static final AiThinkingConfig DEFAULT_THINKING_CONFIG = new AiThinkingConfig(false, 1024);

    private PaneAiAnswerSupport() {
    }

    static boolean isThinkingEnabled(AiConfigReader aiConfigReader) {
        return readThinkingConfig(aiConfigReader).isEnabled();
    }

    static AiThinkingConfig readThinkingConfig(AiConfigReader aiConfigReader) {
        if (aiConfigReader == null) {
            return DEFAULT_THINKING_CONFIG;
        }
        try {
            AiThinkingConfig thinkingConfig = aiConfigReader.read().getThinkingConfig();
            return thinkingConfig == null ? DEFAULT_THINKING_CONFIG : thinkingConfig;
        } catch (Exception ignored) {
            return DEFAULT_THINKING_CONFIG;
        }
    }

    static AiProviderType readProviderType(AiConfigReader aiConfigReader) {
        if (aiConfigReader == null) {
            return AiProviderType.OPENAI_COMPATIBLE;
        }
        try {
            AiProviderType providerType = aiConfigReader.read().getProviderType();
            return providerType == null ? AiProviderType.OPENAI_COMPATIBLE : providerType;
        } catch (Exception ignored) {
            return AiProviderType.OPENAI_COMPATIBLE;
        }
    }

    static AiRuntimeConfig readRuntimeConfig(AiConfigReader aiConfigReader) {
        if (aiConfigReader == null) {
            return null;
        }
        try {
            return aiConfigReader.read();
        } catch (Exception ignored) {
            return null;
        }
    }
}
