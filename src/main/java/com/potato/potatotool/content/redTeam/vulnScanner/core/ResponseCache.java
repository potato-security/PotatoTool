package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.utils.network.CustomHttpResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 响应缓存器
 * 用于缓存POC执行过程中的HTTP响应，支持diff差异对比
 *
 * 主要用途：
 * 1. 布尔盲注检测 - 对比真假条件下的响应差异
 * 2. 时间盲注检测 - 记录响应时间
 * 3. 响应长度对比 - 检测长度变化
 * 4. 多块raw请求 - 支持索引访问（body_1, body_2等）
 *
 * @author Potato
 * @date 2025-11-01
 */
public class ResponseCache {
    
    /**
     * 缓存的响应数据（线程安全）
     * Key: 步骤ID或自定义标识
     * Value: 缓存的响应信息
     */
    private final ConcurrentHashMap<String, CachedResponse> cache;

    /**
     * 最大缓存大小（防止内存溢出）
     */
    private static final int MAX_CACHE_SIZE = 50;

    public ResponseCache() {
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * 缓存响应
     * 
     * @param key 缓存键（通常使用步骤ID）
     * @param response HTTP响应对象
     * @param requestTime 请求时间戳
     * @param responseTime 响应时间戳
     */
    public void put(String key, CustomHttpResponse response, long requestTime, long responseTime) {
        if (key == null || response == null) {
            return;
        }

        // 简单的大小限制：超过上限时清空（POC间缓存不需要保留）
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.clear();
        }

        CachedResponse cached = new CachedResponse(
            response.getResponseCode(),
            response.getTextStr(),
            response.getHeaderFieldsText(),
            response.getByteArray(),
            responseTime - requestTime,
            requestTime,
            responseTime
        );

        cache.put(key, cached);
    }

    /**
     * 缓存多个响应（用于多块raw请求）
     *
     * 存储格式：
     * - key_1 -> 第1个响应
     * - key_2 -> 第2个响应
     * - ...
     *
     * 这样DSL matcher可以通过body_1, body_2, status_code_2等访问各个响应
     *
     * @param baseKey 基础缓存键（步骤ID）
     * @param responses 响应列表
     */
    public void putMultipleResponses(String baseKey, List<CustomHttpResponse> responses) {
        if (baseKey == null || responses == null || responses.isEmpty()) {
            return;
        }

        // 为每个响应创建带索引的缓存键
        for (int i = 0; i < responses.size(); i++) {
            CustomHttpResponse response = responses.get(i);
            if (response != null) {
                String indexedKey = baseKey + "_" + (i + 1);
                long requestTime = System.currentTimeMillis() - response.getResponseTime();
                long responseTime = System.currentTimeMillis();

                CachedResponse cached = new CachedResponse(
                    response.getResponseCode(),
                    response.getTextStr(),
                    response.getHeaderFieldsText(),
                    response.getByteArray(),
                    response.getResponseTime(),
                    requestTime,
                    responseTime
                );

                cache.put(indexedKey, cached);
            }
        }

        // 同时缓存最后一个响应到基础key（兼容性）
        if (!responses.isEmpty()) {
            CustomHttpResponse lastResponse = responses.get(responses.size() - 1);
            long requestTime = System.currentTimeMillis() - lastResponse.getResponseTime();
            long responseTime = System.currentTimeMillis();

            CachedResponse cached = new CachedResponse(
                lastResponse.getResponseCode(),
                lastResponse.getTextStr(),
                lastResponse.getHeaderFieldsText(),
                lastResponse.getByteArray(),
                lastResponse.getResponseTime(),
                requestTime,
                responseTime
            );

            cache.put(baseKey, cached);
        }
    }

    /**
     * 获取缓存的响应
     * 
     * @param key 缓存键
     * @return 缓存的响应，如果不存在则返回null
     */
    public CachedResponse get(String key) {
        if (key == null) {
            return null;
        }
        return cache.get(key);
    }
    
    /**
     * 检查是否存在缓存
     * 
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean contains(String key) {
        if (key == null) {
            return false;
        }
        return cache.containsKey(key);
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 对比两个响应的差异
     * 
     * @param key1 第一个响应的键
     * @param key2 第二个响应的键
     * @param compareType 对比类型：body, header, status, length, time
     * @return 差异结果，具体含义取决于对比类型
     */
    public DiffResult diff(String key1, String key2, String compareType) {
        CachedResponse resp1 = get(key1);
        CachedResponse resp2 = get(key2);
        
        if (resp1 == null || resp2 == null) {
            return new DiffResult(false, 0, "缓存的响应不存在");
        }
        
        return diff(resp1, resp2, compareType);
    }
    
    /**
     * 对比两个响应对象的差异
     * 
     * @param resp1 第一个响应
     * @param resp2 第二个响应
     * @param compareType 对比类型
     * @return 差异结果
     */
    public static DiffResult diff(CachedResponse resp1, CachedResponse resp2, String compareType) {
        if (compareType == null || compareType.isEmpty()) {
            compareType = "body"; // 默认对比body
        }
        
        switch (compareType.toLowerCase()) {
            case "body":
                return diffBody(resp1, resp2);
            case "header":
                return diffHeader(resp1, resp2);
            case "status":
            case "code":
                return diffStatus(resp1, resp2);
            case "length":
            case "size":
                return diffLength(resp1, resp2);
            case "time":
                return diffTime(resp1, resp2);
            default:
                return diffBody(resp1, resp2);
        }
    }
    
