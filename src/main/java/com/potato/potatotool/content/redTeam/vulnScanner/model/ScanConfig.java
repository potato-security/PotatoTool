package com.potato.potatotool.content.redTeam.vulnScanner.model;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.PocCategory;

import java.util.*;

/**
 * 漏洞扫描配置类
 */
public class ScanConfig {
    // ========== 基础配置 ==========
    private int threads = 20; // 默认线程数
    private String protocol; // 协议类型过滤
    private PocObj.Severity severity; // 严重程度过滤
    private boolean debug = false; // 是否开启调试模式
    private String proxy; // 全局代理
    private int timeout = 15; // HTTP请求超时时间（秒），用于连接和读取超时设置
    private int retries = 2; // 重试次数
    private boolean followRedirects = true; // 是否跟随重定向
    private String userAgent = ""; // 用户代理，为空时走默认随机UA逻辑
    private Map<String, String> headers = new HashMap<>(); // 请求头
    private int maxResponseSize = 1024 * 1024; // 最大响应大小（字节），默认1MB
    
    // ========== 新增：输入类型与扫描模式 ==========
    /**
     * 输入类型（URL/IP:端口/域名/本地路径）
     */
    private InputType inputType = InputType.URL;
    
    /**
     * 是否自动检测输入类型
     */
    private boolean autoDetectInputType = true;
    
    /**
     * 扫描模式
     */
    private ScanMode scanMode = ScanMode.STANDARD;
    
    // ========== 新增：POC 来源选择 ==========
    /**
     * 启用的 POC 格式
     * 默认启用所有格式
     */
    private Set<String> enabledPocFormats = new HashSet<>(Arrays.asList(
        "nuclei", "goby", "xray", "pocsuite"
    ));
    
    // ========== 新增：分类筛选 ==========
    /**
     * 启用的 POC 分类（空集合表示不限制）
     */
    private Set<PocCategory> enabledCategories = new HashSet<>();
    
    /**
     * 排除的 POC 分类
     * 默认排除 OSINT、TOKEN_SPRAY、CREDENTIAL_STUFFING
     */
    private Set<PocCategory> excludedCategories = new HashSet<>(Arrays.asList(
        PocCategory.OSINT,
        PocCategory.TOKEN_SPRAY,
        PocCategory.CREDENTIAL_STUFFING
    ));
    
    // ========== 新增：指纹识别配置 ==========
    /**
     * 是否跳过指纹识别
     * 跳过后将直接运行所有选中的 POC（默认模式）
     */
    private boolean skipFingerprint = false;
    
    /**
     * 指纹识别超时时间（秒）
     */
    private int fingerprintTimeout = 10;

    // ========== 新增：分阶段扫描配置 ==========
    /**
     * 是否启用蜜罐检测（阶段0）
     * 启用后会在扫描前检测目标是否为蜜罐
     */
    private boolean enableHoneypotDetection = true;

    /**
     * 检测到蜜罐时是否停止扫描
     */
    private boolean stopOnHoneypot = false;

    /**
     * 是否启用配置审计（阶段4）
     * 启用后会执行通用配置审计类 POC
     */
    private boolean enableConfigAudit = true;

    /**
     * 严重程度过滤阈值（用于 QUICK 模式）
     * 只执行高于等于该级别的漏洞 POC
     */
    private PocObj.Severity minSeverity;
    
    // ========== 新增：特殊 Flag ==========
    /**
     * 启用 Headless 模板（DOM XSS 等）
     */
    private boolean enableHeadless = false;
    
    /**
     * 启用 Code 模板（本地代码执行）
     */
    private boolean enableCode = false;
    
    /**
     * 启用 Fuzz 模板（模糊测试）
     */
    private boolean enableFuzz = false;
    
    // ========== 新增：去重配置 ==========
    /**
     * 是否启用 POC 去重
     */
    private boolean enableDeduplication = true;
    
    // ========== 新增：响应缓存配置 ==========
    /**
     * 是否启用响应缓存（用于请求复用）
     */
    private boolean enableResponseCache = true;
    
    /**
     * 响应缓存 TTL（毫秒）
     */
    private long responseCacheTtlMs = 30000;
    
    // ========== 新增：请求聚类配置 ==========
    /**
     * 是否启用请求聚类
     * 将相同请求特征的 POC 合并，减少重复请求
     */
    private boolean enableClustering = true;
    
    // ========== 新增：File 扫描特殊配置 ==========
    /**
     * 本地目标路径（用于 File 扫描）
     */
    private String localTargetPath;

    /**
     * 目标协议类型（自动检测或手动设置）
     * 用于智能模式下过滤不匹配协议的 POC
     * 例如：http, https, ssh, mysql, redis 等
     */
    private String targetProtocol;

    /**
     * 文件扩展名过滤（用于 File 扫描）
     */
    private List<String> fileExtensions = new ArrayList<>();
    
