package com.potato.potatotool.utils.ai.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nTextUtils;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.network.ProxyUtils;

import java.net.URI;

public class AiConfigReader {

    public AiRuntimeConfig read() {
        JsonObject aiConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.AI);
        boolean useProxy = ProxyUtils.isMainProxyEnabled() && ProxyUtils.isServiceProxyEnabled(ConfigConstants.AI);
        return read(aiConfig, useProxy);
    }

    protected AiRuntimeConfig read(JsonObject aiConfig, boolean useProxy) {
        if (aiConfig == null) {
            throw new IllegalStateException(I18nTextUtils.getString("ai.attach.error.config.missing"));
        }
        String provider = readString(aiConfig, ConfigConstants.AI_PROVIDER, "OPENAI");

        String plainBaseUrl = readString(aiConfig, ConfigConstants.AI_BASE_URL, "");
        String plainApiKey = readString(aiConfig, ConfigConstants.AI_API_KEY, "");
        String plainModelName = readString(aiConfig, ConfigConstants.AI_MODEL_NAME, "");

        boolean hasPlainAiConfig = !isBlank(plainBaseUrl) || !isBlank(plainApiKey) || !isBlank(plainModelName);
        boolean useBuiltinGateway = readBoolean(aiConfig, ConfigConstants.AI_USE_BUILTIN_GATEWAY, !hasPlainAiConfig);

        String baseUrl;
        String apiKey;
        String modelName;
        if (useBuiltinGateway) {
            baseUrl = decryptConfig(aiConfig, ConfigConstants.AI_LOCAL_BASE_URL);
            apiKey = decryptConfig(aiConfig, ConfigConstants.AI_LOCAL_API_KEY);
            modelName = decryptConfig(aiConfig, ConfigConstants.AI_LOCAL_MODEL_NAME);
            provider = "OPENAI";
        } else {
            baseUrl = plainBaseUrl;
            apiKey = plainApiKey;
            modelName = plainModelName;
        }

        validateRequired("AI.base_url", baseUrl);
        validateRequired("AI.api_key", apiKey);
        validateRequired("AI.model_name", modelName);
        validateHttpUrl("AI.base_url", baseUrl);

        int timeoutMs = readInt(aiConfig, ConfigConstants.AI_TIMEOUT_MS, 60000);
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.error.config.positive", "AI.timeout_ms"));
        }

        AiThinkingConfig thinkingConfig = readThinkingConfig(aiConfig);

        return new AiRuntimeConfig(
                AiProviderType.fromString(provider),
                baseUrl,
                apiKey,
                modelName,
                timeoutMs,
                useProxy,
                thinkingConfig,
                useBuiltinGateway
        );
    }

    private AiThinkingConfig readThinkingConfig(JsonObject aiConfig) {
        JsonObject thinking = aiConfig.has(ConfigConstants.AI_THINKING) && aiConfig.get(ConfigConstants.AI_THINKING).isJsonObject()
                ? aiConfig.getAsJsonObject(ConfigConstants.AI_THINKING)
                : new JsonObject();

        boolean enabled = readBoolean(thinking, ConfigConstants.AI_THINKING_ENABLED, false);
        int budgetTokens = readInt(
                thinking,
                ConfigConstants.AI_THINKING_BUDGET_TOKENS,
                AiThinkingConfig.DEFAULT_BUDGET_TOKENS
        );

        if (budgetTokens <= 0) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.error.config.positive", "AI.thinking.budget_tokens"));
        }

        return new AiThinkingConfig(enabled, budgetTokens);
    }

    private String decryptConfig(JsonObject aiConfig, String key) {
        String encrypted = readString(aiConfig, key, "");
        if (isBlank(encrypted)) {
            return "";
        }
        String decrypted = AESUtils.decryptLocalConfig(encrypted);
        return decrypted == null ? "" : decrypted.trim();
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
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.error.config.required", field));
        }
    }

    private void validateHttpUrl(String field, String value) {
        if (isBlank(value)) {
            return;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                throw new IllegalArgumentException(I18nTextUtils.getString("ai.error.config.http.url", field));
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException(I18nTextUtils.getString("ai.error.config.http.url", field));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.error.config.http.url", field));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
