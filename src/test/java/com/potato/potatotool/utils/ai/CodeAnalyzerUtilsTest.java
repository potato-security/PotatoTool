package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CodeAnalyzerUtils 测试")
class CodeAnalyzerUtilsTest {

    @Test
    @DisplayName("统一分析失败时直接返回错误")
    void buildEvilCodeAnalysisResultReturnSingleErrorWhenFailed() {
        AiChatService aiService = new StubAiChatService("AI 连接超时，请检查网络或代理");

        String result = CodeAnalyzerUtils.buildEvilCodeAnalysisResult("payload", "aes", aiService);

        assertEquals("AI 连接超时，请检查网络或代理", result);
    }

    @Test
    @DisplayName("统一分析成功时返回完整内容")
    void buildEvilCodeAnalysisResultReturnSingleResponse() {
        AiChatService aiService = new StubAiChatService("综合分析结果");

        String result = CodeAnalyzerUtils.buildEvilCodeAnalysisResult("payload", "aes", aiService);

        assertEquals("综合分析结果", result);
    }

    @Test
    @DisplayName("流式分析成功时输出统一章节")
    void streamEvilCodeAnalysisAppendUnifiedSection() {
        StubStreamAiChatService aiService = new StubStreamAiChatService(
                new AiStreamEvent[]{AiStreamEvent.token("综合"), AiStreamEvent.token("分析"), AiStreamEvent.done()}
        );
        RecordingAppender appender = new RecordingAppender();

        CodeAnalyzerUtils.streamEvilCodeAnalysis("payload", "aes", aiService, appender);

        String text = appender.getText();
        assertTrue(text.contains("综合分析与处置建议"));
        assertTrue(text.contains("综合分析"));
    }

    @Test
    @DisplayName("流式分析失败且无内容时仅输出错误")
    void streamEvilCodeAnalysisReturnErrorWhenNoContent() {
        StubStreamAiChatService aiService = new StubStreamAiChatService(
                new AiStreamEvent[]{AiStreamEvent.error("AI 连接超时，请检查网络或代理")}
        );
        RecordingAppender appender = new RecordingAppender();

        CodeAnalyzerUtils.streamEvilCodeAnalysis("payload", "aes", aiService, appender);

        assertEquals("AI 连接超时，请检查网络或代理", appender.getText());
    }

    @Test
    @DisplayName("流式分析中途失败时保留已有内容并追加错误说明")
    void streamEvilCodeAnalysisKeepContentWhenFailed() {
        StubStreamAiChatService aiService = new StubStreamAiChatService(
                new AiStreamEvent[]{AiStreamEvent.token("分析结果"), AiStreamEvent.error("AI 请求失败")}
        );
        RecordingAppender appender = new RecordingAppender();

        CodeAnalyzerUtils.streamEvilCodeAnalysis("payload", "aes", aiService, appender);

        String text = appender.getText();
        assertTrue(text.contains("分析结果"));
        assertTrue(text.contains("AI生成异常"));
    }

    @Test
    @DisplayName("流式反编译优化成功时直接输出内容")
    void streamOptimizedCodeAppendTokens() {
        StubStreamAiChatService aiService = new StubStreamAiChatService(
                new AiStreamEvent[]{AiStreamEvent.token("优化"), AiStreamEvent.token("结果"), AiStreamEvent.done()}
        );
        RecordingAppender appender = new RecordingAppender();

        CodeAnalyzerUtils.streamOptimizedCode("class A {}", aiService, appender);

        assertEquals("优化结果", appender.getText());
    }

    @Test
    @DisplayName("流式反编译优化失败且无内容时显示错误")
    void streamOptimizedCodeReturnErrorWhenNoContent() {
        StubStreamAiChatService aiService = new StubStreamAiChatService(
                new AiStreamEvent[]{AiStreamEvent.error("AI 请求失败")}
        );
        RecordingAppender appender = new RecordingAppender();

        CodeAnalyzerUtils.streamOptimizedCode("class A {}", aiService, appender);

        assertEquals("AI 请求失败", appender.getText());
    }

