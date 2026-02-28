package com.potato.potatotool.content.redTeam.vulnScanner.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP连接池管理器
 * 无锁设计：仅做统计计数，不做连接限制，避免锁竞争成为扫描瓶颈
 * 如需限流可通过 Semaphore 在上层实现
 */
public class ConnectionPoolManager {

    private static final ConnectionPoolManager INSTANCE = new ConnectionPoolManager();

    // 主机连接计数器（无锁统计）
    private final ConcurrentHashMap<String, AtomicInteger> hostConnections = new ConcurrentHashMap<>();

    // 全局连接统计
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    // host 缓存，避免重复解析 URL
    private final ConcurrentHashMap<String, String> hostCache = new ConcurrentHashMap<>();

    private ConnectionPoolManager() {
    }

    public static ConnectionPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取连接（无锁，仅统计）
     * @param targetUrl 目标URL
     * @return 始终返回 true
     */
    public boolean acquireConnection(String targetUrl) {
        String host = extractHost(targetUrl);
        if (host == null || host.trim().isEmpty()) {
            return true; // 不阻止扫描
        }

        hostConnections.computeIfAbsent(host, k -> new AtomicInteger(0)).incrementAndGet();
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
        return true;
    }

    /**
     * 释放连接（无锁，仅统计）
     * @param targetUrl 目标URL
     */
    public void releaseConnection(String targetUrl) {
        String host = extractHost(targetUrl);
        if (host == null || host.trim().isEmpty()) {
            return;
        }

        AtomicInteger connections = hostConnections.get(host);
        if (connections != null) {
            int val = connections.decrementAndGet();
            if (val < 0) connections.compareAndSet(val, 0); // 防止负数
        }
        activeConnections.decrementAndGet();
    }

    /**
     * 获取连接池状态
     */
    public ConnectionPoolStatus getStatus() {
        return new ConnectionPoolStatus(
            totalConnections.get(),
            activeConnections.get(),
            hostConnections.size(),
            calculateAverageConnectionsPerHost()
        );
    }

    /**
     * 清理空闲连接（无操作，保留接口兼容）
     */
    public void cleanupIdleConnections() {
        // 无锁模式下不需要清理
    }

    /**
     * 重置连接池
     */
    public void reset() {
        hostConnections.clear();
        hostCache.clear();
        totalConnections.set(0);
        activeConnections.set(0);
    }

    /**
     * 从URL中提取主机名（带缓存）
     */
    private String extractHost(String urlString) {
        if (urlString == null) return null;
        return hostCache.computeIfAbsent(urlString, url -> {
            // 快速解析，避免 new URL() 的开销
            int schemeEnd = url.indexOf("://");
            if (schemeEnd < 0) return null;
            String afterScheme = url.substring(schemeEnd + 3);
            int slashIdx = afterScheme.indexOf('/');
            String hostPort = slashIdx >= 0 ? afterScheme.substring(0, slashIdx) : afterScheme;
            int colonIdx = hostPort.lastIndexOf(':');
            // 处理 IPv6 地址
            if (hostPort.startsWith("[")) {
                int bracketEnd = hostPort.indexOf(']');
                return bracketEnd > 0 ? hostPort.substring(1, bracketEnd) : hostPort;
            }
            return colonIdx > 0 ? hostPort.substring(0, colonIdx) : hostPort;
        });
    }

    /**
     * 计算平均每主机连接数
     */
    private double calculateAverageConnectionsPerHost() {
        if (hostConnections.isEmpty()) {
            return 0.0;
        }
        int total = 0;
        for (AtomicInteger v : hostConnections.values()) {
            total += v.get();
        }
        return (double) total / hostConnections.size();
    }

    /**
     * 关闭连接池管理器
     */
    public void shutdown() {
        reset();
    }

    /**
     * 连接池状态信息
     */
    public static class ConnectionPoolStatus {
        public final int totalConnections;
        public final int activeConnections;
        public final int hostCount;
        public final double averageConnectionsPerHost;

        public ConnectionPoolStatus(int totalConnections, int activeConnections,
                                  int hostCount, double averageConnectionsPerHost) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.hostCount = hostCount;
            this.averageConnectionsPerHost = averageConnectionsPerHost;
        }

        @Override
        public String toString() {
            return String.format("ConnectionPool[total=%d, active=%d, hosts=%d, avg=%.2f]",
                totalConnections, activeConnections, hostCount, averageConnectionsPerHost);
        }
    }

    /**
     * 获取连接池配置信息
     */
    public static class PoolConfig {
        public static final int CONNECTION_TIMEOUT = 15000;
        public static final int IDLE_TIMEOUT = 30000;
    }
}