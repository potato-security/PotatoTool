package com.potato.potatotool.utils.ai.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.AiAttachmentFallbackSupport;
import com.potato.potatotool.utils.ai.AiAttachmentUtils;
import com.potato.potatotool.utils.ai.AiRequestTimeouts;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiMessage;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
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
import java.util.Map;

public class AnthropicProviderAdapter implements AiProviderAdapter {
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String ANTHROPIC_FILES_BETA = "files-api-2025-04-14";
    private static final int DEFAULT_MAX_TOKENS = 4096;

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

        List<UploadedAnthropicFile> uploadedFiles = new ArrayList<UploadedAnthropicFile>();
        try {
            for (AiAttachment attachment : attachments) {
                if (!AiAttachmentUtils.isSupportedByProvider(AiProviderType.ANTHROPIC, attachment)) {
                    throw new IllegalArgumentException(
                            I18nTextUtils.getString("ai.attach.error.anthropic.unsupported", attachment.getFileName())
                    );
                }
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
                                    List<UploadedAnthropicFile> uploadedFiles,
                                    List<AiAttachment> inlineImageAttachments) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("x-api-key", runtimeConfig.getApiKey());
        headers.put("anthropic-version", ANTHROPIC_VERSION);
        if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
            headers.put("anthropic-beta", ANTHROPIC_FILES_BETA);
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", runtimeConfig.getModelName());
        body.addProperty("stream", request.isStream());
        body.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
        if (!request.getSystemPrompt().trim().isEmpty()) {
            body.addProperty("system", request.getSystemPrompt());
        }
        AiThinkingConfig thinkingConfig = request.getThinkingConfig();
        if (thinkingConfig != null && thinkingConfig.isEnabled()) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "enabled");
            thinking.addProperty("budget_tokens", thinkingConfig.getBudgetTokens());
            body.add("thinking", thinking);
        }

        JsonArray messages = new JsonArray();
        for (AiMessage item : request.getHistory()) {
            messages.add(createMessage(item));
        }
        messages.add(createUserMessage(request.getQuestion(), uploadedFiles, inlineImageAttachments));
        body.add("messages", messages);

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
        return true;
    }

    @Override
    public boolean supportsThinkingBudget(AiRuntimeConfig runtimeConfig) {
        return true;
    }

    @Override
    public List<AiStreamEvent> parseSseLine(String line) {
        List<AiStreamEvent> events = new ArrayList<AiStreamEvent>();
        if (line == null) {
            return events;
        }
        String payload = line.trim();
        if (payload.isEmpty() || payload.startsWith("event:") || payload.startsWith(":")) {
            return events;
        }
        String transportError = resolveTransportErrorMessage(payload);
        if (!transportError.isEmpty()) {
            events.add(AiStreamEvent.error(transportError));
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
                events.add(AiStreamEvent.error(message.isEmpty() ? I18nTextUtils.getString("ai.status.error") : message));
                return events;
            }

            String type = safeString(json, "type");
            if ("content_block_delta".equals(type) && json.has("delta") && json.get("delta").isJsonObject()) {
                JsonObject delta = json.getAsJsonObject("delta");
                String thinking = safeString(delta, "thinking");
                if (thinking.isEmpty() && "thinking_delta".equals(safeString(delta, "type"))) {
                    thinking = safeString(delta, "text");
                }
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
            String message = readErrorMessage(json);
            if (!message.isEmpty()) {
                return message;
            }
            return extractTextBlocks(json.getAsJsonArray("content"));
        } catch (Exception e) {
            return I18nTextUtils.getString("ai.error.response.parse", e.getMessage());
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

    private JsonObject createUserMessage(String question,
                                         List<UploadedAnthropicFile> uploadedFiles,
                                         List<AiAttachment> inlineImageAttachments) {
        if ((uploadedFiles == null || uploadedFiles.isEmpty())
                && (inlineImageAttachments == null || inlineImageAttachments.isEmpty())) {
            return createMessage(new AiMessage("user", question));
        }

        JsonObject result = new JsonObject();
        result.addProperty("role", "user");
        JsonArray content = new JsonArray();

        JsonObject textBlock = new JsonObject();
        textBlock.addProperty("type", "text");
        textBlock.addProperty("text", question == null ? "" : question);
        content.add(textBlock);

        if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
            for (UploadedAnthropicFile uploadedFile : uploadedFiles) {
                if (uploadedFile == null) {
                    continue;
                }
                JsonObject block = new JsonObject();
                block.addProperty("type", uploadedFile.isImage() ? "image" : "document");

                JsonObject source = new JsonObject();
                source.addProperty("type", "file");
                source.addProperty("file_id", uploadedFile.getFileId());
                block.add("source", source);
                content.add(block);
            }
        } else {
            for (AiAttachment attachment : inlineImageAttachments) {
                if (attachment == null) {
                    continue;
                }
                JsonObject block = new JsonObject();
                block.addProperty("type", "image");

                JsonObject source = new JsonObject();
                source.addProperty("type", "base64");
                source.addProperty("media_type", resolveInlineImageMimeType(attachment));
                try {
                    source.addProperty("data", AiAttachmentUtils.readBase64(attachment));
                } catch (Exception e) {
                    throw new IllegalStateException(
                            I18nTextUtils.getString("ai.attach.read.failed", attachment.getFileName()),
                            e
                    );
                }
                block.add("source", source);
                content.add(block);
            }
        }

        result.add("content", content);
        return result;
    }

    private UploadedAnthropicFile uploadAttachment(AiRuntimeConfig runtimeConfig, AiAttachment attachment) throws Exception {
        if (attachment == null) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.attach.error.empty"));
        }
        File file = attachment.toFile();
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException(I18nTextUtils.getString("ai.attach.error.not.exists", attachment.getFileName()));
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("x-api-key", runtimeConfig.getApiKey());
        headers.put("anthropic-version", ANTHROPIC_VERSION);
        headers.put("anthropic-beta", ANTHROPIC_FILES_BETA);

        Map<String, Object> formParameters = new HashMap<String, Object>();
        formParameters.put("purpose", "user_data");
        formParameters.put("file", new RequestObj.FormFilePart(file, attachment.getFileName(), resolveUploadMimeType(attachment)));

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
            String message = readErrorMessage(json);
            if (!message.isEmpty()) {
                throw new IllegalStateException(message);
            }
            String fileId = safeString(json, "id");
            if (fileId.isEmpty()) {
                throw new IllegalStateException(I18nTextUtils.getString("ai.attach.error.anthropic.upload.no.file.id"));
            }
            return new UploadedAnthropicFile(fileId, AiAttachmentUtils.isImage(attachment));
        }
    }

    private void deleteUploadedFiles(AiRuntimeConfig runtimeConfig, List<UploadedAnthropicFile> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return;
        }
        for (UploadedAnthropicFile uploadedFile : uploadedFiles) {
            if (uploadedFile == null || uploadedFile.getFileId().trim().isEmpty()) {
                continue;
            }
            try {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("x-api-key", runtimeConfig.getApiKey());
                headers.put("anthropic-version", ANTHROPIC_VERSION);
                headers.put("anthropic-beta", ANTHROPIC_FILES_BETA);

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
                AiProviderType.ANTHROPIC,
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

    private String resolveUploadMimeType(AiAttachment attachment) {
        if (AiAttachmentUtils.isImage(attachment) || AiAttachmentUtils.isPdf(attachment)) {
            return attachment.getMimeType();
        }
        return "text/plain";
    }

    private String resolveFilesEndpoint(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/v1/messages")) {
            return url.substring(0, url.length() - "/messages".length()) + "/files";
        }
        if (url.endsWith("/messages")) {
            return url.substring(0, url.length() - "/messages".length()) + "/files";
        }
        if (url.endsWith("/v1")) {
            return url + "/files";
        }
        return url + "/v1/files";
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
            String blockType = safeString(block, "type");
            String text = "thinking".equals(blockType)
                    ? safeString(block, "thinking")
                    : safeString(block, "text");
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

    private String resolveInlineImageMimeType(AiAttachment attachment) {
        String mimeType = AiAttachmentUtils.normalizeMimeType(attachment == null ? "" : attachment.getMimeType());
        return mimeType.startsWith("image/") ? mimeType : "image/jpeg";
    }

    private static final class UploadedAnthropicFile {
        private final String fileId;
        private final boolean image;

        private UploadedAnthropicFile(String fileId, boolean image) {
            this.fileId = fileId == null ? "" : fileId;
            this.image = image;
        }

        private String getFileId() {
            return fileId;
        }

        private boolean isImage() {
            return image;
        }
    }
}
