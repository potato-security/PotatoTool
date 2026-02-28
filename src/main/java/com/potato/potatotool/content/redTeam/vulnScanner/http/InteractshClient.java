package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Interactsh 完整协议客户端
 * 
 * 实现了完整的 Interactsh OOB 交互检测协议：
 * 1. RSA 密钥对生成
 * 2. 客户端注册 (POST /register)
 * 3. 交互轮询 (GET /poll)
 * 4. AES-GCM 解密交互数据
 * 5. 客户端注销 (POST /deregister)
 * 
 * 支持的交互类型：
 * - DNS 查询
 * - HTTP/HTTPS 请求
 * - SMTP 连接
 * - LDAP 查询
 * 
 * @author Potato
 * @date 2025-11-26
 */
public class InteractshClient {
    
    
    // 默认服务器列表
    private static final String[] DEFAULT_SERVERS = {
        "oast.pro",
        "oast.live", 
        "oast.site",
        "oast.online",
        "oast.fun",
        "oast.me",
        "interactsh.com"
    };
    
    // RSA 密钥长度
    private static final int RSA_KEY_SIZE = 2048;
    
    // AES-GCM 参数
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    // 客户端状态
    private String serverUrl;
    private String correlationId;
    private String secretKey;
    private KeyPair rsaKeyPair;
    private boolean registered = false;
    
    // 交互记录缓存
    private final List<Interaction> interactions = Collections.synchronizedList(new ArrayList<>());
    
    // 后台轮询
    private ScheduledExecutorService pollExecutor;
    private volatile boolean polling = false;
    
    // 回调监听器
    private InteractionCallback callback;
    
    // 全局客户端实例缓存
    private static final ConcurrentHashMap<String, InteractshClient> CLIENT_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 交互记录
     */
    public static class Interaction {
        private String uniqueId;
        private String fullId;
        private String rawRequest;
        private String rawResponse;
        private String remoteAddress;
        private String protocol;      // dns, http, smtp, ldap
        private String qType;         // DNS 查询类型
        private long timestamp;
        private Map<String, Object> extra = new HashMap<>();
        
        public String getUniqueId() { return uniqueId; }
        public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }
        public String getFullId() { return fullId; }
        public void setFullId(String fullId) { this.fullId = fullId; }
        public String getRawRequest() { return rawRequest; }
        public void setRawRequest(String rawRequest) { this.rawRequest = rawRequest; }
        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
        public String getRemoteAddress() { return remoteAddress; }
        public void setRemoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public String getQType() { return qType; }
        public void setQType(String qType) { this.qType = qType; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getExtra() { return extra; }
        
        @Override
        public String toString() {
            return String.format("Interaction{protocol=%s, remoteAddress=%s, timestamp=%d}", 
                protocol, remoteAddress, timestamp);
        }
    }
    
    /**
     * 交互回调接口
     */
    public interface InteractionCallback {
        void onInteraction(Interaction interaction);
        void onError(String error);
    }
    
    /**
     * 创建新的 Interactsh 客户端
     */
    public InteractshClient() {
        this(DEFAULT_SERVERS[0]);
    }
    
    /**
     * 创建指定服务器的 Interactsh 客户端
     * 
     * @param server 服务器地址（不含协议前缀）
     */
    public InteractshClient(String server) {
        this.serverUrl = server.startsWith("http") ? server : "https://" + server;
    }
    
    /**
     * 获取或创建全局客户端实例
     * 
     * @param server 服务器地址
     * @return 客户端实例
     */
    public static InteractshClient getInstance(String server) {
        return CLIENT_CACHE.computeIfAbsent(server, InteractshClient::new);
    }
    
    /**
     * 获取默认服务器的客户端
     */
    public static InteractshClient getDefaultInstance() {
        return getInstance(DEFAULT_SERVERS[0]);
    }
    
