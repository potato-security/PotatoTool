package com.potato.potatotool.utils.ai.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiMessage;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.network.RequestObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GeminiProviderAdapter 测试")
class GeminiProviderAdapterTest {

    private final GeminiProviderAdapter adapter = new GeminiProviderAdapter();

    @Test
    @DisplayName("构造请求会在流式时使用 streamGenerateContent 端点")
    void buildRequestNormalizeStreamEndpoint() {
        AiRuntimeConfig config = new AiRuntimeConfig(
                AiProviderType.GEMINI,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash",
                "key",
                "gemini-2.5-flash",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiChatRequest request = new AiChatRequest(
                "你好",
                "你好",
                Arrays.asList(new AiMessage("assistant", "历史回答")),
                new AiThinkingConfig(false, 1024),
                true,
                "系统提示"
        );

        RequestObj requestObj = adapter.buildRequest(config, request);
        String body = new String(requestObj.getPostData(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse", requestObj.getUrl());
        assertEquals("系统提示",
                json.getAsJsonObject("systemInstruction")
                        .getAsJsonArray("parts")
                        .get(0)
                        .getAsJsonObject()
                        .get("text")
                        .getAsString());
        assertEquals(2, json.getAsJsonArray("contents").size());
    }

    @Test
    @DisplayName("解析流式和非流式 Gemini 文本")
    void parseStreamAndNonStreamResponse() {
        List<AiStreamEvent> tokenEvents = adapter.parseSseLine("data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"你好\"}]}}]}");
        assertEquals(1, tokenEvents.size());
        assertEquals(AiStreamEvent.Type.TOKEN, tokenEvents.get(0).getType());
        assertEquals("你好", tokenEvents.get(0).getContent());

        String text = adapter.parseResponse("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"综合结果\"}]}}]}");
        assertEquals("综合结果", text);
    }

    @Test
    @DisplayName("非流式端点会去掉 alt=sse 并切回 generateContent")
    void buildRequestNormalizeNonStreamEndpoint() {
        AiRuntimeConfig config = new AiRuntimeConfig(
                AiProviderType.GEMINI,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse",
                "key",
                "gemini-2.5-flash",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiChatRequest request = new AiChatRequest("你好", null, new AiThinkingConfig(false, 1024), false);

        RequestObj requestObj = adapter.buildRequest(config, request);

        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent", requestObj.getUrl());
        assertTrue(requestObj.getHeaders().containsKey("x-goog-api-key"));
    }
}