    @Test
    @DisplayName("会话已失效时不发起请求且不写入任何内容")
    void streamEvilCodeAnalysisSkipAllWhenGateInactive() {
        PhaseStreamAiChatService aiService = new PhaseStreamAiChatService(null, false,
                new AiStreamEvent[]{AiStreamEvent.token("结果"), AiStreamEvent.done()}
        );
        RecordingAppender appender = new RecordingAppender();
        MutableGate gate = new MutableGate();
        gate.close();

        boolean success = CodeAnalyzerUtils.streamEvilCodeAnalysis("payload", "aes", aiService, appender, gate);

        assertFalse(success);
        assertEquals(0, aiService.getCallCount(), "会话失效后不应发起请求");
        assertEquals("", appender.getText());
    }

    @Test
    @DisplayName("会话失效后不再发起续写请求")
    void streamEvilCodeAnalysisStopContinuationWhenGateClosed() {
        MutableGate gate = new MutableGate();
        PhaseStreamAiChatService aiService = new PhaseStreamAiChatService(gate, true,
                new AiStreamEvent[]{AiStreamEvent.token("前半段"), AiStreamEvent.error("流读取超时")},
                new AiStreamEvent[]{AiStreamEvent.token("续写内容"), AiStreamEvent.done()}
        );
        RecordingAppender appender = new RecordingAppender();

        boolean success = CodeAnalyzerUtils.streamEvilCodeAnalysis("payload", "aes", aiService, appender, gate);

        assertFalse(success);
        assertEquals(1, aiService.getCallCount(), "会话失效后不应再发起续写请求");
        assertFalse(appender.getText().contains("续写内容"));
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

        @Override
        public String askNoStream(String systemPrompt, String question) {
            return askNoStream(question);
        }
    }

    private static class StubStreamAiChatService extends AiChatService {
        private final AiStreamEvent[][] events;
        private int index;

        private StubStreamAiChatService(AiStreamEvent[]... events) {
            this.events = events;
        }

        @Override
        public void streamChat(String question, EventListener listener) {
            if (index >= events.length) {
                return;
            }
            AiStreamEvent[] current = events[index++];
            for (AiStreamEvent event : current) {
                listener.onEvent(event);
            }
        }

        @Override
        public void streamChat(String systemPrompt, String question, EventListener listener) {
            streamChat(question, listener);
        }
    }

    private static class MutableGate implements CodeAnalyzerUtils.StreamGate {
        private boolean active = true;

        @Override
        public boolean isActive() {
            return active;
        }

        private void close() {
            active = false;
        }
    }

    private static class PhaseStreamAiChatService extends AiChatService {
        private final MutableGate gate;
        private final boolean closeGateAfterPhase;
        private final AiStreamEvent[][] phases;
        private int index;
        private int callCount;

        private PhaseStreamAiChatService(MutableGate gate, boolean closeGateAfterPhase, AiStreamEvent[]... phases) {
            this.gate = gate;
            this.closeGateAfterPhase = closeGateAfterPhase;
            this.phases = phases;
        }

        @Override
        public void streamChat(String question, EventListener listener) {
            callCount++;
            if (index >= phases.length) {
                return;
            }
            for (AiStreamEvent event : phases[index++]) {
                listener.onEvent(event);
            }
            if (closeGateAfterPhase && gate != null) {
                gate.close();
            }
        }

        @Override
        public void streamChat(String systemPrompt, String question, EventListener listener) {
            streamChat(question, listener);
        }

        private int getCallCount() {
            return callCount;
        }
    }

    private static class RecordingAppender implements CodeAnalyzerUtils.StreamAppender {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void setText(String text) {
            builder.setLength(0);
            if (text != null) {
                builder.append(text);
            }
        }

        @Override
        public void append(String text) {
            if (text != null) {
                builder.append(text);
            }
        }

        private String getText() {
            return builder.toString();
        }
    }
}
