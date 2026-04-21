package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.ai.config.AiConfigReader;

final class PaneAiAnswerSupport {

    private PaneAiAnswerSupport() {
    }

    static boolean isThinkingEnabled(AiConfigReader aiConfigReader) {
        if (aiConfigReader == null) {
            return false;
        }
        try {
            return aiConfigReader.read().getThinkingConfig().isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }
}
