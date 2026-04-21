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
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleProviderAdapter implements AiProviderAdapter {

    @Override
    public RequestObj buildRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + runtimeConfig.getApiKey());

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
        messages.add(createMessage("user", request.getQuestion()));
        body.add("messages", messages);

        AiThinkingConfig thinkingConfig = request.getThinkingConfig();
        if (thinkingConfig != null && thinkingConfig.isEnabled()) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("enabled", true);
            thinking.addProperty("budget_tokens", thinkingConfig.getBudgetTokens());
            body.add("thinking", thinking);
        }

        int timeoutSec = Math.max(1, runtimeConfig.getTimeoutMs() / 1000);
        RequestObj requestObj = new RequestObj()
                .setMethod("POST")
                .setPostMethod("JSON")
                .setUrl(runtimeConfig.getBaseUrl())
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
        if (line == null || line.trim().isEmpty()) {
            return events;
        }

        if (line.startsWith("[[")) {
            events.add(AiStreamEvent.error("流式连接异常: " + line));
            return events;
        }

        String payload = line;
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
                    message = "AI 请求失败";
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

            if (json.has("error") && json.get("error").isJsonObject()) {
                JsonObject error = json.getAsJsonObject("error");
                String message = safeString(error, "message");
                return message.isEmpty() ? "AI 请求失败" : message;
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
            return safeString(message, "content");
        } catch (Exception e) {
            return "AI 响应解析失败: " + e.getMessage();
        }
    }

    private JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content == null ? "" : content);
        return message;
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
        return "";
    }


}
