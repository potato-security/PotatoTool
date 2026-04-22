package com.potato.potatotool.utils.ai.provider;

import com.potato.potatotool.utils.ai.AiAttachmentSupportResolver;
import com.potato.potatotool.utils.ai.AiAttachmentUtils;
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
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.potato.potatotool.utils.network.RequestUtils.requests;

public class OpenAiCompatibleProviderAdapter implements AiProviderAdapter {
    private static final String OPENAI_USER_DATA_PURPOSE = "user_data";

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
            return extractMessageText(message);
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
                    throw new IllegalStateException("读取附件失败: " + attachment.getFileName(), e);
                }
                imagePart.add("image_url", imageUrl);
                content.add(imagePart);
            }
        }

        message.add("content", content);
        return message;
    }

    private UploadedOpenAiFile uploadAttachment(AiRuntimeConfig runtimeConfig, AiAttachment attachment) throws Exception {
        if (attachment == null) {
            throw new IllegalArgumentException("附件不能为空");
        }
        File file = attachment.toFile();
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("附件不存在: " + attachment.getFileName());
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + runtimeConfig.getApiKey());

        Map<String, Object> formParameters = new HashMap<String, Object>();
        formParameters.put("purpose", OPENAI_USER_DATA_PURPOSE);
        formParameters.put("file", new RequestObj.FormFilePart(file, attachment.getFileName(), attachment.getMimeType()));

        int timeoutSec = Math.max(1, runtimeConfig.getTimeoutMs() / 1000);
        RequestObj requestObj = new RequestObj()
                .setMethod("POST")
                .setPostMethod("FORM")
                .setUrl(resolveFilesEndpoint(runtimeConfig.getBaseUrl()))
                .setHeaders(headers)
                .setFormParameters(formParameters)
                .setTimeOut(timeoutSec)
                .setReadTimeout(timeoutSec)
                .setWriteTimeout(timeoutSec)
                .setCallTimeout(timeoutSec)
                .setInternalAiRequest(runtimeConfig.isBuiltinAi());

        ProxyUtils.applyProxy(requestObj, runtimeConfig.isUseProxy());

        try (CustomHttpResponse response = requests(requestObj)) {
            JsonObject json = JsonParser.parseString(response.getTextStr()).getAsJsonObject();
            String errorMessage = readErrorMessage(json);
            if (!errorMessage.isEmpty()) {
                throw new IllegalStateException(errorMessage);
            }
            String fileId = safeString(json, "id");
            if (fileId.isEmpty()) {
                throw new IllegalStateException("OpenAI 附件上传失败: 未返回 file id");
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

                int timeoutSec = Math.max(1, runtimeConfig.getTimeoutMs() / 1000);
                RequestObj requestObj = new RequestObj()
                        .setMethod("DELETE")
                        .setUrl(resolveFilesEndpoint(runtimeConfig.getBaseUrl()) + "/" + uploadedFile.getFileId())
                        .setHeaders(headers)
                        .setTimeOut(timeoutSec)
                        .setReadTimeout(timeoutSec)
                        .setWriteTimeout(timeoutSec)
                        .setCallTimeout(timeoutSec)
                        .setInternalAiRequest(runtimeConfig.isBuiltinAi());
                ProxyUtils.applyProxy(requestObj, runtimeConfig.isUseProxy());
                requests(requestObj).close();
            } catch (Exception ignored) {
            }
        }
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
        return message.isEmpty() ? "AI 请求失败" : message;
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
