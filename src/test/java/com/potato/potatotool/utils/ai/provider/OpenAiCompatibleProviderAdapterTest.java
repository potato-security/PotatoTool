package com.potato.potatotool.utils.ai.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OpenAiCompatibleProviderAdapter 测试")
class OpenAiCompatibleProviderAdapterTest {

    private final OpenAiCompatibleProviderAdapter adapter = new OpenAiCompatibleProviderAdapter();

    @Test
    @DisplayName("未知 OpenAI-compatible 中转不注入厂商专属 thinking 参数")
    void buildRequestDoesNotInjectThinkingForUnknownRelay() {
        JsonObject json = buildBody(
                runtimeConfig("https://relay.example.com/v1/chat/completions", "custom-model", false),
                new AiThinkingConfig(true, 2048)
        );

        assertEquals("custom-model", json.get("model").getAsString());
        assertTrue(json.get("stream").getAsBoolean());
        assertFalse(json.has("reasoning_effort"));
        assertFalse(json.has("reasoning"));
        assertFalse(json.has("thinking"));
        assertFalse(json.has("enable_thinking"));
        assertFalse(json.has("thinking_budget"));
        assertFalse(json.has("chat_template_kwargs"));
    }

    @Test
    @DisplayName("OpenAI 官方 reasoning 模型使用 reasoning_effort，非 reasoning 模型不注入")
    void buildRequestUsesOpenAiReasoningEffortOnlyForReasoningFamilies() {
        JsonObject enabled = buildBody(
                runtimeConfig("https://api.openai.com/v1/chat/completions", "gpt-5-future", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject disabled = buildBody(
                runtimeConfig("https://api.openai.com/v1/chat/completions", "gpt-5.1-future", false),
                new AiThinkingConfig(false, 2048)
        );
        JsonObject nonReasoning = buildBody(
                runtimeConfig("https://api.openai.com/v1/chat/completions", "gpt-4.1", false),
                new AiThinkingConfig(true, 2048)
        );

        assertEquals("medium", enabled.get("reasoning_effort").getAsString());
        assertEquals("none", disabled.get("reasoning_effort").getAsString());
        assertFalse(nonReasoning.has("reasoning_effort"));
    }

    @Test
    @DisplayName("OpenRouter host 优先使用 reasoning 对象并透传预算")
    void buildRequestUsesOpenRouterReasoningObjectByHost() {
        JsonObject enabled = buildBody(
                runtimeConfig("https://openrouter.ai/api/v1/chat/completions", "qwen-next", false),
                new AiThinkingConfig(true, 4096)
        );
        JsonObject disabled = buildBody(
                runtimeConfig("https://openrouter.ai/api/v1/chat/completions", "qwen-next", false),
                new AiThinkingConfig(false, 4096)
        );

        assertEquals(4096, enabled.getAsJsonObject("reasoning").get("max_tokens").getAsInt());
        assertFalse(enabled.has("chat_template_kwargs"));
        assertFalse(disabled.getAsJsonObject("reasoning").get("enabled").getAsBoolean());
        assertFalse(disabled.getAsJsonObject("reasoning").has("max_tokens"));
        assertTrue(adapter.supportsThinkingBudget(runtimeConfig("https://openrouter.ai/api/v1", "any-model", false)));
    }

    @Test
    @DisplayName("DashScope/Qwen Cloud host 使用 enable_thinking 与 thinking_budget")
    void buildRequestUsesQwenCloudThinkingByHost() {
        JsonObject enabled = buildBody(
                runtimeConfig("https://dashscope.aliyuncs.com/compatible-mode/v1", "model-from-alias", false),
                new AiThinkingConfig(true, 8192)
        );
        JsonObject disabled = buildBody(
                runtimeConfig("https://dashscope.aliyuncs.com/compatible-mode/v1", "model-from-alias", false),
                new AiThinkingConfig(false, 8192)
        );

        assertTrue(enabled.get("enable_thinking").getAsBoolean());
        assertEquals(8192, enabled.get("thinking_budget").getAsInt());
        assertFalse(disabled.get("enable_thinking").getAsBoolean());
        assertFalse(disabled.has("thinking_budget"));
        assertTrue(adapter.supportsThinkingBudget(runtimeConfig("https://dashscope.aliyuncs.com/compatible-mode/v1", "any-model", false)));
    }

    @Test
    @DisplayName("SiliconFlow host 使用 enable_thinking 并约束预算范围")
    void buildRequestUsesSiliconFlowThinkingBudgetByHost() {
        JsonObject lowBudget = buildBody(
                runtimeConfig("https://api.siliconflow.cn/v1/chat/completions", "provider/model", false),
                new AiThinkingConfig(true, 32)
        );
        JsonObject highBudget = buildBody(
                runtimeConfig("https://api.siliconflow.cn/v1/chat/completions", "provider/model", false),
                new AiThinkingConfig(true, 999999)
        );

        assertTrue(lowBudget.get("enable_thinking").getAsBoolean());
        assertEquals(128, lowBudget.get("thinking_budget").getAsInt());
        assertEquals(32768, highBudget.get("thinking_budget").getAsInt());
        assertTrue(adapter.supportsThinkingBudget(runtimeConfig("https://api.siliconflow.cn/v1", "provider/model", false)));
    }

    @Test
    @DisplayName("DeepSeek/Z.ai/BigModel host 使用 thinking.type")
    void buildRequestUsesThinkingTypeForDeepSeekAndGlmHosts() {
        JsonObject deepSeek = buildBody(
                runtimeConfig("https://api.deepseek.com", "custom-alias", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject zAi = buildBody(
                runtimeConfig("https://api.z.ai/api/paas/v4/chat/completions", "custom-alias", false),
                new AiThinkingConfig(false, 2048)
        );

        assertEquals("enabled", deepSeek.getAsJsonObject("thinking").get("type").getAsString());
        assertEquals("high", deepSeek.get("reasoning_effort").getAsString());
        assertEquals("disabled", zAi.getAsJsonObject("thinking").get("type").getAsString());
        assertFalse(zAi.has("reasoning_effort"));
    }

    @Test
    @DisplayName("AIHubMix/Groq/Together/Fireworks host 使用各自公开 reasoning 形态")
    void buildRequestUsesKnownRelayReasoningDialects() {
        JsonObject aihubmix = buildBody(
                runtimeConfig("https://aihubmix.com/v1/chat/completions", "any-model", false),
                new AiThinkingConfig(false, 2048)
        );
        JsonObject groqQwen = buildBody(
                runtimeConfig("https://api.groq.com/openai/v1/chat/completions", "Qwen/Qwen-Future", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject together = buildBody(
                runtimeConfig("https://api.together.xyz/v1/chat/completions", "Qwen/Qwen-Future", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject fireworks = buildBody(
                runtimeConfig("https://api.fireworks.ai/inference/v1/chat/completions", "accounts/provider/models/kimi-future", false),
                new AiThinkingConfig(false, 2048)
        );
        JsonObject togetherUnknown = buildBody(
                runtimeConfig("https://api.together.xyz/v1/chat/completions", "meta-llama/general-model", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject fireworksUnknown = buildBody(
                runtimeConfig("https://api.fireworks.ai/inference/v1/chat/completions", "accounts/provider/models/general-model", false),
                new AiThinkingConfig(true, 2048)
        );

        assertEquals("none", aihubmix.get("reasoning_effort").getAsString());
        assertEquals("default", groqQwen.get("reasoning_effort").getAsString());
        assertTrue(together.getAsJsonObject("reasoning").get("enabled").getAsBoolean());
        assertEquals("low", fireworks.get("reasoning_effort").getAsString());
        assertFalse(togetherUnknown.has("reasoning"));
        assertFalse(fireworksUnknown.has("reasoning_effort"));
    }

    @Test
    @DisplayName("model 兜底按家族匹配，避免锁死具体版本")
    void buildRequestFallsBackByModelFamily() {
        JsonObject qwen = buildBody(
                runtimeConfig("https://localhost:8000/v1/chat/completions", "Qwen/Qwen-Future", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject glm = buildBody(
                runtimeConfig("https://custom-relay.example.com/v1/chat/completions", "glm-future", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject deepSeekLocal = buildBody(
                runtimeConfig("http://127.0.0.1:8000/v1/chat/completions", "deepseek-future", false),
                new AiThinkingConfig(false, 2048)
        );

        assertTrue(qwen.getAsJsonObject("chat_template_kwargs").get("enable_thinking").getAsBoolean());
        assertEquals(2048, qwen.getAsJsonObject("chat_template_kwargs").get("thinking_budget").getAsInt());
        assertEquals("enabled", glm.getAsJsonObject("thinking").get("type").getAsString());
        assertFalse(deepSeekLocal.getAsJsonObject("chat_template_kwargs").get("thinking").getAsBoolean());
        assertFalse(deepSeekLocal.getAsJsonObject("chat_template_kwargs").has("thinking_budget"));
        assertFalse(qwen.has("reasoning_effort"));
    }

    @Test
    @DisplayName("host 与 model 为 OR 关系，host 命中时优先于模型族")
    void resolveThinkingDialectUsesHostOrModelWithHostPriority() {
        JsonObject hostHit = buildBody(
                runtimeConfig("https://dashscope.aliyuncs.com/compatible-mode/v1", "not-qwen-alias", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject modelHit = buildBody(
                runtimeConfig("https://custom-relay.example.com/v1/chat/completions", "qwen-future", false),
                new AiThinkingConfig(true, 2048)
        );
        JsonObject hostPriority = buildBody(
                runtimeConfig("https://openrouter.ai/api/v1/chat/completions", "qwen-future", false),
                new AiThinkingConfig(true, 2048)
        );

        assertTrue(hostHit.get("enable_thinking").getAsBoolean());
        assertTrue(modelHit.getAsJsonObject("chat_template_kwargs").get("enable_thinking").getAsBoolean());
        assertTrue(hostPriority.has("reasoning"));
        assertFalse(hostPriority.has("chat_template_kwargs"));
    }

    @Test
    @DisplayName("内置 AI 开启 thinking 时透传 true 和预算")
    void buildBuiltinGatewayRequestPassesEnabledThinkingAndBudget() {
        AiRuntimeConfig config = runtimeConfig(
                "https://gateway.example.com/pt-ai/v1/chat/completions",
                "potato-default",
                true
        );

        RequestObj requestObj = adapter.buildRequest(config, new AiChatRequest("test", null, new AiThinkingConfig(true, 4096), true));
        JsonObject json = parseBody(requestObj);

        assertTrue(requestObj.isInternalAiRequest());
        assertTrue(adapter.supportsThinking(config));
        assertTrue(adapter.supportsThinkingBudget(config));
        assertTrue(json.getAsJsonObject("chat_template_kwargs").get("enable_thinking").getAsBoolean());
        assertEquals(4096, json.getAsJsonObject("chat_template_kwargs").get("thinking_budget").getAsInt());
        assertFalse(json.has("reasoning_effort"));
        assertFalse(json.has("enable_thinking"));
        assertFalse(json.has("thinking_budget"));
    }

    @Test
    @DisplayName("内置 AI 关闭 thinking 时透传 false 且不透传预算")
    void buildBuiltinGatewayRequestPassesDisabledThinkingWithoutBudget() {
        AiRuntimeConfig config = runtimeConfig(
                "https://gateway.example.com/pt-ai/v1/chat/completions",
                "potato-default",
                true
        );

        RequestObj requestObj = adapter.buildRequest(config, new AiChatRequest("test", null, new AiThinkingConfig(false, 4096), true));
        JsonObject json = parseBody(requestObj);
        JsonObject chatTemplateKwargs = json.getAsJsonObject("chat_template_kwargs");

        assertTrue(requestObj.isInternalAiRequest());
        assertTrue(adapter.supportsThinking(config));
        assertTrue(adapter.supportsThinkingBudget(config));
        assertFalse(chatTemplateKwargs.get("enable_thinking").getAsBoolean());
        assertFalse(chatTemplateKwargs.has("thinking_budget"));
        assertFalse(json.has("reasoning_effort"));
        assertFalse(json.has("enable_thinking"));
        assertFalse(json.has("thinking_budget"));
        assertFalse(json.has("thinking"));
        assertFalse(json.has("reasoning"));
    }

    @Test
    @DisplayName("thinking 关闭态与预算传输按各协议矩阵保持一致")
    void buildRequestKeepsThinkingDisableAndBudgetContract() {
        JsonObject qwenCloudOff = buildBody(
                runtimeConfig("https://dashscope.aliyuncs.com/compatible-mode/v1", "model-from-alias", false),
                new AiThinkingConfig(false, 8192)
        );
        JsonObject siliconFlowOff = buildBody(
                runtimeConfig("https://api.siliconflow.cn/v1/chat/completions", "provider/model", false),
                new AiThinkingConfig(false, 8192)
        );
        JsonObject zAiOff = buildBody(
                runtimeConfig("https://api.z.ai/api/paas/v4/chat/completions", "custom-alias", false),
                new AiThinkingConfig(false, 8192)
        );
        JsonObject qwenLocalOff = buildBody(
                runtimeConfig("https://localhost:8000/v1/chat/completions", "qwen-future", false),
                new AiThinkingConfig(false, 8192)
        );
        JsonObject togetherOff = buildBody(
                runtimeConfig("https://api.together.xyz/v1/chat/completions", "Qwen/Qwen-Future", false),
                new AiThinkingConfig(false, 8192)
        );

        assertFalse(qwenCloudOff.get("enable_thinking").getAsBoolean());
        assertFalse(qwenCloudOff.has("thinking_budget"));

        assertFalse(siliconFlowOff.get("enable_thinking").getAsBoolean());
        assertFalse(siliconFlowOff.has("thinking_budget"));

        assertEquals("disabled", zAiOff.getAsJsonObject("thinking").get("type").getAsString());
        assertFalse(zAiOff.has("reasoning_effort"));

        assertFalse(qwenLocalOff.getAsJsonObject("chat_template_kwargs").get("enable_thinking").getAsBoolean());
        assertFalse(qwenLocalOff.getAsJsonObject("chat_template_kwargs").has("thinking_budget"));

        assertFalse(togetherOff.getAsJsonObject("reasoning").get("enabled").getAsBoolean());
        assertFalse(togetherOff.getAsJsonObject("reasoning").has("max_tokens"));
    }

    @Test
    @DisplayName("OpenAI-compatible 请求会把常见 base_url 归一化为聊天端点")
    void buildRequestNormalizesCommonBaseUrls() {
        AiRuntimeConfig deepSeekConfig = runtimeConfig("https://api.deepseek.com", "deepseek-chat", false);
        AiRuntimeConfig gatewayConfig = runtimeConfig("https://gateway.example.com/v1", "gpt-4.1", false);

        RequestObj deepSeekRequest = adapter.buildRequest(deepSeekConfig, new AiChatRequest("test", null, new AiThinkingConfig(false, 1024), true));
        RequestObj gatewayRequest = adapter.buildRequest(gatewayConfig, new AiChatRequest("test", null, new AiThinkingConfig(false, 1024), true));

        assertEquals("https://api.deepseek.com/chat/completions", deepSeekRequest.getUrl());
        assertEquals("https://gateway.example.com/v1/chat/completions", gatewayRequest.getUrl());
    }

    @Test
    @DisplayName("解析 SSE token thinking done error")
    void parseSseLineCoversMainCases() {
        List<AiStreamEvent> thinkingEvents = adapter.parseSseLine("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"思考\"},\"finish_reason\":null}]}");
        assertEquals(1, thinkingEvents.size());
        assertEquals(AiStreamEvent.Type.THINKING_TOKEN, thinkingEvents.get(0).getType());

        List<AiStreamEvent> reasoningEvents = adapter.parseSseLine("data: {\"choices\":[{\"delta\":{\"reasoning\":\"推理\"},\"finish_reason\":null}]}");
        assertEquals(1, reasoningEvents.size());
        assertEquals(AiStreamEvent.Type.THINKING_TOKEN, reasoningEvents.get(0).getType());

        List<AiStreamEvent> reasoningDetailEvents = adapter.parseSseLine("data: {\"choices\":[{\"delta\":{\"reasoning_details\":[{\"type\":\"reasoning.text\",\"text\":\"详细\"}]},\"finish_reason\":null}]}");
        assertEquals(1, reasoningDetailEvents.size());
        assertEquals(AiStreamEvent.Type.THINKING_TOKEN, reasoningDetailEvents.get(0).getType());

        List<AiStreamEvent> reasoningDetailObjectEvents = adapter.parseSseLine("data: {\"choices\":[{\"delta\":{\"reasoning_details\":{\"thinking\":\"对象\"}},\"finish_reason\":null}]}");
        assertEquals(1, reasoningDetailObjectEvents.size());
        assertEquals(AiStreamEvent.Type.THINKING_TOKEN, reasoningDetailObjectEvents.get(0).getType());

        List<AiStreamEvent> tokenEvents = adapter.parseSseLine("data: {\"choices\":[{\"delta\":{\"content\":\"答案\"},\"finish_reason\":null}]}");
        assertEquals(1, tokenEvents.size());
        assertEquals(AiStreamEvent.Type.TOKEN, tokenEvents.get(0).getType());

        List<AiStreamEvent> doneEvents = adapter.parseSseLine("data: [DONE]");
        assertEquals(1, doneEvents.size());
        assertEquals(AiStreamEvent.Type.DONE, doneEvents.get(0).getType());

        List<AiStreamEvent> errorEvents = adapter.parseSseLine("data: {\"error\":{\"message\":\"bad request\"}}");
        assertEquals(1, errorEvents.size());
        assertEquals(AiStreamEvent.Type.ERROR, errorEvents.get(0).getType());
        assertTrue(errorEvents.get(0).getContent().contains("bad request"));
    }

    @Test
    @DisplayName("OpenAI 官方与中转均优先尝试远程上传")
    void resolveAttachmentSupportForOfficialAndRelay() {
        AiRuntimeConfig officialConfig = runtimeConfig("https://api.openai.com/v1/chat/completions", "gpt-4.1", false);
        AiRuntimeConfig customGatewayConfig = runtimeConfig("https://gateway.example.com/v1/chat/completions", "gpt-4.1", false);
        AiAttachment imageAttachment = new AiAttachment("screen.png", "/tmp/screen.png", 1024L, "image/png");
        AiAttachment pdfAttachment = new AiAttachment("report.pdf", "/tmp/report.pdf", 1024L, "application/pdf");

        AiAttachmentSupportResult officialSupport = adapter.resolveAttachmentSupport(officialConfig, Collections.singletonList(pdfAttachment));
        AiAttachmentSupportResult relayImageSupport = adapter.resolveAttachmentSupport(customGatewayConfig, Collections.singletonList(imageAttachment));
        AiAttachmentSupportResult relayPdfSupport = adapter.resolveAttachmentSupport(customGatewayConfig, Collections.singletonList(pdfAttachment));

        assertTrue(officialSupport.isSupported());
        assertEquals(AiAttachmentMode.REMOTE_UPLOAD_REFERENCE, officialSupport.getMode());
        assertTrue(relayImageSupport.isSupported());
        assertEquals(AiAttachmentMode.REMOTE_UPLOAD_REFERENCE, relayImageSupport.getMode());
        assertTrue(relayPdfSupport.isSupported());
        assertEquals(AiAttachmentMode.REMOTE_UPLOAD_REFERENCE, relayPdfSupport.getMode());
    }

    @Test
    @DisplayName("非官方 OpenAI 中转会把图片转成 inline image_url")
    void prepareRequestForRelayUsesInlineImageUrl() throws Exception {
        File imageFile = File.createTempFile("openai-inline-", ".png");
        imageFile.deleteOnExit();
        Files.write(imageFile.toPath(), new byte[]{1, 2, 3, 4});

        AiRuntimeConfig config = runtimeConfig("https://gateway.example.com/api/v1/chat/completions", "gpt-4.1", false);
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

        assertEquals("image_url",
                json.getAsJsonArray("messages")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("content")
                        .get(1)
                        .getAsJsonObject()
                        .get("type")
                        .getAsString());
        assertTrue(body.contains("data:image/png;base64,"));
    }

    private JsonObject buildBody(AiRuntimeConfig config, AiThinkingConfig thinkingConfig) {
        return parseBody(adapter.buildRequest(
                config,
                new AiChatRequest(
                        "你好",
                        Arrays.asList(new AiMessage("user", "历史问题"), new AiMessage("assistant", "历史回答")),
                        thinkingConfig,
                        true
                )
        ));
    }

    private JsonObject parseBody(RequestObj requestObj) {
        return JsonParser.parseString(new String(requestObj.getPostData(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private AiRuntimeConfig runtimeConfig(String baseUrl, String modelName, boolean builtinAi) {
        return new AiRuntimeConfig(
                AiProviderType.OPENAI,
                baseUrl,
                "key",
                modelName,
                60000,
                false,
                new AiThinkingConfig(false, 1024),
                builtinAi
        );
    }
}
