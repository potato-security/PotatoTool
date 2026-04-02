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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OpenAiCompatibleProviderAdapter 测试")
class OpenAiCompatibleProviderAdapterTest {

    private final OpenAiCompatibleProviderAdapter adapter = new OpenAiCompatibleProviderAdapter();

    @Test
    @DisplayName("构造请求体包含 thinking")
    void buildRequestWithThinking() {
        AiRuntimeConfig config = new AiRuntimeConfig(
                AiProviderType.OPENAI_COMPATIBLE,
                "https://api.example.com/v1/chat/completions",
                "key",
                "glm",
                60000,
                false,
                new AiThinkingConfig(true, 2048),
                false
        );

        AiChatRequest request = new AiChatRequest(
                "你好",
                Arrays.asList(new AiMessage("user", "历史问题"), new AiMessage("assistant", "历史回答")),
                new AiThinkingConfig(true, 2048),
                true
        );

        RequestObj requestObj = adapter.buildRequest(config, request);
        String body = new String(requestObj.getPostData(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertEquals("glm", json.get("model").getAsString());
        assertTrue(json.get("stream").getAsBoolean());
        assertTrue(json.has("thinking"));
        assertEquals(2048, json.getAsJsonObject("thinking").get("budget_tokens").getAsInt());
        assertEquals(3, json.getAsJsonArray("messages").size());
        assertFalse(requestObj.isInternalAiRequest());
    }

    @Test
    @DisplayName("构造请求体在 thinking 关闭时不注入")
    void buildRequestWithoutThinking() {
        AiRuntimeConfig config = new AiRuntimeConfig(
                AiProviderType.OPENAI_COMPATIBLE,
                "https://api.example.com/v1/chat/completions",
                "key",
                "glm",
                60000,
                true,
                new AiThinkingConfig(false, 1024),
                true
        );

        AiChatRequest request = new AiChatRequest("test", null, new AiThinkingConfig(false, 1024), true);
        RequestObj requestObj = adapter.buildRequest(config, request);
        String body = new String(requestObj.getPostData(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertFalse(json.has("thinking"));
        assertTrue(requestObj.isInternalAiRequest());
    }

    @Test
    @DisplayName("解析 SSE token thinking done error")
    void parseSseLineCoversMainCases() {
        List<AiStreamEvent> thinkingEvents = adapter.parseSseLine("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"思考\"},\"finish_reason\":null}]}");
        assertEquals(1, thinkingEvents.size());
        assertEquals(AiStreamEvent.Type.THINKING_TOKEN, thinkingEvents.get(0).getType());

        List<AiStreamEvent> tokenEvents = adapter.parseSseLine("data: {\"choices\":[{\"delta\":{\"content\":\"答案\"},\"finish_reason\":null}]}");
        assertEquals(1, tokenEvents.size());
        assertEquals(AiStreamEvent.Type.TOKEN, tokenEvents.get(0).getType());

        List<AiStreamEvent> doneEvents = adapter.parseSseLine("data: [DONE]");
        assertEquals(1, doneEvents.size());
        assertEquals(AiStreamEvent.Type.DONE, doneEvents.get(0).getType());

        List<AiStreamEvent> errorEvents = adapter.parseSseLine("data: {\"error\":{\"message\":\"bad request\"}}");
        assertEquals(1, errorEvents.size());
        assertEquals(AiStreamEvent.Type.ERROR, errorEvents.get(0).getType());
        assertTrue(errorEvents.get(0).getContent().contains("bad request"));
    }
}
