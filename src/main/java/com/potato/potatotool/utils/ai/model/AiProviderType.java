package com.potato.potatotool.utils.ai.model;

public enum AiProviderType {
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    GEMINI;

    public static AiProviderType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OPENAI_COMPATIBLE;
        }
        try {
            return AiProviderType.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return OPENAI_COMPATIBLE;
        }
    }
}