    /**
     * 注册客户端并获取交互 URL
     * 
     * @return 交互 URL（如 xxx.oast.pro）
     * @throws Exception 注册失败
     */
    public synchronized String register() throws Exception {
        if (registered) {
            return getInteractionUrl();
        }
        
        // 1. 生成 RSA 密钥对
        rsaKeyPair = generateRSAKeyPair();
        
        // 2. 生成随机 correlation ID 和 secret
        correlationId = generateCorrelationId();
        secretKey = generateSecretKey();
        
        // 3. 准备注册请求（公钥需要 PEM 格式）
        String publicKeyPem = convertPublicKeyToPem(rsaKeyPair.getPublic());
        
        JsonObject registerRequest = new JsonObject();
        registerRequest.addProperty("public-key", publicKeyPem);
        registerRequest.addProperty("secret-key", secretKey);
        registerRequest.addProperty("correlation-id", correlationId);
        
        // 4. 发送注册请求
        String registerUrl = serverUrl + "/register";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        RequestObj requestObj = new RequestObj()
            .setUrl(registerUrl)
            .setMethod("POST")
            .setHeaders(headers)
            .setPostData(registerRequest)
            .setTimeOut(15)
            .setReadTimeout(15);
        
        try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
            int statusCode = response.getResponseCode();
            String responseBody = response.getTextStr();
            
            if (statusCode == 200 || statusCode == 201) {
                // 解析响应
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                
                // 检查是否返回了新的 correlation-id
                if (jsonResponse.has("correlation-id")) {
                    correlationId = jsonResponse.get("correlation-id").getAsString();
                }
                
                registered = true;
                System.out.println("[Interactsh] 注册成功: " + getInteractionUrl());
                return getInteractionUrl();
                
            } else {
                throw new Exception("注册失败: HTTP " + statusCode + " - " + responseBody);
            }
        }
    }
    
    /**
     * 获取交互 URL
     * 
     * @return 完整的交互 URL
     */
    public String getInteractionUrl() {
        if (correlationId == null) {
            return null;
        }
        // 从 serverUrl 提取域名
        String domain = serverUrl.replace("https://", "").replace("http://", "");
        return correlationId + "." + domain;
    }
    
    /**
     * 生成唯一的子域名 URL
     * 用于区分不同的漏洞测试点
     * 
     * @param identifier 标识符
     * @return 唯一的交互 URL
     */
    public String generateUniqueUrl(String identifier) {
        if (correlationId == null) {
            return null;
        }
        String uniquePart = generateShortId();
        String domain = serverUrl.replace("https://", "").replace("http://", "");
        return uniquePart + "." + correlationId + "." + domain;
    }
    
    /**
     * 轮询获取交互记录
     * 
     * @return 新的交互记录列表
     */
    public List<Interaction> poll() {
        if (!registered || correlationId == null) {
            return Collections.emptyList();
        }
        
        List<Interaction> newInteractions = new ArrayList<>();
        
        try {
            String pollUrl = serverUrl + "/poll?id=" + correlationId + "&secret=" + secretKey;
            
            RequestObj requestObj = new RequestObj()
                .setUrl(pollUrl)
                .setMethod("GET")
                .setTimeOut(10)
                .setReadTimeout(10);
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    String responseBody = response.getTextStr();
                    
                    if (responseBody != null && !responseBody.trim().isEmpty()) {
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        
                        if (jsonResponse.has("data") && jsonResponse.has("aes_key")) {
                            // 解密数据
                            String encryptedData = jsonResponse.get("data").getAsString();
                            String encryptedAesKey = jsonResponse.get("aes_key").getAsString();
                            
                            // 使用 RSA 私钥解密 AES 密钥
                            byte[] aesKey = decryptAesKey(encryptedAesKey);
                            
                            // 使用 AES 密钥解密数据
                            String decryptedData = decryptData(encryptedData, aesKey);
                            
                            // 解析交互记录
                            JsonArray interactionsArray = JsonParser.parseString(decryptedData).getAsJsonArray();
                            for (JsonElement element : interactionsArray) {
                                Interaction interaction = parseInteraction(element.getAsJsonObject());
                                if (interaction != null) {
                                    newInteractions.add(interaction);
                                    interactions.add(interaction);
                                    
                                    // 触发回调
                                    if (callback != null) {
                                        callback.onInteraction(interaction);
                                    }
                                }
                            }
                        } else if (jsonResponse.has("data") && jsonResponse.get("data").isJsonArray()) {
                            // 未加密的数据（某些服务器可能不加密）
                            JsonArray dataArray = jsonResponse.getAsJsonArray("data");
                            for (JsonElement element : dataArray) {
                                Interaction interaction = parseInteraction(element.getAsJsonObject());
                                if (interaction != null) {
                                    newInteractions.add(interaction);
                                    interactions.add(interaction);
                                    
                                    if (callback != null) {
                                        callback.onInteraction(interaction);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Interactsh] 轮询失败: " + e.getMessage());
            if (callback != null) {
                callback.onError("轮询失败: " + e.getMessage());
            }
        }
        
        return newInteractions;
    }
    
    /**
     * 启动后台轮询
     * 
     * @param intervalSeconds 轮询间隔（秒）
     * @param callback 交互回调
     */
    public void startPolling(int intervalSeconds, InteractionCallback callback) {
        if (polling) {
            return;
        }
        
        this.callback = callback;
        this.polling = true;
        
        pollExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Interactsh-Poller");
            t.setDaemon(true);
            return t;
        });
        
        pollExecutor.scheduleAtFixedRate(() -> {
            try {
                poll();
            } catch (Exception e) {
                System.err.println("[Interactsh] 后台轮询异常: " + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
        
        System.out.println("[Interactsh] 后台轮询已启动，间隔 " + intervalSeconds + " 秒");
    }
    
    /**
     * 停止后台轮询
     */
    public void stopPolling() {
        polling = false;
        if (pollExecutor != null) {
            pollExecutor.shutdown();
            try {
                pollExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                pollExecutor.shutdownNow();
            }
            pollExecutor = null;
        }
        System.out.println("[Interactsh] 后台轮询已停止");
    }
    
    /**
     * 注销客户端
     */
    public void deregister() {
        if (!registered) {
            return;
        }
        
        stopPolling();
        
        try {
            JsonObject deregisterRequest = new JsonObject();
            deregisterRequest.addProperty("correlation-id", correlationId);
            deregisterRequest.addProperty("secret-key", secretKey);
            
            String deregisterUrl = serverUrl + "/deregister";
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            
            RequestObj requestObj = new RequestObj()
                .setUrl(deregisterUrl)
                .setMethod("POST")
                .setHeaders(headers)
                .setPostData(deregisterRequest)
                .setTimeOut(10)
                .setReadTimeout(10);
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    System.out.println("[Interactsh] 注销成功");
                }
            }
        } catch (Exception e) {
            System.err.println("[Interactsh] 注销失败: " + e.getMessage());
        } finally {
            registered = false;
            correlationId = null;
            secretKey = null;
            rsaKeyPair = null;
        }
    }
    
    /**
     * 检查是否有交互记录
     * 
     * @param url 要检查的 URL（可以是完整 URL 或子域名）
     * @return 是否有交互
     */
    public boolean hasInteraction(String url) {
        // 先轮询一次获取最新数据
        poll();
        
        // 检查是否匹配
        String checkId = extractIdFromUrl(url);
        for (Interaction interaction : interactions) {
            if (interaction.getFullId() != null && interaction.getFullId().contains(checkId)) {
                return true;
            }
            if (interaction.getUniqueId() != null && interaction.getUniqueId().equals(checkId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 等待交互（阻塞）
     * 
     * @param url 要检查的 URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 交互记录，如果超时返回 null
     */
    public Interaction waitForInteraction(String url, int timeoutSeconds) {
        String checkId = extractIdFromUrl(url);
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            poll();
            
            for (Interaction interaction : interactions) {
                if (interaction.getFullId() != null && interaction.getFullId().contains(checkId)) {
                    return interaction;
                }
            }
            
            try {
                Thread.sleep(2000); // 每 2 秒轮询一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return null;
    }
    
    /**
     * 获取所有交互记录
     */
    public List<Interaction> getInteractions() {
        return new ArrayList<>(interactions);
    }
    
    /**
     * 清除交互记录缓存
     */
    public void clearInteractions() {
        interactions.clear();
    }
    
    /**
     * 是否已注册
     */
    public boolean isRegistered() {
        return registered;
    }
    
    /**
     * 获取 correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 生成 RSA 密钥对
     */
    private KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(RSA_KEY_SIZE, new SecureRandom());
        return keyGen.generateKeyPair();
    }
    
    /**
     * 将公钥转换为 Interactsh 期望的格式
     * 格式：PEM -> Base64 编码
     * 注意：类型必须是 "RSA PUBLIC KEY"，且整个 PEM 需要再做一次 Base64 编码
     */
    private String convertPublicKeyToPem(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(encoded);
        
        // 构建 PEM 格式（每 64 字符换行）
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN RSA PUBLIC KEY-----\n");
        
        int lineLength = 64;
        for (int i = 0; i < base64Key.length(); i += lineLength) {
            int end = Math.min(i + lineLength, base64Key.length());
            pem.append(base64Key, i, end).append("\n");
        }
        
        pem.append("-----END RSA PUBLIC KEY-----");
        
        // Interactsh 协议要求对整个 PEM 再做一次 Base64 编码
        return Base64.getEncoder().encodeToString(pem.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成 correlation ID（33 字符的随机字符串）
     */
    private String generateCorrelationId() {
        return generateRandomString(33);
    }
    
    /**
     * 生成 secret key
     */
    private String generateSecretKey() {
        return generateRandomString(32);
    }
    
    /**
     * 生成短 ID（用于唯一 URL）
     */
    private String generateShortId() {
        return generateRandomString(8);
    }
    
    /**
     * 生成随机字符串（小写字母和数字）
     */
    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 使用 RSA 私钥解密 AES 密钥
     */
    private byte[] decryptAesKey(String encryptedAesKeyBase64) throws Exception {
        byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyBase64);
        
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
        
        return cipher.doFinal(encryptedAesKey);
    }
    
    /**
     * 使用 AES-GCM 解密数据
     */
    private String decryptData(String encryptedDataBase64, byte[] aesKey) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
        
        // 提取 IV（前 12 字节）
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        
        // 提取密文
        byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
        
        // 解密
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        
        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    
    /**
     * 解析交互记录
     */
    private Interaction parseInteraction(JsonObject json) {
        try {
            Interaction interaction = new Interaction();
            
            if (json.has("unique-id")) {
                interaction.setUniqueId(json.get("unique-id").getAsString());
            }
            if (json.has("full-id")) {
                interaction.setFullId(json.get("full-id").getAsString());
            }
            if (json.has("raw-request")) {
                interaction.setRawRequest(json.get("raw-request").getAsString());
            }
            if (json.has("raw-response")) {
                interaction.setRawResponse(json.get("raw-response").getAsString());
            }
            if (json.has("remote-address")) {
                interaction.setRemoteAddress(json.get("remote-address").getAsString());
            }
            if (json.has("protocol")) {
                interaction.setProtocol(json.get("protocol").getAsString());
            }
            if (json.has("q-type")) {
                interaction.setQType(json.get("q-type").getAsString());
            }
            if (json.has("timestamp")) {
                try {
                    interaction.setTimestamp(json.get("timestamp").getAsLong());
                } catch (Exception e) {
                    interaction.setTimestamp(System.currentTimeMillis());
                }
            }
            
            return interaction;
        } catch (Exception e) {
            System.err.println("[Interactsh] 解析交互记录失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 URL 提取 ID
     */
    private String extractIdFromUrl(String url) {
        if (url == null) {
            return "";
        }
        // 移除协议前缀
        url = url.replace("http://", "").replace("https://", "");
        // 取第一个点之前的部分
        int dotIndex = url.indexOf('.');
        if (dotIndex > 0) {
            return url.substring(0, dotIndex);
        }
        return url;
    }
    
    /**
     * 关闭客户端
     */
    public void close() {
        deregister();
        CLIENT_CACHE.remove(serverUrl);
    }
}
