package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiAttachmentSupportResolver 测试")
class AiAttachmentSupportResolverTest {

    @Test
    @DisplayName("OpenAI 官方与中转对附件模式不同")
    void openAiOfficialAndRelayUseDifferentAttachmentModes() {
        AiAttachment imageAttachment = new AiAttachment("screen.png", "/tmp/screen.png", 1024L, "image/png");
        AiAttachment pdfAttachment = new AiAttachment("report.pdf", "/tmp/report.pdf", 1024L, "application/pdf");

        AiAttachmentSupportResult officialImage = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://api.openai.com/v1/chat/completions"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult relayImage = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://gateway.example.com/api/v1/chat/completions"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult relayPdf = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://gateway.example.com/api/v1/chat/completions"),
                Collections.singletonList(pdfAttachment)
        );

        assertTrue(officialImage.isSupported());
        assertEquals(AiAttachmentMode.REMOTE_UPLOAD_REFERENCE, officialImage.getMode());
        assertTrue(relayImage.isSupported());
        assertEquals(AiAttachmentMode.INLINE_IMAGE, relayImage.getMode());
        assertFalse(relayPdf.isSupported());
    }

    @Test
    @DisplayName("Gemini 中转允许图片和 PDF inline 但限制总大小")
    void geminiRelaySupportsInlineMediaWithTighterLimits() {
        AiAttachment pdfAttachment = new AiAttachment("report.pdf", "/tmp/report.pdf", 1024L, "application/pdf");
        AiAttachment oversizedAttachment = new AiAttachment(
                "big.pdf",
                "/tmp/big.pdf",
                AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES + 1L,
                "application/pdf"
        );

        AiAttachmentSupportResult relayPdf = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.GEMINI, "https://gateway.example.com/api/v1beta/models/gemini-2.5-flash"),
                Collections.singletonList(pdfAttachment)
        );
        AiAttachmentSupportResult relayOversized = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.GEMINI, "https://gateway.example.com/api/v1beta/models/gemini-2.5-flash"),
                Collections.singletonList(oversizedAttachment)
        );

        assertTrue(relayPdf.isSupported());
        assertEquals(AiAttachmentMode.INLINE_MEDIA, relayPdf.getMode());
        assertFalse(relayOversized.isSupported());
        assertTrue(relayOversized.getMessage().contains("10 MB"));
    }

    @Test
    @DisplayName("未知 relay 的普通文档和压缩包会按协议边界被拦截")
    void relayRejectsUnsupportedOfficeAndArchiveAttachments() {
        AiAttachment docxAttachment = new AiAttachment(
                "report.docx",
                "/tmp/report.docx",
                1024L,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        AiAttachment zipAttachment = new AiAttachment("sample.zip", "/tmp/sample.zip", 1024L, "application/zip");

        AiAttachmentSupportResult openAiRelayDocx = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://gateway.example.com/api/v1/chat/completions"),
                Collections.singletonList(docxAttachment)
        );
        AiAttachmentSupportResult anthropicRelayDocx = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.ANTHROPIC, "https://gateway.example.com/api/v1/messages"),
                Collections.singletonList(docxAttachment)
        );
        AiAttachmentSupportResult geminiRelayZip = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.GEMINI, "https://gateway.example.com/api/v1beta/models/gemini-2.5-flash"),
                Collections.singletonList(zipAttachment)
        );

        assertFalse(openAiRelayDocx.isSupported());
        assertTrue(openAiRelayDocx.getMessage().contains("图片附件"));
        assertFalse(anthropicRelayDocx.isSupported());
        assertTrue(anthropicRelayDocx.getMessage().contains("图片附件"));
        assertFalse(geminiRelayZip.isSupported());
        assertTrue(geminiRelayZip.getMessage().contains("PDF"));
    }

    @Test
    @DisplayName("GLM Qwen DeepSeek 官方 OpenAI 兼容端点按模型能力分流")
    void officialOpenAiCompatibleVendorsUseHostAndModelCapabilities() {
        AiAttachment imageAttachment = new AiAttachment("screen.png", "/tmp/screen.png", 1024L, "image/png");

        AiAttachmentSupportResult glmVision = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4.6v-flash"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult glmText = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4.6"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult qwenVision = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen3.5-plus"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult qwenText = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-turbo"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult deepSeek = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://api.deepseek.com/chat/completions", "deepseek-chat"),
                Collections.singletonList(imageAttachment)
        );

        assertTrue(glmVision.isSupported());
        assertEquals(AiAttachmentMode.INLINE_IMAGE, glmVision.getMode());
        assertFalse(glmText.isSupported());
        assertTrue(glmText.getMessage().contains("GLM"));

        assertTrue(qwenVision.isSupported());
        assertEquals(AiAttachmentMode.INLINE_IMAGE, qwenVision.getMode());
        assertFalse(qwenText.isSupported());
        assertTrue(qwenText.getMessage().contains("Qwen"));

        assertFalse(deepSeek.isSupported());
        assertTrue(deepSeek.getMessage().contains("DeepSeek"));
    }

    @Test
    @DisplayName("未知中转也会基于已知文本模型名拦截附件")
    void unknownRelayStillBlocksKnownTextOnlyModels() {
        AiAttachment imageAttachment = new AiAttachment("screen.png", "/tmp/screen.png", 1024L, "image/png");

        AiAttachmentSupportResult glmRelayText = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://gateway.example.com/api/v1/chat/completions", "glm-4.6"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult qwenRelayText = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://gateway.example.com/api/v1/chat/completions", "qwen3-max-2026-01-23"),
                Collections.singletonList(imageAttachment)
        );
        AiAttachmentSupportResult deepSeekRelayText = AiAttachmentSupportResolver.resolve(
                runtime(AiProviderType.OPENAI_COMPATIBLE, "https://gateway.example.com/api/v1/chat/completions", "deepseek-chat"),
                Collections.singletonList(imageAttachment)
        );

        assertFalse(glmRelayText.isSupported());
        assertTrue(glmRelayText.getMessage().contains("视觉"));
        assertFalse(qwenRelayText.isSupported());
        assertTrue(qwenRelayText.getMessage().contains("视觉"));
        assertFalse(deepSeekRelayText.isSupported());
        assertTrue(deepSeekRelayText.getMessage().contains("视觉"));
    }

    private AiRuntimeConfig runtime(AiProviderType providerType, String baseUrl) {
        return runtime(providerType, baseUrl, "test-model");
    }

    private AiRuntimeConfig runtime(AiProviderType providerType, String baseUrl, String modelName) {
        return new AiRuntimeConfig(
                providerType,
                baseUrl,
                "key",
                modelName,
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                false
        );
    }
}