    /**
     * 扫描模式枚举
     * 提供多种预设扫描策略，满足不同场景需求
     */
    public enum ScanMode {
        /**
         * 资产识别模式
         * 只运行指纹识别，获取技术栈信息
         * POC类别：TECHNOLOGIES, EXPOSED_PANELS, HONEYPOT, NETWORK_DETECTION
         */
        DISCOVERY,

        /**
         * 快速模式
         * 指纹识别 + 高危/严重漏洞
         * POC类别：指纹 + CVES(CRITICAL/HIGH) + DEFAULT_LOGINS
         */
        QUICK,

        /**
         * 标准模式（默认）
         * 指纹识别 + 核心漏洞 + 配置审计
         * 排除：OSINT, TOKEN_SPRAY, FUZZING, CREDENTIAL_STUFFING
         */
        STANDARD,

        /**
         * 深度模式
         * 全量扫描，包括 Fuzzing 和 Headless
         * 仅排除需要外部输入的（TOKEN_SPRAY, CREDENTIAL_STUFFING）
         */
        DEEP,

        /**
         * OSINT 模式
         * 信息收集，适合渗透测试前期
         * POC类别：OSINT, TECHNOLOGIES, EXPOSED_PANELS
         */
        OSINT,

        /**
         * 合规检查模式
         * SSL配置 + DNS配置 + 信息泄露
         * POC类别：SSL, DNS_CONFIG, MISCONFIGURATION, EXPOSURES
         */
        COMPLIANCE,

        /**
         * 自定义模式
         * 用户手动选择 POC 分类，不自动应用任何规则
         */
        CUSTOM;

        /**
         * 获取模式的中文描述
         */
        public String getDescription() {
            switch (this) {
                case DISCOVERY: return "资产识别";
                case QUICK: return "快速扫描";
                case STANDARD: return "标准扫描";
                case DEEP: return "深度扫描";
                case OSINT: return "信息收集";
                case COMPLIANCE: return "合规检查";
                case CUSTOM: return "自定义";
                default: return name();
            }
        }

        /**
         * 判断该模式是否需要指纹识别
         */
        public boolean requiresFingerprint() {
            switch (this) {
                case DISCOVERY:
                case QUICK:
                case STANDARD:
                case DEEP:
                    return true;
                case OSINT:
                case COMPLIANCE:
                case CUSTOM:
                    return false;
                default:
                    return false;
            }
        }
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public PocObj.Severity getSeverity() {
        return severity;
    }

    public void setSeverity(PocObj.Severity severity) {
        this.severity = severity;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public int getMaxResponseSize() {
        return maxResponseSize;
    }

    public void setMaxResponseSize(int maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
    }
    
    // ========== 新增字段的 Getter/Setter ==========
    
    public InputType getInputType() {
        return inputType;
    }
    
    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }
    
    public boolean isAutoDetectInputType() {
        return autoDetectInputType;
    }
    
    public void setAutoDetectInputType(boolean autoDetectInputType) {
        this.autoDetectInputType = autoDetectInputType;
    }
    
    public ScanMode getScanMode() {
        return scanMode;
    }
    
    public void setScanMode(ScanMode scanMode) {
        this.scanMode = scanMode;
    }
    
    public Set<String> getEnabledPocFormats() {
        return enabledPocFormats;
    }
    
    public void setEnabledPocFormats(Set<String> enabledPocFormats) {
        this.enabledPocFormats = enabledPocFormats;
    }
    
    public Set<PocCategory> getEnabledCategories() {
        return enabledCategories;
    }
    
    public void setEnabledCategories(Set<PocCategory> enabledCategories) {
        this.enabledCategories = enabledCategories;
    }
    
    public Set<PocCategory> getExcludedCategories() {
        return excludedCategories;
    }
    
    public void setExcludedCategories(Set<PocCategory> excludedCategories) {
        this.excludedCategories = excludedCategories;
    }
    
    public boolean isSkipFingerprint() {
        return skipFingerprint;
    }
    
    public void setSkipFingerprint(boolean skipFingerprint) {
        this.skipFingerprint = skipFingerprint;
    }
    
    public int getFingerprintTimeout() {
        return fingerprintTimeout;
    }
    
    public void setFingerprintTimeout(int fingerprintTimeout) {
        this.fingerprintTimeout = fingerprintTimeout;
    }

    public boolean isEnableHoneypotDetection() {
        return enableHoneypotDetection;
    }

    public void setEnableHoneypotDetection(boolean enableHoneypotDetection) {
        this.enableHoneypotDetection = enableHoneypotDetection;
    }

    public boolean isStopOnHoneypot() {
        return stopOnHoneypot;
    }

    public void setStopOnHoneypot(boolean stopOnHoneypot) {
        this.stopOnHoneypot = stopOnHoneypot;
    }

    public boolean isEnableConfigAudit() {
        return enableConfigAudit;
    }

    public void setEnableConfigAudit(boolean enableConfigAudit) {
        this.enableConfigAudit = enableConfigAudit;
    }

    public PocObj.Severity getMinSeverity() {
        return minSeverity;
    }

