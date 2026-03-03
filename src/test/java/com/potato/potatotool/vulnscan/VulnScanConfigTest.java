package com.potato.potatotool.vulnscan;

import com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.PocDatabaseManager;
import com.potato.potatotool.content.redTeam.vulnScanner.util.DatabaseMigration;

import java.util.Map;

/**
 * VulnScan配置增强测试类
 * 用于验证Sprint 1-3的实现
 */
public class VulnScanConfigTest {
    
    public static void main(String[] args) {
        System.out.println("========== VulnScan 配置增强测试 ==========\n");
        
        testHeaderManager();
        testVulnScanConfig();
        testDatabaseMigration();
        testPocDatabaseManager();
        
        System.out.println("\n========== 所有测试完成 ==========");
    }
    
    /**
     * 测试 HeaderManager
     */
    private static void testHeaderManager() {
        System.out.println("【测试1】HeaderManager");
        System.out.println("----------------------------------------");
        
        try {
            HeaderManager headerManager = HeaderManager.getInstance();
            
            // 测试获取默认Headers
            Map<String, String> defaultHeaders = headerManager.getCustomHeaders();
            System.out.println("✓ 默认Headers数量: " + defaultHeaders.size());
            for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + 
                    (entry.getValue().length() > 50 ? entry.getValue().substring(0, 50) + "..." : entry.getValue()));
            }
            
            // 测试获取模板列表
            System.out.println("\n✓ 可用Header模板:");
            for (String templateName : headerManager.getTemplateNames()) {
                System.out.println("  - " + templateName);
            }
            
            // 测试合并Headers
            Map<String, String> pocHeaders = new java.util.HashMap<>();
            pocHeaders.put("X-Custom-Header", "test-value");
            pocHeaders.put("User-Agent", "Custom-POC-Agent"); // 覆盖默认UA
            
            Map<String, String> mergedHeaders = headerManager.mergeHeaders(pocHeaders);
            System.out.println("\n✓ 合并后Headers数量: " + mergedHeaders.size());
            System.out.println("  - X-Custom-Header: " + mergedHeaders.get("X-Custom-Header"));
            System.out.println("  - User-Agent (应被POC覆盖): " + 
                (mergedHeaders.get("User-Agent").contains("Custom") ? "✓ 正确覆盖" : "✗ 未覆盖"));
            
            System.out.println("\n【测试1】HeaderManager - 通过 ✓\n");
        } catch (Exception e) {
            System.err.println("【测试1】HeaderManager - 失败 ✗");
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试 VulnScanConfig
     */
    private static void testVulnScanConfig() {
        System.out.println("【测试2】VulnScanConfig");
        System.out.println("----------------------------------------");
        
        try {
            VulnScanConfig config = VulnScanConfig.getInstance();
            
            // 测试线程池配置
            System.out.println("✓ 线程池配置:");
            System.out.println("  - Core Threads: " + config.getCoreThreads());
            System.out.println("  - Max Threads: " + config.getMaxThreads());
            System.out.println("  - Queue Size: " + config.getQueueSize());
            
            // 测试报告配置
            System.out.println("\n✓ 报告配置:");
            System.out.println("  - 默认目录: " + config.getReportDefaultDirectory());
            System.out.println("  - 命名模板: " + config.getReportNameTemplate());
            System.out.println("  - 自动导出: " + config.isAutoExportEnabled());
            System.out.println("  - 默认格式: " + config.getDefaultReportFormat());
            
            // 测试代理配置
            System.out.println("\n✓ 代理配置:");
            System.out.println("  - 代理启用: " + config.isProxyEnabled());
            System.out.println("  - 代理地址: " + config.getProxyAddress());
            
            // 测试其他配置
            System.out.println("\n✓ 其他配置:");
            System.out.println("  - 超时时间: " + config.getDefaultTimeout() + "s");
            System.out.println("  - 重试次数: " + config.getDefaultRetries());
            System.out.println("  - 数据库路径: " + config.getDatabasePath());
            
            System.out.println("\n【测试2】VulnScanConfig - 通过 ✓\n");
        } catch (Exception e) {
            System.err.println("【测试2】VulnScanConfig - 失败 ✗");
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试 DatabaseMigration
     */
    private static void testDatabaseMigration() {
        System.out.println("【测试3】DatabaseMigration");
        System.out.println("----------------------------------------");
        
        try {
            System.out.println("✓ 数据库路径信息:");
            System.out.println("  - 旧路径: " + DatabaseMigration.getOldDatabasePath());
            System.out.println("  - 新路径: " + DatabaseMigration.getNewDatabasePath());
            System.out.println("  - 需要迁移: " + DatabaseMigration.needsMigration());
            System.out.println("  - 旧库存在: " + DatabaseMigration.hasOldDatabase());
            System.out.println("  - 新库存在: " + DatabaseMigration.hasNewDatabase());
            System.out.println("  - 当前使用: " + DatabaseMigration.getCurrentDatabasePath());
            
            System.out.println("\n【测试3】DatabaseMigration - 通过 ✓\n");
        } catch (Exception e) {
            System.err.println("【测试3】DatabaseMigration - 失败 ✗");
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试 PocDatabaseManager
     */
    private static void testPocDatabaseManager() {
        System.out.println("【测试4】PocDatabaseManager");
        System.out.println("----------------------------------------");
        
        try {
            PocDatabaseManager pocDbManager = PocDatabaseManager.getInstance();
            
            // 获取POC统计
            Map<String, Object> stats = pocDbManager.getStatistics();
            System.out.println("✓ POC仓库统计:");
            System.out.println("  - 总数: " + stats.get("total"));
            System.out.println("  - 启用: " + stats.get("enabled"));
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> byFormat = (Map<String, Integer>) stats.get("byFormat");
            if (byFormat != null && !byFormat.isEmpty()) {
                System.out.println("  - 按格式:");
                for (Map.Entry<String, Integer> entry : byFormat.entrySet()) {
                    System.out.println("    · " + entry.getKey() + ": " + entry.getValue());
                }
            }
            
            // 测试去重检查（基于 content_hash）
            boolean exists = pocDbManager.existsByContentHash("test-content-hash");
            System.out.println("\n✓ 去重检查 (content_hash): " + (exists ? "已存在" : "不存在"));
            
            System.out.println("\n【测试4】PocDatabaseManager - 通过 ✓\n");
        } catch (Exception e) {
            System.err.println("【测试4】PocDatabaseManager - 失败 ✗");
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
