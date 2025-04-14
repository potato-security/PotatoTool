package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocManager;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 漏洞扫描服务，提供漏洞扫描的高级接口
 * @author Potato
 * @date 2025/3/21 16:00
 */
public class VulnScanService {
    // 单例实例
    private static VulnScanService instance;
    
    // 扫描执行器
    private final VulnScanExecutor scanExecutor;
    
    // 扫描结果监听器列表
    private final List<Consumer<VulnScanExecutor.ScanResult>> resultListeners = new CopyOnWriteArrayList<>();
    
    // 扫描完成监听器列表
    private final List<Consumer<List<VulnScanExecutor.ScanResult>>> completeListeners = new CopyOnWriteArrayList<>();
    
    // 默认POC目录
//    private static final String DEFAULT_POC_DIR = "/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/poc";
    private static final String DEFAULT_POC_DIR = "C:\\Users\\potato\\Desktop\\PotatoTool\\src\\main\\java\\com\\potato\\potatotool\\content\\redTeam\\vulnScanner\\poc";
    /**
     * 私有构造函数
     */
    private VulnScanService() {
        // 创建扫描执行器
        VulnScanExecutor.ScanConfig config = new VulnScanExecutor.ScanConfig();
        config.setThreads(10); // 默认10个线程
        config.setDebug(false); // 默认不开启调试模式
        
        scanExecutor = new VulnScanExecutor(config);
    }
    
    /**
     * 获取单例实例
     * @return VulnScanService实例
     */
    public static synchronized VulnScanService getInstance() {
        if (instance == null) {
            instance = new VulnScanService();
        }
        return instance;
    }
    
    /**
     * 加载POC
     * @param pocDirPath POC目录路径，如果为null则使用默认目录
     * @return 加载的POC数量
     */
    public int loadPocs(String pocDirPath) {
        if (pocDirPath == null || pocDirPath.isEmpty()) {
            pocDirPath = DEFAULT_POC_DIR;
        }
        
        // 确保目录存在
        File pocDir = new File(pocDirPath);
        if (!pocDir.exists() || !pocDir.isDirectory()) {
            System.err.println("POC目录不存在: " + pocDirPath);
            return 0;
        }
        
        return scanExecutor.loadPocs(pocDirPath);
    }
    
    /**
     * 获取已加载的POC列表
     * @return POC列表
     */
    public List<PocObj.Poc> getPocList() {
        return scanExecutor.getPocList();
    }
    
    /**
     * 设置目标
     * @param targets 目标字符串，可以是单个URL、多个URL（以逗号分隔）或包含URL的文件路径
     * @return 解析的目标数量
     */
    public int setTargets(String targets) {
        return scanExecutor.parseTargets(targets);
    }
    
    /**
     * 获取目标列表
     * @return 目标列表
     */
    public List<String> getTargetList() {
        return scanExecutor.getTargetList();
    }
    
    /**
     * 设置扫描配置
     * @param config 扫描配置
     */
    public void setScanConfig(VulnScanExecutor.ScanConfig config) {
        scanExecutor.setScanConfig(config);
    }
    
    /**
     * 获取扫描配置
     * @return 扫描配置
     */
    public VulnScanExecutor.ScanConfig getScanConfig() {
        return scanExecutor.getScanConfig();
    }
    
    /**
     * 设置线程数
     * @param threads 线程数
     */
    public void setThreads(int threads) {
        VulnScanExecutor.ScanConfig config = scanExecutor.getScanConfig();
        config.setThreads(threads);
        scanExecutor.setScanConfig(config);
    }
    
    /**
     * 设置协议过滤
     * @param protocol 协议类型
     */
    public void setProtocolFilter(String protocol) {
        VulnScanExecutor.ScanConfig config = scanExecutor.getScanConfig();
        config.setProtocol(protocol);
        scanExecutor.setScanConfig(config);
    }
    
    /**
     * 设置严重程度过滤
     * @param severity 严重程度
     */
    public void setSeverityFilter(PocObj.Severity severity) {
        VulnScanExecutor.ScanConfig config = scanExecutor.getScanConfig();
        config.setSeverity(severity);
        scanExecutor.setScanConfig(config);
    }
    
