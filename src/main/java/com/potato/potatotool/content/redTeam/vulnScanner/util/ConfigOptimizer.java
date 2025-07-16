package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置优化器
 * 根据扫描性能动态调整扫描参数
 */
public class ConfigOptimizer {
    
    private static final ConfigOptimizer INSTANCE = new ConfigOptimizer();
    
    // 性能阈值 - 优化：调整阈值以更好地适应实际性能
    private static final double HIGH_SUCCESS_RATE = 0.88; // 优化：降低高成功率阈值
    private static final double LOW_SUCCESS_RATE = 0.70; // 优化：降低低成功率阈值
    private static final long FAST_RESPONSE_TIME = 600; // 0.6秒 - 优化：更严格的快速响应标准
    private static final long SLOW_RESPONSE_TIME = 2500; // 2.5秒 - 优化：更严格的慢响应标准
    
    // 动态调整参数 - 二次优化：平衡性能与稳定性
    private final AtomicInteger recommendedThreads = new AtomicInteger(15); // 优化：提高默认推荐线程数
    private final AtomicInteger recommendedTimeout = new AtomicInteger(12); // 优化：提高默认超时时间
    private final AtomicInteger recommendedRetries = new AtomicInteger(2); // 优化：提高默认重试次数
    private final AtomicLong lastOptimizationTime = new AtomicLong(0);
    
    // 优化间隔（毫秒）
    private static final long OPTIMIZATION_INTERVAL = 20000; // 20秒 - 优化：缩短优化间隔
    
    private ConfigOptimizer() {}
    
    public static ConfigOptimizer getInstance() {
        return INSTANCE;
    }
    
    /**
     * 根据性能指标优化配置
     * @param metrics 当前性能指标
     * @param config 当前配置
     * @return 优化后的配置建议
     */
    public OptimizationSuggestion optimizeConfig(PerformanceMonitor.PerformanceMetrics metrics, ScanConfig config) {
        long currentTime = System.currentTimeMillis();
        
        // 检查是否需要优化（避免频繁调整）
        if (currentTime - lastOptimizationTime.get() < OPTIMIZATION_INTERVAL) {
            return new OptimizationSuggestion(false, config, "优化间隔未到");
        }
        
        lastOptimizationTime.set(currentTime);
        
        // 分析性能指标
        double successRate = metrics.getSuccessRate() / 100.0;
        long avgResponseTime = metrics.averageScanTime;

        
        ScanConfig optimizedConfig = new ScanConfig();
        copyConfig(config, optimizedConfig);
        
        StringBuilder suggestions = new StringBuilder();
        boolean hasChanges = false;
        
        // 根据成功率调整线程数
        if (successRate > HIGH_SUCCESS_RATE && avgResponseTime < FAST_RESPONSE_TIME) {
            // 性能良好，可以增加并发
            int newThreads = Math.min(config.getThreads() + 2, 20);
            if (newThreads != config.getThreads()) {
                optimizedConfig.setThreads(newThreads);
                recommendedThreads.set(newThreads);
                suggestions.append("增加线程数到 ").append(newThreads).append("（性能良好）\n");
                hasChanges = true;
            }
        } else if (successRate < LOW_SUCCESS_RATE || avgResponseTime > SLOW_RESPONSE_TIME) {
            // 性能较差，减少并发
            int newThreads = Math.max(config.getThreads() - 2, 2);
            if (newThreads != config.getThreads()) {
                optimizedConfig.setThreads(newThreads);
                recommendedThreads.set(newThreads);
                suggestions.append("减少线程数到 ").append(newThreads).append("（性能较差）\n");
                hasChanges = true;
            }
        }
        
        // 根据响应时间调整超时设置
        if (avgResponseTime > SLOW_RESPONSE_TIME) {
            int newTimeout = Math.min(config.getTimeout() + 5, 30);
            if (newTimeout != config.getTimeout()) {
                optimizedConfig.setTimeout(newTimeout);
                recommendedTimeout.set(newTimeout);
                suggestions.append("增加超时时间到 ").append(newTimeout).append("秒（响应较慢）\n");
                hasChanges = true;
            }
        } else if (avgResponseTime < FAST_RESPONSE_TIME) {
            int newTimeout = Math.max(config.getTimeout() - 2, 5);
            if (newTimeout != config.getTimeout()) {
                optimizedConfig.setTimeout(newTimeout);
                recommendedTimeout.set(newTimeout);
                suggestions.append("减少超时时间到 ").append(newTimeout).append("秒（响应较快）\n");
                hasChanges = true;
            }
        }
        
        // 根据成功率调整重试次数
        if (successRate < LOW_SUCCESS_RATE) {
            int newRetries = Math.min(config.getRetries() + 1, 5);
            if (newRetries != config.getRetries()) {
                optimizedConfig.setRetries(newRetries);
                recommendedRetries.set(newRetries);
                suggestions.append("增加重试次数到 ").append(newRetries).append("（成功率较低）\n");
                hasChanges = true;
            }
        } else if (successRate > HIGH_SUCCESS_RATE) {
            int newRetries = Math.max(config.getRetries() - 1, 0);
            if (newRetries != config.getRetries()) {
                optimizedConfig.setRetries(newRetries);
                recommendedRetries.set(newRetries);
                suggestions.append("减少重试次数到 ").append(newRetries).append("（成功率较高）\n");
                hasChanges = true;
            }
        }
        
        
        return new OptimizationSuggestion(hasChanges, optimizedConfig, suggestions.toString());
    }
    
