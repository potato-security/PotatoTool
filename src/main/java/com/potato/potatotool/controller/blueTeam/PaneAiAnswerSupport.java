package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.ai.provider.AiProviderRegistry;

final class PaneAiAnswerSupport {

    private static final AiThinkingConfig DEFAULT_THINKING_CONFIG =
            new AiThinkingConfig(false, AiThinkingConfig.DEFAULT_BUDGET_TOKENS);

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
            return AiProviderType.OPENAI;
        }
        try {
            AiProviderType providerType = aiConfigReader.read().getProviderType();
            return providerType == null ? AiProviderType.OPENAI : providerType;
        } catch (Exception ignored) {
            return AiProviderType.OPENAI;
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

    static boolean supportsThinking(AiProviderRegistry aiProviderRegistry, AiConfigReader aiConfigReader) {
        AiRuntimeConfig runtimeConfig = readRuntimeConfig(aiConfigReader);
        if (runtimeConfig == null) {
            return false;
        }
        try {
            return aiProviderRegistry.get(runtimeConfig.getProviderType()).supportsThinking(runtimeConfig);
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean supportsThinkingBudget(AiProviderRegistry aiProviderRegistry, AiConfigReader aiConfigReader) {
        AiRuntimeConfig runtimeConfig = readRuntimeConfig(aiConfigReader);
        if (runtimeConfig == null) {
            return false;
        }
        try {
            return aiProviderRegistry.get(runtimeConfig.getProviderType()).supportsThinkingBudget(runtimeConfig);
        } catch (Exception ignored) {
            return false;
        }
    }
}
