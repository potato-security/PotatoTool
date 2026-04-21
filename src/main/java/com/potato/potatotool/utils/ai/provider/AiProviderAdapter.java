package com.potato.potatotool.utils.ai.provider;

import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.List;

public interface AiProviderAdapter {

    RequestObj buildRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request);

    List<AiStreamEvent> parseSseLine(String line);

    default String parseResponse(String body) {
        return "";
    }
}
