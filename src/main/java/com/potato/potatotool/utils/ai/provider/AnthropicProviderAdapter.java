package com.potato.potatotool.utils.ai.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiMessage;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnthropicProviderAdapter implements AiProviderAdapter {
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 4096;

    @Override
    public RequestObj buildRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("x-api-key", runtimeConfig.getApiKey());
        headers.put("anthropic-version", ANTHROPIC_VERSION);

        JsonObject body = new JsonObject();
        body.addProperty("model", runtimeConfig.getModelName());
        body.addProperty("stream", request.isStream());
        body.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
        if (!request.getSystemPrompt().trim().isEmpty()) {
            body.addProperty("system", request.getSystemPrompt());
        }

        JsonArray messages = new JsonArray();
        for (AiMessage item : request.getHistory()) {
            messages.add(createMessage(item));
        }
        messages.add(createMessage(new AiMessage("user", request.getQuestion())));
        body.add("messages", messages);

        int timeoutSec = Math.max(1, runtimeConfig.getTimeoutMs() / 1000);
        RequestObj requestObj = new RequestObj()
                .setMethod("POST")
                .setPostMethod("JSON")
                .setUrl(resolveRequestUrl(runtimeConfig.getBaseUrl()))
                .setHeaders(headers)
                .setPostData(new Gson().toJson(body))
                .setTimeOut(timeoutSec)
                .setReadTimeout(timeoutSec)
                .setWriteTimeout(timeoutSec)
                .setCallTimeout(timeoutSec)
                .setInternalAiRequest(runtimeConfig.isBuiltinAi());

        ProxyUtils.applyProxy(requestObj, runtimeConfig.isUseProxy());
        return requestObj;
    }

    @Override
    public List<AiStreamEvent> parseSseLine(String line) {
        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        if (line == null) {
            return events;
        }
        String payload = line.trim();
        if (payload.isEmpty() || payload.startsWith("event:")) {
            return events;
        }
        if (payload.startsWith("data:")) {
            payload = payload.substring(5).trim();
        }
        if (payload.isEmpty()) {
            return events;
        }

        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            if ("error".equals(safeString(json, "type"))) {
                String message = readErrorMessage(json);
                events.add(AiStreamEvent.error(message.isEmpty() ? "AI 请求失败" : message));
                return events;
            }

            String type = safeString(json, "type");
            if ("content_block_delta".equals(type) && json.has("delta") && json.get("delta").isJsonObject()) {
                JsonObject delta = json.getAsJsonObject("delta");
                String thinking = safeString(delta, "thinking");
                if (!thinking.isEmpty()) {
                    events.add(AiStreamEvent.thinkingToken(thinking));
                }
                String text = safeString(delta, "text");
                if (!text.isEmpty()) {
                    events.add(AiStreamEvent.token(text));
                }
            } else if ("message_stop".equals(type)) {
                events.add(AiStreamEvent.done());
            }
        } catch (Exception e) {
            events.add(AiStreamEvent.error("流式响应解析失败: " + e.getMessage()));
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
            String message = readErrorMessage(json);
            if (!message.isEmpty()) {
                return message;
            }
            return extractTextBlocks(json.getAsJsonArray("content"));
        } catch (Exception e) {
            return "AI 响应解析失败: " + e.getMessage();
        }
    }

    private JsonObject createMessage(AiMessage message) {
        JsonObject result = new JsonObject();
        String role = message == null ? "user" : safeRole(message.getRole());
        result.addProperty("role", role);
        JsonArray content = new JsonArray();
        JsonObject textBlock = new JsonObject();
        textBlock.addProperty("type", "text");
        textBlock.addProperty("text", message == null || message.getContent() == null ? "" : message.getContent());
        content.add(textBlock);
        result.add("content", content);
        return result;
    }

    private String safeRole(String role) {
        if ("assistant".equalsIgnoreCase(role)) {
            return "assistant";
        }
        return "user";
    }

    private String resolveRequestUrl(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.isEmpty()) {
            return url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/v1/messages") || url.endsWith("/messages")) {
            return url;
        }
        if (url.endsWith("/v1")) {
            return url + "/messages";
        }
        return url + "/v1/messages";
    }

    private String extractTextBlocks(JsonArray contentArray) {
        if (contentArray == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonElement element : contentArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject block = element.getAsJsonObject();
            if (!"text".equals(safeString(block, "type"))) {
                continue;
            }
            String text = safeString(block, "text");
            if (!text.isEmpty()) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String readErrorMessage(JsonObject json) {
        if (json == null) {
            return "";
        }
        if (json.has("error") && json.get("error").isJsonObject()) {
            JsonObject error = json.getAsJsonObject("error");
            return safeString(error, "message");
        }
        return "";
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
}
