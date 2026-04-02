package com.potato.potatotool.utils.ai.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiMessage;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.provider.AiProviderAdapter;
import com.potato.potatotool.utils.ai.provider.AiProviderRegistry;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.utils.network.RequestUtils.requests;

public class AiChatService {

    public interface EventListener {
        void onEvent(AiStreamEvent event);
    }

    interface StreamTransport {
        void stream(RequestObj requestObj, Consumer<String> lineConsumer) throws Exception;
    }

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final AiConfigReader configReader;
    private final AiProviderRegistry providerRegistry;
    private final StreamTransport streamTransport;
    private final List<AiMessage> history = new ArrayList<AiMessage>();

    public AiChatService() {
        this(new AiConfigReader(), new AiProviderRegistry());
    }

    public AiChatService(AiConfigReader configReader, AiProviderRegistry providerRegistry) {
        this(configReader, providerRegistry, (requestObj, lineConsumer) -> {
            try (CustomHttpResponse response = requests(requestObj)) {
                response.getSSEStreamingJson(lineConsumer::accept);
            }
        });
    }

    AiChatService(AiConfigReader configReader,
                  AiProviderRegistry providerRegistry,
                  StreamTransport streamTransport) {
        this.configReader = configReader;
        this.providerRegistry = providerRegistry;
        this.streamTransport = streamTransport;
    }

    public void streamChat(String question, EventListener listener) {
        if (question == null || question.trim().isEmpty()) {
            listener.onEvent(AiStreamEvent.error("问题不能为空"));
            return;
        }

        String askText = question.trim();
        AiRuntimeConfig runtimeConfig;
        try {
            runtimeConfig = configReader.read();
        } catch (Exception e) {
            listener.onEvent(AiStreamEvent.error("AI 配置错误: " + e.getMessage()));
            return;
        }

        AiProviderAdapter adapter = providerRegistry.get(runtimeConfig.getProviderType());
        AiChatRequest request = new AiChatRequest(askText, history, runtimeConfig.getThinkingConfig(), true);
        RequestObj requestObj = adapter.buildRequest(runtimeConfig, request);

        final StringBuilder answerBuffer = new StringBuilder();

        try {
            streamTransport.stream(requestObj, line -> {
                List<AiStreamEvent> events = adapter.parseSseLine(line);
                for (AiStreamEvent event : events) {
                    if (event.getType() == AiStreamEvent.Type.TOKEN) {
                        answerBuffer.append(event.getContent());
                    }
                    listener.onEvent(event);
                }
            });

            if (answerBuffer.length() > 0) {
                appendHistory("user", askText);
                appendHistory("assistant", answerBuffer.toString());
            }
        } catch (Exception e) {
            listener.onEvent(AiStreamEvent.error(normalizeError(e, runtimeConfig)));
        }
    }

    public String askNoStream(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "问题不能为空";
        }

        String askText = question.trim();
        AiRuntimeConfig runtimeConfig;
        try {
            runtimeConfig = configReader.read();
        } catch (Exception e) {
            return "AI 配置错误: " + e.getMessage();
        }

        AiProviderAdapter adapter = providerRegistry.get(runtimeConfig.getProviderType());
        AiChatRequest request = new AiChatRequest(askText, history, runtimeConfig.getThinkingConfig(), false);
        RequestObj requestObj = adapter.buildRequest(runtimeConfig, request);

        try (CustomHttpResponse response = requests(requestObj)) {
            String body = response.getTextStr();
            String parsed = parseNonStreamResponse(body);
            if (parsed != null) {
                appendHistory("user", askText);
                appendHistory("assistant", parsed);
            }
            return parsed == null ? "" : parsed;
        } catch (Exception e) {
            return normalizeError(e, runtimeConfig);
        }
    }

    static String parseNonStreamResponse(String body) {
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

    private static String safeString(JsonObject obj, String key) {
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

    public Set<String> extractBracketedResult(String aiJudgement) {
        Set<String> result = new LinkedHashSet<String>();
        String[] patterns = {"\\[\\[\\[([^\\[\\]]*)\\]\\]\\]", "\\[\\[([^\\[\\]]*)\\]\\]", "\\[([^\\[\\]]*)\\]"};

        for (String patternString : patterns) {
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(aiJudgement);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        }
        return result;
    }

    public void clearHistory() {
        history.clear();
    }

    private void appendHistory(String role, String content) {
        if (content == null) {
            content = "";
        }
        history.add(new AiMessage(role, content));
        while (history.size() > MAX_HISTORY_MESSAGES) {
            history.remove(0);
        }
    }

    private String normalizeError(Exception e, AiRuntimeConfig runtimeConfig) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "AI 请求失败";
        }
        if (message.contains("Connect timed out")) {
            return "AI 连接超时，请检查网络或代理";
        }
        if (!runtimeConfig.isBuiltinAi()) {
            return message;
        }
        return sanitizeEndpointMessage(message, runtimeConfig);
    }

    private String sanitizeEndpointMessage(String message, AiRuntimeConfig runtimeConfig) {
        if (message == null || message.trim().isEmpty()) {
            return "AI 请求失败";
        }

        String result = message;
        result = result.replaceAll("(?i)https?://[^\\s,，;；)]+", "内置AI服务地址");

        String host = resolveConfiguredHost(runtimeConfig.getBaseUrl());
        if (!host.isEmpty()) {
            String ipPortPattern = "(?i)/" + Pattern.quote(host) + ":\\d+";
            result = result.replaceAll(ipPortPattern, "/内置AI服务主机");

            String hostPortPattern = "(?i)(?<![a-z0-9_.-])" + Pattern.quote(host) + ":\\d+";
            result = result.replaceAll(hostPortPattern, "内置AI服务主机");

            String bareHostPattern = "(?i)(?<![a-z0-9_.-])" + Pattern.quote(host) + "(?![a-z0-9_.-])";
            result = result.replaceAll(bareHostPattern, "内置AI服务主机");
        }

        result = result.replaceAll("(?i)/(?:\\d{1,3}\\.){3}\\d{1,3}:\\d+", "/内置AI服务主机");
        result = result.replaceAll("(?i)(?<![a-z0-9_.-])(?:\\d{1,3}\\.){3}\\d{1,3}:\\d+", "内置AI服务主机");
        result = result.replaceAll("(?i)/[a-z0-9.-]+\\.[a-z]{2,}:\\d+", "/内置AI服务主机");
        result = result.replaceAll("(?i)(?<![a-z0-9_.-])[a-z0-9.-]+\\.[a-z]{2,}:\\d+", "内置AI服务主机");

        return result;
    }

    private String resolveConfiguredHost(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "";
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            String host = uri.getHost();
            if (host != null && !host.trim().isEmpty()) {
                return host.trim();
            }
        } catch (Exception ignored) {
        }

        String raw = baseUrl.trim();
        String noScheme = raw.replaceFirst("(?i)^https?://", "");
        int slashIndex = noScheme.indexOf('/');
        String hostPort = slashIndex > 0 ? noScheme.substring(0, slashIndex) : noScheme;
        if (hostPort.startsWith("/")) {
            hostPort = hostPort.substring(1);
        }
        int colonIndex = hostPort.indexOf(':');
        return colonIndex > 0 ? hostPort.substring(0, colonIndex) : hostPort;
    }
}
