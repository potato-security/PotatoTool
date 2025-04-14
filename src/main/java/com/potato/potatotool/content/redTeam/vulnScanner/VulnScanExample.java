package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 漏洞扫描示例类，展示如何使用漏洞扫描功能
 * @author Potato
 * @date 2025/3/21 17:00
 */
public class VulnScanExample {
    
    public static void main(String[] args) {
        // 获取漏洞扫描服务实例
        VulnScanService scanService = VulnScanService.getInstance();
        
        // 加载POC
        String pocDirPath = "/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/poc";
        int pocCount = scanService.loadPocs(pocDirPath);
        System.out.println("加载了 " + pocCount + " 个POC");
        
        // 设置扫描目标
        String targets = "example.com,test.example.org";
        int targetCount = scanService.setTargets(targets);
        System.out.println("设置了 " + targetCount + " 个目标");
        
        // 配置扫描参数
        scanService.setThreads(20); // 设置线程数
        scanService.setProtocolFilter("http"); // 只扫描HTTP协议的POC
        // scanService.setSeverityFilter(PocObj.Severity.HIGH); // 只扫描高危及以上的POC
        scanService.setDebugMode(debugMode); // 开启调试模式
        
        // 添加扫描结果监听器
        scanService.addResultListener(result -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = sdf.format(new Date(result.getTimestamp()));
            System.out.println("[" + time + "] 发现漏洞: " + result.getTarget() + " - " + result.getPoc().getName());
        });
        
        // 添加扫描完成监听器
        scanService.addCompleteListener(results -> {
            System.out.println("\n扫描完成，共发现 " + results.size() + " 个漏洞");
            
            // 输出漏洞统计信息
            int criticalCount = 0;
            int highCount = 0;
            int mediumCount = 0;
            int lowCount = 0;
            
            for (VulnScanExecutor.ScanResult result : results) {
                PocObj.Severity severity = result.getPoc().getSeverity();
                if (severity == PocObj.Severity.CRITICAL) {
                    criticalCount++;
                } else if (severity == PocObj.Severity.HIGH) {
                    highCount++;
                } else if (severity == PocObj.Severity.MEDIUM) {
                    mediumCount++;
                } else if (severity == PocObj.Severity.LOW) {
                    lowCount++;
                }
            }
            
            System.out.println("严重: " + criticalCount);
            System.out.println("高危: " + highCount);
            System.out.println("中危: " + mediumCount);
            System.out.println("低危: " + lowCount);
        });
        
        // 开始扫描
        System.out.println("开始扫描...");
        scanService.startScan();
        
        // 等待扫描完成
        while (scanService.isScanning()) {
            try {
                // 每秒输出一次进度
                System.out.println("扫描进度: " + scanService.getScanProgress() + "%");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 使用漏洞扫描服务的示例方法
     * @param targetUrl 目标URL
     * @return 扫描结果列表
     */
    public static List<VulnScanExecutor.ScanResult> scanTarget(String targetUrl) {
        // 获取漏洞扫描服务实例
        VulnScanService scanService = VulnScanService.getInstance();
        
        // 加载POC
        scanService.loadPocs(null); // 使用默认POC目录
        
        // 设置扫描目标
        scanService.setTargets(targetUrl);
        
        // 开始扫描
        scanService.startScan();
        
        // 等待扫描完成
        while (scanService.isScanning()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // 返回扫描结果
        return scanService.getScanResults();
    }
}