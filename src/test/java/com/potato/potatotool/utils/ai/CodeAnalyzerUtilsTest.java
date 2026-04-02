package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.service.AiChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CodeAnalyzerUtils 测试")
class CodeAnalyzerUtilsTest {

    @Test
    @DisplayName("首段分析失败时只返回一次错误")
    void buildEvilCodeAnalysisResultReturnSingleErrorWhenFirstCallFailed() {
        AiChatService aiService = new StubAiChatService(
                "AI 连接超时，请检查网络或代理",
                "不应返回"
        );

        String result = CodeAnalyzerUtils.buildEvilCodeAnalysisResult("payload", "aes", aiService);

        assertEquals("AI 连接超时，请检查网络或代理", result);
    }

    @Test
    @DisplayName("首段成功二段为空时仅返回首段")
    void buildEvilCodeAnalysisResultReturnFirstWhenSecondEmpty() {
        AiChatService aiService = new StubAiChatService("分析结果", "   ");

        String result = CodeAnalyzerUtils.buildEvilCodeAnalysisResult("payload", "aes", aiService);

        assertEquals("分析结果", result);
    }

    @Test
    @DisplayName("两段都成功时按双换行拼接")
    void buildEvilCodeAnalysisResultCombineTwoSections() {
        AiChatService aiService = new StubAiChatService("分析结果", "响应建议");

        String result = CodeAnalyzerUtils.buildEvilCodeAnalysisResult("payload", "aes", aiService);

        assertEquals("分析结果\n\n响应建议", result);
    }

    private static class StubAiChatService extends AiChatService {
        private final String[] responses;
        private int index;

        private StubAiChatService(String... responses) {
            this.responses = responses;
        }

        @Override
        public String askNoStream(String question) {
            if (index >= responses.length) {
                return "";
            }
            return responses[index++];
        }
    }
}
