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

    @Test
    @DisplayName("只有官方 Gemini Host 才允许附件上传")
    void resolveAttachmentSupportForOfficialAndRelay() {
        AiRuntimeConfig officialConfig = new AiRuntimeConfig(
                AiProviderType.GEMINI,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash",
                "key",
                "gemini-2.5-flash",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiRuntimeConfig customGatewayConfig = new AiRuntimeConfig(
                AiProviderType.GEMINI,
                "https://gateway.example.com/v1beta/models/gemini-2.5-flash",
                "key",
                "gemini-2.5-flash",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiAttachment imageAttachment = new AiAttachment("screen.png", "/tmp/screen.png", 1024L, "image/png");
        AiAttachment pdfAttachment = new AiAttachment("report.pdf", "/tmp/report.pdf", 1024L, "application/pdf");
        AiAttachment zipAttachment = new AiAttachment("sample.zip", "/tmp/sample.zip", 1024L, "application/zip");

        AiAttachmentSupportResult officialSupport = adapter.resolveAttachmentSupport(officialConfig, Collections.singletonList(pdfAttachment));
        AiAttachmentSupportResult relayImageSupport = adapter.resolveAttachmentSupport(customGatewayConfig, Collections.singletonList(imageAttachment));
        AiAttachmentSupportResult relayZipSupport = adapter.resolveAttachmentSupport(customGatewayConfig, Collections.singletonList(zipAttachment));

        assertTrue(officialSupport.isSupported());
        assertEquals(AiAttachmentMode.REMOTE_UPLOAD_REFERENCE, officialSupport.getMode());
        assertTrue(relayImageSupport.isSupported());
        assertEquals(AiAttachmentMode.INLINE_MEDIA, relayImageSupport.getMode());
        assertFalse(relayZipSupport.isSupported());
    }

    @Test
    @DisplayName("非官方 Gemini 中转会把附件转成 inlineData")
    void prepareRequestForRelayUsesInlineData() throws Exception {
        File pdfFile = File.createTempFile("gemini-inline-", ".pdf");
        pdfFile.deleteOnExit();
        Files.write(pdfFile.toPath(), new byte[]{1, 2, 3, 4});

        AiRuntimeConfig config = new AiRuntimeConfig(
                AiProviderType.GEMINI,
                "https://gateway.example.com/api/v1beta/models/gemini-2.5-flash",
                "key",
                "gemini-2.5-flash",
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
        AiChatRequest request = new AiChatRequest(
                "分析 PDF",
                "分析 PDF",
                null,
                Collections.singletonList(new AiAttachment(pdfFile.getName(), pdfFile.getAbsolutePath(), pdfFile.length(), "application/pdf")),
                new AiThinkingConfig(false, 1024),
                true,
                ""
        );

        AiProviderAdapter.PreparedRequest preparedRequest = adapter.prepareRequest(config, request);
        String body = new String(preparedRequest.getRequestObj().getPostData(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(
                json.getAsJsonArray("contents")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("parts")
                        .get(1)
                        .getAsJsonObject()
                        .has("inlineData")
        );
        assertTrue(body.contains("\"mimeType\":\"application/pdf\""));
    }
}
