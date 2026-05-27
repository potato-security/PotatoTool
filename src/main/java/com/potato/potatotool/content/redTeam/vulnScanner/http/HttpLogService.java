package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP反连平台服务
 * 支持多个HTTP反连平台，用于检测SSRF、XXE、RCE等漏洞
 *
 * 支持平台：
 * - interactsh.com: 免费，开源，功能强大（推荐）
 * - 自建平台: 支持自定义服务器
 *
 * @author Potato
 * @date 2025-11-01
 */
public class HttpLogService {

    /**
     * HTTP反连平台枚举
     */
    public enum Platform {
        INTERACTSH("interactsh.com"),
        CUSTOM("custom");

        private final String name;

        Platform(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * HTTP反连信息
     */
    public static class HttpLogInfo {
        private final String url;
        private final String uniqueId;
        private final String correlationId;
        private final long createTime;
        private final Platform platform;

        public HttpLogInfo(String url, String uniqueId, String correlationId, Platform platform) {
            this.url = url;
            this.uniqueId = uniqueId;
            this.correlationId = correlationId;
            this.createTime = System.currentTimeMillis();
            this.platform = platform;
        }

        public String getUrl() { return url; }
        public String getUniqueId() { return uniqueId; }
        public String getCorrelationId() { return correlationId; }
        public long getCreateTime() { return createTime; }
        public Platform getPlatform() { return platform; }
    }

    /**
     * 测试结果类
     */
    public static class TestResult {
        public Platform platform;
        public boolean success;
        public String message;
        public String url;
        public long responseTime;

        public TestResult(Platform platform) {
            this.platform = platform;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s (耗时: %dms, URL: %s)",
                    platform.getName(),
                    success ? "✓ 成功" : "✗ 失败",
                    message,
                    responseTime,
                    url != null ? url : "N/A");
        }
    }

    private static final ConcurrentHashMap<String, HttpLogInfo> HTTPLOG_CACHE = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    private static Platform currentPlatform = Platform.INTERACTSH;

    private static volatile String interactshServer = "oast.pro";
    private static volatile String interactshToken = null;
    private static volatile InteractshClient interactshClient = null;

    private static volatile String customServer = null;
    private static volatile String customToken = null;

    private static volatile long cacheDurationMs = 60 * 60 * 1000;
    private static volatile int interactionWaitSeconds = 8;

    private static InteractshClient getInteractshClient() {
        if (interactshClient == null) {
            synchronized (HttpLogService.class) {
                if (interactshClient == null) {
                    interactshClient = new InteractshClient(interactshServer, interactshToken);
                }
            }
        }
        return interactshClient;
    }

    private static void resetInteractshClient() {
        synchronized (HttpLogService.class) {
            if (interactshClient != null) {
                interactshClient.close();
                interactshClient = null;
            }
        }
    }

    public static synchronized void setCacheDurationSeconds(long seconds) {
        if (seconds > 0) {
            cacheDurationMs = seconds * 1000;
            removeExpiredCacheEntries();
        }
    }

    public static synchronized void setInteractionWaitSeconds(int seconds) {
        interactionWaitSeconds = Math.max(0, seconds);
    }

    public static int getInteractionWaitSeconds() {
        return interactionWaitSeconds;
    }

    public static synchronized void configureInteractsh(String server, String token) {
        if (server == null || server.trim().isEmpty()) {
            return;
        }
        interactshServer = server.trim();
        interactshToken = token == null ? null : token.trim();
        resetInteractshClient();
        clearCache();
        System.out.println("已配置 interactsh: " + interactshServer);
    }

    public static synchronized void configureCustom(String server, String token) {
        if (server == null || server.trim().isEmpty()) {
            return;
        }
        customServer = server.trim();
        customToken = token == null ? null : token.trim();
        clearCache();
        System.out.println("已配置自定义HTTP反连平台: " + customServer);
    }

    public static synchronized void setPlatform(Platform platform) {
        if (platform != null && currentPlatform != platform) {
            currentPlatform = platform;
            clearCache();
            System.out.println("已切换HTTP反连平台至: " + platform.getName());
        }
    }

    public static Platform getCurrentPlatform() {
        return currentPlatform;
    }

