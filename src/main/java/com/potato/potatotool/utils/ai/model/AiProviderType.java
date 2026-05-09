package com.potato.potatotool.utils.ai.model;

public enum AiProviderType {
    OPENAI,
    ANTHROPIC,
    GEMINI;

    public static AiProviderType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OPENAI;
        }
        String normalized = value.trim().toUpperCase();
        if ("OPENAI".equals(normalized)) {
            return OPENAI;
        }
        try {
            return AiProviderType.valueOf(normalized);
        } catch (Exception ignored) {
            return OPENAI;
        }
    }
}