    /**
     * 获取推荐的配置参数
     */
    public RecommendedConfig getRecommendedConfig() {
        return new RecommendedConfig(
            recommendedThreads.get(),
            recommendedTimeout.get(),
            recommendedRetries.get()
        );
    }
    
    /**
     * 根据目标数量和POC数量预估最优配置
     */
    public ScanConfig estimateOptimalConfig(int targetCount, int pocCount, ScanConfig baseConfig) {
        ScanConfig optimizedConfig = new ScanConfig();
        copyConfig(baseConfig, optimizedConfig);
        
        int totalTasks = targetCount * pocCount;
        
        // 根据任务总数调整线程数 - 优化：更激进的并发配置
        int optimalThreads;
        if (totalTasks < 30) {
            optimalThreads = Math.min(10, Math.max(3, totalTasks)); // 优化：提高小任务的线程数
        } else if (totalTasks < 100) {
            optimalThreads = Math.min(20, totalTasks / 3); // 优化：更激进的中等任务配置
        } else if (totalTasks < 500) {
            optimalThreads = Math.min(35, totalTasks / 6); // 优化：更激进的大任务配置
        } else if (totalTasks < 2000) {
            optimalThreads = Math.min(50, totalTasks / 15); // 优化：超大任务配置
        } else {
            optimalThreads = Math.min(80, totalTasks / 40); // 优化：极大任务配置
        }
        
        optimizedConfig.setThreads(Math.max(5, optimalThreads)); // 优化：提高最小线程数
        
        // 根据目标数量调整超时时间 - 优化：更智能的超时配置
        if (targetCount > 200) {
            optimizedConfig.setTimeout(Math.max(baseConfig.getTimeout(), 18)); // 优化：大量目标时增加超时
        } else if (targetCount > 50) {
            optimizedConfig.setTimeout(Math.max(baseConfig.getTimeout(), 15));
        } else {
            optimizedConfig.setTimeout(Math.max(baseConfig.getTimeout(), 12)); // 优化：少量目标时适中超时
        }
        
        // 根据任务复杂度调整重试次数 - 优化：智能重试配置
        if (totalTasks > 1000) {
            optimizedConfig.setRetries(Math.max(baseConfig.getRetries(), 3)); // 大量任务时增加重试
        } else {
            optimizedConfig.setRetries(Math.max(baseConfig.getRetries(), 2));
        }
        
        return optimizedConfig;
    }
    
    /**
     * 复制配置
     */
    private void copyConfig(ScanConfig source, ScanConfig target) {
        target.setThreads(source.getThreads());
        target.setTimeout(source.getTimeout());
        target.setRetries(source.getRetries());
        target.setProtocol(source.getProtocol());
        target.setSeverity(source.getSeverity());
        target.setDebug(source.isDebug());
        target.setFollowRedirects(source.isFollowRedirects());
        target.setProxy(source.getProxy());
        target.setUserAgent(source.getUserAgent());
        target.setHeaders(source.getHeaders());
    }
    
    /**
     * 优化建议结果
     */
    public static class OptimizationSuggestion {
        public final boolean hasOptimizations;
        public final ScanConfig optimizedConfig;
        public final String suggestions;
        
        public OptimizationSuggestion(boolean hasOptimizations, ScanConfig optimizedConfig, String suggestions) {
            this.hasOptimizations = hasOptimizations;
            this.optimizedConfig = optimizedConfig;
            this.suggestions = suggestions;
        }
        
        @Override
        public String toString() {
            if (!hasOptimizations) {
                return "无需优化: " + suggestions;
            }
            return "优化建议:\n" + suggestions;
        }
    }
    
    /**
     * 推荐配置
     */
    public static class RecommendedConfig {
        public final int threads;
        public final int timeout;
        public final int retries;
        
        public RecommendedConfig(int threads, int timeout, int retries) {
            this.threads = threads;
            this.timeout = timeout;
            this.retries = retries;
        }
        
        @Override
        public String toString() {
            return String.format("推荐配置: 线程数=%d, 超时=%d秒, 重试=%d次", 
                threads, timeout, retries);
        }
    }
}