    public static synchronized String generateHttpLogUrl() {
        try {
            removeExpiredCacheEntries();
            switch (currentPlatform) {
                case CUSTOM:
                    if (customServer != null && !customServer.isEmpty()) {
                        return generateCustomUrl();
                    }
                    System.err.println("警告: 自定义平台未配置，切换至interactsh");
                    currentPlatform = Platform.INTERACTSH;
                    return generateInteractshUrl();
                case INTERACTSH:
                default:
                    return generateInteractshUrl();
            }
        } catch (Exception e) {
            System.err.println("生成HTTP反连URL失败: " + e.getMessage());
            return generateFallbackUrl();
        }
    }

    private static String generateInteractshUrl() {
        try {
            InteractshClient client = getInteractshClient();
            if (!client.isRegistered()) {
                client.register();
            }

            String uniqueId = StrUtils.generateRandomString(8, 8).toLowerCase();
            String fullUrl = client.generateUniqueUrl(uniqueId);
            if (fullUrl == null) {
                return generateSimpleInteractshUrl();
            }

            String url = "http://" + fullUrl;
            String callbackId = extractUniqueIdFromUrl(fullUrl);
            if (callbackId == null || callbackId.trim().isEmpty()) {
                callbackId = uniqueId;
            }
            HttpLogInfo info = new HttpLogInfo(url, callbackId, client.getCorrelationId(), Platform.INTERACTSH);
            HTTPLOG_CACHE.put(callbackId, info);
            return url;
        } catch (Exception e) {
            System.err.println("[Interactsh] 完整协议注册失败，使用简化模式: " + e.getMessage());
            return generateSimpleInteractshUrl();
        }
    }

    private static String generateSimpleInteractshUrl() {
        String uniqueId = StrUtils.generateRandomString(16, 16).toLowerCase();
        String url = "http://" + uniqueId + "." + interactshServer;
        HttpLogInfo info = new HttpLogInfo(url, uniqueId, uniqueId, Platform.INTERACTSH);
        HTTPLOG_CACHE.put(uniqueId, info);
        return url;
    }

    private static String generateCustomUrl() {
        String uniqueId = StrUtils.generateRandomString(16, 16).toLowerCase();
        String normalized = customServer.endsWith("/") ? customServer.substring(0, customServer.length() - 1) : customServer;
        String url = normalized + "/" + uniqueId;
        HttpLogInfo info = new HttpLogInfo(url, uniqueId, uniqueId, Platform.CUSTOM);
        HTTPLOG_CACHE.put(uniqueId, info);
        return url;
    }

    private static String generateFallbackUrl() {
        String uniqueId = StrUtils.generateRandomString(16, 16).toLowerCase();
        return "http://" + uniqueId + ".httplog.example.com";
    }

    public static String queryHttpLogRecords(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        removeExpiredCacheEntries();
        HttpLogInfo info = findHttpLogInfo(url);
        if (info != null) {
            switch (info.getPlatform()) {
                case INTERACTSH:
                    return queryInteractshRecords(info);
                case CUSTOM:
                    return queryCustomRecords(info);
                default:
                    return null;
            }
        }

        if (url.contains("interactsh") || url.contains("oast")) {
            return queryInteractshRecordsByUrl(url);
        }
        return null;
    }

    private static boolean matchesInteraction(HttpLogInfo info, InteractshClient.Interaction interaction) {
        if (info == null || interaction == null) {
            return false;
        }
        String uniqueId = info.getUniqueId();
        return interaction.getFullId() != null && interaction.getFullId().contains(uniqueId)
                || interaction.getUniqueId() != null && interaction.getUniqueId().equals(uniqueId);
    }

    private static InteractshClient.Interaction findMatchedInteractshInteraction(HttpLogInfo info) {
        try {
            InteractshClient client = getInteractshClient();
            if (!client.isRegistered()) {
                return null;
            }
            client.poll();
            List<InteractshClient.Interaction> interactions = client.getInteractions();
            for (InteractshClient.Interaction interaction : interactions) {
                if (matchesInteraction(info, interaction)) {
                    return interaction;
                }
            }
        } catch (Exception e) {
            System.err.println("[Interactsh] 查询交互失败: " + e.getMessage());
        }
        return null;
    }

