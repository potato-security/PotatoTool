package com.potato.potatotool.utils.ai.service;

import com.potato.potatotool.utils.ai.AiAttachmentDispatchPlanner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentDispatchPlan;
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

    public interface StreamSession {
        void cancel();

        boolean isCancelled();
    }

    interface StreamTransport {
        void stream(RequestObj requestObj, StreamSession streamSession, Consumer<String> lineConsumer) throws Exception;
    }

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final AiConfigReader configReader;
    private final AiProviderRegistry providerRegistry;
    private final StreamTransport streamTransport;
    private final List<AiMessage> history = new ArrayList<AiMessage>();
    private final Object streamSessionLock = new Object();
    private StreamController activeStreamSession;

    public AiChatService() {
        this(new AiConfigReader(), new AiProviderRegistry());
    }

    public AiChatService(AiConfigReader configReader, AiProviderRegistry providerRegistry) {
        this(configReader, providerRegistry, (requestObj, streamSession, lineConsumer) -> {
            try (CustomHttpResponse response = requests(requestObj)) {
                StreamController.bind(streamSession, response);
                if (streamSession.isCancelled()) {
                    return;
                }
                response.getSSEStreamingJson(line -> {
                    if (streamSession.isCancelled()) {
                        response.disconnect();
                        return;
                    }
                    lineConsumer.accept(line);
                });
            } finally {
                StreamController.clear(streamSession);
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
        streamChat(null, question, question, listener);
    }

    public void streamChat(String systemPrompt, String question, EventListener listener) {
        streamChat(systemPrompt, question, question, listener);
    }

    public void streamChat(String systemPrompt, String question, String historyQuestion, EventListener listener) {
        streamChat(systemPrompt, question, historyQuestion, null, null, listener);
    }

    public void streamChat(String systemPrompt,
                           String question,
                           String historyQuestion,
                           List<AiAttachment> attachments,
                           EventListener listener) {
        streamChat(systemPrompt, question, historyQuestion, attachments, null, listener);
    }

    public void streamChat(String systemPrompt,
                           String question,
                           String historyQuestion,
                           com.potato.potatotool.utils.ai.model.AiThinkingConfig thinkingOverride,
                           EventListener listener) {
        streamChat(systemPrompt, question, historyQuestion, null, thinkingOverride, listener);
    }

    public void streamChat(String systemPrompt,
                           String question,
                           String historyQuestion,
                           List<AiAttachment> attachments,
                           com.potato.potatotool.utils.ai.model.AiThinkingConfig thinkingOverride,
                           EventListener listener) {
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
        AiAttachmentDispatchPlan attachmentPlan = AiAttachmentDispatchPlanner.plan(runtimeConfig, adapter, attachments);
        if (!attachmentPlan.isSupported()) {
            listener.onEvent(AiStreamEvent.error(attachmentPlan.getMessage()));
            return;
        }
        String requestQuestion = mergeQuestionWithAttachmentContext(askText, attachmentPlan.getQuestionSuffix());
        AiChatRequest request = new AiChatRequest(
                requestQuestion,
                normalizeHistoryQuestion(historyQuestion, askText),
                history,
                attachmentPlan.getRequestAttachments(),
                thinkingOverride == null ? runtimeConfig.getThinkingConfig() : thinkingOverride,
                true,
                systemPrompt
        );
        AiProviderAdapter.PreparedRequest preparedRequest;
        try {
            preparedRequest = adapter.prepareRequest(runtimeConfig, request);
        } catch (Exception e) {
            listener.onEvent(AiStreamEvent.error(normalizeError(e, runtimeConfig)));
            return;
        }
        RequestObj requestObj = preparedRequest.getRequestObj();

        final StringBuilder answerBuffer = new StringBuilder();
        final StreamController streamSession = beginStreamSession();

        try {
            streamTransport.stream(requestObj, streamSession, line -> {
                if (streamSession.isCancelled()) {
                    return;
                }
                List<AiStreamEvent> events = adapter.parseSseLine(line);
                for (AiStreamEvent event : events) {
                    if (streamSession.isCancelled() && event.getType() != AiStreamEvent.Type.ERROR) {
                        return;
                    }
                    if (event.getType() == AiStreamEvent.Type.TOKEN) {
                        answerBuffer.append(event.getContent());
                    }
                    listener.onEvent(event);
                }
            });

            if (answerBuffer.length() > 0) {
                appendHistory("user", request.getHistoryQuestion());
                appendHistory("assistant", answerBuffer.toString());
            }
        } catch (Exception e) {
            if (!streamSession.isCancelled()) {
                listener.onEvent(AiStreamEvent.error(normalizeError(e, runtimeConfig)));
            }
        } finally {
            clearActiveStream(streamSession);
            preparedRequest.cleanup();
        }
    }

    public void cancelActiveStream() {
        StreamController currentSession;
        synchronized (streamSessionLock) {
            currentSession = activeStreamSession;
        }
        if (currentSession != null) {
            currentSession.cancel();
        }
    }

    public String askNoStream(String question) {
        return askNoStream(null, question, question);
    }

    public String askNoStream(String systemPrompt, String question) {
        return askNoStream(systemPrompt, question, question);
    }

    public String askNoStream(String systemPrompt, String question, String historyQuestion) {
        return askNoStream(systemPrompt, question, historyQuestion, null);
    }

    public String askNoStream(String systemPrompt,
                              String question,
                              String historyQuestion,
                              List<AiAttachment> attachments) {
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
        AiAttachmentDispatchPlan attachmentPlan = AiAttachmentDispatchPlanner.plan(runtimeConfig, adapter, attachments);
        if (!attachmentPlan.isSupported()) {
            return attachmentPlan.getMessage();
        }
        String requestQuestion = mergeQuestionWithAttachmentContext(askText, attachmentPlan.getQuestionSuffix());
        AiChatRequest request = new AiChatRequest(
                requestQuestion,
                normalizeHistoryQuestion(historyQuestion, askText),
                history,
                attachmentPlan.getRequestAttachments(),
                runtimeConfig.getThinkingConfig(),
                false,
                systemPrompt
        );
        AiProviderAdapter.PreparedRequest preparedRequest;
        try {
            preparedRequest = adapter.prepareRequest(runtimeConfig, request);
        } catch (Exception e) {
            return normalizeError(e, runtimeConfig);
        }
        RequestObj requestObj = preparedRequest.getRequestObj();

        try (CustomHttpResponse response = requests(requestObj)) {
            String body = response.getTextStr();
            String parsed = adapter.parseResponse(body);
            if (parsed != null) {
                appendHistory("user", request.getHistoryQuestion());
                appendHistory("assistant", parsed);
            }
            return parsed == null ? "" : parsed;
        } catch (Exception e) {
            return normalizeError(e, runtimeConfig);
        } finally {
            preparedRequest.cleanup();
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

    private String mergeQuestionWithAttachmentContext(String question, String questionSuffix) {
        String baseQuestion = question == null ? "" : question.trim();
        String suffix = questionSuffix == null ? "" : questionSuffix.trim();
        if (suffix.isEmpty()) {
            return baseQuestion;
        }
        if (baseQuestion.isEmpty()) {
            return suffix;
        }
        return baseQuestion + "\n\n" + suffix;
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

    private String normalizeHistoryQuestion(String historyQuestion, String fallbackQuestion) {
        String candidate = historyQuestion == null ? "" : historyQuestion.trim();
        if (!candidate.isEmpty()) {
            return candidate;
        }
        return fallbackQuestion == null ? "" : fallbackQuestion.trim();
    }

    private StreamController beginStreamSession() {
        synchronized (streamSessionLock) {
            if (activeStreamSession != null) {
                activeStreamSession.cancel();
            }
            activeStreamSession = new StreamController();
            return activeStreamSession;
        }
    }

    private void clearActiveStream(StreamController streamSession) {
        synchronized (streamSessionLock) {
            if (activeStreamSession == streamSession) {
                activeStreamSession = null;
            }
        }
    }

    private static final class StreamController implements StreamSession {
        private boolean cancelled;
        private CustomHttpResponse response;

        @Override
        public synchronized void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            if (response != null) {
                response.disconnect();
                response = null;
            }
        }

        @Override
        public synchronized boolean isCancelled() {
            return cancelled;
        }

        private synchronized void bindResponse(CustomHttpResponse response) {
            if (cancelled) {
                if (response != null) {
                    response.disconnect();
                }
                return;
            }
            this.response = response;
        }

        private synchronized void clearResponse() {
            response = null;
        }

        private static void bind(StreamSession streamSession, CustomHttpResponse response) {
            if (streamSession instanceof StreamController) {
                ((StreamController) streamSession).bindResponse(response);
            }
        }

        private static void clear(StreamSession streamSession) {
            if (streamSession instanceof StreamController) {
                ((StreamController) streamSession).clearResponse();
            }
        }
    }
}
