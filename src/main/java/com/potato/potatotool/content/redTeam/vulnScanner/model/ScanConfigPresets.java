package com.potato.potatotool.content.redTeam.vulnScanner.model;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.PocCategory;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig.ScanMode;

/**
 * 扫描配置预设工厂类
 * 提供针对不同用户场景的预设配置，简化用户操作
 *
 * <p>支持的场景：
 * <ul>
 *   <li>渗透测试 - 全量扫描，适合专业渗透测试人员</li>
 *   <li>安全运维 - 配置审计 + 高危漏洞，适合日常安全运维</li>
 *   <li>合规检查 - SSL/DNS 配置 + 信息泄露检测</li>
 *   <li>资产识别 - 快速资产发现和技术栈识别</li>
 *   <li>代码审计 - 本地代码安全扫描</li>
 * </ul>
 */
public class ScanConfigPresets {

    /**
     * 渗透测试配置
     * 全量扫描，包括 OSINT 和 Fuzzing
     *
     * <p>特点：
     * <ul>
     *   <li>深度扫描模式</li>
     *   <li>启用 Headless 浏览器测试</li>
     *   <li>启用模糊测试</li>
     *   <li>仅排除需要外部凭证的 POC</li>
     * </ul>
     *
     * @return 渗透测试扫描配置
     */
    public static ScanConfig createPentestConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.DEEP);
        config.setEnableHeadless(true);
        config.setEnableFuzz(true);
        config.setEnableCode(true);
        config.setEnableHoneypotDetection(true);
        config.setStopOnHoneypot(false); // 渗透测试不因蜜罐停止

        // 清除默认排除，添加最小排除集
        config.getExcludedCategories().clear();
        config.excludeCategory(PocCategory.TOKEN_SPRAY);
        config.excludeCategory(PocCategory.CREDENTIAL_STUFFING);

        // 包含 OSINT
        config.enableCategory(PocCategory.OSINT);

        return config;
    }

    /**
     * 安全运维配置
     * 配置审计 + 高危漏洞，适合日常安全运维
     *
     * <p>特点：
     * <ul>
     *   <li>标准扫描模式</li>
     *   <li>排除 OSINT、Fuzzing 等不适合运维的分类</li>
     *   <li>聚焦于配置错误和已知漏洞检测</li>
     * </ul>
     *
     * @return 安全运维扫描配置
     */
    public static ScanConfig createSecOpsConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.STANDARD);
        config.setEnableHoneypotDetection(true);
        config.setStopOnHoneypot(true); // 运维场景检测到蜜罐应停止
        config.setEnableConfigAudit(true);

        // 排除不适合运维场景的分类
        config.excludeCategory(PocCategory.OSINT);
        config.excludeCategory(PocCategory.FUZZING);
        config.excludeCategory(PocCategory.TOKEN_SPRAY);
        config.excludeCategory(PocCategory.CREDENTIAL_STUFFING);
        config.excludeCategory(PocCategory.TAKEOVERS);
        config.excludeCategory(PocCategory.HEADLESS);
        config.excludeCategory(PocCategory.CODE);

        return config;
    }

    /**
     * 合规检查配置
     * SSL配置 + DNS配置 + 信息泄露
     *
     * <p>特点：
     * <ul>
     *   <li>合规检查模式</li>
     *   <li>只启用配置审计相关分类</li>
     *   <li>不检测漏洞，只检测配置问题</li>
     * </ul>
     *
     * @return 合规检查扫描配置
     */
    public static ScanConfig createComplianceConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.COMPLIANCE);
        config.setSkipFingerprint(true); // 合规检查不需要指纹
        config.setEnableHoneypotDetection(false);
        config.setEnableConfigAudit(true);

        // 只启用特定分类
        config.getEnabledCategories().add(PocCategory.SSL);
        config.getEnabledCategories().add(PocCategory.DNS_CONFIG);
        config.getEnabledCategories().add(PocCategory.MISCONFIGURATION);
        config.getEnabledCategories().add(PocCategory.EXPOSURES);

        return config;
    }

    /**
     * 快速资产识别配置
     * 仅进行指纹识别，获取技术栈信息
     *
     * <p>特点：
     * <ul>
     *   <li>资产识别模式</li>
     *   <li>只启用指纹识别类 POC</li>
     *   <li>快速扫描，适合大规模资产发现</li>
     * </ul>
     *
     * @return 资产识别扫描配置
     */
    public static ScanConfig createDiscoveryConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.DISCOVERY);
        config.setEnableHoneypotDetection(true);
        config.setStopOnHoneypot(true); // 资产识别时检测到蜜罐应标记
        config.setEnableConfigAudit(false);

        // 只启用指纹识别类
        config.getEnabledCategories().add(PocCategory.TECHNOLOGIES);
        config.getEnabledCategories().add(PocCategory.EXPOSED_PANELS);
        config.getEnabledCategories().add(PocCategory.HONEYPOT);
        config.getEnabledCategories().add(PocCategory.NETWORK_DETECTION);

        return config;
    }

    /**
     * 代码审计配置（本地文件扫描）
     * 扫描本地代码中的安全问题
     *
     * <p>特点：
     * <ul>
     *   <li>自定义模式</li>
     *   <li>跳过指纹识别（本地文件不需要）</li>
     *   <li>只启用 File 类别 POC</li>
     * </ul>
     *
     * @param localPath 本地代码路径
     * @return 代码审计扫描配置
     */
    public static ScanConfig createCodeAuditConfig(String localPath) {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.CUSTOM);
        config.setInputType(InputType.LOCAL_PATH);
        config.setLocalTargetPath(localPath);
        config.setSkipFingerprint(true);
        config.setEnableHoneypotDetection(false);
        config.setEnableConfigAudit(false);
        config.setEnableCode(true);

        // 只启用 File 类别
        config.getEnabledCategories().add(PocCategory.FILE_KEYS);
        config.getEnabledCategories().add(PocCategory.FILE_MALWARE);
        config.getEnabledCategories().add(PocCategory.FILE_WEBSHELL);
        config.getEnabledCategories().add(PocCategory.FILE_AUDIT);

        return config;
    }

    /**
     * 快速漏洞扫描配置
     * 只扫描高危/严重漏洞
     *
     * <p>特点：
     * <ul>
     *   <li>快速模式</li>
     *   <li>只检测 HIGH 和 CRITICAL 级别漏洞</li>
     *   <li>适合快速评估目标安全状态</li>
     * </ul>
     *
     * @return 快速漏洞扫描配置
     */
    public static ScanConfig createQuickScanConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.QUICK);
        config.setMinSeverity(PocObj.Severity.HIGH); // 只扫描高危及以上
        config.setEnableHoneypotDetection(true);
        config.setStopOnHoneypot(false);
        config.setEnableConfigAudit(false); // 快速扫描不做配置审计

        // 排除时间消耗较大的分类
        config.excludeCategory(PocCategory.OSINT);
        config.excludeCategory(PocCategory.FUZZING);
        config.excludeCategory(PocCategory.TOKEN_SPRAY);
        config.excludeCategory(PocCategory.CREDENTIAL_STUFFING);
        config.excludeCategory(PocCategory.HEADLESS);
        config.excludeCategory(PocCategory.CODE);

        return config;
    }

    /**
     * 网络服务扫描配置
     * 针对 IP:端口 目标的网络服务扫描
     *
     * <p>特点：
     * <ul>
     *   <li>标准模式</li>
     *   <li>只启用 Network 类别 POC</li>
     *   <li>适合扫描非 HTTP 服务</li>
     * </ul>
     *
     * @return 网络服务扫描配置
     */
    public static ScanConfig createNetworkScanConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.STANDARD);
        config.setInputType(InputType.IP_PORT);
        config.setEnableHoneypotDetection(true);

        // 只启用 Network 类别
        config.getEnabledCategories().add(PocCategory.NETWORK_CVE);
        config.getEnabledCategories().add(PocCategory.NETWORK_MISCONFIG);
        config.getEnabledCategories().add(PocCategory.NETWORK_DEFAULT_LOGIN);
        config.getEnabledCategories().add(PocCategory.NETWORK_DETECTION);

        return config;
    }

    /**
     * DNS 安全检测配置
     * 针对域名的 DNS 安全检测
     *
     * <p>特点：
     * <ul>
     *   <li>合规检查模式</li>
     *   <li>只启用 DNS 类别 POC</li>
     *   <li>适合检测 DNS 配置问题和子域名接管风险</li>
     * </ul>
     *
     * @return DNS 安全检测配置
     */
    public static ScanConfig createDnsScanConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.COMPLIANCE);
        config.setInputType(InputType.DOMAIN);
        config.setSkipFingerprint(true);
        config.setEnableHoneypotDetection(false);

        // 只启用 DNS 类别
        config.getEnabledCategories().add(PocCategory.DNS_TAKEOVER);
        config.getEnabledCategories().add(PocCategory.DNS_CONFIG);

        return config;
    }

    /**
     * 根据扫描模式创建默认配置
     *
     * @param mode 扫描模式
     * @return 对应模式的默认配置
     */
    public static ScanConfig createConfigForMode(ScanMode mode) {
        switch (mode) {
            case DISCOVERY:
                return createDiscoveryConfig();
            case QUICK:
                return createQuickScanConfig();
            case STANDARD:
                return new ScanConfig(); // 标准模式使用默认配置
            case DEEP:
                return createPentestConfig();
            case OSINT:
                return createOsintConfig();
            case COMPLIANCE:
                return createComplianceConfig();
            case CUSTOM:
            default:
                ScanConfig config = new ScanConfig();
                config.setScanMode(ScanMode.CUSTOM);
                return config;
        }
    }

    /**
     * OSINT 信息收集配置
     *
     * @return OSINT 扫描配置
     */
    public static ScanConfig createOsintConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.OSINT);
        config.setSkipFingerprint(true); // OSINT 不需要指纹识别
        config.setEnableHoneypotDetection(false);
        config.setEnableConfigAudit(false);

        // 启用 OSINT 相关分类
        config.enableCategory(PocCategory.OSINT);
        config.enableCategory(PocCategory.TECHNOLOGIES);
        config.enableCategory(PocCategory.EXPOSED_PANELS);
        config.enableCategory(PocCategory.EXPOSURES);

        return config;
    }

    /**
     * 获取所有预设场景的描述信息
     *
     * @return 预设场景描述数组
     */
    public static PresetInfo[] getAllPresets() {
        return new PresetInfo[] {
            new PresetInfo("pentest", "渗透测试", "全量扫描，包括 OSINT 和 Fuzzing，适合专业渗透测试"),
            new PresetInfo("secops", "安全运维", "配置审计 + 高危漏洞，适合日常安全运维"),
            new PresetInfo("compliance", "合规检查", "SSL/DNS 配置 + 信息泄露检测"),
            new PresetInfo("discovery", "资产识别", "快速资产发现和技术栈识别"),
            new PresetInfo("quick", "快速扫描", "只扫描高危/严重漏洞，快速评估"),
            new PresetInfo("network", "网络服务", "针对 IP:端口 的网络服务扫描"),
            new PresetInfo("dns", "DNS 检测", "DNS 配置问题和子域名接管风险检测"),
            new PresetInfo("code", "代码审计", "本地代码安全扫描"),
            new PresetInfo("osint", "信息收集", "OSINT 信息收集，适合渗透测试前期")
        };
    }

    /**
     * 预设信息类
     */
    public static class PresetInfo {
        private final String id;
        private final String name;
        private final String description;

        public PresetInfo(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
