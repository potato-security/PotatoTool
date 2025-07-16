package com.potato.potatotool.content.redTeam.vulnScanner.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 性能监控工具类
 * 用于监控扫描过程中的性能指标
 */
public class PerformanceMonitor {
    
    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();
    
    // 性能指标
    private final AtomicLong totalScanTime = new AtomicLong(0);
    private final AtomicInteger totalScans = new AtomicInteger(0);
    private final AtomicInteger successfulScans = new AtomicInteger(0);
    private final AtomicInteger failedScans = new AtomicInteger(0);

    
    // 线程池性能指标
    private final ConcurrentHashMap<String, AtomicInteger> threadPoolTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> threadPoolTime = new ConcurrentHashMap<>();
    
    // POC性能指标
    private final ConcurrentHashMap<String, AtomicInteger> pocUsageCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> pocExecutionTime = new ConcurrentHashMap<>();
    
    private long scanStartTime;
    private boolean monitoring = false;
    
    private PerformanceMonitor() {}
    
    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }
    
    /**
     * 开始监控
     */
    public void startMonitoring() {
        monitoring = true;
        scanStartTime = System.currentTimeMillis();
        reset();
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        monitoring = false;
    }
    
    /**
     * 重置所有指标
     */
    public void reset() {
        totalScanTime.set(0);
        totalScans.set(0);
        successfulScans.set(0);
        failedScans.set(0);
        threadPoolTasks.clear();
        threadPoolTime.clear();
        pocUsageCount.clear();
        pocExecutionTime.clear();
    }
    
    /**
     * 记录扫描结果
     */
    public void recordScan(long duration, boolean successful) {
        if (!monitoring) return;
        
        totalScanTime.addAndGet(duration);
        totalScans.incrementAndGet();
        
        if (successful) {
            successfulScans.incrementAndGet();
        } else {
            failedScans.incrementAndGet();
        }
    }
    

    
    /**
     * 记录线程池性能
     */
    public void recordThreadPoolPerformance(String poolName, long duration) {
        if (!monitoring || poolName == null) return;
        
        threadPoolTasks.computeIfAbsent(poolName, k -> new AtomicInteger(0)).incrementAndGet();
        threadPoolTime.computeIfAbsent(poolName, k -> new AtomicLong(0)).addAndGet(duration);
    }
    
    /**
     * 记录POC使用情况
     */
    public void recordPocUsage(String pocId, long duration) {
        if (!monitoring || pocId == null) return;
        
        pocUsageCount.computeIfAbsent(pocId, k -> new AtomicInteger(0)).incrementAndGet();
        pocExecutionTime.computeIfAbsent(pocId, k -> new AtomicLong(0)).addAndGet(duration);
    }
    
    /**
     * 获取性能报告
     */
    public String getPerformanceReport() {
        if (!monitoring) {
            return "性能监控未启用";
        }
        
        StringBuilder report = new StringBuilder();
        long totalTime = System.currentTimeMillis() - scanStartTime;
        
        report.append("\n=== 扫描性能报告 ===").append("\n");
        report.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("总扫描时间: ").append(totalTime).append(" ms\n");
        
        // 基础统计
        report.append("\n--- 基础统计 ---\n");
        report.append("总扫描次数: ").append(totalScans.get()).append("\n");
        report.append("成功扫描: ").append(successfulScans.get()).append("\n");
        report.append("失败扫描: ").append(failedScans.get()).append("\n");
        
        if (totalScans.get() > 0) {
            report.append("成功率: ").append(String.format("%.2f%%", 
                (double) successfulScans.get() / totalScans.get() * 100)).append("\n");
            report.append("平均扫描时间: ").append(totalScanTime.get() / totalScans.get()).append(" ms\n");
        }
        
        // 线程池统计
        if (!threadPoolTasks.isEmpty()) {
            report.append("\n--- 线程池统计 ---\n");
            for (String poolName : threadPoolTasks.keySet()) {
                int tasks = threadPoolTasks.get(poolName).get();
                long time = threadPoolTime.get(poolName).get();
                report.append(poolName).append(": ")
                    .append(tasks).append(" 任务, ")
                    .append("平均时间: ").append(tasks > 0 ? time / tasks : 0).append(" ms\n");
            }
        }
        
        // POC性能统计（显示前10个最常用的POC）
        if (!pocUsageCount.isEmpty()) {
            report.append("\n--- POC性能统计 (Top 10) ---\n");
            pocUsageCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().get() - e1.getValue().get())
                .limit(10)
                .forEach(entry -> {
                    String pocId = entry.getKey();
                    int count = entry.getValue().get();
                    long time = pocExecutionTime.getOrDefault(pocId, new AtomicLong(0)).get();
                    report.append(pocId).append(": ")
                        .append(count).append(" 次, ")
                        .append("平均时间: ").append(count > 0 ? time / count : 0).append(" ms\n");
                });
        }
        
        return report.toString();
    }
    
    /**
     * 获取实时性能指标
     */
    public PerformanceMetrics getCurrentMetrics() {
        return new PerformanceMetrics(
            totalScans.get(),
            successfulScans.get(),
            failedScans.get(),
            totalScans.get() > 0 ? totalScanTime.get() / totalScans.get() : 0
        );
    }
    
    /**
     * 性能指标数据类
     */
    public static class PerformanceMetrics {
        public final int totalScans;
        public final int successfulScans;
        public final int failedScans;
        public final long averageScanTime;
        
        public PerformanceMetrics(int totalScans, int successfulScans, int failedScans,
                                long averageScanTime) {
            this.totalScans = totalScans;
            this.successfulScans = successfulScans;
            this.failedScans = failedScans;
            this.averageScanTime = averageScanTime;
        }
        
        public double getSuccessRate() {
            return totalScans > 0 ? (double) successfulScans / totalScans * 100 : 0;
        }
        

    }
}