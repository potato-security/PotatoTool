package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PerformanceMonitor;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ConfigOptimizer;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ConnectionPoolManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 优化后的漏洞扫描示例
 * 展示如何使用优化后的扫描器功能
 */
public class OptimizedVulnScanExample {
    
    public static void main(String[] args) {
        // 创建扫描服务实例
        VulnScanService scanService = VulnScanService.getInstance();
        
        try {
            // 1. 加载POC文件
//            String pocDirectory = "src/main/resources/pocs";
            String pocDirectory = "/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/gobyAllPoc";
            scanService.loadPocs(pocDirectory);
            logSafely("已加载POC数量: " + scanService.getPocCount());
            
            // 2. 设置扫描目标
            List<String> targets = Arrays.asList(
                "https://potato.gold",
                "http://example.com"
//                ,"https://test.example.com"
            );
            scanService.setTargets(targets);
            
            // 3. 创建优化的扫描配置
            ScanConfig config = createOptimizedConfig(targets.size(), scanService.getPocCount());
            scanService.setScanConfig(config);
            
            // 4. 设置扫描回调
            CountDownLatch scanLatch = new CountDownLatch(1);
            
            scanService.setResultCallback(new VulnScanCallback() {
                @Override
                public void onResult(ScanResult result) {
                    if (result.isVulnerable()) {
                        logSafely("发现漏洞: " + result.getTarget() + 
                            " - " + result.getPoc().getName());
                        logSafely("漏洞详情: " + result.getDetails());
                    }
                }
                
                @Override
                public void onComplete(List<ScanResult> results) {
                    logSafely("\n=== 扫描完成 ===");
                    
                    // 统计结果
                    long vulnerableCount = results.stream()
                        .mapToLong(r -> r.isVulnerable() ? 1 : 0)
                        .sum();
                    
                    logSafely("总扫描结果: " + results.size());
                    logSafely("发现漏洞: " + vulnerableCount);
                    
                    // 输出性能统计
                    printPerformanceStats();
                    
                    // 输出优化建议
                    printOptimizationSuggestions(config);
                    
                    scanLatch.countDown();
                }
                
                public void onError(Exception e) {
                    logErrorSafely("扫描出错: " + e.getMessage());
                    e.printStackTrace();
                    scanLatch.countDown();
                }
            });
            
            // 5. 启动扫描
            logSafely("\n开始扫描...");
            scanService.startScan();
            
            // 6. 监控扫描进度
            monitorScanProgress(scanService);
            
            // 7. 等待扫描完成
            if (!scanLatch.await(300, TimeUnit.SECONDS)) {
                logErrorSafely("扫描超时，强制停止");
                scanService.stopScan();
            }
            
        } catch (Exception e) {
            logErrorSafely("扫描过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            logSafely("正在清理资源...");
            scanService.stopScan();
            
            // 等待一段时间确保所有资源清理完成
            try {
                Thread.sleep(2000); // 增加等待时间确保资源完全清理
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            logSafely("资源清理完成，程序将自然退出...");
        }
    }
    
    /**
     * 创建优化的扫描配置
     */
    private static ScanConfig createOptimizedConfig(int targetCount, int pocCount) {
        ScanConfig config = new ScanConfig();
        
        // 使用配置优化器估算最优参数
        ConfigOptimizer optimizer = ConfigOptimizer.getInstance();
        ScanConfig optimizedConfig = optimizer.estimateOptimalConfig(targetCount, pocCount, config);
        
        // 应用优化配置，但设置合理的上限
        int recommendedThreads = Math.min(optimizedConfig.getThreads(), 20); // 限制最大线程数
        int recommendedTimeout = Math.max(optimizedConfig.getTimeout(), 15); // 确保最小超时时间
        
        config.setThreads(recommendedThreads);
        config.setTimeout(recommendedTimeout);
        config.setRetries(2);
        config.setDebug(true);
        config.setFollowRedirects(true);
        
        // 设置用户代理
        config.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        logSafely("使用优化配置: 线程数=" + config.getThreads() + 
            ", 超时=" + config.getTimeout() + "秒, 重试=" + config.getRetries() + "次");
        
        return config;
    }
    
    /**
     * 监控扫描进度
     */
    private static void monitorScanProgress(VulnScanService scanService) {
        Thread progressThread = new Thread(() -> {
            try {
                while (scanService.isScanning()) {
                    Thread.sleep(5000); // 每5秒输出一次进度
                    
                    // 获取实时性能指标
                    PerformanceMonitor.PerformanceMetrics metrics = 
                        PerformanceMonitor.getInstance().getCurrentMetrics();
                    
                    logSafely(String.format("进度: 已完成 %d 个任务, 成功率: %.1f%%, 平均响应时间: %d ms",
                        metrics.totalScans, metrics.getSuccessRate(), metrics.averageScanTime));
                    
                    // 输出连接池状态
                    ConnectionPoolManager.ConnectionPoolStatus poolStatus = 
                        ConnectionPoolManager.getInstance().getStatus();
                    logSafely(String.format("连接池: 活跃连接 %d, 总连接 %d", 
                        poolStatus.activeConnections, poolStatus.totalConnections));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        progressThread.setDaemon(true);
        progressThread.setName("ScanProgressMonitor");
        progressThread.start();
    }
    
    /**
     * 输出性能统计
     */
    private static void printPerformanceStats() {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        PerformanceMonitor.PerformanceMetrics metrics = monitor.getCurrentMetrics();
        
        logSafely("\n=== 性能统计 ===");
        logSafely(String.format("总扫描次数: %d", metrics.totalScans));
        logSafely(String.format("成功扫描: %d", metrics.successfulScans));
        logSafely(String.format("失败扫描: %d", metrics.failedScans));
        logSafely(String.format("成功率: %.2f%%", metrics.getSuccessRate()));
        logSafely(String.format("平均扫描时间: %d ms", metrics.averageScanTime));
        
        
        // 输出连接池统计
        ConnectionPoolManager.ConnectionPoolStatus poolStatus = 
            ConnectionPoolManager.getInstance().getStatus();
        logSafely("\n=== 连接池统计 ===");
        logSafely(poolStatus.toString());
    }
    
    /**
     * 输出优化建议
     */
    private static void printOptimizationSuggestions(ScanConfig currentConfig) {
        ConfigOptimizer optimizer = ConfigOptimizer.getInstance();
        PerformanceMonitor.PerformanceMetrics metrics = 
            PerformanceMonitor.getInstance().getCurrentMetrics();
        
        ConfigOptimizer.OptimizationSuggestion suggestion = 
            optimizer.optimizeConfig(metrics, currentConfig);
        
        logSafely("\n=== 优化建议 ===");
        logSafely(suggestion.toString());
        
        // 输出推荐配置
        ConfigOptimizer.RecommendedConfig recommended = optimizer.getRecommendedConfig();
        logSafely(recommended.toString());
     }
     
     // 线程安全的日志方法
     private static synchronized void logSafely(String message) {
         System.out.println(message);
     }
     
     private static synchronized void logErrorSafely(String message) {
         System.err.println(message);
     }
 }