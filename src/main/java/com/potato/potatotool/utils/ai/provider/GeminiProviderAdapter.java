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

public class GeminiProviderAdapter implements AiProviderAdapter {

    @Override
    public RequestObj buildRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("x-goog-api-key", runtimeConfig.getApiKey());

        JsonObject body = new JsonObject();
        if (!request.getSystemPrompt().trim().isEmpty()) {
            JsonObject systemInstruction = new JsonObject();
            JsonArray instructionParts = new JsonArray();
            JsonObject instructionPart = new JsonObject();
            instructionPart.addProperty("text", request.getSystemPrompt());
            instructionParts.add(instructionPart);
            systemInstruction.add("parts", instructionParts);
            body.add("systemInstruction", systemInstruction);
        }
        JsonArray contents = new JsonArray();
        for (AiMessage item : request.getHistory()) {
            contents.add(createContent(item));
        }
        contents.add(createContent(new AiMessage("user", request.getQuestion())));
        body.add("contents", contents);

        int timeoutSec = Math.max(1, runtimeConfig.getTimeoutMs() / 1000);
        RequestObj requestObj = new RequestObj()
                .setMethod("POST")
                .setPostMethod("JSON")
                .setUrl(resolveRequestUrl(runtimeConfig.getBaseUrl(), request.isStream()))
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
        if (payload.isEmpty() || "[DONE]".equals(payload)) {
            events.add(AiStreamEvent.done());
            return events;
        }

        try {
            JsonElement parsed = JsonParser.parseString(payload);
            if (parsed.isJsonArray()) {
                JsonArray array = parsed.getAsJsonArray();
                for (JsonElement element : array) {
                    appendResponseEvents(events, element);
                }
            } else {
                appendResponseEvents(events, parsed);
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
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonArray()) {
                JsonArray array = parsed.getAsJsonArray();
                if (array.size() == 0 || !array.get(0).isJsonObject()) {
                    return "";
                }
                return extractText(array.get(0).getAsJsonObject());
            }
            return extractText(parsed.getAsJsonObject());
        } catch (Exception e) {
            return "AI 响应解析失败: " + e.getMessage();
        }
    }

    private JsonObject createContent(AiMessage message) {
        JsonObject content = new JsonObject();
        content.addProperty("role", "assistant".equalsIgnoreCase(message.getRole()) ? "model" : "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", message.getContent() == null ? "" : message.getContent());
        parts.add(part);
        content.add("parts", parts);
        return content;
    }

    private void appendResponseEvents(List<AiStreamEvent> events, JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        String errorMessage = readErrorMessage(json);
        if (!errorMessage.isEmpty()) {
            events.add(AiStreamEvent.error(errorMessage));
            return;
        }
        String text = extractCandidateText(json);
        if (!text.isEmpty()) {
            events.add(AiStreamEvent.token(text));
        }
    }

    private String extractText(JsonObject json) {
        if (json == null) {
            return "";
        }
        String errorMessage = readErrorMessage(json);
        if (!errorMessage.isEmpty()) {
            return errorMessage;
        }
        return extractCandidateText(json);
    }

    private String extractCandidateText(JsonObject json) {
        JsonArray candidates = safeArray(json, "candidates");
        if (candidates.size() == 0 || !candidates.get(0).isJsonObject()) {
            return "";
        }
        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject content = safeObject(candidate, "content");
        JsonArray parts = safeArray(content, "parts");
        StringBuilder builder = new StringBuilder();
        for (JsonElement partElement : parts) {
            if (!partElement.isJsonObject()) {
                continue;
            }
            String text = safeString(partElement.getAsJsonObject(), "text");
            if (!text.isEmpty()) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String readErrorMessage(JsonObject json) {
        JsonObject error = safeObject(json, "error");
        return safeString(error, "message");
    }

    private JsonArray safeArray(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return obj.getAsJsonArray(key);
    }

    private JsonObject safeObject(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return obj.getAsJsonObject(key);
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

    private String resolveRequestUrl(String baseUrl, boolean stream) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.isEmpty()) {
            return url;
        }

        int queryIndex = url.indexOf('?');
        String path = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        String query = queryIndex >= 0 ? url.substring(queryIndex + 1) : "";

        if (path.endsWith(":streamGenerateContent")) {
            path = path.substring(0, path.length() - ":streamGenerateContent".length());
        } else if (path.endsWith(":generateContent")) {
            path = path.substring(0, path.length() - ":generateContent".length());
        }

        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String resolved = path + (stream ? ":streamGenerateContent" : ":generateContent");
        String normalizedQuery = rebuildQuery(query, stream);
        return normalizedQuery.isEmpty() ? resolved : resolved + "?" + normalizedQuery;
    }

    private String rebuildQuery(String originalQuery, boolean stream) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return stream ? "alt=sse" : "";
        }
        String[] parts = originalQuery.split("&");
        List<String> filtered = new ArrayList<String>();
        boolean hasAltSse = false;
        for (String part : parts) {
            String item = part == null ? "" : part.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (item.startsWith("alt=")) {
                hasAltSse = "alt=sse".equalsIgnoreCase(item);
                if (stream && !hasAltSse) {
                    filtered.add("alt=sse");
                    hasAltSse = true;
                }
                continue;
            }
            filtered.add(item);
        }
        if (stream && !hasAltSse) {
            filtered.add("alt=sse");
        }
        StringBuilder builder = new StringBuilder();
        for (String item : filtered) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(item);
        }
        return builder.toString();
    }
}
