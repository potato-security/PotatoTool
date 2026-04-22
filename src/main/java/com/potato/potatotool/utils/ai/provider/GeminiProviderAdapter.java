package com.potato.potatotool.utils.ai.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.ai.AiAttachmentUtils;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiMessage;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.potato.potatotool.utils.network.RequestUtils.requests;

public class GeminiProviderAdapter implements AiProviderAdapter {
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
        if (supportResult.getMode() == AiAttachmentMode.INLINE_MEDIA) {
            return PreparedRequest.of(buildRequest(runtimeConfig, request, null, attachments));
        }

        List<UploadedGeminiFile> uploadedFiles = new ArrayList<UploadedGeminiFile>();
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
                                    List<UploadedGeminiFile> uploadedFiles,
                                    List<AiAttachment> inlineAttachments) {
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
        contents.add(createUserContent(request.getQuestion(), uploadedFiles, inlineAttachments));
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

    private JsonObject createUserContent(String question,
                                         List<UploadedGeminiFile> uploadedFiles,
                                         List<AiAttachment> inlineAttachments) {
        if ((uploadedFiles == null || uploadedFiles.isEmpty())
                && (inlineAttachments == null || inlineAttachments.isEmpty())) {
            return createContent(new AiMessage("user", question));
        }

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", question == null ? "" : question);
        parts.add(textPart);

        if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
            for (UploadedGeminiFile uploadedFile : uploadedFiles) {
                if (uploadedFile == null) {
                    continue;
                }
                JsonObject filePart = new JsonObject();
                JsonObject fileData = new JsonObject();
                fileData.addProperty("mimeType", uploadedFile.getMimeType());
                fileData.addProperty("fileUri", uploadedFile.getFileUri());
                filePart.add("fileData", fileData);
                parts.add(filePart);
            }
        } else {
            for (AiAttachment attachment : inlineAttachments) {
                if (attachment == null) {
                    continue;
                }
                JsonObject inlinePart = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", attachment.getMimeType());
                try {
                    inlineData.addProperty("data", AiAttachmentUtils.readBase64(attachment));
                } catch (Exception e) {
                    throw new IllegalStateException("读取附件失败: " + attachment.getFileName(), e);
                }
                inlinePart.add("inlineData", inlineData);
                parts.add(inlinePart);
            }
        }
        content.add("parts", parts);
        return content;
    }

    private UploadedGeminiFile uploadAttachment(AiRuntimeConfig runtimeConfig, AiAttachment attachment) throws Exception {
        if (attachment == null) {
            throw new IllegalArgumentException("附件不能为空");
        }
        File file = attachment.toFile();
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("附件不存在: " + attachment.getFileName());
        }

        int timeoutSec = Math.max(1, runtimeConfig.getTimeoutMs() / 1000);

        Map<String, String> startHeaders = new HashMap<String, String>();
        startHeaders.put("Content-Type", "application/json");
        startHeaders.put("x-goog-api-key", runtimeConfig.getApiKey());
        startHeaders.put("X-Goog-Upload-Protocol", "resumable");
        startHeaders.put("X-Goog-Upload-Command", "start");
        startHeaders.put("X-Goog-Upload-Header-Content-Length", String.valueOf(Math.max(0L, attachment.getFileSize())));
        startHeaders.put("X-Goog-Upload-Header-Content-Type", attachment.getMimeType());

        JsonObject metadata = new JsonObject();
        JsonObject fileMetadata = new JsonObject();
        fileMetadata.addProperty("display_name", attachment.getFileName());
        metadata.add("file", fileMetadata);

        RequestObj startRequest = new RequestObj()
                .setMethod("POST")
                .setPostMethod("JSON")
                .setUrl(resolveUploadEndpoint(runtimeConfig.getBaseUrl()))
                .setHeaders(startHeaders)
                .setPostData(new Gson().toJson(metadata))
                .setTimeOut(timeoutSec)
                .setReadTimeout(timeoutSec)
                .setWriteTimeout(timeoutSec)
                .setCallTimeout(timeoutSec)
                .setInternalAiRequest(runtimeConfig.isBuiltinAi());
        ProxyUtils.applyProxy(startRequest, runtimeConfig.isUseProxy());

        String uploadUrl;
        try (CustomHttpResponse response = requests(startRequest)) {
            List<String> uploadUrls = response.getHeaderField("X-Goog-Upload-URL");
            uploadUrl = uploadUrls == null || uploadUrls.isEmpty() ? "" : uploadUrls.get(0);
            if (uploadUrl == null || uploadUrl.trim().isEmpty()) {
                throw new IllegalStateException("Gemini 附件上传失败: 未返回上传地址");
            }
        }

        Map<String, String> uploadHeaders = new HashMap<String, String>();
        uploadHeaders.put("X-Goog-Upload-Offset", "0");
        uploadHeaders.put("X-Goog-Upload-Command", "upload, finalize");

        RequestObj uploadRequest = new RequestObj()
                .setMethod("POST")
                .setPostMethod("RAW")
                .setUrl(uploadUrl)
                .setHeaders(uploadHeaders)
                .setPostData(file)
                .setTimeOut(timeoutSec)
                .setReadTimeout(timeoutSec)
                .setWriteTimeout(timeoutSec)
                .setCallTimeout(timeoutSec)
                .setInternalAiRequest(runtimeConfig.isBuiltinAi());
        ProxyUtils.applyProxy(uploadRequest, runtimeConfig.isUseProxy());

        try (CustomHttpResponse response = requests(uploadRequest)) {
            JsonObject json = JsonParser.parseString(response.getTextStr()).getAsJsonObject();
            String errorMessage = readErrorMessage(json);
            if (!errorMessage.isEmpty()) {
                throw new IllegalStateException(errorMessage);
            }
            JsonObject fileObj = safeObject(json, "file");
            String fileName = safeString(fileObj, "name");
            String fileUri = safeString(fileObj, "uri");
            String mimeType = safeString(fileObj, "mimeType");
            if (fileName.isEmpty() || fileUri.isEmpty()) {
                throw new IllegalStateException("Gemini 附件上传失败: 未返回文件引用");
            }
            return new UploadedGeminiFile(fileName, fileUri, mimeType.isEmpty() ? attachment.getMimeType() : mimeType);
        }
    }

    private void deleteUploadedFiles(AiRuntimeConfig runtimeConfig, List<UploadedGeminiFile> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return;
        }
        for (UploadedGeminiFile uploadedFile : uploadedFiles) {
            if (uploadedFile == null || uploadedFile.getName().trim().isEmpty()) {
                continue;
            }
            try {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("x-goog-api-key", runtimeConfig.getApiKey());

                int timeoutSec = Math.max(1, runtimeConfig.getTimeoutMs() / 1000);
                RequestObj requestObj = new RequestObj()
                        .setMethod("DELETE")
                        .setUrl(resolveApiBase(runtimeConfig.getBaseUrl()) + "/" + uploadedFile.getName())
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

    private String resolveUploadEndpoint(String baseUrl) {
        String apiBase = resolveApiBase(baseUrl);
        int slashIndex = apiBase.indexOf('/', apiBase.indexOf("//") + 2);
        String origin = slashIndex < 0 ? apiBase : apiBase.substring(0, slashIndex);
        String versionPath = slashIndex < 0 ? "/v1beta" : apiBase.substring(slashIndex);
        return origin + "/upload" + versionPath + "/files";
    }

    private String resolveApiBase(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            url = url.substring(0, queryIndex);
        }
        if (url.contains("/v1beta/")) {
            return url.substring(0, url.indexOf("/v1beta/") + "/v1beta".length());
        }
        if (url.endsWith("/v1beta")) {
            return url;
        }
        if (url.contains("/v1/")) {
            return url.substring(0, url.indexOf("/v1/") + "/v1".length());
        }
        if (url.endsWith("/v1")) {
            return url;
        }
        int schemeIndex = url.indexOf("//");
        if (schemeIndex >= 0) {
            int slashIndex = url.indexOf('/', schemeIndex + 2);
            if (slashIndex >= 0) {
                return url.substring(0, slashIndex) + "/v1beta";
            }
            if (!url.isEmpty()) {
                return url + "/v1beta";
            }
        }
        return "https://generativelanguage.googleapis.com/v1beta";
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

    private static final class UploadedGeminiFile {
        private final String name;
        private final String fileUri;
        private final String mimeType;

        private UploadedGeminiFile(String name, String fileUri, String mimeType) {
            this.name = name == null ? "" : name;
            this.fileUri = fileUri == null ? "" : fileUri;
            this.mimeType = mimeType == null ? "" : mimeType;
        }

        private String getName() {
            return name;
        }

        private String getFileUri() {
            return fileUri;
        }

        private String getMimeType() {
            return mimeType;
        }
    }
}
