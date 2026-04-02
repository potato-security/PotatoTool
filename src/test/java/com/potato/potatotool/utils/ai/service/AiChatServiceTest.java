package com.potato.potatotool.utils.ai.service;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.ai.provider.AiProviderAdapter;
import com.potato.potatotool.utils.ai.provider.AiProviderRegistry;
import com.potato.potatotool.utils.network.RequestObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiChatService 测试")
class AiChatServiceTest {

    @Test
    @DisplayName("流式事件顺序与历史上限")
    void streamEventOrderAndHistoryLimit() {
        AiConfigReader reader = new StubConfigReader(false, "https://api.example.com/v1/chat/completions");
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register(AiProviderType.OPENAI_COMPATIBLE, new StubProviderAdapter());

        AiChatService.StreamTransport transport = (requestObj, lineConsumer) -> {
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

    private AiChatService createServiceWithError(boolean builtinAi, String baseUrl, String errorMessage) {
        AiConfigReader reader = new StubConfigReader(builtinAi, baseUrl);
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register(AiProviderType.OPENAI_COMPATIBLE, new StubProviderAdapter());
        AiChatService.StreamTransport transport = (requestObj, lineConsumer) -> {
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
            ai.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI_COMPATIBLE");
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
}
