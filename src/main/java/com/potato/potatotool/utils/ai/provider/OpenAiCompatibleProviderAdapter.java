package com.potato.potatotool.utils.ai.provider;

import com.potato.potatotool.utils.ai.AiAttachmentFallbackSupport;
import com.potato.potatotool.utils.ai.AiAttachmentSupportResolver;
import com.potato.potatotool.utils.ai.AiAttachmentUtils;
import com.potato.potatotool.utils.ai.AiRequestTimeouts;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiMessage;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.ai.transport.AiHttpExecutor;
import com.potato.potatotool.utils.core.I18nTextUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenAiCompatibleProviderAdapter implements AiProviderAdapter {
    private static final String OPENAI_USER_DATA_PURPOSE = "user_data";
    private static final String MODEL_ALIAS_HEADER = "X-Potato-Model-Alias";
    private static final String ACCEPT_SSE = "text/event-stream";
    private static final String DEFAULT_REASONING_EFFORT = "medium";
    private static final String LOW_REASONING_EFFORT = "low";
    private static final String DEEPSEEK_REASONING_EFFORT = "high";
    private static final int SILICONFLOW_MIN_THINKING_BUDGET = 128;
    private static final int SILICONFLOW_MAX_THINKING_BUDGET = 32768;

    @Override
    public PreparedRequest prepareRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request) throws Exception {
        List<AiAttachment> attachments = request.getAttachments();
        if (attachments == null || attachments.isEmpty()) {
            return PreparedRequest.of(buildRequest(runtimeConfig, request));
        }

        AiAttachmentSupportResult supportResult = resolveAttachmentSupport(runtimeConfig, attachments);
        if (!supportResult.isSupported()) {
            throw new IllegalArgumentException(supportResult.getMessage());
        }
        if (supportResult.getMode() == AiAttachmentMode.INLINE_IMAGE) {
            return PreparedRequest.of(buildRequest(runtimeConfig, request, null, attachments));
        }

        List<UploadedOpenAiFile> uploadedFiles = new ArrayList<UploadedOpenAiFile>();
        try {
            for (AiAttachment attachment : attachments) {
                uploadedFiles.add(uploadAttachment(runtimeConfig, attachment));
            }
            RequestObj requestObj = buildRequest(runtimeConfig, request, uploadedFiles, null);
            return PreparedRequest.of(requestObj, () -> deleteUploadedFiles(runtimeConfig, uploadedFiles));
        } catch (Exception e) {
            deleteUploadedFiles(runtimeConfig, uploadedFiles);
            PreparedRequest fallbackRequest = buildUploadFallbackRequest(runtimeConfig, request, attachments, e);
            if (fallbackRequest != null) {
                return fallbackRequest;
            }
            throw e;
        }
    }

    @Override
    public RequestObj buildRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request) {
        return buildRequest(runtimeConfig, request, null, null);
    }

    private RequestObj buildRequest(AiRuntimeConfig runtimeConfig,
                                    AiChatRequest request,
                                    List<UploadedOpenAiFile> uploadedFiles,
                                    List<AiAttachment> inlineImageAttachments) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + runtimeConfig.getApiKey());
        if (request.isStream()) {
            headers.put("Accept", ACCEPT_SSE);
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", runtimeConfig.getModelName());
        body.addProperty("stream", request.isStream());

        JsonArray messages = new JsonArray();
        if (!request.getSystemPrompt().trim().isEmpty()) {
            messages.add(createMessage("system", request.getSystemPrompt()));
        }
        for (AiMessage item : request.getHistory()) {
            messages.add(createMessage(item.getRole(), item.getContent()));
        }
        messages.add(createUserMessage(request.getQuestion(), uploadedFiles, inlineImageAttachments));
        body.add("messages", messages);
        applyThinkingConfig(runtimeConfig, request, body);

        RequestObj requestObj = AiRequestTimeouts.apply(new RequestObj()
                .setMethod("POST")
                .setPostMethod("JSON")
                .setUrl(resolveRequestUrl(runtimeConfig.getBaseUrl()))
                .setHeaders(headers)
                .setPostData(new Gson().toJson(body))
                .setInternalAiRequest(runtimeConfig.isBuiltinAi()), runtimeConfig, request.isStream());

        ProxyUtils.applyProxy(requestObj, runtimeConfig.isUseProxy());
        return requestObj;
    }

    @Override
    public boolean supportsThinking(AiRuntimeConfig runtimeConfig) {
        return resolveThinkingDialect(runtimeConfig) != ThinkingDialect.NONE;
    }

    @Override
    public boolean supportsThinkingBudget(AiRuntimeConfig runtimeConfig) {
        return resolveThinkingDialect(runtimeConfig).supportsBudget();
    }

    @Override
    public List<AiStreamEvent> parseSseLine(String line) {
        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        if (line == null || line.trim().isEmpty()) {
            return events;
        }

        String transportError = resolveTransportErrorMessage(line);
        if (!transportError.isEmpty()) {
            events.add(AiStreamEvent.error(transportError));
            return events;
        }

        String payload = line;
        if (payload.startsWith(":") || payload.startsWith("event:")) {
            return events;
        }
        if (payload.startsWith("data:")) {
            payload = payload.substring(5).trim();
        }

        if ("[DONE]".equals(payload)) {
            events.add(AiStreamEvent.done());
            return events;
        }

        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            if (json.has("error") && json.get("error").isJsonObject()) {
                JsonObject error = json.getAsJsonObject("error");
                String message = safeString(error, "message");
                if (message.isEmpty()) {
                    message = I18nTextUtils.getString("ai.status.error");
                }
                events.add(AiStreamEvent.error(message));
                return events;
            }

            JsonArray choices = json.has("choices") && json.get("choices").isJsonArray()
                    ? json.getAsJsonArray("choices") : new JsonArray();
            if (choices.size() == 0) {
                return events;
            }

            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            if (firstChoice.has("delta") && firstChoice.get("delta").isJsonObject()) {
                JsonObject delta = firstChoice.getAsJsonObject("delta");

                String thinkingText = readThinkingText(delta);
                if (!thinkingText.isEmpty()) {
                    events.add(AiStreamEvent.thinkingToken(thinkingText));
                }

                String token = safeString(delta, "content");
                if (!token.isEmpty()) {
                    events.add(AiStreamEvent.token(token));
                }
            }

            if (firstChoice.has("finish_reason") && !firstChoice.get("finish_reason").isJsonNull()) {
                events.add(AiStreamEvent.done());
            }
        } catch (Exception e) {
            events.add(AiStreamEvent.error(I18nTextUtils.getString("ai.error.stream.parse", e.getMessage())));
        }
        return events;
    }

    @Override
    public String parseResponse(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "";
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            if (json.has("error") && json.get("error").isJsonObject()) {
                JsonObject error = json.getAsJsonObject("error");
                String message = safeString(error, "message");
                return message.isEmpty() ? I18nTextUtils.getString("ai.status.error") : message;
            }

            JsonArray choices = json.has("choices") && json.get("choices").isJsonArray()
                    ? json.getAsJsonArray("choices") : new JsonArray();
            if (choices.size() == 0 || !choices.get(0).isJsonObject()) {
                return "";
            }
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            if (!firstChoice.has("message") || !firstChoice.get("message").isJsonObject()) {
                return "";
            }
            JsonObject message = firstChoice.getAsJsonObject("message");
            return extractMessageText(message);
        } catch (Exception e) {
            return I18nTextUtils.getString("ai.error.response.parse", e.getMessage());
        }
    }

    private JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content == null ? "" : content);
        return message;
    }

    private JsonObject createUserMessage(String question,
                                         List<UploadedOpenAiFile> uploadedFiles,
                                         List<AiAttachment> inlineImageAttachments) {
        if ((uploadedFiles == null || uploadedFiles.isEmpty())
                && (inlineImageAttachments == null || inlineImageAttachments.isEmpty())) {
            return createMessage("user", question);
        }

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        JsonArray content = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", question == null ? "" : question);
        content.add(textPart);

        if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
            for (UploadedOpenAiFile uploadedFile : uploadedFiles) {
                if (uploadedFile == null) {
                    continue;
                }
                JsonObject filePart = new JsonObject();
                filePart.addProperty("type", "file");

                JsonObject file = new JsonObject();
                file.addProperty("file_id", uploadedFile.getFileId());
                filePart.add("file", file);
                content.add(filePart);
            }
        } else {
            for (AiAttachment attachment : inlineImageAttachments) {
                if (attachment == null) {
                    continue;
                }
                JsonObject imagePart = new JsonObject();
                imagePart.addProperty("type", "image_url");

                JsonObject imageUrl = new JsonObject();
                try {
                    imageUrl.addProperty("url", AiAttachmentUtils.readDataUrl(attachment));
                } catch (Exception e) {
                    throw new IllegalStateException(
                            I18nTextUtils.getString("ai.attach.read.failed", attachment.getFileName()),
                            e
                    );
                }
                imagePart.add("image_url", imageUrl);
                content.add(imagePart);
            }
        }

        message.add("content", content);
        return message;
    }

    private void applyThinkingConfig(AiRuntimeConfig runtimeConfig, AiChatRequest request, JsonObject body) {
        AiThinkingConfig thinkingConfig = request == null ? null : request.getThinkingConfig();
        if (thinkingConfig == null) {
            return;
        }

        ThinkingDialect dialect = resolveThinkingDialect(runtimeConfig);
        switch (dialect) {
            case OPENAI_REASONING_EFFORT:
                body.addProperty("reasoning_effort", resolveOpenAiReasoningEffort(runtimeConfig, thinkingConfig));
                break;
            case REASONING_EFFORT_NONE:
                body.addProperty("reasoning_effort", thinkingConfig.isEnabled()
                        ? DEFAULT_REASONING_EFFORT
                        : "none");
                break;
            case REASONING_EFFORT_LOW:
                body.addProperty("reasoning_effort", thinkingConfig.isEnabled()
                        ? DEFAULT_REASONING_EFFORT
                        : LOW_REASONING_EFFORT);
                break;
            case GROQ_QWEN_REASONING_EFFORT:
                body.addProperty("reasoning_effort", thinkingConfig.isEnabled() ? "default" : "none");
                break;
            case OPENROUTER_REASONING:
                JsonObject reasoning = new JsonObject();
                if (thinkingConfig.isEnabled()) {
                    if (thinkingConfig.getBudgetTokens() > 0) {
                        reasoning.addProperty("max_tokens", thinkingConfig.getBudgetTokens());
                    } else {
                        reasoning.addProperty("effort", DEFAULT_REASONING_EFFORT);
                    }
                } else {
                    reasoning.addProperty("enabled", false);
                }
                body.add("reasoning", reasoning);
                break;
            case REASONING_ENABLED:
                JsonObject reasoningEnabled = new JsonObject();
                reasoningEnabled.addProperty("enabled", thinkingConfig.isEnabled());
                body.add("reasoning", reasoningEnabled);
                break;
            case QWEN_CLOUD:
                body.addProperty("enable_thinking", thinkingConfig.isEnabled());
                if (thinkingConfig.isEnabled() && thinkingConfig.getBudgetTokens() > 0) {
                    body.addProperty("thinking_budget", thinkingConfig.getBudgetTokens());
                }
                break;
            case SILICONFLOW:
                body.addProperty("enable_thinking", thinkingConfig.isEnabled());
                if (thinkingConfig.isEnabled() && thinkingConfig.getBudgetTokens() > 0) {
                    body.addProperty("thinking_budget", clamp(
                            thinkingConfig.getBudgetTokens(),
                            SILICONFLOW_MIN_THINKING_BUDGET,
                            SILICONFLOW_MAX_THINKING_BUDGET
                    ));
                }
                break;
            case DEEPSEEK_THINKING:
                addThinkingType(body, thinkingConfig.isEnabled());
                if (thinkingConfig.isEnabled()) {
                    body.addProperty("reasoning_effort", DEEPSEEK_REASONING_EFFORT);
                }
                break;
            case GLM_THINKING:
                addThinkingType(body, thinkingConfig.isEnabled());
                break;
            case QWEN_CHAT_TEMPLATE:
                addChatTemplateThinking(body, "enable_thinking", thinkingConfig, true);
                break;
            case CHAT_TEMPLATE_THINKING:
                addChatTemplateThinking(body, "thinking", thinkingConfig, true);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void addThinkingType(JsonObject body, boolean enabled) {
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", enabled ? "enabled" : "disabled");
        body.add("thinking", thinking);
    }

    private void addChatTemplateThinking(JsonObject body,
                                         String key,
                                         AiThinkingConfig thinkingConfig,
                                         boolean supportsBudget) {
        JsonObject chatTemplateKwargs = new JsonObject();
        chatTemplateKwargs.addProperty(key, thinkingConfig.isEnabled());
        if (thinkingConfig.isEnabled() && supportsBudget && thinkingConfig.getBudgetTokens() > 0) {
            chatTemplateKwargs.addProperty("thinking_budget", thinkingConfig.getBudgetTokens());
        }
        body.add("chat_template_kwargs", chatTemplateKwargs);
    }

    private String resolveOpenAiReasoningEffort(AiRuntimeConfig runtimeConfig, AiThinkingConfig thinkingConfig) {
        if (thinkingConfig.isEnabled()) {
            return DEFAULT_REASONING_EFFORT;
        }
        return supportsOpenAiNoneReasoning(runtimeConfig == null ? "" : runtimeConfig.getModelName())
                ? "none"
                : LOW_REASONING_EFFORT;
    }

    private ThinkingDialect resolveThinkingDialect(AiRuntimeConfig runtimeConfig) {
        String host = normalizeHost(runtimeConfig == null ? "" : runtimeConfig.getBaseUrl());
        String model = normalizeModel(runtimeConfig == null ? "" : runtimeConfig.getModelName());

        if (runtimeConfig != null && runtimeConfig.isBuiltinAi()) {
            return ThinkingDialect.QWEN_CHAT_TEMPLATE;
        }
        if (hostMatches(host, "openrouter.ai")) {
            return ThinkingDialect.OPENROUTER_REASONING;
        }
        if (hostContains(host, "aihubmix")) {
            return ThinkingDialect.REASONING_EFFORT_NONE;
        }
        if (hostContains(host, "fireworks.ai")) {
            return isKnownFireworksReasoningModel(model) ? ThinkingDialect.REASONING_EFFORT_LOW : ThinkingDialect.NONE;
        }
        if (hostContains(host, "groq.com")) {
            if (isQwenModel(model)) {
                return ThinkingDialect.GROQ_QWEN_REASONING_EFFORT;
            }
            if (isGptOssModel(model)) {
                return ThinkingDialect.OPENAI_REASONING_EFFORT;
            }
            return ThinkingDialect.NONE;
        }
        if (hostContains(host, "together.ai") || hostContains(host, "together.xyz")) {
            if (isGptOssModel(model) || isDeepSeekV4ProModel(model)) {
                return ThinkingDialect.OPENAI_REASONING_EFFORT;
            }
            return isTogetherHybridReasoningModel(model) ? ThinkingDialect.REASONING_ENABLED : ThinkingDialect.NONE;
        }
        if (hostMatches(host, "api.x.ai")) {
            return ThinkingDialect.OPENAI_REASONING_EFFORT;
        }
        if (hostContains(host, "siliconflow")) {
            return ThinkingDialect.SILICONFLOW;
        }
        if (hostContains(host, "dashscope") || hostContains(host, "qwencloud")) {
            return ThinkingDialect.QWEN_CLOUD;
        }
        if (hostContains(host, "modelscope") && isQwenModel(model)) {
            return ThinkingDialect.QWEN_CLOUD;
        }
        if (hostContains(host, "deepseek")) {
            return ThinkingDialect.DEEPSEEK_THINKING;
        }
        if (hostMatches(host, "api.z.ai")
                || hostContains(host, "bigmodel")
                || hostContains(host, "zhipuai")) {
            return ThinkingDialect.GLM_THINKING;
        }
        if (hostMatches(host, "api.openai.com")
                || hostMatches(host, "openai.azure.com")
                || hostMatches(host, "cognitiveservices.azure.com")) {
            return isOpenAiReasoningModel(model) ? ThinkingDialect.OPENAI_REASONING_EFFORT : ThinkingDialect.NONE;
        }

        if (isQwenModel(model)) {
            return ThinkingDialect.QWEN_CHAT_TEMPLATE;
        }
        if (isGlmModel(model)) {
            return isLocalHost(host) ? ThinkingDialect.CHAT_TEMPLATE_THINKING : ThinkingDialect.GLM_THINKING;
        }
        if (isDeepSeekModel(model)) {
            return isLocalHost(host) ? ThinkingDialect.CHAT_TEMPLATE_THINKING : ThinkingDialect.DEEPSEEK_THINKING;
        }
        if (isOpenAiReasoningModel(model)) {
            return ThinkingDialect.OPENAI_REASONING_EFFORT;
        }

        return ThinkingDialect.NONE;
    }

    private boolean isQwenModel(String model) {
        return modelMatchesFamily(model, "qwen") || modelMatchesFamily(model, "qwq");
    }

    private boolean isGlmModel(String model) {
        return modelMatchesFamily(model, "glm")
                || modelMatchesFamily(model, "chatglm")
                || modelContainsFamily(model, "thudm")
                || modelContainsFamily(model, "zai-org");
    }

    private boolean isDeepSeekModel(String model) {
        return modelMatchesFamily(model, "deepseek") || modelContainsFamily(model, "deepseek-ai");
    }

    private boolean isMiniMaxModel(String model) {
        return modelMatchesFamily(model, "minimax") || modelContainsFamily(model, "minimaxai");
    }

    private boolean isKimiModel(String model) {
        return modelMatchesFamily(model, "kimi")
                || modelContainsFamily(model, "moonshot")
                || modelContainsFamily(model, "moonshotai");
    }

    private boolean isGemmaModel(String model) {
        return modelMatchesFamily(model, "gemma") || modelContainsFamily(model, "google/gemma");
    }

    private boolean isDeepSeekV4ProModel(String model) {
        String leaf = modelLeaf(model);
        return leaf.startsWith("deepseek-v4") && leaf.indexOf("pro") >= 0;
    }

    private boolean isKnownFireworksReasoningModel(String model) {
        return isGptOssModel(model)
                || isMiniMaxModel(model)
                || isKimiModel(model)
                || isQwenModel(model)
                || isDeepSeekModel(model);
    }

    private boolean isTogetherHybridReasoningModel(String model) {
        return isQwenModel(model)
                || isKimiModel(model)
                || isGlmModel(model)
                || isGemmaModel(model)
                || (isDeepSeekModel(model) && !isDeepSeekV4ProModel(model));
    }

    private boolean isGptOssModel(String model) {
        return modelMatchesFamily(model, "gpt-oss") || modelContainsFamily(model, "openai/gpt-oss");
    }

    private boolean isOpenAiReasoningModel(String model) {
        return isGptOssModel(model) || modelMatchesOpenAiOFamily(model) || modelMatchesOpenAiGptReasoningFamily(model);
    }

    private boolean modelMatchesOpenAiOFamily(String model) {
        String leaf = modelLeaf(model);
        return leaf.length() >= 2 && leaf.charAt(0) == 'o' && Character.isDigit(leaf.charAt(1));
    }

    private boolean modelMatchesOpenAiGptReasoningFamily(String model) {
        String leaf = modelLeaf(model);
        if (!leaf.startsWith("gpt-") || leaf.length() <= 4 || !Character.isDigit(leaf.charAt(4))) {
            return false;
        }
        int index = 4;
        int major = 0;
        while (index < leaf.length() && Character.isDigit(leaf.charAt(index))) {
            major = major * 10 + Character.digit(leaf.charAt(index), 10);
            index++;
        }
        return major >= 5;
    }

    private boolean supportsOpenAiNoneReasoning(String model) {
        String leaf = modelLeaf(model);
        if (!leaf.startsWith("gpt-") || leaf.length() <= 4 || !Character.isDigit(leaf.charAt(4))) {
            return false;
        }
        int index = 4;
        int major = 0;
        while (index < leaf.length() && Character.isDigit(leaf.charAt(index))) {
            major = major * 10 + Character.digit(leaf.charAt(index), 10);
            index++;
        }
        if (major > 5) {
            return true;
        }
        if (major < 5 || index >= leaf.length() || leaf.charAt(index) != '.') {
            return false;
        }
        index++;
        int minor = 0;
        boolean hasMinor = false;
        while (index < leaf.length() && Character.isDigit(leaf.charAt(index))) {
            minor = minor * 10 + Character.digit(leaf.charAt(index), 10);
            hasMinor = true;
            index++;
        }
        return hasMinor && minor >= 1;
    }

    private boolean modelMatchesFamily(String model, String family) {
        String leaf = modelLeaf(model);
        return startsWithFamily(leaf, family) || startsWithFamily(model, family);
    }

    private boolean modelContainsFamily(String model, String family) {
        if (model == null || model.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeModel(model);
        String normalizedFamily = normalizeModel(family);
        return normalized.equals(normalizedFamily)
                || normalized.indexOf("/" + normalizedFamily) >= 0
                || normalized.indexOf(normalizedFamily + "/") >= 0
                || normalized.indexOf("-" + normalizedFamily + "-") >= 0;
    }

    private boolean startsWithFamily(String value, String family) {
        if (value == null || family == null) {
            return false;
        }
        if (value.equals(family)) {
            return true;
        }
        if (!value.startsWith(family) || value.length() == family.length()) {
            return false;
        }
        char next = value.charAt(family.length());
        return !Character.isLetter(next);
    }

    private String modelLeaf(String model) {
        String normalized = normalizeModel(model);
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private String normalizeModel(String model) {
        return model == null ? "" : model.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String normalizeHost(String url) {
        String host = resolveHost(url);
        return host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hostMatches(String host, String domain) {
        if (host == null || domain == null) {
            return false;
        }
        String normalizedDomain = domain.toLowerCase(Locale.ROOT);
        return host.equals(normalizedDomain) || host.endsWith("." + normalizedDomain);
    }

    private boolean hostContains(String host, String needle) {
        return host != null && needle != null && host.indexOf(needle.toLowerCase(Locale.ROOT)) >= 0;
    }

    private boolean isLocalHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        return "localhost".equals(host)
                || "127.0.0.1".equals(host)
                || "0.0.0.0".equals(host)
                || "::1".equals(host)
                || "host.docker.internal".equals(host)
                || host.endsWith(".local")
                || host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.startsWith("172.16.")
                || host.startsWith("172.17.")
                || host.startsWith("172.18.")
                || host.startsWith("172.19.")
                || host.startsWith("172.20.")
                || host.startsWith("172.21.")
                || host.startsWith("172.22.")
                || host.startsWith("172.23.")
                || host.startsWith("172.24.")
                || host.startsWith("172.25.")
                || host.startsWith("172.26.")
                || host.startsWith("172.27.")
                || host.startsWith("172.28.")
                || host.startsWith("172.29.")
                || host.startsWith("172.30.")
                || host.startsWith("172.31.");
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private UploadedOpenAiFile uploadAttachment(AiRuntimeConfig runtimeConfig, AiAttachment attachment) throws Exception {
        if (attachment == null) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.attach.error.empty"));
        }
        File file = attachment.toFile();
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.attach.error.not.exists", attachment.getFileName()));
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + runtimeConfig.getApiKey());
        applyBuiltinGatewayHeaders(headers, runtimeConfig);

        Map<String, Object> formParameters = new HashMap<String, Object>();
        formParameters.put("purpose", OPENAI_USER_DATA_PURPOSE);
        formParameters.put("file", new RequestObj.FormFilePart(file, attachment.getFileName(), attachment.getMimeType()));

        RequestObj requestObj = AiRequestTimeouts.applyStandard(new RequestObj()
                .setMethod("POST")
                .setPostMethod("FORM")
                .setUrl(resolveFilesEndpoint(runtimeConfig.getBaseUrl()))
                .setHeaders(headers)
                .setFormParameters(formParameters)
                .setInternalAiRequest(runtimeConfig.isBuiltinAi()), runtimeConfig);

        ProxyUtils.applyProxy(requestObj, runtimeConfig.isUseProxy());

        try (CustomHttpResponse response = AiHttpExecutor.requests(requestObj)) {
            JsonObject json = JsonParser.parseString(response.getTextStr()).getAsJsonObject();
            String errorMessage = readErrorMessage(json);
            if (!errorMessage.isEmpty()) {
                throw new IllegalStateException(errorMessage);
            }
            String fileId = safeString(json, "id");
            if (fileId.isEmpty()) {
                throw new IllegalStateException(I18nTextUtils.getString("ai.attach.error.openai.upload.no.file.id"));
            }
            return new UploadedOpenAiFile(fileId);
        }
    }

    private void deleteUploadedFiles(AiRuntimeConfig runtimeConfig, List<UploadedOpenAiFile> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return;
        }
        for (UploadedOpenAiFile uploadedFile : uploadedFiles) {
            if (uploadedFile == null || uploadedFile.getFileId().trim().isEmpty()) {
                continue;
            }
            try {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + runtimeConfig.getApiKey());
                applyBuiltinGatewayHeaders(headers, runtimeConfig);

                RequestObj requestObj = AiRequestTimeouts.applyStandard(new RequestObj()
                        .setMethod("DELETE")
                        .setUrl(resolveFilesEndpoint(runtimeConfig.getBaseUrl()) + "/" + uploadedFile.getFileId())
                        .setHeaders(headers)
                        .setInternalAiRequest(runtimeConfig.isBuiltinAi()), runtimeConfig);
                ProxyUtils.applyProxy(requestObj, runtimeConfig.isUseProxy());
                AiHttpExecutor.requests(requestObj).close();
            } catch (Exception ignored) {
            }
        }
    }

    private PreparedRequest buildUploadFallbackRequest(AiRuntimeConfig runtimeConfig,
                                                       AiChatRequest request,
                                                       List<AiAttachment> attachments,
                                                       Exception uploadException) throws Exception {
        if (!AiAttachmentFallbackSupport.shouldFallbackAfterUploadFailure(uploadException)) {
            return null;
        }
        AiAttachmentFallbackSupport.FallbackPlan fallbackPlan = AiAttachmentFallbackSupport.buildFallbackPlan(
                AiProviderType.OPENAI,
                request,
                attachments
        );
        if (fallbackPlan == null) {
            return null;
        }
        RequestObj requestObj = buildRequest(
                runtimeConfig,
                fallbackPlan.getRequest(),
                null,
                fallbackPlan.getInlineAttachments()
        );
        return PreparedRequest.of(requestObj);
    }

    private void applyBuiltinGatewayHeaders(Map<String, String> headers, AiRuntimeConfig runtimeConfig) {
        if (headers == null || runtimeConfig == null || !runtimeConfig.isBuiltinAi()) {
            return;
        }
        String modelAlias = runtimeConfig.getModelName();
        if (modelAlias == null || modelAlias.trim().isEmpty()) {
            return;
        }
        headers.put(MODEL_ALIAS_HEADER, modelAlias.trim());
    }

    private String resolveFilesEndpoint(String baseUrl) {
        String apiBase = resolveApiBase(baseUrl);
        return apiBase.endsWith("/files") ? apiBase : apiBase + "/files";
    }

    private String resolveRequestUrl(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.isEmpty()) {
            return url;
        }

        int queryIndex = url.indexOf('?');
        String path = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        String query = queryIndex >= 0 ? url.substring(queryIndex + 1) : "";

        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.endsWith("/chat/completions")) {
            return query.isEmpty() ? path : path + "?" + query;
        }

        String apiBase = resolveApiBase(path);
        apiBase = normalizeApiBaseForChat(apiBase);
        String resolved = apiBase.endsWith("/chat/completions") ? apiBase : apiBase + "/chat/completions";
        return query.isEmpty() ? resolved : resolved + "?" + query;
    }

    private String resolveApiBase(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            url = url.substring(0, queryIndex);
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/chat/completions")) {
            return url.substring(0, url.length() - "/chat/completions".length());
        }
        if (url.endsWith("/responses")) {
            return url.substring(0, url.length() - "/responses".length());
        }
        int v1Index = url.indexOf("/v1/");
        if (v1Index >= 0) {
            return url.substring(0, v1Index + 3);
        }
        return url;
    }

    private String normalizeApiBaseForChat(String apiBase) {
        String base = apiBase == null ? "" : apiBase.trim();
        if (base.isEmpty()) {
            return base;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        if (base.endsWith("/v1") || base.endsWith("/v4")) {
            return base;
        }

        String host = resolveHost(base);
        if ("api.openai.com".equalsIgnoreCase(host)) {
            return base + "/v1";
        }
        if ("api.deepseek.com".equalsIgnoreCase(host)) {
            return base;
        }
        if (host != null && !host.trim().isEmpty() && base.indexOf('/', base.indexOf("//") + 2) < 0) {
            return base + "/v1";
        }
        return base;
    }

    private String resolveHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractMessageText(JsonObject message) {
        if (message == null || !message.has("content")) {
            return "";
        }
        JsonElement content = message.get("content");
        if (content == null || content.isJsonNull()) {
            return "";
        }
        if (content.isJsonPrimitive()) {
            return safeString(message, "content");
        }
        if (!content.isJsonArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (JsonElement item : content.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject part = item.getAsJsonObject();
            String text = safeString(part, "text");
            if (!text.isEmpty()) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String readErrorMessage(JsonObject json) {
        if (json == null || !json.has("error") || !json.get("error").isJsonObject()) {
            return "";
        }
        JsonObject error = json.getAsJsonObject("error");
        String message = safeString(error, "message");
        return message.isEmpty() ? I18nTextUtils.getString("ai.status.error") : message;
    }

    private String safeString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return "";
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String readThinkingText(JsonObject delta) {
        String directThinking = safeString(delta, "thinking");
        if (!directThinking.isEmpty()) {
            return directThinking;
        }
        String reasoningContent = safeString(delta, "reasoning_content");
        if (!reasoningContent.isEmpty()) {
            return reasoningContent;
        }
        String reasoning = safeString(delta, "reasoning");
        if (!reasoning.isEmpty()) {
            return reasoning;
        }
        String reasoningDetails = readReasoningDetails(delta);
        if (!reasoningDetails.isEmpty()) {
            return reasoningDetails;
        }
        return "";
    }

    private String readReasoningDetails(JsonObject delta) {
        if (delta == null || !delta.has("reasoning_details")) {
            return "";
        }
        JsonElement reasoningDetails = delta.get("reasoning_details");
        if (reasoningDetails == null || reasoningDetails.isJsonNull()) {
            return "";
        }
        if (reasoningDetails.isJsonObject()) {
            return readReasoningDetailObject(reasoningDetails.getAsJsonObject());
        }
        if (!reasoningDetails.isJsonArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (JsonElement item : reasoningDetails.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                continue;
            }
            String text = readReasoningDetailObject(item.getAsJsonObject());
            if (!text.isEmpty()) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String readReasoningDetailObject(JsonObject detail) {
        String text = safeString(detail, "text");
        if (!text.isEmpty()) {
            return text;
        }
        text = safeString(detail, "thinking");
        if (!text.isEmpty()) {
            return text;
        }
        text = safeString(detail, "summary");
        if (!text.isEmpty()) {
            return text;
        }
        return safeString(detail, "reasoning");
    }

    private enum ThinkingDialect {
        NONE(false),
        OPENAI_REASONING_EFFORT(false),
        REASONING_EFFORT_NONE(false),
        REASONING_EFFORT_LOW(false),
        GROQ_QWEN_REASONING_EFFORT(false),
        OPENROUTER_REASONING(true),
        REASONING_ENABLED(false),
        QWEN_CLOUD(true),
        SILICONFLOW(true),
        DEEPSEEK_THINKING(false),
        GLM_THINKING(false),
        QWEN_CHAT_TEMPLATE(true),
        CHAT_TEMPLATE_THINKING(true);

        private final boolean supportsBudget;

        ThinkingDialect(boolean supportsBudget) {
            this.supportsBudget = supportsBudget;
        }

        private boolean supportsBudget() {
            return supportsBudget;
        }
    }

    private static final class UploadedOpenAiFile {
        private final String fileId;

        private UploadedOpenAiFile(String fileId) {
            this.fileId = fileId == null ? "" : fileId;
        }

        private String getFileId() {
            return fileId;
        }
    }

}
