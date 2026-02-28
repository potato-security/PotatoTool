package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP反连平台服务
 * 支持多个HTTP反连平台，用于检测SSRF、XXE、RCE等漏洞
 * 
 * 支持平台：
 * - interactsh.com: 免费，开源，功能强大（推荐）
 * - requestbin.com: 免费，简单易用
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
        REQUESTBIN("requestbin.com"),
        CUSTOM("custom");
        
        private final String name;
        
        Platform(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    // 缓存生成的HTTP反连URL，避免重复请求
    private static final ConcurrentHashMap<String, HttpLogInfo> HTTPLOG_CACHE = new ConcurrentHashMap<>();
    
    // 当前使用的平台
    private static Platform currentPlatform = Platform.INTERACTSH;
    
    // interactsh 配置
    private static volatile String interactshServer = "oast.pro"; // 默认服务器
    private static volatile String interactshToken = null;
    private static final long CACHE_DURATION = 60 * 60 * 1000; // 1小时缓存
    
    // Interactsh 完整协议客户端
    private static volatile InteractshClient interactshClient = null;
    
    // 自定义平台配置
    private static String customServer = null;
    private static String customToken = null;
    
    private static final Gson gson = new Gson();
    
    /**
     * 获取或创建 Interactsh 客户端
     * @return InteractshClient 实例
     */
    private static InteractshClient getInteractshClient() {
        if (interactshClient == null) {
            synchronized (HttpLogService.class) {
                if (interactshClient == null) {
                    interactshClient = new InteractshClient(interactshServer);
                }
            }
        }
        return interactshClient;
    }
    
    /**
     * HTTP反连信息
     */
    public static class HttpLogInfo {
        private String url;              // 完整URL
        private String uniqueId;         // 唯一标识
        private String correlationId;    // 关联ID（用于查询）
        private long createTime;         // 创建时间
        private Platform platform;       // 平台类型
        
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
     * 配置 interactsh 平台
     * @param server 服务器地址（如：oast.pro, interact.sh等）
     * @param token API Token（可选，用于私有部署）
     */
    public static void configureInteractsh(String server, String token) {
        if (server != null && !server.isEmpty()) {
            interactshServer = server;
            interactshToken = token;
            System.out.println("已配置 interactsh: " + server);
        }
    }
    
    /**
     * 配置自定义平台
     * @param server 服务器地址
     * @param token API Token
     */
    public static void configureCustom(String server, String token) {
        if (server != null && !server.isEmpty()) {
            customServer = server;
            customToken = token;
            System.out.println("已配置自定义HTTP反连平台: " + server);
        }
    }
    
    /**
     * 设置当前使用的平台
     * @param platform 平台枚举
     */
    public static void setPlatform(Platform platform) {
        if (platform != null) {
            currentPlatform = platform;
            System.out.println("已切换HTTP反连平台至: " + platform.getName());
        }
    }
    
    /**
     * 获取当前使用的平台
     */
    public static Platform getCurrentPlatform() {
        return currentPlatform;
    }
    
    /**
     * 生成一个唯一的HTTP反连URL
     * 根据当前配置的平台自动选择
     * @return HTTP反连完整URL
     */
    public static synchronized String generateHttpLogUrl() {
        try {
            // 根据当前平台生成URL
            switch (currentPlatform) {
                case REQUESTBIN:
                    return generateRequestBinUrl();
                case CUSTOM:
                    if (customServer != null) {
                        return generateCustomUrl();
                    } else {
                        System.err.println("警告: 自定义平台未配置，切换至interactsh");
                        currentPlatform = Platform.INTERACTSH;
                    }
                    // fall through
                case INTERACTSH:
                default:
                    return generateInteractshUrl();
            }
            
        } catch (Exception e) {
            System.err.println("生成HTTP反连URL失败: " + e.getMessage());
            return generateFallbackUrl();
        }
    }
    
    /**
     * 从 interactsh 生成URL（使用完整协议）
     * 通过 InteractshClient 注册并获取交互 URL
     */
    private static String generateInteractshUrl() {
        try {
            InteractshClient client = getInteractshClient();
            
            // 如果尚未注册，先进行注册
            if (!client.isRegistered()) {
                client.register();
            }
            
            // 生成唯一的交互 URL
            String uniqueId = StrUtils.generateRandomString(8, 8).toLowerCase();
            String fullUrl = client.generateUniqueUrl(uniqueId);
            
            if (fullUrl == null) {
                // 降级到简单 URL 生成
                return generateSimpleInteractshUrl();
            }
            
            String url = "http://" + fullUrl;
            String correlationId = client.getCorrelationId();
            
            // 缓存信息
            HttpLogInfo info = new HttpLogInfo(url, uniqueId, correlationId, Platform.INTERACTSH);
            HTTPLOG_CACHE.put(uniqueId, info);
            
            System.out.println("[Interactsh] 生成 OOB URL: " + url);
            return url;
            
        } catch (Exception e) {
            System.err.println("[Interactsh] 完整协议注册失败，使用简化模式: " + e.getMessage());
            return generateSimpleInteractshUrl();
        }
    }
    
    /**
     * 生成简化的 interactsh URL（不使用注册）
     * 用于降级场景或简单检测
     */
    private static String generateSimpleInteractshUrl() {
        try {
            String uniqueId = StrUtils.generateRandomString(16, 16).toLowerCase();
            String correlationId = uniqueId;
            
            String url = "http://" + uniqueId + "." + interactshServer;
            
            HttpLogInfo info = new HttpLogInfo(url, uniqueId, correlationId, Platform.INTERACTSH);
            HTTPLOG_CACHE.put(uniqueId, info);
            
            System.out.println("[Interactsh] 生成简化 URL: " + url);
            return url;
            
        } catch (Exception e) {
            System.err.println("[Interactsh] 简化 URL 生成失败: " + e.getMessage());
            return generateFallbackUrl();
        }
    }
    
    /**
     * 从 requestbin 生成URL
     */
    private static String generateRequestBinUrl() {
        try {
            // RequestBin API: 创建新的bin
            RequestObj requestObj = new RequestObj()
                    .setUrl("https://requestbin.com/api/v1/bins")
                    .setMethod("POST")
                    .setPostData("{}".getBytes("UTF-8"))
                    .setTimeOut(10)
                    .setReadTimeout(10);
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                int statusCode = response.getResponseCode();
                if (statusCode == 200 || statusCode == 201) {
                    String responseBody = response.getTextStr();
                    
                    // 解析响应获取bin URL
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    if (jsonResponse.has("name")) {
                        String binName = jsonResponse.get("name").getAsString();
                        String binUrl = "https://requestbin.com/" + binName;
                        
                        // 缓存信息
                        HttpLogInfo info = new HttpLogInfo(binUrl, binName, binName, Platform.REQUESTBIN);
                        HTTPLOG_CACHE.put(binName, info);
                        
                        System.out.println("生成RequestBin URL: " + binUrl);
                        return binUrl;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("从RequestBin生成URL失败: " + e.getMessage());
        }
        
        // 降级到interactsh
        System.err.println("RequestBin生成失败，降级到interactsh");
        currentPlatform = Platform.INTERACTSH;
        return generateInteractshUrl();
    }
    
    /**
     * 从自定义平台生成URL
     */
    private static String generateCustomUrl() {
        if (customServer == null || customServer.isEmpty()) {
            System.err.println("警告: 自定义服务器未配置");
            return generateFallbackUrl();
        }
        
        // 自定义平台的URL格式: http://server/uniqueId
        String uniqueId = StrUtils.generateRandomString(16, 16).toLowerCase();
        String url = customServer + "/" + uniqueId;
        
        // 缓存信息
        HttpLogInfo info = new HttpLogInfo(url, uniqueId, uniqueId, Platform.CUSTOM);
        HTTPLOG_CACHE.put(uniqueId, info);
        
        return url;
    }
    
    /**
     * 生成回退URL（当无法连接到平台时）
     * @return 占位符URL
     */
    private static String generateFallbackUrl() {
        String uniqueId = StrUtils.generateRandomString(16, 16).toLowerCase();
        return "http://" + uniqueId + ".httplog.example.com";
    }
    
    /**
     * 查询指定URL的HTTP请求记录
     * 自动识别平台并调用对应的查询方法
     * @param url HTTP反连URL
     * @return 请求记录（JSON格式），如果无记录则返回null
     */
    public static String queryHttpLogRecords(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // 从缓存中查找信息
        HttpLogInfo info = findHttpLogInfo(url);
        if (info != null) {
            switch (info.getPlatform()) {
                case INTERACTSH:
                    return queryInteractshRecords(info);
                case REQUESTBIN:
                    return queryRequestBinRecords(info);
                case CUSTOM:
                    return queryCustomRecords(info);
            }
        }
        
        // 无法识别，尝试根据URL判断
        if (url.contains("interactsh") || url.contains("oast")) {
            return queryInteractshRecordsByUrl(url);
        } else if (url.contains("requestbin")) {
            return queryRequestBinRecordsByUrl(url);
        }
        
        return null;
    }
    
    /**
     * 查询 interactsh 的HTTP记录（使用完整协议）
     * 
     * 通过 InteractshClient 进行轮询查询：
     * - 使用 RSA 密钥对进行加密通信
     * - 使用 secret 进行身份验证
     * - 支持 AES-GCM 解密响应数据
     */
    private static String queryInteractshRecords(HttpLogInfo info) {
        try {
            InteractshClient client = getInteractshClient();
            
            if (!client.isRegistered()) {
                System.err.println("[Interactsh] 客户端未注册，无法查询记录");
                return null;
            }
            
            // 轮询获取交互记录
            List<InteractshClient.Interaction> interactions = client.poll();
            
            if (interactions.isEmpty()) {
                return null;
            }
            
            // 过滤与当前 URL 相关的交互
            String uniqueId = info.getUniqueId();
            List<InteractshClient.Interaction> matchedInteractions = new ArrayList<>();
            
            for (InteractshClient.Interaction interaction : interactions) {
                if (interaction.getFullId() != null && interaction.getFullId().contains(uniqueId)) {
                    matchedInteractions.add(interaction);
                }
            }
            
            if (matchedInteractions.isEmpty()) {
                // 返回所有交互（可能是其他 URL 的）
                return convertInteractionsToJson(interactions);
            }
            
            return convertInteractionsToJson(matchedInteractions);
            
        } catch (Exception e) {
            System.err.println("[Interactsh] 查询记录失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 将交互记录转换为 JSON 字符串
     */
    private static String convertInteractionsToJson(List<InteractshClient.Interaction> interactions) {
        if (interactions == null || interactions.isEmpty()) {
            return null;
        }
        
        JsonArray jsonArray = new JsonArray();
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
            jsonArray.add(obj);
        }
        
        return jsonArray.toString();
    }
    
    /**
     * 查询 RequestBin 的HTTP记录
     */
    private static String queryRequestBinRecords(HttpLogInfo info) {
        try {
            String apiUrl = "https://requestbin.com/api/v1/bins/" + info.getUniqueId() + "/requests";
            
            RequestObj requestObj = new RequestObj()
                    .setUrl(apiUrl)
                    .setMethod("GET")
                    .setTimeOut(10)
                    .setReadTimeout(10);
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    return response.getTextStr();
                }
            }
        } catch (Exception e) {
            System.err.println("查询RequestBin记录失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 查询自定义平台的HTTP记录
     */
    private static String queryCustomRecords(HttpLogInfo info) {
        if (customServer == null || customServer.isEmpty()) {
            return null;
        }
        
        try {
            String apiUrl = customServer + "/api/records/" + info.getUniqueId();
            
            RequestObj requestObj = new RequestObj()
                    .setUrl(apiUrl)
                    .setMethod("GET")
                    .setTimeOut(10)
                    .setReadTimeout(10);
            
            if (customToken != null && !customToken.isEmpty()) {
                requestObj.setBearerToken(customToken);
            }
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    return response.getTextStr();
                }
            }
        } catch (Exception e) {
            System.err.println("查询自定义平台记录失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 根据URL查询interactsh记录（兼容方法）
     */
    private static String queryInteractshRecordsByUrl(String httpUrl) {
        // 从URL提取uniqueId
        String uniqueId = extractUniqueIdFromUrl(httpUrl);
        if (uniqueId != null) {
            HttpLogInfo info = new HttpLogInfo(httpUrl, uniqueId, uniqueId, Platform.INTERACTSH);
            return queryInteractshRecords(info);
        }
        return null;
    }
    
    /**
     * 根据URL查询RequestBin记录（兼容方法）
     */
    private static String queryRequestBinRecordsByUrl(String httpUrl) {
        // 从URL提取bin name
        String binName = extractBinNameFromUrl(httpUrl);
        if (binName != null) {
            HttpLogInfo info = new HttpLogInfo(httpUrl, binName, binName, Platform.REQUESTBIN);
            return queryRequestBinRecords(info);
        }
        return null;
    }
    
    /**
     * 从缓存中查找HttpLogInfo
     */
    private static HttpLogInfo findHttpLogInfo(String url) {
        for (HttpLogInfo info : HTTPLOG_CACHE.values()) {
            if (info.getUrl().equals(url)) {
                return info;
            }
        }
        return null;
    }
    
    /**
     * 从URL中提取唯一标识
     */
    private static String extractUniqueIdFromUrl(String url) {
        try {
            // 提取域名的第一部分
            String host = url.replaceFirst("https?://", "").split("/")[0];
            return host.split("\\.")[0];
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从RequestBin URL中提取bin name
     */
    private static String extractBinNameFromUrl(String url) {
        try {
            // RequestBin URL格式: https://requestbin.com/binName
            String[] parts = url.split("/");
            if (parts.length >= 4) {
                return parts[3];
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
    
    /**
     * 检查HTTP反连URL是否有请求记录
     * @param url HTTP反连URL
     * @return true表示有请求记录
     */
    public static boolean hasHttpRequest(String url) {
        String records = queryHttpLogRecords(url);
        return records != null && !records.trim().isEmpty() && !records.equals("[]");
    }
    
    /**
     * 清除HTTP反连缓存
     */
    public static synchronized void clearCache() {
        HTTPLOG_CACHE.clear();
        System.out.println("HTTP反连缓存已清除");
    }
    
    // ==================== 连通性测试方法 ====================
    
    /**
     * 测试 interactsh 平台连通性
     * @return 测试结果对象
     */
    public static TestResult testInteractshConnectivity() {
        TestResult result = new TestResult(Platform.INTERACTSH);
        long startTime = System.currentTimeMillis();
        
        try {
            // 生成测试URL
            String testUrl = generateInteractshUrl();
            result.url = testUrl;
            result.success = testUrl != null && !testUrl.contains("example.com");
            result.message = result.success ? "连接成功，服务器: " + interactshServer : "生成URL失败";
            result.responseTime = System.currentTimeMillis() - startTime;
            
        } catch (Exception e) {
            result.success = false;
            result.message = "连接失败: " + e.getMessage();
            result.responseTime = System.currentTimeMillis() - startTime;
        }
        
        return result;
    }
    
    /**
     * 测试结果类
     */
    public static class TestResult {
        public Platform platform;
        public boolean success;
        public String message;
        public String url;
        public long responseTime; // 毫秒
        
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
                url != null ? url : "N/A"
            );
        }
    }
    
    // ==================== OOB 检测便捷方法 ====================
    
    /**
     * 检查指定 URL 是否有交互记录
     * 
     * @param url OOB URL
     * @return 是否检测到交互
     */
    public static boolean hasInteraction(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // 尝试使用 InteractshClient 检查
        try {
            InteractshClient client = getInteractshClient();
            if (client.isRegistered()) {
                return client.hasInteraction(url);
            }
        } catch (Exception e) {
            // 降级到查询 API
        }
        
        // 降级：通过查询记录判断
        String records = queryHttpLogRecords(url);
        return records != null && !records.isEmpty() && !records.equals("[]");
    }
    
    /**
     * 等待交互（阻塞方法）
     * 
     * @param url OOB URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 交互记录，如果超时返回 null
     */
    public static InteractshClient.Interaction waitForInteraction(String url, int timeoutSeconds) {
        try {
            InteractshClient client = getInteractshClient();
            if (client.isRegistered()) {
                return client.waitForInteraction(url, timeoutSeconds);
            }
        } catch (Exception e) {
            System.err.println("[HttpLogService] 等待交互失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取 Interactsh 客户端（用于高级操作）
     * 
     * @return InteractshClient 实例
     */
    public static InteractshClient getClient() {
        return getInteractshClient();
    }
    
    /**
     * 关闭 Interactsh 客户端（清理资源）
     */
    public static void closeClient() {
        if (interactshClient != null) {
            synchronized (HttpLogService.class) {
                if (interactshClient != null) {
                    interactshClient.close();
                    interactshClient = null;
                }
            }
        }
    }
}


















