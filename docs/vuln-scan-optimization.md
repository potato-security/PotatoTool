# PotatoTool 漏洞扫描优化方案

## 一、当前问题分析

### 1.1 输入类型与 POC 分类不匹配

**现状**：当前只简单排除 `OSINT`、`TOKEN_SPRAY`、`CREDENTIAL_STUFFING`，没有根据输入类型智能选择 POC。

**问题**：
- 对 URL 目标扫描时，仍会加载 `network/`、`file/` 协议的 POC（即使无法执行）
- 对 IP:Port 目标扫描时，HTTP 类 POC 无法正确执行
- 导致大量无效 POC 被加载和尝试执行

**当前 `selectPocsForScan()` 逻辑**（PaneVulScan.java:973）：
```java
for (PocObj.Poc poc : allPocs) {
    // 只做格式筛选和特殊分类排除
    if (!enabledFormats.contains(poc.getOriginalFormat())) continue;
    if (config.getExcludedCategories().contains(category)) continue;
    // 缺少：输入类型与 POC 协议的匹配检查
    result.add(poc);
}
```

### 1.2 缺少扫描阶段划分

**现状**：虽然有 `SMART` 模式，但没有真正实现分阶段扫描。

**问题**：
- 没有先进行蜜罐检测（避免浪费时间在蜜罐上）
- 没有先进行指纹识别再精准打击
- 所有 POC 并发执行，无法根据前序结果调整后续策略

**参考 Nuclei 的智能模式**：
```
阶段1: 蜜罐检测 → 如果是蜜罐，提前告警并可选停止
阶段2: 指纹识别 → 获取技术栈（WordPress? Tomcat?）
阶段3: 精准扫描 → 根据技术栈只运行相关 CVE
```

### 1.3 POC 分类体系不够细化

**现状 PocCategory**：
```java
// 当前分类是扁平的，缺少层级关系
CVES, CNVD, VULNERABILITIES, DEFAULT_LOGINS, ...
```

**问题**：
- 无法区分"漏洞检测类"和"信息收集类"
- 无法按"协议层级"筛选（HTTP vs Network vs DNS）
- 难以实现"只扫描高危漏洞"等需求

### 1.4 缺少用户场景预设

**问题**：不同用户角色有截然不同的需求：

| 用户角色 | 主要需求 | 应排除的 POC |
|---------|---------|-------------|
| 渗透测试 | 全量漏洞检测，包括 OSINT | 无 |
| 安全运维 | 配置审计 + 高危漏洞 | OSINT, FUZZING, TOKEN_SPRAY |
| 合规检查 | SSL/DNS 配置 + 信息泄露 | 漏洞利用类 |
| 快速识别 | 技术栈指纹 + 暴露面板 | 所有漏洞类 |
| 代码审计 | 本地文件扫描 | 所有网络 POC |

---

## 二、业界方案调研

### 2.1 Nessus 策略体系

