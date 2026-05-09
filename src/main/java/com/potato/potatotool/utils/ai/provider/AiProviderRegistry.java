package com.potato.potatotool.utils.ai.provider;

import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.core.I18nTextUtils;

import java.util.HashMap;
import java.util.Map;

public class AiProviderRegistry {
    private final Map<AiProviderType, AiProviderAdapter> adapters = new HashMap<AiProviderType, AiProviderAdapter>();

    public AiProviderRegistry() {
        register(AiProviderType.OPENAI, new OpenAiCompatibleProviderAdapter());
        register(AiProviderType.ANTHROPIC, new AnthropicProviderAdapter());
        register(AiProviderType.GEMINI, new GeminiProviderAdapter());
    }

    public void register(AiProviderType type, AiProviderAdapter adapter) {
        adapters.put(type, adapter);
    }

    public AiProviderAdapter get(AiProviderType type) {
        AiProviderAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.error.provider.unregistered", type));
        }
        return adapter;
    }
}
