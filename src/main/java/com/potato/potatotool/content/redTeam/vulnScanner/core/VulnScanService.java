package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.core.FingerprintService.FingerprintResult;
import com.potato.potatotool.content.redTeam.vulnScanner.core.SmartPocSelector.PocSelectionResult;
import com.potato.potatotool.content.redTeam.vulnScanner.core.SmartPocSelector.MultiTargetSelectionResult;
import com.potato.potatotool.content.redTeam.vulnScanner.event.*;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.PocLoadException;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocRepository;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanState;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.PocDatabaseInitializer;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.PocDatabaseManager;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;

import static com.potato.potatotool.ToStart.debugMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 漏洞扫描服务 V2 (重构版本)
 * 提供高级扫描接口，使用新的架构
 * 
 * @author Potato
 * @date 2025-10-28
 */
public class VulnScanService {

    private static VulnScanService instance;

    // 核心组件
    private final PocRepository pocRepository;
    private final ScanEngine scanEngine;
    private final ScanConfig scanConfig;
    private final VulnScanConfig vulnConfig;
    private final SmartPocSelector smartPocSelector;

    // 目标列表
    private final List<String> targetList = new ArrayList<>();
    
    private VulnScanService() {
        this.vulnConfig = VulnScanConfig.getInstance();
        this.scanConfig = createDefaultScanConfig();
        this.pocRepository = new PocRepository();
        this.scanEngine = new ScanEngine(scanConfig);
        this.smartPocSelector = new SmartPocSelector(
            pocRepository, scanEngine.getPocExecutor(), scanEngine.getEventDispatcher());

        // 设置默认事件监听器
        setupDefaultEventListeners();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized VulnScanService getInstance() {
        if (instance == null) {
            instance = new VulnScanService();
        }
        return instance;
    }
    
    /**
     * 创建默认扫描配置
     */
    private ScanConfig createDefaultScanConfig() {
        ScanConfig config = new ScanConfig();
        config.setThreads(vulnConfig.getDefaultThreads());
        config.setTimeout(vulnConfig.getDefaultTimeout());
        config.setRetries(vulnConfig.getDefaultRetries());
        config.setMaxResponseSize(vulnConfig.getMaxResponseSize());
        config.setUserAgent(vulnConfig.getDefaultUserAgent());
        config.setDebug(vulnConfig.isDebugEnabled());
        return config;
    }
    
    /**
     * 设置默认事件监听器
     */
    private void setupDefaultEventListeners() {
        ScanLogger logger = ScanLogger.getInstance();

        scanEngine.addEventListener(new ScanEventListener() {
            @Override
            public void onScanStarted(ScanStartedEvent event) {
                logger.info("SCAN", "扫描开始: 共 " + event.getTotalTasks() + " 个任务");
            }

            @Override
            public void onScanProgress(ScanProgressEvent event) {
                // 进度更新不记录日志，避免日志过多
            }

            @Override
            public void onVulnerabilityFound(VulnerabilityFoundEvent event) {
                logger.success("SCAN", "[漏洞] 目标: " + event.getResult().getTarget() +
                    ", POC: " + event.getResult().getPoc().getName());
            }

            @Override
            public void onScanCompleted(ScanCompletedEvent event) {
                logger.info("SCAN", "扫描完成: 发现 " + event.getVulnerabilityCount() +
                    " 个漏洞, 耗时 " + event.getDuration() + "ms");
            }

            @Override
            public void onScanError(ScanErrorEvent event) {
                // 错误信息仅在 debug 模式下输出
                if (scanConfig.isDebug()) {
                    String error = event.getErrorMessage();
                    logger.error("SCAN", "扫描错误: " + error);
                    event.logUnrecognizedExpression(error);
                }
            }
        });
    }
    
    // ==================== POC管理 ====================
    
    /**
     * 加载POC目录（保留用于测试或手动加载）
     */
    public int loadPocs(String pocDirectory) throws PocLoadException {
        return pocRepository.loadDirectory(pocDirectory);
    }
    
    /**
     * 从数据库加载启用的 POC 到内存仓库
     * 这是新的默认加载方式，替代之前的文件系统加载
     */
    public int loadDefaultPocs() throws PocLoadException {
        try {
            // 等待 POC 数据库初始化完成（最多等待 30 秒）
            PocDatabaseInitializer initializer = PocDatabaseInitializer.getInstance();
            if (!initializer.isInitialized()) {
                if (debugMode) {
                    System.out.println("等待 POC 数据库初始化完成...");
                }
                boolean initCompleted = initializer.waitForInitialization(30000);
                if (!initCompleted) {
                    System.err.println("POC 数据库初始化超时，尝试继续加载");
                }
            }

            // 从数据库加载启用的 POC
            PocDatabaseManager pocDbManager = PocDatabaseManager.getInstance();
            List<PocObj.Poc> pocs = pocDbManager.getAllEnabledPocs();

            int count = 0;
            for (PocObj.Poc poc : pocs) {
                if (pocRepository.addPoc(poc)) {
                    count++;
                }
            }

            if (debugMode) {
                System.out.println("从数据库加载了 " + count + " 个 POC 到内存仓库");
            }

            return count;
        } catch (Exception e) {
            throw new PocLoadException("从数据库加载 POC 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从文件目录加载 POC（保留用于测试或手动加载）
     */
    public int loadPocsFromDirectory(String pocDirectory) throws PocLoadException {
        return pocRepository.loadDirectory(pocDirectory);
    }
    
    /**
     * 获取POC数量
     */
    public int getPocCount() {
        return pocRepository.getCount();
    }
    
    /**
     * 获取所有POC
     */
    public List<PocObj.Poc> getAllPocs() {
        return pocRepository.getAllPocs();
    }
    
    /**
     * 根据标签搜索POC
     */
    public List<PocObj.Poc> searchPocsByTag(String tag) {
        return pocRepository.findByTag(tag);
    }
    
    /**
     * 根据严重程度搜索POC
     */
    public List<PocObj.Poc> searchPocsBySeverity(PocObj.Severity severity) {
        return pocRepository.findBySeverity(severity);
    }
    
    /**
     * 关键词搜索POC
     */
    public List<PocObj.Poc> searchPocs(String keyword) {
        return pocRepository.search(keyword);
    }
    
    // ==================== 目标管理 ====================
    
    /**
     * 设置目标列表
     */
    public void setTargets(List<String> targets) {
        targetList.clear();
        if (targets != null) {
            targetList.addAll(targets);
        }
    }

    /**
     * 添加目标
     */
    public void addTarget(String target) {
        if (target != null && !target.trim().isEmpty()) {
            targetList.add(normalizeTarget(target.trim()));
        }
    }
    
    /**
     * 规范化目标URL
     */
    private String normalizeTarget(String target) {
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            return "http://" + target;
        }
        return target;
    }
    
    /**
     * 清空目标列表
     */
    public void clearTargets() {
        targetList.clear();
    }

    /**
     * 获取目标数量
     */
    public int getTargetCount() {
        return targetList.size();
    }
    
    // ==================== 扫描配置 ====================
    
    /**
     * 设置线程数
     */
    public void setThreads(int threads) {
        scanConfig.setThreads(threads);
    }
    
    /**
     * 设置超时时间（秒）
     */
    public void setTimeout(int timeout) {
        scanConfig.setTimeout(timeout);
    }
    
    /**
     * 设置调试模式
     */
    public void setDebug(boolean debug) {
        scanConfig.setDebug(debug);
    }
    
    /**
     * 设置代理
     */
    public void setProxy(String proxy) {
        scanConfig.setProxy(proxy);
    }
    
    /**
     * 获取扫描配置
     */
    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    public ScanConfig getScanConfigSnapshot() {
        return cloneScanConfig(scanConfig);
    }

    public List<String> getTargetsSnapshot() {
        return new ArrayList<>(targetList);
    }

    private ScanConfig cloneScanConfig(ScanConfig source) {
        ScanConfig target = new ScanConfig();
        target.setThreads(source.getThreads());
        target.setProtocol(source.getProtocol());
        target.setTags(source.getTags());
        target.setSeverity(source.getSeverity());
        target.setDebug(source.isDebug());
        target.setProxy(source.getProxy());
        target.setTimeout(source.getTimeout());
        target.setRetries(source.getRetries());
        target.setFollowRedirects(source.isFollowRedirects());
        target.setUserAgent(source.getUserAgent());
        target.setHeaders(new java.util.HashMap<>(source.getHeaders()));
        target.setMaxResponseSize(source.getMaxResponseSize());
        target.setInputType(source.getInputType());
        target.setAutoDetectInputType(source.isAutoDetectInputType());
        target.setScanMode(source.getScanMode());
        target.setEnabledPocFormats(new java.util.HashSet<>(source.getEnabledPocFormats()));
        target.setEnabledCategories(new java.util.HashSet<>(source.getEnabledCategories()));
        target.setExcludedCategories(new java.util.HashSet<>(source.getExcludedCategories()));
        target.setSkipFingerprint(source.isSkipFingerprint());
        target.setFingerprintTimeout(source.getFingerprintTimeout());
        target.setEnableHoneypotDetection(source.isEnableHoneypotDetection());
        target.setStopOnHoneypot(source.isStopOnHoneypot());
        target.setEnableConfigAudit(source.isEnableConfigAudit());
        target.setMinSeverity(source.getMinSeverity());
        target.setEnableHeadless(source.isEnableHeadless());
        target.setEnableCode(source.isEnableCode());
        target.setEnableFuzz(source.isEnableFuzz());
        target.setEnableDeduplication(source.isEnableDeduplication());
        target.setEnableResponseCache(source.isEnableResponseCache());
        target.setResponseCacheTtlMs(source.getResponseCacheTtlMs());
        target.setEnableClustering(source.isEnableClustering());
        target.setLocalTargetPath(source.getLocalTargetPath());
        target.setTargetProtocol(source.getTargetProtocol());
        target.setFileExtensions(new ArrayList<>(source.getFileExtensions()));
        return target;
    }

    // ==================== 扫描控制 ====================
    
    /**
     * 开始扫描（使用所有POC）
     */
    public void startScan() {
        List<PocObj.Poc> pocs = pocRepository.getAllPocs();
        startScan(getTargetsSnapshot(), pocs);
    }

    public void startScan(List<String> targets, List<PocObj.Poc> pocs) {
        scanEngine.setFingerprintResult(null);
        scanEngine.setFingerprintResultMap(null);
        scanEngine.startScan(new ArrayList<>(targets), pocs, getScanConfigSnapshot());
    }

    /**
     * 开始扫描（使用指定POC）
     */
    public void startScan(List<PocObj.Poc> pocs) {
        startScan(getTargetsSnapshot(), pocs);
    }

    public void startScan(List<String> targets, List<PocObj.Poc> pocs, FingerprintResult fingerprint) {
        scanEngine.setFingerprintResult(fingerprint);
        scanEngine.setFingerprintResultMap(null);
        scanEngine.startScan(new ArrayList<>(targets), pocs, getScanConfigSnapshot());
    }

    /**
     * 智能 POC 筛选（供 PaneVulScan 调用）
     */
    public PocSelectionResult selectAndFilterPocs(String target, ScanConfig config) {
        return smartPocSelector.selectAndFilterPocs(target, config);
    }

    /**
     * 开始扫描（使用指定POC + 指纹结果 + UI配置同步）
     */
    public void startScan(List<PocObj.Poc> pocs, FingerprintResult fingerprint) {
        startScan(getTargetsSnapshot(), pocs, fingerprint);
    }

    /**
     * 多目标智能 POC 筛选
     */
    public MultiTargetSelectionResult selectAndFilterPocsMultiTarget(List<String> targets, ScanConfig config) {
        return smartPocSelector.selectAndFilterPocsMultiTarget(targets, config);
    }

    /**
     * 开始扫描（使用指定POC + 多目标指纹映射）
     */
    public void startScan(List<PocObj.Poc> pocs, Map<String, FingerprintResult> fingerprintMap) {
        startScan(getTargetsSnapshot(), pocs, fingerprintMap);
    }

    public void startScan(List<String> targets, List<PocObj.Poc> pocs, Map<String, FingerprintResult> fingerprintMap) {
        scanEngine.setFingerprintResult(null);
        scanEngine.setFingerprintResultMap(fingerprintMap);
        scanEngine.startScan(new ArrayList<>(targets), pocs, getScanConfigSnapshot());
    }

    /**
     * 将 UI 构建的 ScanConfig 中的关键参数同步到内部 scanConfig
     * 确保 ScanEngine 使用正确的配置
     */
    public void applyScanConfig(ScanConfig uiConfig) {
        scanConfig.setThreads(uiConfig.getThreads());
        scanConfig.setTimeout(uiConfig.getTimeout());
        scanConfig.setDebug(uiConfig.isDebug());
        scanConfig.setProxy(uiConfig.getProxy());
        scanConfig.setEnableClustering(uiConfig.isEnableClustering());
        scanConfig.setEnableResponseCache(uiConfig.isEnableResponseCache());
        scanConfig.setRetries(uiConfig.getRetries());
        scanConfig.setEnableHeadless(uiConfig.isEnableHeadless());
        scanConfig.setEnableCode(uiConfig.isEnableCode());
        scanConfig.setEnableFuzz(uiConfig.isEnableFuzz());
    }
    
    /**
     * 暂停扫描
     */
    public void pauseScan() {
        scanEngine.pauseScan();
    }
    
    /**
     * 恢复扫描
     */
    public void resumeScan(ScanState state) {
        // 从状态中恢复POC列表
        List<String> pocIds = state.getPocIds();
        List<PocObj.Poc> pocs = new ArrayList<>();
        for (String id : pocIds) {
            PocObj.Poc poc = pocRepository.getPoc(id);
            if (poc != null) {
                pocs.add(poc);
            }
        }

        if (pocs.isEmpty()) {
            throw new IllegalStateException("无法加载POC，可能已被删除");
        }

        scanEngine.resumeScan(state, pocs);
    }
    
    /**
     * 停止扫描
     */
    public void stopScan() {
        scanEngine.stopScan();
    }
    
    /**
     * 是否正在扫描
     */
    public boolean isScanning() {
        return scanEngine.isScanning();
    }
    
    /**
     * 是否已暂停
     */
    public boolean isPaused() {
        return scanEngine.isPaused();
    }

    /**
     * 获取当前扫描任务ID
     * 用于在保存历史记录时关联同一次扫描
     */
    public String getCurrentScanId() {
        return scanEngine.getCurrentScanId();
    }

    /**
     * 加载所有扫描任务（用于历史记录）
     */
    public List<ScanState> loadAllScanTasks() {
        try {
            return VulnScanDatabase.getInstance().loadAllScanTasks();
        } catch (Exception e) {
            System.err.println("加载扫描任务失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 加载指定扫描任务
     */
    public ScanState loadScanTask(String scanId) {
        try {
            return VulnScanDatabase.getInstance().loadScanState(scanId);
        } catch (Exception e) {
            System.err.println("加载扫描任务失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 查询未完成的扫描（仅 PAUSED 和 RUNNING 状态）
     */
    public List<ScanState> loadPausedScans() {
        try {
            return VulnScanDatabase.getInstance().loadPausedScans();
        } catch (Exception e) {
            System.err.println("加载未完成扫描失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // ==================== 结果获取 ====================
    
    /**
     * 获取扫描结果
     */
    public List<ScanResult> getScanResults() {
        return scanEngine.getScanResults();
    }
    
    /**
     * 获取漏洞数量
     */
    public int getVulnerabilityCount() {
        return (int) scanEngine.getScanResults().stream()
            .filter(ScanResult::isVulnerable)
            .count();
    }
    
    // ==================== 事件监听 ====================
    
    /**
     * 添加事件监听器
     */
    public void addEventListener(ScanEventListener listener) {
        scanEngine.addEventListener(listener);
    }
    
    /**
     * 移除事件监听器
     */
    public void removeEventListener(ScanEventListener listener) {
        scanEngine.removeEventListener(listener);
    }
    
    // ==================== 生命周期 ====================
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        scanEngine.shutdown();
    }
    
    /**
     * 获取POC仓库
     */
    public PocRepository getPocRepository() {
        return pocRepository;
    }
}