[Nessus](https://www.tenable.com/products/nessus) 提供多种扫描策略模板：
- **Basic Network Scan**：通用网络漏洞扫描
- **Web Application Tests**：Web 应用专项
- **Malware Scan**：恶意软件检测
- **Policy Compliance**：合规性检查

**核心特点**：
- 按"Plugin Family"（插件家族）分类筛选
- 支持凭证/无凭证两种模式
- 可按严重度、CVE 年份过滤

### 2.2 OpenVAS 扫描配置

[OpenVAS](https://www.openvas.org/) 提供预设扫描配置：
- **Full and Fast**：全量快速扫描
- **Full and Deep**：全量深度扫描（包括危险检测）
- **Discovery**：仅服务发现

**核心特点**：
- NVT Families（网络漏洞测试家族）分类
- 可配置单个 NVT 的启用/禁用
- 支持增量扫描

### 2.3 Nuclei 智能扫描

[Nuclei](https://github.com/projectdiscovery/nuclei) 的 `-as` 自动扫描模式：

```bash
# 智能模式：先指纹识别，再根据技术栈精准扫描
nuclei -u https://example.com -as
```

**核心特点**：
1. **Wappalyzer 指纹识别**：自动检测技术栈
2. **Tag 关联映射**：根据识别出的技术选择相关模板
3. **Template Clustering**：相同请求的模板合并，减少网络流量
4. **Workflow 条件执行**：先指纹识别，根据结果决定是否运行后续模板

---

## 三、优化方案设计

### 3.1 输入类型智能匹配

**方案**：在 POC 筛选时增加输入类型与协议的匹配检查。

```java
// 新增方法：检查 POC 是否适用于当前输入类型
private boolean isPocApplicableForInputType(PocObj.Poc poc, InputType inputType) {
    String protocol = poc.getProtocol();
    PocCategory category = poc.getCategory();

    switch (inputType) {
        case URL:
            // URL 输入只运行 HTTP 类 POC
            return isHttpCategory(category) || category == PocCategory.SSL;

        case IP_PORT:
            // IP:Port 输入只运行 Network 类 POC
            return isNetworkCategory(category);

        case DOMAIN:
            // 域名输入只运行 DNS 类 POC
            return isDnsCategory(category);

        case LOCAL_PATH:
        case LOCAL_FILE:
            // 本地路径只运行 File 类 POC
            return isFileCategory(category);

        case ANY:
        default:
            return true;
    }
}
```

**分类辅助方法**：
```java
private boolean isHttpCategory(PocCategory cat) {
    return cat == PocCategory.CVES || cat == PocCategory.CNVD
        || cat == PocCategory.VULNERABILITIES || cat == PocCategory.DEFAULT_LOGINS
        || cat == PocCategory.IOT || cat == PocCategory.MISCONFIGURATION
        || cat == PocCategory.EXPOSURES || cat == PocCategory.EXPOSED_PANELS
        || cat == PocCategory.TECHNOLOGIES || cat == PocCategory.HONEYPOT
        || cat == PocCategory.OSINT || cat == PocCategory.TOKEN_SPRAY
        || cat == PocCategory.FUZZING || cat == PocCategory.CREDENTIAL_STUFFING
        || cat == PocCategory.TAKEOVERS || cat == PocCategory.MISCELLANEOUS;
}
```

### 3.2 扫描模式重新设计

**方案**：重新设计 ScanMode 枚举，提供更细粒度的控制。

```java
public enum ScanMode {
    /**
     * 资产识别模式
     * 只运行指纹识别，获取技术栈信息
     * POC类别：TECHNOLOGIES, EXPOSED_PANELS, HONEYPOT
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
     * 用户手动选择 POC 分类
     */
    CUSTOM
}
```

### 3.3 POC 分类体系优化

**方案**：引入三级分类体系。

```java
/**
 * POC 协议层级（一级分类）
 */
public enum PocProtocolLayer {
    HTTP,       // Web 应用协议
    NETWORK,    // 网络层协议（TCP/UDP）
    DNS,        // DNS 协议
    FILE,       // 本地文件
    SSL,        // SSL/TLS
    HEADLESS,   // 无头浏览器
    CODE,       // 代码执行
    JAVASCRIPT  // JS 运行时
}

/**
 * POC 功能类型（二级分类）
 */
public enum PocFunctionType {
    VULNERABILITY,  // 漏洞检测
    FINGERPRINT,    // 指纹识别
    CONFIG_AUDIT,   // 配置审计
    INFO_GATHERING, // 信息收集
    CREDENTIAL,     // 凭证相关
    SPECIAL         // 特殊用途
}

/**
 * POC 详细分类（三级分类）- 保持现有 PocCategory
 */
public enum PocCategory {
    // ... 现有分类
}
```

**分类映射表**：
```java
// 建立映射关系
Map<PocCategory, PocProtocolLayer> protocolMapping = Map.of(
    PocCategory.CVES, PocProtocolLayer.HTTP,
    PocCategory.NETWORK_CVE, PocProtocolLayer.NETWORK,
    PocCategory.DNS_TAKEOVER, PocProtocolLayer.DNS,
    // ...
);

Map<PocCategory, PocFunctionType> functionMapping = Map.of(
    PocCategory.CVES, PocFunctionType.VULNERABILITY,
    PocCategory.TECHNOLOGIES, PocFunctionType.FINGERPRINT,
    PocCategory.MISCONFIGURATION, PocFunctionType.CONFIG_AUDIT,
    PocCategory.OSINT, PocFunctionType.INFO_GATHERING,
    // ...
);
```

### 3.4 分阶段扫描执行

**方案**：实现真正的分阶段扫描引擎。

```java
public class PhasedScanEngine {

    /**
     * 执行分阶段扫描
     */
    public void executePhasedScan(List<String> targets, ScanConfig config) {
        // 阶段 1: 蜜罐检测（可选，默认开启）
        if (config.isEnableHoneypotDetection()) {
            HoneypotResult honeypotResult = runHoneypotDetection(targets);
            if (honeypotResult.isHoneypot()) {
                eventDispatcher.dispatch(new HoneypotDetectedEvent(honeypotResult));
                if (config.isStopOnHoneypot()) {
                    return;
                }
            }
        }

        // 阶段 2: 指纹识别（智能模式）
        FingerprintResult fingerprint = null;
        if (config.getScanMode() != ScanMode.DISCOVERY && !config.isSkipFingerprint()) {
            fingerprint = runFingerprintScan(targets);
            eventDispatcher.dispatch(new FingerprintCompletedEvent(fingerprint));
        }

        // 阶段 3: 核心漏洞扫描（根据指纹结果筛选 POC）
        List<PocObj.Poc> selectedPocs = selectPocsByFingerprint(fingerprint, config);
        runVulnerabilityScan(targets, selectedPocs);

        // 阶段 4: 配置审计（可选）
        if (config.isEnableConfigAudit()) {
            runConfigAuditScan(targets);
        }
    }

    /**
     * 根据指纹结果筛选 POC
     */
    private List<PocObj.Poc> selectPocsByFingerprint(FingerprintResult fp, ScanConfig config) {
        if (fp == null || fp.getTechnologies().isEmpty()) {
            // 无指纹结果，返回所有符合条件的 POC
            return filterPocsByConfig(getAllPocs(), config);
        }

        Set<String> techTags = fp.getTechnologies().stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        return getAllPocs().stream()
            .filter(poc -> {
                // 基础筛选
                if (!passBasicFilter(poc, config)) return false;

                // 指纹匹配筛选
                Set<String> pocTags = poc.getNormalizedTags();
                return pocTags.isEmpty() || // 无标签的 POC 不限制
                       pocTags.stream().anyMatch(techTags::contains);
            })
            .collect(Collectors.toList());
    }
}
```

### 3.5 用户场景预设配置

**方案**：提供场景预设工厂方法。

```java
public class ScanConfigPresets {

    /**
     * 渗透测试配置
     * 全量扫描，包括 OSINT 和 Fuzzing
     */
    public static ScanConfig createPentestConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.DEEP);
        config.setEnableHeadless(true);
        config.setEnableFuzz(true);
        config.getExcludedCategories().clear(); // 不排除任何分类
        // 仍排除需要外部凭证的
        config.excludeCategory(PocCategory.TOKEN_SPRAY);
        config.excludeCategory(PocCategory.CREDENTIAL_STUFFING);
        return config;
    }

    /**
     * 安全运维配置
     * 配置审计 + 高危漏洞
     */
    public static ScanConfig createSecOpsConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.STANDARD);
        // 排除不适合运维场景的
        config.excludeCategory(PocCategory.OSINT);
        config.excludeCategory(PocCategory.FUZZING);
        config.excludeCategory(PocCategory.TOKEN_SPRAY);
        config.excludeCategory(PocCategory.CREDENTIAL_STUFFING);
        config.excludeCategory(PocCategory.TAKEOVERS);
        return config;
    }

    /**
     * 合规检查配置
     */
    public static ScanConfig createComplianceConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.COMPLIANCE);
        // 只启用特定分类
        config.getEnabledCategories().add(PocCategory.SSL);
        config.getEnabledCategories().add(PocCategory.DNS_CONFIG);
        config.getEnabledCategories().add(PocCategory.MISCONFIGURATION);
        config.getEnabledCategories().add(PocCategory.EXPOSURES);
        return config;
    }

    /**
     * 快速资产识别配置
     */
    public static ScanConfig createDiscoveryConfig() {
        ScanConfig config = new ScanConfig();
        config.setScanMode(ScanMode.DISCOVERY);
        config.setSkipFingerprint(false);
        // 只启用指纹识别类
        config.getEnabledCategories().add(PocCategory.TECHNOLOGIES);
        config.getEnabledCategories().add(PocCategory.EXPOSED_PANELS);
        config.getEnabledCategories().add(PocCategory.HONEYPOT);
        return config;
    }

    /**
     * 代码审计配置（本地文件扫描）
     */
    public static ScanConfig createCodeAuditConfig() {
        ScanConfig config = new ScanConfig();
        config.setInputType(InputType.LOCAL_PATH);
        config.setSkipFingerprint(true);
        config.getEnabledCategories().add(PocCategory.FILE_KEYS);
        config.getEnabledCategories().add(PocCategory.FILE_MALWARE);
        config.getEnabledCategories().add(PocCategory.FILE_WEBSHELL);
        config.getEnabledCategories().add(PocCategory.FILE_AUDIT);
        return config;
    }
}
```

### 3.6 UI 优化建议

**方案**：在 UI 层面提供更直观的配置方式。

```
┌─────────────────────────────────────────────────────────────┐
│ 扫描模式                                                      │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│ │ 🚀 快速  │ │ 📋 标准  │ │ 🔬 深度  │ │ 🔍 OSINT │ │ ⚙️ 自定义│  │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
├─────────────────────────────────────────────────────────────┤
│ 输入类型（自动检测）                                           │
│ ○ URL (https://example.com)                                 │
│ ○ IP:端口 (192.168.1.1:22)                                  │
│ ○ 域名 (example.com)                                        │
│ ○ 本地路径 (/path/to/code)                                  │
├─────────────────────────────────────────────────────────────┤
│ POC 来源                                                     │
│ ☑ Nuclei  ☑ Goby  ☑ Xray  ☑ Pocsuite                        │
├─────────────────────────────────────────────────────────────┤
│ 高级选项                                                     │
│ ☐ 启用蜜罐检测        ☐ 启用指纹识别优化                       │
│ ☐ 启用 Headless      ☐ 启用 Fuzzing                          │
│ ☐ 包含 OSINT POC     ☐ 包含 Token-Spray POC                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 四、实施计划

### 第一阶段：输入类型匹配（优先级：高）

1. 修改 `selectPocsForScan()` 方法，增加输入类型与协议的匹配检查
2. 在 `PocClassifier` 中添加协议层级判断方法
3. 测试不同输入类型的 POC 筛选效果

### 第二阶段：扫描模式重构（优先级：高）

1. 重新设计 `ScanMode` 枚举
2. 为每种模式定义默认的 POC 分类包含/排除规则
3. 更新 UI 的模式选择逻辑

### 第三阶段：分阶段扫描（优先级：中）

1. 实现 `PhasedScanEngine` 分阶段扫描引擎
2. 添加蜜罐检测阶段
3. 实现基于指纹结果的 POC 动态筛选

### 第四阶段：用户场景预设（优先级：中）

1. 实现 `ScanConfigPresets` 预设工厂
2. 在 UI 添加场景选择入口
3. 支持用户自定义场景保存

### 第五阶段：增量扫描支持（优先级：低）

1. 记录已扫描的 POC ID 和时间
2. 支持"只扫描新增 POC"选项
3. 支持"只扫描指定年份 CVE"选项

---

## 五、总结

### 核心优化点

| 优化项 | 当前状态 | 优化后 | 影响 |
|-------|---------|--------|-----|
| 输入类型匹配 | 不匹配 | 智能匹配 | 减少 60%+ 无效 POC |
| 扫描模式 | 2种 | 7种 | 覆盖更多场景 |
| 分阶段扫描 | 无 | 4阶段 | 更智能精准 |
| 用户预设 | 无 | 5种 | 开箱即用 |
| POC 分类 | 1级 | 3级 | 更细粒度控制 |

### 预期效果

1. **减少无效扫描**：通过输入类型匹配，减少 60% 以上的无效 POC 加载
2. **提升扫描效率**：通过分阶段扫描和指纹优化，减少 40% 的网络请求
3. **满足多场景需求**：通过模式预设，支持渗透测试/安全运维/合规检查等多种场景
4. **降低学习成本**：通过直观的 UI 和预设配置，新用户可快速上手

---

## 参考资料

- [Nuclei Ultimate Guide](https://blog.projectdiscovery.io/ultimate-nuclei-guide/)
- [Nessus Vulnerability Scanner](https://www.tenable.com/products/nessus)
- [OpenVAS Documentation](https://www.openvas.org/)
- [Nuclei GitHub](https://github.com/projectdiscovery/nuclei)
- [15 Open Source Vulnerability Scanners for 2025](https://www.techbloat.com/15-open-source-vulnerability-scanners-for-2025.html)
