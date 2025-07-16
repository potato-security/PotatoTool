package com.potato.potatotool.content.redTeam.vulnScanner.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * HTTP连接池管理器
 * 用于优化HTTP连接的复用和管理
 */
public class ConnectionPoolManager {
    
    private static final ConnectionPoolManager INSTANCE = new ConnectionPoolManager();
    
    // 注意：为了达到100%的POC覆盖率，已移除主机连接数限制
    // 连接超时时间（毫秒） - 优化：减少到15秒以提高响应速度
    private static final int CONNECTION_TIMEOUT = 15000;
    // 连接空闲超时时间（毫秒） - 优化：减少到30秒以更快释放资源
    private static final int IDLE_TIMEOUT = 30000;
    // 连接获取锁等待时间（毫秒） - 优化：增加等待时间以减少连接获取失败
    private static final int LOCK_WAIT_TIMEOUT = 3000;
    
    // 主机连接计数器
    private final ConcurrentHashMap<String, AtomicInteger> hostConnections = new ConcurrentHashMap<>();
    // 主机连接锁
    private final ConcurrentHashMap<String, ReentrantLock> hostLocks = new ConcurrentHashMap<>();
    // 连接最后使用时间
    private final ConcurrentHashMap<String, Long> connectionLastUsed = new ConcurrentHashMap<>();
    
    // 全局连接统计
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    // 清理线程
    private Thread cleanupThread;
    
    private ConnectionPoolManager() {
        // 启动清理线程
        startCleanupThread();
    }
    
    public static ConnectionPoolManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取连接
     * @param targetUrl 目标URL
     * @return 是否成功获取连接
     */
    public boolean acquireConnection(String targetUrl) {
        String host = extractHost(targetUrl);
        if (host == null || host.trim().isEmpty()) {
            System.err.println("无效的URL，无法提取主机名: " + targetUrl);
            return false;
        }
        
        ReentrantLock lock = hostLocks.computeIfAbsent(host, k -> new ReentrantLock());
        
        // 简化重试机制：由于移除了连接限制，大部分情况下第一次就能成功
        for (int retry = 0; retry < 2; retry++) {
            try {
                // 尝试获取锁
                if (lock.tryLock(LOCK_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    try {
                        AtomicInteger connections = hostConnections.computeIfAbsent(host, k -> new AtomicInteger(0));
                         
                        // 移除连接数限制检查，确保100%POC覆盖率
                        // 注释：为了达到100%的POC覆盖率，不再限制每个主机的连接数
                        // 所有扫描任务都将被执行，不会因为连接数限制而跳过
                        
                        // 增加连接计数（仅用于统计，不做限制）
                        connections.incrementAndGet();
                        totalConnections.incrementAndGet();
                        activeConnections.incrementAndGet();
                        
                        // 记录连接使用时间
                        String connectionKey = host + "_" + System.nanoTime();
                        connectionLastUsed.put(connectionKey, System.currentTimeMillis());
                        
                        // 输出连接统计信息（用于监控）
                        if (connections.get() % 100 == 0) {
                            System.out.println("主机 " + host + " 当前连接数: " + connections.get() + " (无限制模式)");
                        }
                        
                        return true;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    if (retry == 1) { // 最后一次重试
                        System.out.println("获取连接锁超时: " + host + ", 当前连接数: " + 
                            hostConnections.getOrDefault(host, new AtomicInteger(0)).get());
                        return false;
                    }
                    // 等待一段时间后重试
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("获取连接时被中断: " + host);
                return false;
            }
        }
        
        // 连接获取失败（通常是锁竞争导致）
        System.out.println("连接获取失败: " + host + " (可能是锁竞争)");
        return false;
    }
    
    /**
     * 释放连接
     * @param targetUrl 目标URL
     */
    public void releaseConnection(String targetUrl) {
        String host = extractHost(targetUrl);
        if (host == null || host.trim().isEmpty()) {
            return;
        }
        
        ReentrantLock lock = hostLocks.get(host);
        if (lock != null) {
            lock.lock();
            try {
                AtomicInteger connections = hostConnections.get(host);
                if (connections != null && connections.get() > 0) {
                    connections.decrementAndGet();
                    activeConnections.decrementAndGet();
                    
                    // 移除连接使用记录
                    connectionLastUsed.remove(host + "_" + Thread.currentThread().getId());
                }
            } finally {
                lock.unlock();
            }
        }
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
     * 清理空闲连接
     */
    public void cleanupIdleConnections() {
        long currentTime = System.currentTimeMillis();
        
        connectionLastUsed.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > IDLE_TIMEOUT) {
                String[] parts = entry.getKey().split("_");
                if (parts.length >= 1) {
                    String host = parts[0];
                    ReentrantLock lock = hostLocks.get(host);
                    if (lock != null) {
                        lock.lock();
                        try {
                            AtomicInteger connections = hostConnections.get(host);
                            if (connections != null && connections.get() > 0) {
                                connections.decrementAndGet();
                                activeConnections.decrementAndGet();
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * 重置连接池
     */
    public void reset() {
        hostConnections.clear();
        connectionLastUsed.clear();
        totalConnections.set(0);
        activeConnections.set(0);
    }
    
    /**
     * 从URL中提取主机名
     */
    private String extractHost(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (MalformedURLException e) {
            // 尝试简单解析
            if (urlString.contains("://")) {
                String[] parts = urlString.split("://");
                if (parts.length > 1) {
                    String hostPart = parts[1].split("/")[0];
                    return hostPart.split(":")[0];
                }
            }
            return null;
        }
    }
    
    /**
     * 计算平均每主机连接数
     */
    private double calculateAverageConnectionsPerHost() {
        if (hostConnections.isEmpty()) {
            return 0.0;
        }
        
        int totalHostConnections = hostConnections.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
            
        return (double) totalHostConnections / hostConnections.size();
    }
    
    /**
     * 启动清理线程
     */
    private void startCleanupThread() {
        cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000); // 每30秒清理一次
                    cleanupIdleConnections();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        cleanupThread.setDaemon(true);
        cleanupThread.setName("ConnectionPool-Cleanup");
        cleanupThread.start();
    }
    
    /**
     * 关闭连接池管理器
     */
    public void shutdown() {
        System.out.println("正在关闭连接池管理器...");
        
        // 中断清理线程
        if (cleanupThread != null && cleanupThread.isAlive()) {
            cleanupThread.interrupt();
            try {
                cleanupThread.join(2000); // 等待2秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 清理所有连接
        reset();
        
        // 清理锁
        hostLocks.clear();
        
        System.out.println("连接池管理器已关闭");
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
     * 注意：为了达到100%POC覆盖率，连接数限制已被移除
     */
    public static class PoolConfig {
        // 注意：为了达到100%的POC覆盖率，连接数限制已被移除
        // 不再设置主机连接数限制
        public static final int CONNECTION_TIMEOUT = ConnectionPoolManager.CONNECTION_TIMEOUT;
        public static final int IDLE_TIMEOUT = ConnectionPoolManager.IDLE_TIMEOUT;
    }
}