    /**
     * 设置调试模式
     * @param debug 是否开启调试模式
     */
    public void setDebugMode(boolean debug) {
        VulnScanExecutor.ScanConfig config = scanExecutor.getScanConfig();
        config.setDebug(debug);
        scanExecutor.setScanConfig(config);
    }
    
    /**
     * 设置代理
     * @param proxy 代理地址
     */
    public void setProxy(String proxy) {
        VulnScanExecutor.ScanConfig config = scanExecutor.getScanConfig();
        config.setProxy(proxy);
        scanExecutor.setScanConfig(config);
    }
    
    /**
     * 添加扫描结果监听器
     * @param listener 监听器
     */
    public void addResultListener(Consumer<VulnScanExecutor.ScanResult> listener) {
        if (listener != null) {
            resultListeners.add(listener);
        }
    }
    
    /**
     * 移除扫描结果监听器
     * @param listener 监听器
     */
    public void removeResultListener(Consumer<VulnScanExecutor.ScanResult> listener) {
        resultListeners.remove(listener);
    }
    
    /**
     * 添加扫描完成监听器
     * @param listener 监听器
     */
    public void addCompleteListener(Consumer<List<VulnScanExecutor.ScanResult>> listener) {
        if (listener != null) {
            completeListeners.add(listener);
        }
    }
    
    /**
     * 移除扫描完成监听器
     * @param listener 监听器
     */
    public void removeCompleteListener(Consumer<List<VulnScanExecutor.ScanResult>> listener) {
        completeListeners.remove(listener);
    }
    
    /**
     * 开始扫描
     * @return 是否成功启动扫描
     */
    public boolean startScan() {
        if (scanExecutor.isScanning()) {
            System.err.println("扫描任务正在进行中，请等待当前扫描完成");
            return false;
        }
        
        if (scanExecutor.getPocList().isEmpty()) {
            System.err.println("未加载任何POC，请先加载POC");
            return false;
        }
        
        if (scanExecutor.getTargetList().isEmpty()) {
            System.err.println("未设置任何目标，请先设置目标");
            return false;
        }
        
        // 创建回调
        VulnScanExecutor.ScanCallback callback = new VulnScanExecutor.ScanCallback() {
            @Override
            public void onResult(VulnScanExecutor.ScanResult result) {
                // 通知所有结果监听器
                for (Consumer<VulnScanExecutor.ScanResult> listener : resultListeners) {
                    try {
                        listener.accept(result);
                    } catch (Exception e) {
                        System.err.println("通知结果监听器时发生异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onComplete(List<VulnScanExecutor.ScanResult> results) {
                // 通知所有完成监听器
                for (Consumer<List<VulnScanExecutor.ScanResult>> listener : completeListeners) {
                    try {
                        listener.accept(results);
                    } catch (Exception e) {
                        System.err.println("通知完成监听器时发生异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        };
        
        // 启动扫描
        scanExecutor.startScan(callback);
        return true;
    }
    
    /**
     * 停止扫描
     */
    public void stopScan() {
        scanExecutor.stopScan();
    }
    
    /**
     * 是否正在扫描
     * @return 是否正在扫描
     */
    public boolean isScanning() {
        return scanExecutor.isScanning();
    }
    
    /**
     * 获取扫描结果
     * @return 扫描结果列表
     */
    public List<VulnScanExecutor.ScanResult> getScanResults() {
        return scanExecutor.getScanResults();
    }
    
    /**
     * 获取漏洞数量
     * @return 漏洞数量
     */
    public int getVulnerableCount() {
        int count = 0;
        for (VulnScanExecutor.ScanResult result : scanExecutor.getScanResults()) {
            if (result.isVulnerable()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取扫描进度（简单估算）
     * @return 扫描进度（0-100）
     */
    public int getScanProgress() {
        if (!scanExecutor.isScanning()) {
            return 0; // 未开始扫描
        }
        
        int totalTasks = scanExecutor.getTargetList().size() * scanExecutor.getPocList().size();
        int completedTasks = scanExecutor.getScanResults().size();
        
        if (totalTasks == 0) {
            return 0;
        }
        
        return (int) ((completedTasks * 100.0) / totalTasks);
    }
    
    /**
     * 清空扫描结果
     */
    public void clearResults() {
        scanExecutor.getScanResults().clear();
    }
}