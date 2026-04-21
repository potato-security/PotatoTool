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

@DisplayName("AnthropicProviderAdapter 测试")
class AnthropicProviderAdapterTest {

    private final AnthropicProviderAdapter adapter = new AnthropicProviderAdapter();

    @Test
    @DisplayName("构造请求会归一化到 messages 端点")
    void buildRequestNormalizeMessagesEndpoint() {
        AiRuntimeConfig config = new AiRuntimeConfig(
                AiProviderType.ANTHROPIC,
                "https://api.anthropic.com",
                "key",
                "claude-3-7-sonnet-latest",
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

        assertEquals("https://api.anthropic.com/v1/messages", requestObj.getUrl());
        assertEquals("claude-3-7-sonnet-latest", json.get("model").getAsString());
        assertTrue(json.get("stream").getAsBoolean());
        assertEquals("系统提示", json.get("system").getAsString());
        assertEquals(2, json.getAsJsonArray("messages").size());
    }

    @Test
    @DisplayName("解析流式 text delta 和结束事件")
    void parseSseLineTextDeltaAndDone() {
        List<AiStreamEvent> tokenEvents = adapter.parseSseLine("data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"你好\"}}");
        assertEquals(1, tokenEvents.size());
        assertEquals(AiStreamEvent.Type.TOKEN, tokenEvents.get(0).getType());
        assertEquals("你好", tokenEvents.get(0).getContent());

        List<AiStreamEvent> doneEvents = adapter.parseSseLine("data: {\"type\":\"message_stop\"}");
        assertEquals(1, doneEvents.size());
        assertEquals(AiStreamEvent.Type.DONE, doneEvents.get(0).getType());
    }

    @Test
    @DisplayName("解析非流式响应文本和错误")
    void parseResponseTextAndError() {
        String text = adapter.parseResponse("{\"content\":[{\"type\":\"text\",\"text\":\"综合结果\"}]}");
        assertEquals("综合结果", text);

        String error = adapter.parseResponse("{\"type\":\"error\",\"error\":{\"message\":\"bad request\"}}");
        assertEquals("bad request", error);
    }
}
