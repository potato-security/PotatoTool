package com.potato.potatotool.utils.ai.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.network.ProxyUtils;

public class AiConfigReader {

    public AiRuntimeConfig read() {
        JsonObject aiConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.AI);
        boolean useProxy = ProxyUtils.isMainProxyEnabled() && ProxyUtils.isServiceProxyEnabled(ConfigConstants.AI);
        return read(aiConfig, useProxy);
    }

    protected AiRuntimeConfig read(JsonObject aiConfig, boolean useProxy) {
        if (aiConfig == null) {
            throw new IllegalStateException("AI 配置不存在");
        }
        String provider = readString(aiConfig, ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");

        String plainBaseUrl = readString(aiConfig, ConfigConstants.AI_BASE_URL, "");
        String plainApiKey = readString(aiConfig, ConfigConstants.AI_API_KEY, "");
        String plainModelName = readString(aiConfig, ConfigConstants.AI_MODEL_NAME, "");

        boolean hasPlainAiConfig = !isBlank(plainBaseUrl) || !isBlank(plainApiKey) || !isBlank(plainModelName);

        String baseUrl = plainBaseUrl;
        String apiKey = plainApiKey;
        String modelName = plainModelName;

        if (isBlank(baseUrl)) {
            baseUrl = decryptConfig(aiConfig, ConfigConstants.AI_LOCAL_BASE_URL);
        }
        if (isBlank(apiKey)) {
            apiKey = decryptConfig(aiConfig, ConfigConstants.AI_LOCAL_API_KEY);
        }
        if (isBlank(modelName)) {
            modelName = decryptConfig(aiConfig, ConfigConstants.AI_LOCAL_MODEL_NAME);
        }

        if (isBlank(baseUrl)) {
            baseUrl = readString(aiConfig, "AI_API_Base", "");
        }
        if (isBlank(apiKey)) {
            apiKey = readString(aiConfig, "AI_API_Key", "");
        }
        if (isBlank(modelName)) {
            modelName = readString(aiConfig, "AI_Model", "");
        }

        validateRequired("AI.base_url", baseUrl);
        validateRequired("AI.api_key", apiKey);
        validateRequired("AI.model_name", modelName);

        int timeoutMs = readInt(aiConfig, ConfigConstants.AI_TIMEOUT_MS, 60000);
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("AI.timeout_ms 必须大于 0");
        }

        AiThinkingConfig thinkingConfig = readThinkingConfig(aiConfig);
        boolean isBuiltinAi = !hasPlainAiConfig;

        return new AiRuntimeConfig(
                AiProviderType.fromString(provider),
                baseUrl,
                apiKey,
                modelName,
                timeoutMs,
                useProxy,
                thinkingConfig,
                isBuiltinAi
        );
    }

    private AiThinkingConfig readThinkingConfig(JsonObject aiConfig) {
        JsonObject thinking = aiConfig.has(ConfigConstants.AI_THINKING) && aiConfig.get(ConfigConstants.AI_THINKING).isJsonObject()
                ? aiConfig.getAsJsonObject(ConfigConstants.AI_THINKING)
                : new JsonObject();

        boolean enabled = readBoolean(thinking, ConfigConstants.AI_THINKING_ENABLED, false);
        int budgetTokens = readInt(thinking, ConfigConstants.AI_THINKING_BUDGET_TOKENS, 1024);

        if (budgetTokens <= 0) {
            throw new IllegalArgumentException("AI.thinking.budget_tokens 必须大于 0");
        }

        return new AiThinkingConfig(enabled, budgetTokens);
    }

    private String decryptConfig(JsonObject aiConfig, String key) {
        String encrypted = resolveEncryptedValue(aiConfig, key);
        if (isBlank(encrypted)) {
            return "";
        }
        String decrypted = AESUtils.decryptLocalConfig(encrypted);
        return decrypted == null ? "" : decrypted.trim();
    }

    private String resolveEncryptedValue(JsonObject aiConfig, String key) {
        String encrypted = readString(aiConfig, key, "");
        if (!isBlank(encrypted)) {
            return encrypted;
        }

        String legacyKey = null;
        if (ConfigConstants.AI_LOCAL_BASE_URL.equals(key)) {
            legacyKey = "Local_AI_API_Base";
        } else if (ConfigConstants.AI_LOCAL_API_KEY.equals(key)) {
            legacyKey = "Local_AI_API_Key";
        } else if (ConfigConstants.AI_LOCAL_MODEL_NAME.equals(key)) {
            legacyKey = "Local_AI_Model";
        }

        if (legacyKey == null) {
            return "";
        }
        return readString(aiConfig, legacyKey, "");
    }

    private String readString(JsonObject obj, String key, String defaultValue) {
        if (obj == null || !obj.has(key)) {
            return defaultValue;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        String value = element.getAsString();
        return value == null ? defaultValue : value.trim();
    }

    private int readInt(JsonObject obj, String key, int defaultValue) {
        if (obj == null || !obj.has(key)) {
            return defaultValue;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private boolean readBoolean(JsonObject obj, String key, boolean defaultValue) {
        if (obj == null || !obj.has(key)) {
            return defaultValue;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private void validateRequired(String field, String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
