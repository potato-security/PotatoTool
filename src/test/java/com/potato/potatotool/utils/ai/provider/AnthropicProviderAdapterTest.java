package com.potato.potatotool.utils.ai.provider;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
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

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    @DisplayName("只有官方 Anthropic Host 才允许附件上传")
    void resolveAttachmentSupportForOfficialAndRelay() {
        AiRuntimeConfig officialConfig = new AiRuntimeConfig(
                AiProviderType.ANTHROPIC,
                "https://api.anthropic.com/v1/messages",
                "key",
                "claude-3-7-sonnet-latest",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiRuntimeConfig customGatewayConfig = new AiRuntimeConfig(
                AiProviderType.ANTHROPIC,
                "https://gateway.example.com/v1/messages",
                "key",
                "claude-3-7-sonnet-latest",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiAttachment imageAttachment = new AiAttachment("screen.png", "/tmp/screen.png", 1024L, "image/png");
        AiAttachment pdfAttachment = new AiAttachment("report.pdf", "/tmp/report.pdf", 1024L, "application/pdf");

        AiAttachmentSupportResult officialSupport = adapter.resolveAttachmentSupport(officialConfig, Collections.singletonList(pdfAttachment));
        AiAttachmentSupportResult relayImageSupport = adapter.resolveAttachmentSupport(customGatewayConfig, Collections.singletonList(imageAttachment));
        AiAttachmentSupportResult relayPdfSupport = adapter.resolveAttachmentSupport(customGatewayConfig, Collections.singletonList(pdfAttachment));

        assertTrue(officialSupport.isSupported());
        assertEquals(AiAttachmentMode.REMOTE_UPLOAD_REFERENCE, officialSupport.getMode());
        assertTrue(relayImageSupport.isSupported());
        assertEquals(AiAttachmentMode.INLINE_IMAGE, relayImageSupport.getMode());
        assertFalse(relayPdfSupport.isSupported());
    }

    @Test
    @DisplayName("非官方 Anthropic 中转会把图片转成 base64 image block")
    void prepareRequestForRelayUsesInlineImageBlock() throws Exception {
        File imageFile = File.createTempFile("anthropic-inline-", ".png");
        imageFile.deleteOnExit();
        Files.write(imageFile.toPath(), new byte[]{1, 2, 3, 4});

        AiRuntimeConfig config = new AiRuntimeConfig(
                AiProviderType.ANTHROPIC,
                "https://gateway.example.com/api/v1/messages",
                "key",
                "claude-3-7-sonnet-latest",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiChatRequest request = new AiChatRequest(
                "看图分析",
                "看图分析",
                null,
                Collections.singletonList(new AiAttachment(imageFile.getName(), imageFile.getAbsolutePath(), imageFile.length(), "image/png")),
                new AiThinkingConfig(false, 1024),
                true,
                ""
        );

        AiProviderAdapter.PreparedRequest preparedRequest = adapter.prepareRequest(config, request);
        String body = new String(preparedRequest.getRequestObj().getPostData(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertEquals("image",
                json.getAsJsonArray("messages")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("content")
                        .get(1)
                        .getAsJsonObject()
                        .get("type")
                        .getAsString());
        assertTrue(body.contains("\"type\":\"base64\""));
    }
}