    /**
     * 对比响应体差异
     */
    private static DiffResult diffBody(CachedResponse resp1, CachedResponse resp2) {
        String body1 = resp1.getBody() != null ? resp1.getBody() : "";
        String body2 = resp2.getBody() != null ? resp2.getBody() : "";
        
        boolean isDifferent = !body1.equals(body2);
        
        // 计算差异度（字符差异百分比）
        int maxLen = Math.max(body1.length(), body2.length());
        if (maxLen == 0) {
            return new DiffResult(isDifferent, 0.0, "两个响应体都为空");
        }
        
        // 如果内容不同，计算差异
        if (isDifferent) {
            // 计算长度差异
            int diffChars = Math.abs(body1.length() - body2.length());
            // 如果长度相同但内容不同，计算内容差异（简单的字符差异）
            if (diffChars == 0) {
                // 计算不同字符的数量
                int differentChars = 0;
                int minLen = Math.min(body1.length(), body2.length());
                for (int i = 0; i < minLen; i++) {
                    if (body1.charAt(i) != body2.charAt(i)) {
                        differentChars++;
                    }
                }
                diffChars = differentChars;
            }
            double diffPercent = (double) diffChars / maxLen * 100;
            return new DiffResult(true, diffPercent, 
                String.format("Body差异: %d字符 (%.2f%%)", diffChars, diffPercent));
        } else {
            return new DiffResult(false, 0.0, "Body相同");
        }
    }
    
    /**
     * 对比响应头差异
     */
    private static DiffResult diffHeader(CachedResponse resp1, CachedResponse resp2) {
        String header1 = resp1.getHeader() != null ? resp1.getHeader() : "";
        String header2 = resp2.getHeader() != null ? resp2.getHeader() : "";
        
        boolean isDifferent = !header1.equals(header2);
        
        return new DiffResult(isDifferent, isDifferent ? 100.0 : 0.0, 
            "Header " + (isDifferent ? "不同" : "相同"));
    }
    
    /**
     * 对比状态码差异
     */
    private static DiffResult diffStatus(CachedResponse resp1, CachedResponse resp2) {
        boolean isDifferent = resp1.getStatusCode() != resp2.getStatusCode();
        
        return new DiffResult(isDifferent, 
            Math.abs(resp1.getStatusCode() - resp2.getStatusCode()), 
            String.format("状态码: %d vs %d", resp1.getStatusCode(), resp2.getStatusCode()));
    }
    
    /**
     * 对比响应长度差异
     */
    private static DiffResult diffLength(CachedResponse resp1, CachedResponse resp2) {
        int len1 = resp1.getBody() != null ? resp1.getBody().length() : 0;
        int len2 = resp2.getBody() != null ? resp2.getBody().length() : 0;
        
        boolean isDifferent = len1 != len2;
        int diffValue = Math.abs(len1 - len2);
        
        return new DiffResult(isDifferent, diffValue, 
            String.format("长度差异: %d字节", diffValue));
    }
    
    /**
     * 对比响应时间差异
     */
    private static DiffResult diffTime(CachedResponse resp1, CachedResponse resp2) {
        long timeDiff = Math.abs(resp1.getResponseTimeMs() - resp2.getResponseTimeMs());
        boolean isDifferent = timeDiff > 100; // 超过100ms认为有差异
        
        return new DiffResult(isDifferent, timeDiff, 
            String.format("时间差异: %dms", timeDiff));
    }
    
    /**
     * 缓存的响应数据
     */
    public static class CachedResponse {
        private final int statusCode;
        private final String body;
        private final String header;
        private final byte[] rawBytes;
        private final long responseTimeMs;
        private final long requestTimestamp;
        private final long responseTimestamp;
        
        public CachedResponse(int statusCode, String body, String header, 
                            byte[] rawBytes, long responseTimeMs,
                            long requestTimestamp, long responseTimestamp) {
            this.statusCode = statusCode;
            this.body = body;
            this.header = header;
            this.rawBytes = rawBytes;
            this.responseTimeMs = responseTimeMs;
            this.requestTimestamp = requestTimestamp;
            this.responseTimestamp = responseTimestamp;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public String getHeader() { return header; }
        public byte[] getRawBytes() { return rawBytes; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public long getRequestTimestamp() { return requestTimestamp; }
        public long getResponseTimestamp() { return responseTimestamp; }
    }
    
    /**
     * 差异对比结果
     */
    public static class DiffResult {
        private final boolean isDifferent;
        private final double diffValue;
        private final String description;
        
        public DiffResult(boolean isDifferent, double diffValue, String description) {
            this.isDifferent = isDifferent;
            this.diffValue = diffValue;
            this.description = description;
        }
        
        public boolean isDifferent() { return isDifferent; }
        public double getDiffValue() { return diffValue; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("DiffResult{different=%s, value=%.2f, desc='%s'}", 
                isDifferent, diffValue, description);
        }
    }
}

