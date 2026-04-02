package com.potato.potatotool.utils.ai.model;

public class AiRuntimeConfig {
    private final AiProviderType providerType;
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final int timeoutMs;
    private final boolean useProxy;
    private final AiThinkingConfig thinkingConfig;
    private final boolean builtinAi;

    public AiRuntimeConfig(AiProviderType providerType,
                           String baseUrl,
                           String apiKey,
                           String modelName,
                           int timeoutMs,
                           boolean useProxy,
                           AiThinkingConfig thinkingConfig,
                           boolean builtinAi) {
        this.providerType = providerType;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.timeoutMs = timeoutMs;
        this.useProxy = useProxy;
        this.thinkingConfig = thinkingConfig;
        this.builtinAi = builtinAi;
    }

    public AiProviderType getProviderType() {
        return providerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public AiThinkingConfig getThinkingConfig() {
        return thinkingConfig;
    }

    public boolean isBuiltinAi() {
        return builtinAi;
    }
}
