package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiAttachmentUtils 测试")
class AiAttachmentUtilsTest {

    @Test
    @DisplayName("上传策略常量保持在约定边界")
    void uploadPolicyConstantsStayAligned() {
        assertEquals(10, AiAttachmentUtils.MAX_ATTACHMENT_COUNT);
        assertEquals(100L * 1024L * 1024L, AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES);
        assertEquals(300L * 1024L * 1024L, AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES);
        assertEquals(10L * 1024L * 1024L, AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES);
        assertEquals(20L * 1024L * 1024L, AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES);
        assertEquals(256L * 1024L, AiAttachmentUtils.MAX_TEXT_CONTEXT_ATTACHMENT_SIZE_BYTES);
        assertEquals(512L * 1024L, AiAttachmentUtils.MAX_TEXT_CONTEXT_TOTAL_SIZE_BYTES);
    }

    @Test
    @DisplayName("Anthropic 只允许图片 PDF 和文本类附件")
    void anthropicCapabilityBoundary() throws Exception {
        AiAttachment zipAttachment = new AiAttachment("sample.zip", "/tmp/sample.zip", 10L, "application/zip");
        AiAttachment pdfAttachment = new AiAttachment("report.pdf", "/tmp/report.pdf", 10L, "application/pdf");
        File codeFile = File.createTempFile("analysis-", ".java");
        try {
            Files.write(codeFile.toPath(), "class Demo {}".getBytes(StandardCharsets.UTF_8));
            AiAttachment codeAttachment = AiAttachmentUtils.createAttachment(codeFile);

            assertFalse(AiAttachmentUtils.isSupportedByProvider(AiProviderType.ANTHROPIC, zipAttachment));
            assertTrue(AiAttachmentUtils.isSupportedByProvider(AiProviderType.ANTHROPIC, pdfAttachment));
            assertTrue(AiAttachmentUtils.isSupportedByProvider(AiProviderType.ANTHROPIC, codeAttachment));
            assertTrue(AiAttachmentUtils.isSupportedByProvider(AiProviderType.OPENAI, zipAttachment));
            assertTrue(AiAttachmentUtils.isSupportedByProvider(AiProviderType.GEMINI, zipAttachment));
        } finally {
            codeFile.delete();
        }
    }

    @Test
    @DisplayName("未知扩展但明确文本 MIME 仍允许上传")
    void unknownExtensionWithTextMimeStillAllowed() throws Exception {
        File textFile = File.createTempFile("notes-", ".custom");
        File binaryFile = File.createTempFile("payload-", ".bin");
        try {
            Files.write(textFile.toPath(), "plain-text".getBytes(StandardCharsets.UTF_8));
            Files.write(binaryFile.toPath(), new byte[]{0x00, 0x01, 0x02, 0x03});

            AiAttachment customText = AiAttachmentUtils.createAttachment(textFile);
            AiAttachment unknownBinary = AiAttachmentUtils.createAttachment(binaryFile);

            assertTrue(AiAttachmentUtils.isSupportedForUpload(customText));
            assertFalse(AiAttachmentUtils.isSupportedForUpload(unknownBinary));
        } finally {
            textFile.delete();
            binaryFile.delete();
        }
    }

    @Test
    @DisplayName("中转 inline 能力按协议收敛")
    void relayInlineCapabilityBoundary() {
        AiAttachment imageAttachment = new AiAttachment("sample.png", "/tmp/sample.png", 10L, "image/png");
        AiAttachment pdfAttachment = new AiAttachment("report.pdf", "/tmp/report.pdf", 10L, "application/pdf");
        AiAttachment codeAttachment = new AiAttachment("analysis.java", "/tmp/analysis.java", 10L, "text/x-java-source");

        assertTrue(AiAttachmentUtils.supportsRelayInline(AiProviderType.OPENAI, imageAttachment));
        assertFalse(AiAttachmentUtils.supportsRelayInline(AiProviderType.OPENAI, pdfAttachment));
        assertTrue(AiAttachmentUtils.supportsRelayInline(AiProviderType.GEMINI, pdfAttachment));
        assertFalse(AiAttachmentUtils.supportsRelayInline(AiProviderType.ANTHROPIC, codeAttachment));
    }

    @Test
    @DisplayName("未知后缀但内容明确为纯文本时可高置信识别")
    void highConfidenceTextDetectionDoesNotRelyOnExtension() throws Exception {
        File file = File.createTempFile("plain-text-", ".blob");
        try {
            Files.write(file.toPath(), "hello\nworld\n".getBytes(StandardCharsets.UTF_8));

            AiAttachment attachment = AiAttachmentUtils.createAttachment(file);

            assertTrue(AiAttachmentUtils.isHighConfidenceTextFile(attachment));
            assertTrue(AiAttachmentUtils.readTextContext(attachment).isSupported());
        } finally {
            file.delete();
        }
    }

    @Test
    @DisplayName("伪装成文本的二进制文件不会被误判为纯文本")
    void binaryFileDisguisedAsTextShouldNotPassTextFallback() throws Exception {
        File file = File.createTempFile("binary-text-", ".txt");
        try {
            Files.write(file.toPath(), new byte[]{0x00, 0x01, 0x02, 0x03, 0x7F});

            AiAttachment attachment = AiAttachmentUtils.createAttachment(file);

            assertFalse(AiAttachmentUtils.isHighConfidenceTextFile(attachment));
            assertFalse(AiAttachmentUtils.readTextContext(attachment).isSupported());
        } finally {
            file.delete();
        }
    }
}
