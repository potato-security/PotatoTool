package com.potato.potatotool.utils.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiPromptUtils 测试")
class AiPromptUtilsTest {

    @Test
    @DisplayName("附件会被编排进对话提示词")
    void buildConversationPromptIncludeAttachmentContext() {
        AiPromptUtils.AttachmentContext attachment = new AiPromptUtils.AttachmentContext(
                "sample.log",
                "/tmp/sample.log",
                321L,
                "POST /shell.jsp",
                false,
                true
        );

        String prompt = AiPromptUtils.buildConversationPrompt("帮我判断是否存在风险", Collections.singletonList(attachment));

        assertTrue(prompt.contains("用户目标"));
        assertTrue(prompt.contains("sample.log"));
        assertTrue(prompt.contains("已截断"));
        assertTrue(prompt.contains("POST /shell.jsp"));
    }

    @Test
    @DisplayName("安全分析提示词包含统一输出结构")
    void buildSecurityAnalysisPromptContainStructuredSections() {
        String prompt = AiPromptUtils.buildSecurityAnalysisPrompt("payload", "base64");

        assertTrue(prompt.contains("性质初判"));
        assertTrue(prompt.contains("检测与审计线索"));
        assertTrue(prompt.contains("处置建议"));
    }
}
