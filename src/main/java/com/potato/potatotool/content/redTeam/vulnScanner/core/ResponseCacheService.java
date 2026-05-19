package com.potato.potatotool.content.redTeam.vulnScanner.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * HTTP 响应缓存服务
 * 用于缓存相同请求的响应，减少重复网络请求
 * 支持 LRU 淘汰策略和 TTL 过期机制
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class ResponseCacheService {
    
    /**
     * 默认缓存大小
     */
    private static final int DEFAULT_MAX_CACHE_SIZE = 1000;
    
    /**
     * 默认缓存 TTL（毫秒）
     */
    private static final long DEFAULT_CACHE_TTL_MS = 30000; // 30 秒
    
    /**
     * LRU 缓存
     */
    private final Map<String, CachedResponse> cache;
    
    /**
     * 统计信息
     */
    private final CacheStats stats = new CacheStats();
    
    /**
     * 读写锁
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 缓存配置
     */
    private int maxCacheSize;
    private long cacheTtlMs;
    
    /**
     * 默认构造函数
     */
    public ResponseCacheService() {
        this(DEFAULT_MAX_CACHE_SIZE, DEFAULT_CACHE_TTL_MS);
    }
    
    /**
     * 带参数构造函数
     */
    public ResponseCacheService(int maxCacheSize, long cacheTtlMs) {
        this.maxCacheSize = maxCacheSize;
        this.cacheTtlMs = cacheTtlMs;
        
        // 创建 LRU 缓存
        this.cache = new LinkedHashMap<String, CachedResponse>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedResponse> eldest) {
                return size() > ResponseCacheService.this.maxCacheSize;
            }
        };
    }
    
    /**
     * 生成缓存 Key
     * 
     * @param baseUrl 基础 URL（如 https://example.com）
     * @param method HTTP 方法
     * @param path 请求路径
     * @param body 请求体（可为 null）
     * @param headers 关键请求头（可为 null）
     * @return 缓存 Key
     */
    public String generateCacheKey(String baseUrl, String method, String path, String body, Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        
        // 基础部分：URL + 方法 + 路径
        sb.append(normalizeUrl(baseUrl))
          .append("|")
          .append(method != null ? method.toUpperCase() : "GET")
          .append("|")
          .append(path != null ? path : "/");
        
        // Body 部分（使用 hash 避免 key 过长）
        if (body != null && !body.isEmpty()) {
            if (body.length() < 100) {
                sb.append("|body:").append(body);
            } else {
                sb.append("|body_hash:").append(body.hashCode());
            }
        }
        
        // 请求头哈希，避免不同认证态/会话错误复用缓存
        if (headers != null && !headers.isEmpty()) {
            List<String> normalizedHeaders = new ArrayList<>();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    normalizedHeaders.add(entry.getKey().toLowerCase(Locale.ROOT) + ":" + entry.getValue());
                }
            }
            if (!normalizedHeaders.isEmpty()) {
                Collections.sort(normalizedHeaders);
                sb.append("|headers_hash:").append(normalizedHeaders.toString().hashCode());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 规范化 URL
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        
        // 移除末尾斜杠
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
    
    /**
     * 获取缓存的响应
     * 
     * @param cacheKey 缓存 Key
     * @return 缓存的响应，如果不存在或已过期返回 null
     */
    public CachedResponse get(String cacheKey) {
        lock.readLock().lock();
        try {
            CachedResponse cached = cache.get(cacheKey);
            
            if (cached != null) {
                // 检查是否过期
                if (!cached.isExpired(cacheTtlMs)) {
                    stats.incrementHits();
                    return cached;
                } else {
                    // 过期了，需要移除（升级为写锁）
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    try {
                        cache.remove(cacheKey);
                        stats.incrementExpired();
                    } finally {
                        lock.readLock().lock();
                        lock.writeLock().unlock();
                    }
                }
            }
            
            stats.incrementMisses();
            return null;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 缓存响应
     * 
     * @param cacheKey 缓存 Key
     * @param statusCode HTTP 状态码
     * @param headers 响应头
     * @param body 响应体
     */
    public void put(String cacheKey, int statusCode, Map<String, String> headers, String body) {
        put(cacheKey, statusCode, headers, body, 0L);
    }

    public void put(String cacheKey, int statusCode, Map<String, String> headers, String body, long responseTimeMs) {
        lock.writeLock().lock();
        try {
            CachedResponse response = new CachedResponse(statusCode, headers, body, System.currentTimeMillis(), responseTimeMs);
            cache.put(cacheKey, response);
            stats.incrementPuts();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanup() {
        lock.writeLock().lock();
        try {
            long now = System.currentTimeMillis();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now, cacheTtlMs));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取统计信息
     */
    public CacheStats getStats() {
        return stats;
    }
    
    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        return stats.getHitRate();
    }
    
    // ========== Getter/Setter ==========
    
    public int getMaxCacheSize() {
        return maxCacheSize;
    }
    
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
    
    public long getCacheTtlMs() {
        return cacheTtlMs;
    }
    
    public void setCacheTtlMs(long cacheTtlMs) {
        this.cacheTtlMs = cacheTtlMs;
    }
    
    // ========== 内部类 ==========
    
    /**
     * 缓存的响应
     */
    public static class CachedResponse {
        private final int statusCode;
        private final Map<String, String> headers;
        private final String body;
        private final long timestamp;
        private final long responseTimeMs;
        
        public CachedResponse(int statusCode, Map<String, String> headers, String body, long timestamp, long responseTimeMs) {
            this.statusCode = statusCode;
            this.headers = headers != null ? new ConcurrentHashMap<>(headers) : new ConcurrentHashMap<>();
            this.body = body;
            this.timestamp = timestamp;
            this.responseTimeMs = responseTimeMs;
        }
        
        public boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
        
        public boolean isExpired(long now, long ttlMs) {
            return now - timestamp > ttlMs;
        }
        
        public int getStatusCode() { return statusCode; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody() { return body; }
        public long getTimestamp() { return timestamp; }
        public long getResponseTimeMs() { return responseTimeMs; }
        
        /**
         * 获取响应体长度
         */
        public int getBodyLength() {
            return body != null ? body.length() : 0;
        }
        
        /**
         * 获取响应头
         */
        public String getHeader(String name) {
            return headers.get(name);
        }
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private long hits = 0;
        private long misses = 0;
        private long puts = 0;
        private long expired = 0;
        
        public synchronized void incrementHits() { hits++; }
        public synchronized void incrementMisses() { misses++; }
        public synchronized void incrementPuts() { puts++; }
        public synchronized void incrementExpired() { expired++; }
        
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getPuts() { return puts; }
        public long getExpired() { return expired; }
        
        public long getTotalRequests() { return hits + misses; }
        
        public double getHitRate() {
            long total = getTotalRequests();
            return total > 0 ? (double) hits / total : 0.0;
        }
        
        public synchronized void reset() {
            hits = 0;
            misses = 0;
            puts = 0;
            expired = 0;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats[hits=%d, misses=%d, puts=%d, expired=%d, hitRate=%.2f%%]",
                hits, misses, puts, expired, getHitRate() * 100);
        }
    }
}
