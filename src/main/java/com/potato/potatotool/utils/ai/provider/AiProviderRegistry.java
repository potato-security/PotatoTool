package com.potato.potatotool.utils.ai.provider;

import com.potato.potatotool.utils.ai.model.AiProviderType;

import java.util.HashMap;
import java.util.Map;

public class AiProviderRegistry {
    private final Map<AiProviderType, AiProviderAdapter> adapters = new HashMap<AiProviderType, AiProviderAdapter>();

    public AiProviderRegistry() {
        register(AiProviderType.OPENAI_COMPATIBLE, new OpenAiCompatibleProviderAdapter());
    }

    public void register(AiProviderType type, AiProviderAdapter adapter) {
        adapters.put(type, adapter);
    }

    public AiProviderAdapter get(AiProviderType type) {
        AiProviderAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException("未注册的 AI Provider: " + type);
        }
        return adapter;
    }
}
