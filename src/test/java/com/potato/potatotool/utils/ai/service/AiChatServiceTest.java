package com.potato.potatotool.utils.ai.service;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.AiAttachmentUtils;
import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.ai.provider.AiProviderAdapter;
import com.potato.potatotool.utils.ai.provider.AiProviderRegistry;
import com.potato.potatotool.utils.network.RequestObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiChatService 测试")
class AiChatServiceTest {

    @Test
    @DisplayName("流式事件顺序与历史上限")
    void streamEventOrderAndHistoryLimit() {
        AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register(AiProviderType.OPENAI, new StubProviderAdapter());

        AiChatService.StreamTransport transport = (requestObj, streamSession, lineConsumer) -> {
            lineConsumer.accept("thinking");
            lineConsumer.accept("token:A");
            lineConsumer.accept("token:B");
            lineConsumer.accept("done");
        };

        AiChatService service = new AiChatService(reader, registry, transport);

        List<AiStreamEvent.Type> types = new ArrayList<AiStreamEvent.Type>();
        StringBuilder answer = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            service.streamChat("Q" + i, event -> {
                types.add(event.getType());
                if (event.getType() == AiStreamEvent.Type.TOKEN) {
                    answer.append(event.getContent());
                }
            });
        }

        assertTrue(types.contains(AiStreamEvent.Type.THINKING_TOKEN));
        assertTrue(types.contains(AiStreamEvent.Type.TOKEN));
        assertTrue(types.contains(AiStreamEvent.Type.DONE));
        assertEquals("ABABABABABAB", answer.toString());
    }

    @Test
    @DisplayName("内置 AI 超时错误输出中文提示")
    void builtinTimeoutMessage() {
        AiChatService service = createServiceWithError(true, "http://127.0.0.1:50003/stream", "Connect timed out");

        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        service.streamChat("hello", events::add);

        assertEquals(1, events.size());
        assertEquals(AiStreamEvent.Type.ERROR, events.get(0).getType());
        assertTrue(events.get(0).getContent().contains("连接超时"));
    }

    @Test
    @DisplayName("内置 AI 地址错误会脱敏")
    void builtinEndpointMessageSanitized() {
        AiChatService service = createServiceWithError(true, "http://127.0.0.1:50003/stream",
                "[×] 请求失败: http://127.0.0.1:50003/stream, 错误: Failed to connect to /127.0.0.1:50003");

        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        service.streamChat("hello", events::add);

        String message = events.get(0).getContent();
        assertTrue(message.contains("内置AI服务地址"));
        assertTrue(message.contains("内置AI服务主机"));
        assertTrue(!message.contains("127.0.0.1:50003"));
    }

    @Test
    @DisplayName("用户显式配置 AI 地址错误不脱敏")
    void customEndpointMessageNotSanitized() {
        AiChatService service = createServiceWithError(false, "https://api.example.com/v1/chat/completions",
                "[×] 请求失败: https://api.example.com/v1/chat/completions, 错误: Failed to connect to /api.example.com:443");

        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        service.streamChat("hello", events::add);

        String message = events.get(0).getContent();
        assertTrue(message.contains("https://api.example.com/v1/chat/completions"));
        assertTrue(message.contains("/api.example.com:443"));
    }

    @Test
    @DisplayName("流式请求支持会话级 thinking 覆盖")
    void streamChatSupportsThinkingOverride() {
        AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
        AiProviderRegistry registry = new AiProviderRegistry();
        CapturingProviderAdapter adapter = new CapturingProviderAdapter();
        registry.register(AiProviderType.OPENAI, adapter);

        AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> lineConsumer.accept("done"));
        service.streamChat(null, "hello", "hello", new AiThinkingConfig(true, 4096), event -> { });

        assertNotNull(adapter.lastThinkingConfig);
        assertTrue(adapter.lastThinkingConfig.isEnabled());
        assertEquals(4096, adapter.lastThinkingConfig.getBudgetTokens());
    }

    @Test
    @DisplayName("停止流式请求不会继续输出后续内容")
    void cancelActiveStreamStopsStreaming() throws Exception {
        AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register(AiProviderType.OPENAI, new StubProviderAdapter());

        CountDownLatch firstTokenSeen = new CountDownLatch(1);
        CountDownLatch cancelObserved = new CountDownLatch(1);
        AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> {
            lineConsumer.accept("token:A");
            firstTokenSeen.countDown();
            long deadline = System.currentTimeMillis() + 2000;
            while (!streamSession.isCancelled() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10L);
            }
            if (streamSession.isCancelled()) {
                cancelObserved.countDown();
                return;
            }
            lineConsumer.accept("token:B");
            lineConsumer.accept("done");
        });

        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        Thread streamThread = new Thread(() -> service.streamChat("hello", events::add));
        streamThread.start();

        assertTrue(firstTokenSeen.await(1, TimeUnit.SECONDS));
        service.cancelActiveStream();

        assertTrue(cancelObserved.await(1, TimeUnit.SECONDS));
        streamThread.join(2000L);
        assertTrue(!streamThread.isAlive());

        int tokenCount = 0;
        StringBuilder tokenBuffer = new StringBuilder();
        for (AiStreamEvent event : events) {
            if (event.getType() == AiStreamEvent.Type.TOKEN) {
                tokenCount++;
                tokenBuffer.append(event.getContent());
            }
            assertTrue(event.getType() != AiStreamEvent.Type.ERROR);
            assertTrue(event.getType() != AiStreamEvent.Type.DONE);
        }

        assertEquals(1, tokenCount);
        assertEquals("A", tokenBuffer.toString());
    }

    @Test
    @DisplayName("流式请求会把附件传入 prepareRequest 并在结束后清理")
    void streamChatPassesAttachmentsAndCleansPreparedRequest() {
        AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
        AiProviderRegistry registry = new AiProviderRegistry();
        PreparedRequestCapturingAdapter adapter = new PreparedRequestCapturingAdapter();
        registry.register(AiProviderType.OPENAI, adapter);

        AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> {
            lineConsumer.accept("token:OK");
            lineConsumer.accept("done");
        });

        List<AiAttachment> attachments = Collections.singletonList(
                new AiAttachment("report.pdf", "/tmp/report.pdf", 123L, "application/pdf")
        );
        service.streamChat("system", "hello", "history", attachments, new AiThinkingConfig(false, 1024), event -> { });

        assertEquals(1, adapter.prepareRequestCount);
        assertEquals(1, adapter.cleanupCount);
        assertNotNull(adapter.lastAttachments);
        assertEquals(1, adapter.lastAttachments.size());
        assertEquals("report.pdf", adapter.lastAttachments.get(0).getFileName());
    }

    @Test
    @DisplayName("prepareRequest 失败时直接返回错误且不进入传输层")
    void prepareRequestFailureReturnsErrorImmediately() {
        AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
        AiProviderRegistry registry = new AiProviderRegistry();
        FailingPrepareAdapter adapter = new FailingPrepareAdapter();
        registry.register(AiProviderType.OPENAI, adapter);

        final boolean[] transportCalled = new boolean[]{false};
        AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> transportCalled[0] = true);

        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        service.streamChat(
                "system",
                "hello",
                "history",
                Collections.singletonList(new AiAttachment("report.pdf", "/tmp/report.pdf", 123L, "application/pdf")),
                new AiThinkingConfig(false, 1024),
                events::add
        );

        assertEquals(1, events.size());
        assertEquals(AiStreamEvent.Type.ERROR, events.get(0).getType());
        assertTrue(events.get(0).getContent().contains("upload failed"));
        assertTrue(!transportCalled[0]);
    }

    @Test
    @DisplayName("传输异常时也会执行 preparedRequest 清理")
    void transportFailureStillCleansPreparedRequest() {
        AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
        AiProviderRegistry registry = new AiProviderRegistry();
        PreparedRequestCapturingAdapter adapter = new PreparedRequestCapturingAdapter();
        registry.register(AiProviderType.OPENAI, adapter);

        AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> {
            throw new RuntimeException("network down");
        });

        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        service.streamChat("hello", events::add);

        assertEquals(1, adapter.cleanupCount);
        assertEquals(1, events.size());
        assertEquals(AiStreamEvent.Type.ERROR, events.get(0).getType());
        assertTrue(events.get(0).getContent().contains("network down"));
    }

    @Test
    @DisplayName("附件能力不支持时会在进入 prepareRequest 前直接返回错误")
    void unsupportedAttachmentCapabilityReturnsErrorBeforePrepare() throws Exception {
        File file = File.createTempFile("unsupported-", ".pdf");
        try {
            Files.write(file.toPath(), "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

            AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
            AiProviderRegistry registry = new AiProviderRegistry();
            UnsupportedAttachmentAdapter adapter = new UnsupportedAttachmentAdapter();
            registry.register(AiProviderType.OPENAI, adapter);

            final boolean[] transportCalled = new boolean[]{false};
            AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> transportCalled[0] = true);

            List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
            service.streamChat(
                    "system",
                    "hello",
                    "history",
                    Collections.singletonList(AiAttachmentUtils.createAttachment(file)),
                    new AiThinkingConfig(false, 1024),
                    events::add
            );

            assertEquals(1, events.size());
            assertEquals(AiStreamEvent.Type.ERROR, events.get(0).getType());
            assertTrue(events.get(0).getContent().contains("不支持附件上传"));
            assertTrue(!transportCalled[0]);
        } finally {
            file.delete();
        }
    }

    @Test
    @DisplayName("不支持直传的纯文本附件会自动降级为上下文补充")
    void plainTextAttachmentFallsBackToInlineContext() throws Exception {
        File file = File.createTempFile("ai-inline-text-", ".blob");
        try {
            Files.write(file.toPath(), "alpha\nbeta\n".getBytes(StandardCharsets.UTF_8));

            AiConfigReader reader = new StubConfigReader(false, "https://gateway.example.com/v1");
            AiProviderRegistry registry = new AiProviderRegistry();
            CapturingQuestionAdapter adapter = new CapturingQuestionAdapter();
            registry.register(AiProviderType.OPENAI, adapter);

            AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> lineConsumer.accept("done"));
            service.streamChat(
                    "system",
                    "请分析",
                    "history",
                    Collections.singletonList(AiAttachmentUtils.createAttachment(file)),
                    new AiThinkingConfig(false, 1024),
                    event -> { }
            );

            assertEquals(1, adapter.prepareRequestCount);
            assertNotNull(adapter.lastAttachments);
            assertEquals(0, adapter.lastAttachments.size());
            assertTrue(adapter.lastQuestion.contains("纯文本附件上下文"));
            assertTrue(adapter.lastQuestion.contains(file.getName()));
            assertTrue(adapter.lastQuestion.contains("alpha"));
        } finally {
            file.delete();
        }
    }

    @Test
    @DisplayName("疑似二进制文本文件不会被错误降级为上下文")
    void binaryTextLikeAttachmentWillNotFallback() throws Exception {
        File file = File.createTempFile("ai-inline-binary-", ".txt");
        try {
            Files.write(file.toPath(), new byte[]{0x00, 0x01, 0x02, 0x03, 0x7F});

            AiConfigReader reader = new StubConfigReader(false, "https://gateway.example.com/v1");
            AiProviderRegistry registry = new AiProviderRegistry();
            CapturingQuestionAdapter adapter = new CapturingQuestionAdapter();
            registry.register(AiProviderType.OPENAI, adapter);

            List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
            AiChatService service = new AiChatService(reader, registry, (requestObj, streamSession, lineConsumer) -> lineConsumer.accept("done"));
            service.streamChat(
                    "system",
                    "请分析",
                    "history",
                    Collections.singletonList(AiAttachmentUtils.createAttachment(file)),
                    new AiThinkingConfig(false, 1024),
                    events::add
            );

            assertEquals(0, adapter.prepareRequestCount);
            assertEquals(1, events.size());
            assertEquals(AiStreamEvent.Type.ERROR, events.get(0).getType());
            assertTrue(events.get(0).getContent().contains("图片附件"));
        } finally {
            file.delete();
        }
    }

    private AiChatService createServiceWithError(boolean builtinAi, String baseUrl, String errorMessage) {
        AiConfigReader reader = new StubConfigReader(builtinAi, baseUrl);
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register(AiProviderType.OPENAI, new StubProviderAdapter());
        AiChatService.StreamTransport transport = (requestObj, streamSession, lineConsumer) -> {
            throw new RuntimeException(errorMessage);
        };
        return new AiChatService(reader, registry, transport);
    }

    private static class StubConfigReader extends AiConfigReader {
        private final boolean builtinAi;
        private final String baseUrl;

        private StubConfigReader(boolean builtinAi, String baseUrl) {
            this.builtinAi = builtinAi;
            this.baseUrl = baseUrl;
        }

        @Override
        public AiRuntimeConfig read() {
            JsonObject ai = new JsonObject();
            ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI");
            ai.addProperty(ConfigConstants.AI_BASE_URL, baseUrl);
            ai.addProperty(ConfigConstants.AI_API_KEY, "test-key");
            ai.addProperty(ConfigConstants.AI_MODEL_NAME, "test-model");

            AiRuntimeConfig config = read(ai, false);
            return new AiRuntimeConfig(
                    config.getProviderType(),
                    config.getBaseUrl(),
                    config.getApiKey(),
                    config.getModelName(),
                    config.getTimeoutMs(),
                    config.isUseProxy(),
                    config.getThinkingConfig(),
                    builtinAi
            );
        }
    }

    private static class StubProviderAdapter implements AiProviderAdapter {

        @Override
        public RequestObj buildRequest(AiRuntimeConfig runtimeConfig, com.potato.potatotool.utils.ai.model.AiChatRequest request) {
            return new RequestObj().setMethod("POST").setUrl(runtimeConfig.getBaseUrl());
        }

        @Override
        public List<AiStreamEvent> parseSseLine(String line) {
            List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
            if ("thinking".equals(line)) {
                events.add(AiStreamEvent.thinkingToken("think"));
            } else if (line.startsWith("token:")) {
                events.add(AiStreamEvent.token(line.substring("token:".length())));
            } else if ("done".equals(line)) {
                events.add(AiStreamEvent.done());
            }
            return events;
        }
    }

    private static class CapturingProviderAdapter extends StubProviderAdapter {
        private AiThinkingConfig lastThinkingConfig;

        @Override
        public RequestObj buildRequest(AiRuntimeConfig runtimeConfig,
                                       com.potato.potatotool.utils.ai.model.AiChatRequest request) {
            lastThinkingConfig = request.getThinkingConfig();
            return super.buildRequest(runtimeConfig, request);
        }
    }

    private static class PreparedRequestCapturingAdapter extends StubProviderAdapter {
        private int prepareRequestCount;
        private int cleanupCount;
        private List<AiAttachment> lastAttachments;

        @Override
        public AiProviderAdapter.PreparedRequest prepareRequest(AiRuntimeConfig runtimeConfig,
                                                                com.potato.potatotool.utils.ai.model.AiChatRequest request) {
            prepareRequestCount++;
            lastAttachments = request.getAttachments();
            RequestObj requestObj = new RequestObj().setMethod("POST").setUrl(runtimeConfig.getBaseUrl());
            return AiProviderAdapter.PreparedRequest.of(requestObj, () -> cleanupCount++);
        }

        @Override
        public AiAttachmentSupportResult resolveAttachmentSupport(AiRuntimeConfig runtimeConfig, List<AiAttachment> attachments) {
            return AiAttachmentSupportResult.supported(
                    AiAttachmentMode.REMOTE_UPLOAD_REFERENCE,
                    10,
                    100L * 1024L * 1024L,
                    300L * 1024L * 1024L
            );
        }
    }

    private static class FailingPrepareAdapter extends StubProviderAdapter {
        @Override
        public AiAttachmentSupportResult resolveAttachmentSupport(AiRuntimeConfig runtimeConfig, List<AiAttachment> attachments) {
            return AiAttachmentSupportResult.supported(
                    AiAttachmentMode.REMOTE_UPLOAD_REFERENCE,
                    10,
                    100L * 1024L * 1024L,
                    300L * 1024L * 1024L
            );
        }

        @Override
        public AiProviderAdapter.PreparedRequest prepareRequest(AiRuntimeConfig runtimeConfig,
                                                                com.potato.potatotool.utils.ai.model.AiChatRequest request) {
            throw new IllegalStateException("upload failed");
        }
    }

    private static class UnsupportedAttachmentAdapter extends StubProviderAdapter {
        @Override
        public AiAttachmentSupportResult resolveAttachmentSupport(AiRuntimeConfig runtimeConfig, List<AiAttachment> attachments) {
            return AiAttachmentSupportResult.unsupported("当前接口不支持附件上传", AiAttachmentMode.NONE, 10, 0L, 0L);
        }
    }

    private static class CapturingQuestionAdapter extends StubProviderAdapter {
        private int prepareRequestCount;
        private String lastQuestion;
        private List<AiAttachment> lastAttachments;

        @Override
        public AiProviderAdapter.PreparedRequest prepareRequest(AiRuntimeConfig runtimeConfig,
                                                                com.potato.potatotool.utils.ai.model.AiChatRequest request) {
            prepareRequestCount++;
            lastQuestion = request.getQuestion();
            lastAttachments = request.getAttachments();
            return AiProviderAdapter.PreparedRequest.of(new RequestObj().setMethod("POST").setUrl(runtimeConfig.getBaseUrl()));
        }
    }
}