    public void setMinSeverity(PocObj.Severity minSeverity) {
        this.minSeverity = minSeverity;
    }

    public boolean isEnableHeadless() {
        return enableHeadless;
    }
    
    public void setEnableHeadless(boolean enableHeadless) {
        this.enableHeadless = enableHeadless;
    }
    
    public boolean isEnableCode() {
        return enableCode;
    }
    
    public void setEnableCode(boolean enableCode) {
        this.enableCode = enableCode;
    }
    
    public boolean isEnableFuzz() {
        return enableFuzz;
    }
    
    public void setEnableFuzz(boolean enableFuzz) {
        this.enableFuzz = enableFuzz;
    }
    
    public boolean isEnableDeduplication() {
        return enableDeduplication;
    }
    
    public void setEnableDeduplication(boolean enableDeduplication) {
        this.enableDeduplication = enableDeduplication;
    }
    
    public boolean isEnableResponseCache() {
        return enableResponseCache;
    }
    
    public void setEnableResponseCache(boolean enableResponseCache) {
        this.enableResponseCache = enableResponseCache;
    }
    
    public long getResponseCacheTtlMs() {
        return responseCacheTtlMs;
    }
    
    public void setResponseCacheTtlMs(long responseCacheTtlMs) {
        this.responseCacheTtlMs = responseCacheTtlMs;
    }
    
    public boolean isEnableClustering() {
        return enableClustering;
    }
    
    public void setEnableClustering(boolean enableClustering) {
        this.enableClustering = enableClustering;
    }
    
    public String getLocalTargetPath() {
        return localTargetPath;
    }
    
    public void setLocalTargetPath(String localTargetPath) {
        this.localTargetPath = localTargetPath;
    }

    public String getTargetProtocol() {
        return targetProtocol;
    }

    public void setTargetProtocol(String targetProtocol) {
        this.targetProtocol = targetProtocol;
    }

    public List<String> getFileExtensions() {
        return fileExtensions;
    }
    
    public void setFileExtensions(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 启用指定的 POC 格式
     */
    public void enablePocFormat(String format) {
        enabledPocFormats.add(format.toLowerCase());
    }
    
    /**
     * 禁用指定的 POC 格式
     */
    public void disablePocFormat(String format) {
        enabledPocFormats.remove(format.toLowerCase());
    }
    
    /**
     * 启用指定的分类
     */
    public void enableCategory(PocCategory category) {
        enabledCategories.add(category);
        excludedCategories.remove(category);
    }
    
    /**
     * 排除指定的分类
     */
    public void excludeCategory(PocCategory category) {
        excludedCategories.add(category);
        enabledCategories.remove(category);
    }
    
    /**
     * 检查是否为智能模式（需要指纹识别的模式）
     * DISCOVERY、QUICK、STANDARD、DEEP 模式都需要指纹识别
     */
    public boolean isSmartMode() {
        return scanMode.requiresFingerprint() && !skipFingerprint;
    }

    /**
     * 检查是否为仅指纹识别模式
     */
    public boolean isDiscoveryMode() {
        return scanMode == ScanMode.DISCOVERY;
    }

    /**
     * 检查是否为快速扫描模式
     */
    public boolean isQuickMode() {
        return scanMode == ScanMode.QUICK;
    }

    /**
     * 检查是否为深度扫描模式
     */
    public boolean isDeepMode() {
        return scanMode == ScanMode.DEEP;
    }

    /**
     * 创建默认 URL 扫描配置
     */
    public static ScanConfig createDefaultUrlConfig() {
        ScanConfig config = new ScanConfig();
        config.setInputType(InputType.URL);
        config.setScanMode(ScanMode.STANDARD);
        return config;
    }

    /**
     * 创建默认 IP:端口 扫描配置
     */
    public static ScanConfig createDefaultIpPortConfig() {
        ScanConfig config = new ScanConfig();
        config.setInputType(InputType.IP_PORT);
        config.setScanMode(ScanMode.STANDARD);
        // IP:端口扫描不需要 HTTP 特有的分类
        config.getEnabledCategories().add(PocCategory.NETWORK_CVE);
        config.getEnabledCategories().add(PocCategory.NETWORK_MISCONFIG);
        config.getEnabledCategories().add(PocCategory.NETWORK_DEFAULT_LOGIN);
        return config;
    }

    /**
     * 创建默认本地文件扫描配置
     */
    public static ScanConfig createDefaultLocalFileConfig() {
        ScanConfig config = new ScanConfig();
        config.setInputType(InputType.LOCAL_PATH);
        config.setSkipFingerprint(true); // 本地扫描不需要指纹识别
        config.setScanMode(ScanMode.CUSTOM);
        // 本地扫描只需要 File 类别
        config.getEnabledCategories().add(PocCategory.FILE_KEYS);
        config.getEnabledCategories().add(PocCategory.FILE_MALWARE);
        config.getEnabledCategories().add(PocCategory.FILE_WEBSHELL);
        config.getEnabledCategories().add(PocCategory.FILE_AUDIT);
        return config;
    }
}