    private static String queryInteractshRecords(HttpLogInfo info) {
        try {
            InteractshClient client = getInteractshClient();
            if (!client.isRegistered()) {
                return null;
            }
            client.poll();

            List<InteractshClient.Interaction> interactions = client.getInteractions();
            if (interactions.isEmpty()) {
                return null;
            }

            List<InteractshClient.Interaction> matched = new ArrayList<>();
            for (InteractshClient.Interaction interaction : interactions) {
                if (matchesInteraction(info, interaction)) {
                    matched.add(interaction);
                }
            }
            return matched.isEmpty() ? null : convertInteractionsToJson(matched);
        } catch (Exception e) {
            System.err.println("[Interactsh] 查询记录失败: " + e.getMessage());
            return null;
        }
    }

    private static String queryCustomRecords(HttpLogInfo info) {
        if (customServer == null || customServer.isEmpty()) {
            return null;
        }

        try {
            String normalized = customServer.endsWith("/") ? customServer.substring(0, customServer.length() - 1) : customServer;
            String apiUrl = normalized + "/api/records/" + info.getUniqueId();
            RequestObj requestObj = new RequestObj()
                    .setUrl(apiUrl)
                    .setMethod("GET")
                    .setTimeOut(10)
                    .setReadTimeout(10);
            HttpHandler.applyProxySettings(requestObj);

            if (customToken != null && !customToken.isEmpty()) {
                requestObj.setBearerToken(customToken);
            }

            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    return response.getTextStr();
                }
                System.err.println("[Custom OOB] 查询失败，状态码=" + response.getResponseCode() + ", url=" + apiUrl);
            }
        } catch (Exception e) {
            System.err.println("查询自定义平台记录失败: " + e.getMessage());
        }
        return null;
    }

    private static String queryInteractshRecordsByUrl(String httpUrl) {
        String uniqueId = extractUniqueIdFromUrl(httpUrl);
        if (uniqueId == null) {
            return null;
        }
        HttpLogInfo info = new HttpLogInfo(httpUrl, uniqueId, uniqueId, Platform.INTERACTSH);
        return queryInteractshRecords(info);
    }

    private static String convertInteractionsToJson(List<InteractshClient.Interaction> interactions) {
        if (interactions == null || interactions.isEmpty()) {
            return null;
        }
        JsonArray array = new JsonArray();
        for (InteractshClient.Interaction interaction : interactions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("protocol", interaction.getProtocol());
            obj.addProperty("remote-address", interaction.getRemoteAddress());
            obj.addProperty("timestamp", interaction.getTimestamp());
            obj.addProperty("unique-id", interaction.getUniqueId());
            obj.addProperty("full-id", interaction.getFullId());
            if (interaction.getRawRequest() != null) {
                obj.addProperty("raw-request", interaction.getRawRequest());
            }
            if (interaction.getRawResponse() != null) {
                obj.addProperty("raw-response", interaction.getRawResponse());
            }
            if (interaction.getQType() != null) {
                obj.addProperty("q-type", interaction.getQType());
            }
            array.add(obj);
        }
        return gson.toJson(array);
    }

    private static HttpLogInfo findHttpLogInfo(String url) {
        String normalized = normalizeLogUrl(url);
        for (HttpLogInfo info : HTTPLOG_CACHE.values()) {
            if (info.getUrl().equals(url) || info.getUrl().equals(normalized)) {
                return info;
            }
        }
        return null;
    }

    private static String extractUniqueIdFromUrl(String url) {
        try {
            String host = extractHost(url);
            return host.split("\\.")[0];
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractHost(String url) {
        if (url == null) {
            return null;
        }
        String normalized = url.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        try {
            if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                return new URL(normalized).getHost();
            }
        } catch (Exception ignored) {
        }
        if (normalized.contains("://")) {
            normalized = normalized.substring(normalized.indexOf("://") + 3);
        }
        normalized = normalized.split("/", 2)[0];
        int at = normalized.lastIndexOf('@');
        if (at >= 0) {
            normalized = normalized.substring(at + 1);
        }
        int colon = normalized.lastIndexOf(':');
        if (colon > 0 && normalized.indexOf(']') < 0) {
            normalized = normalized.substring(0, colon);
        }
        return normalized;
    }

    public static String toBareCallbackHost(String url) {
        String host = extractHost(url);
        return host == null ? url : host;
    }

    private static String normalizeLogUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "http://" + trimmed;
    }

    private static void removeExpiredCacheEntries() {
        long now = System.currentTimeMillis();
        HTTPLOG_CACHE.entrySet().removeIf(entry -> now - entry.getValue().getCreateTime() > cacheDurationMs);
    }

    public static boolean hasHttpRequest(String url) {
        String records = queryHttpLogRecords(url);
        return records != null && !records.trim().isEmpty() && !records.equals("[]");
    }

    public static synchronized void clearCache() {
        HTTPLOG_CACHE.clear();
        System.out.println("HTTP反连缓存已清除");
    }

    public static TestResult testInteractshConnectivity() {
        TestResult result = new TestResult(Platform.INTERACTSH);
        long startTime = System.currentTimeMillis();
        try {
            InteractshClient client = getInteractshClient();
            String interactionUrl;
            if (client.isRegistered()) {
                interactionUrl = client.getInteractionUrl();
            } else {
                interactionUrl = client.register();
            }

            if (interactionUrl != null && !interactionUrl.startsWith("http")) {
                interactionUrl = "http://" + interactionUrl;
            }

            result.url = interactionUrl;
            result.success = client.isRegistered() && interactionUrl != null && !interactionUrl.trim().isEmpty();
            result.message = result.success ? "连接成功，服务器: " + interactshServer : "注册失败";
        } catch (Exception e) {
            result.success = false;
            result.message = "连接失败: " + e.getMessage();
        }
        result.responseTime = System.currentTimeMillis() - startTime;
        return result;
    }

    public static TestResult testCustomConnectivity() {
        TestResult result = new TestResult(Platform.CUSTOM);
        long startTime = System.currentTimeMillis();
        try {
            if (customServer == null || customServer.isEmpty()) {
                result.success = false;
                result.message = "自定义平台未配置";
                result.responseTime = System.currentTimeMillis() - startTime;
                return result;
            }

            String normalized = customServer.endsWith("/")
                    ? customServer.substring(0, customServer.length() - 1)
                    : customServer;
            String testUrl = normalized + "/";

            RequestObj requestObj = new RequestObj()
                    .setUrl(testUrl)
                    .setMethod("GET")
                    .setTimeOut(8)
                    .setReadTimeout(8);
            HttpHandler.applyProxySettings(requestObj);

            if (customToken != null && !customToken.isEmpty()) {
                requestObj.setBearerToken(customToken);
            }

            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                int statusCode = response.getResponseCode();
                result.url = testUrl;
                result.success = statusCode >= 200 && statusCode < 500;
                result.message = result.success
                        ? "连接成功，服务器: " + customServer + "，状态码: " + statusCode
                        : "连接失败，状态码: " + statusCode;
            }
        } catch (Exception e) {
            result.success = false;
            result.message = "连接失败: " + e.getMessage();
        }
        result.responseTime = System.currentTimeMillis() - startTime;
        return result;
    }

    public static boolean hasInteraction(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String records = queryHttpLogRecords(url);
        return records != null && !records.isEmpty() && !records.equals("[]");
    }

    public static InteractshClient.Interaction waitForInteraction(String url, int timeoutSeconds) {
        if (url == null || url.isEmpty() || timeoutSeconds <= 0) {
            return null;
        }

        HttpLogInfo info = findHttpLogInfo(url);
        if (info == null && (url.contains("interactsh") || url.contains("oast"))) {
            String uniqueId = extractUniqueIdFromUrl(url);
            if (uniqueId != null) {
                info = new HttpLogInfo(url, uniqueId, uniqueId, Platform.INTERACTSH);
            }
        }
        if (info == null || info.getPlatform() != Platform.INTERACTSH) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            InteractshClient.Interaction interaction = findMatchedInteractshInteraction(info);
            if (interaction != null) {
                return interaction;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    public static InteractshClient getClient() {
        return getInteractshClient();
    }

    public static InteractshClient.Interaction waitForConfiguredInteraction(String url) {
        return waitForInteraction(url, interactionWaitSeconds);
    }

    public static void closeClient() {
        resetInteractshClient();
    }
}
