package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Potato
 * @date 2025/2/19 16:30
 * 通用POC对象，用于统一不同格式的POC
 */
public class PocObj {

    @Data
    public static class Poc {
        // 基本信息
        private String id;                  // POC唯一标识
        private String name;                // POC名称
        private String author;              // 作者
        private Severity severity;          // 严重程度
        private String description;         // 描述
        private List<String> references;    // 参考链接
        private String product;             // 影响产品
        private String version;             // 影响版本
        private List<String> tags;          // 标签
        private String createTime;          // 创建时间
        private String updateTime;          // 更新时间
        private String protocol;            // 协议类型(http, tcp, udp等)
        private String appPowerLink;        // 应用官网链接
        private String pocDesc;             // POC描述
        private boolean selfContained;      // 是否自包含(Nuclei特性)
        private String flow;                // 流程表达式，如 "http(1) && http(2)" 或 "dns(1) && ssl(1)"
        private String impact;              // 漏洞影响
        private String recommendation;      // 修复建议
        private String homepage;            // 主页

        // 搜索相关
        private Map<String, String> searchQueries = new HashMap<>();  // 搜索语句，如fofa、zoomeye等
        
        // 变量和类型
        // 统一的变量机制，支持多种POC格式：
        // - Nuclei: payloads (多值变量，用于fuzzing)
        // - Goby: GlobalVariables (单值变量，跨步骤共享)
        // - Xray: 其他变量机制
        // 转换时将单值包装为单元素List，保持统一的数据结构
        private Map<String, List<String>> variables = new HashMap<>();
        private VariablesType variablesType = VariablesType.batteringram; // 默认为 batteringram (Nuclei 默认模式)
        private boolean continueOnMatch = false; // Xray continue_ 字段：命中一个 payload 后是否继续（默认 false）

        // 漏洞信息
        private String cveId;               // CVE编号
        private String cweId;               // CWE编号
        private String vulType;             // 漏洞类型
        private String cvssScore;           // CVSS评分
        private String cvssMetrics;         // CVSS向量
        
        // 执行步骤
        private List<PocStep> verifySteps = new ArrayList<>();  // 验证步骤
        private List<PocStep> exploitSteps = new ArrayList<>(); // 利用步骤
        private MatchersCondition stepsCondition = MatchersCondition.AND; // 步骤之间的条件（AND: 所有步骤都要通过，OR: 任一步骤通过即可）
        
        // 全局配置
        private GlobalConfig globalConfig = new GlobalConfig(); // 全局配置
        
        // 原始POC
        private Object originalPoc;         // 原始POC对象
        private String originalFormat;      // 原始POC格式
        
        // ========== 新增：输入类型与分类 ==========
        /**
         * 适用的输入类型（URL/IP:端口/域名/本地路径等）
         */
        private InputType inputType = InputType.URL;
        
        /**
         * POC 分类（CVE/配置错误/信息泄露/指纹识别等）
         */
        private PocCategory category = PocCategory.UNKNOWN;
        
        /**
         * 规范化的产品标签集合（统一转小写、去空格）
         * 用于与指纹识别结果进行匹配
         * 示例: ["wordpress", "apache-tomcat", "spring-boot"]
         */
        private Set<String> normalizedTags = new HashSet<>();
        
        /**
         * 指纹识别结果（扫描时填充，用于报告输出）
         */
        private FingerprintInfo fingerprint;
        
        // ========== 新增：特殊 Flag ==========
        /**
         * 是否需要 -headless flag（Headless 浏览器模板）
         */
        private boolean requiresHeadless = false;
        
        /**
         * 是否需要 -code flag（本地代码执行模板）
         */
        private boolean requiresCode = false;
        
        /**
         * 是否需要 -fuzz flag（模糊测试模板）
         */
        private boolean requiresFuzz = false;
        
        // ========== 新增：File 协议特殊字段 ==========
        /**
         * File 协议的目标类型：DIRECTORY（扫描目录）或 SPECIFIC_FILE（扫描特定文件）
         */
        private FileTargetType fileTargetType = FileTargetType.DIRECTORY;
        
        /**
         * 针对特定文件路径的模板（如扫描 /etc/passwd, ~/.ssh/id_rsa）
         */
        private String specificFilePath;
        
        /**
         * 文件扩展名过滤（仅扫描这些扩展名的文件）
         */
        private List<String> fileExtensions = new ArrayList<>();
    }

    public enum VariablesType {
        batteringram,  // 同步模式：所有变量使用同一个值（同步循环）
        pitchfork,     // 配对模式：多个变量列表索引配对（平行循环）
        clusterbomb    // 笛卡尔积模式：所有变量值的笛卡尔积组合（穷举所有可能）
    }
    
    @Data
    public static class GlobalConfig {
        private int maxRetries;             // 最大重试次数
        private int retryInterval;          // 重试间隔(毫秒)
        private String proxy;               // 全局代理
        private Map<String, String> globalHeaders = new HashMap<>(); // 全局请求头
        private boolean stopAtFirstMatch;   // 首次匹配后停止
        private int threads;                // 线程数
        private boolean cookieReuse;        // 是否复用Cookie
        private Map<String, Object> dnsConfig = new HashMap<>(); // DNS配置
        private Map<String, String> authConfig = new HashMap<>(); // 认证配置
    }
    
    @Data
    public static class PocStep {
        private String stepId;              // 步骤ID
        private String method;              // 请求方法
        private String path;                // 请求路径
        private Map<String, String> headers = new HashMap<>(); // 请求头
        private String body;                // 请求体
        private String dataType;            // 数据类型(json, form, text等)
        private boolean followRedirect;     // 是否跟随重定向
        private String cookie;              // Cookie信息
        private String proxy;               // 代理设置
        private int timeout;                // 超时时间
        private String delay;               // 延迟时间
        private String connectionId;        // 连接ID(用于TCP/UDP)
        private int readSize;               // 读取大小(用于TCP/UDP)
        private List<String> raw = new ArrayList<>(); // 原始请求
        private boolean unsafe;             // 是否不安全请求
        private boolean disableCookie;      // 是否禁用Cookie
        private boolean disablePathAutomerge; // 是否禁用路径自动合并
        private boolean cache;              // 是否缓存请求结果
        private String encoding;            // 请求编码方式
        private boolean compressed;         // 是否压缩
        private String compressionType;     // 压缩类型
        private boolean chunked;            // 是否分块传输
        
        // 认证信息
        private String authType;            // 认证类型(basic, digest, oauth等)
        private String username;            // 用户名
        private String password;            // 密码
        private String token;               // 认证令牌
        
        // 匹配规则
        private List<Matcher> matchers = new ArrayList<>();    // 匹配器
        private MatchersCondition matchersCondition = MatchersCondition.AND; // 匹配条件
        
        // 结果提取
        private List<Matcher> extractors = new ArrayList<>();  // 提取器
        private Map<String, Object> output = new HashMap<>();  // 结果数据
        
        // 重试配置
        private int retries;                // 步骤重试次数
        private int retryInterval;          // 步骤重试间隔
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TcpStep extends PocStep {
        private String host;                // 主机地址
        private String port;                // 端口
        private List<Input> inputs = new ArrayList<>(); // 输入列表
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DnsStep extends PocStep {
        private String domain;              // 域名
        private String type;                // DNS记录类型
        private String resolver;            // DNS解析器
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WebSocketStep extends PocStep {
        private String address;             // WebSocket 地址
        private List<String> messages = new ArrayList<>(); // 要发送的消息列表
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SslStep extends PocStep {
        private String address;             // SSL/TLS 目标地址
        // timeout 字段已在父类 PocStep 中定义，不需要重复声明
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class FileStep extends PocStep {
        private List<String> paths = new ArrayList<>();          // 文件路径列表
        private List<String> extensions = new ArrayList<>();     // 文件扩展名列表
        private boolean recursive = false;   // 是否递归扫描
        private long maxSize = 104857600;    // 最大文件大小（100MB）
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HeadlessStep extends PocStep {
        private String url;                  // 起始 URL
        private List<BrowserAction> actions = new ArrayList<>();  // 浏览器操作列表
    }
    
    @Data
    public static class BrowserAction {
        private String action;               // 操作类型
        private Map<String, String> args;    // 操作参数
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CodeStep extends PocStep {
        private String engine;               // 代码引擎（javascript, python）
        private String source;               // 代码源码
    }
    
    @Data
    public static class Input {
        private String data;                // 输入数据
        private String type;                // 输入类型(hex, text等)
        private String name;                // 输入名称
        private String read;                // 读取规则
        private String encoding;            // 编码方式
    }
    
    @Data
    public static class Matcher {
        private MatcherType type;           // 匹配类型
        private String part;                // 匹配部分
        private List<String> values;        // 匹配值
        private boolean negative;           // 是否为反向匹配
        private String condition;           // 匹配条件
        private String name;                // 匹配器名称
        private int index;                  // 匹配索引
        private boolean caseInsensitive;    // 是否忽略大小写
        private boolean greedy;             // 是否贪婪匹配
        private String internal;            // 内部匹配规则
        private int group;                  // 正则表达式组
        private String attribute;           // XPath属性
        private String encoding;            // 编码方式
        private List<Matcher> subMatchers;  // 子匹配器（用于GROUP类型）
        private OperationType operation;    // 操作类型（新增）
        private String timeUnit;            // 时间单位："s"(秒) 或 "ms"(毫秒)，用于TIME类型匹配器
    }
    
    // 匹配器类型（修改为纯粹的匹配类型）
    public enum MatcherType {
        STATUS,     // 状态码
        SIZE,       // 大小
        WORD,       // 关键词
        REGEX,      // 正则表达式
        BINARY,     // 二进制
        DSL,        // 领域特定语言 (Nuclei DSL)
        CEL,        // Common Expression Language (Xray CEL)
        HASH,       // 哈希
        JSON,       // JSON
        XML,        // XML
        XPATH,      // XPath
        KVAL,       // 键值对
        GROUP,      // 组合匹配（嵌套匹配器）
        TIME,       // 时间
        UNKNOWN     // 未知
    }
    
    // 操作类型（新增）
    public enum OperationType {
        CONTAINS,       // 包含
        NOT_CONTAINS,   // 不包含
        EQUAL,          // 等于（==）
        NOT_EQUAL,      // 不等于（!=）
        GREATER,        // 大于（>）
        LESS,           // 小于（<）
        GREATER_EQUAL,  // 大于等于（>=）
        LESS_EQUAL,     // 小于等于（<=）
        PREFIX,         // 前缀匹配（Start With）
        SUFFIX,         // 后缀匹配（End With）
        REGEX_MATCH,    // 正则匹配
        DIFF,           // 差异对比（Phase 3新增，用于布尔盲注）
        DEFAULT         // 默认操作
    }
    
    // 匹配条件
    public enum MatchersCondition {
        AND,        // 所有匹配器都必须匹配
        OR          // 任一匹配器匹配即可
    }
    
    // 严重程度
    public enum Severity {
        INFO,       // 信息
        LOW,        // 低危
        MEDIUM,     // 中危
        HIGH,       // 高危
        CRITICAL,   // 严重
        UNKNOWN     // 未知
    }
    
    // ========== 新增枚举和类 ==========
    
    /**
     * 输入类型枚举
     * 用于区分 POC 适用的目标类型
     */
    public enum InputType {
        URL,           // HTTP/HTTPS URL (https://example.com)
        IP_PORT,       // IP:端口 (192.168.1.1:22) - Network 协议
        DOMAIN,        // 纯域名 (example.com) - DNS 协议
        LOCAL_PATH,    // 本地目录 (/path/to/code) - File 协议递归扫描
        LOCAL_FILE,    // 本地特定文件 (/etc/passwd) - File 协议单文件
        ANY            // 通用，适用于多种输入类型
    }
    
    /**
     * POC 分类枚举
     * 基于 Nuclei 目录结构和功能用途分类
     */
    public enum PocCategory {
        // HTTP 子分类 - 漏洞类
        CVES,               // CVE 漏洞 (http/cves)
        CNVD,               // CNVD 漏洞 (http/cnvd)
        VULNERABILITIES,    // 通用漏洞 (http/vulnerabilities)
        DEFAULT_LOGINS,     // 默认口令 (http/default-logins)
        IOT,                // 物联网漏洞 (http/iot)
        
        // HTTP 子分类 - 配置审计类
        MISCONFIGURATION,   // 配置错误 (http/misconfiguration)
        EXPOSURES,          // 信息泄露 (http/exposures)
        EXPOSED_PANELS,     // 暴露面板 (http/exposed-panels)
        
        // HTTP 子分类 - 指纹识别类
        TECHNOLOGIES,       // 技术栈指纹 (http/technologies)
        HONEYPOT,           // 蜜罐检测 (http/honeypot)
        
        // HTTP 子分类 - 特殊用途（通常排除）
        OSINT,              // 信息收集 (http/osint)
        TOKEN_SPRAY,        // Token 验证 (http/token-spray)
        FUZZING,            // 模糊测试 (http/fuzzing)
        CREDENTIAL_STUFFING,// 撞库 (http/credential-stuffing)
        TAKEOVERS,          // 接管检测 (http/takeovers)
        
        // Network 子分类
        NETWORK_CVE,        // 网络服务 CVE (network/cves)
        NETWORK_MISCONFIG,  // 网络配置错误 (network/misconfig)
        NETWORK_DEFAULT_LOGIN, // 网络默认口令 (network/default-login)
        NETWORK_DETECTION,  // 网络服务指纹 (network/detection) - 非漏洞
        
        // File 子分类
        FILE_KEYS,          // 密钥扫描 (file/keys)
        FILE_MALWARE,       // 恶意软件 (file/malware)
        FILE_WEBSHELL,      // Webshell (file/webshell)
        FILE_AUDIT,         // 代码审计 (file/audit)
        
        // DNS 子分类
        DNS_TAKEOVER,       // DNS 接管 (dns/*-takeover-*)
        DNS_CONFIG,         // DNS 配置审计 (dns/dmarc, spf 等)
        
        // 其他协议
        SSL,                // SSL/TLS 检测 (ssl/)
        HEADLESS,           // 无头浏览器 (headless/)
        CODE,               // 本地代码执行 (code/)
        JAVASCRIPT,         // JavaScript 协议 (javascript/)
        
        // 其他
        MISCELLANEOUS,      // 杂项
        UNKNOWN;            // 未知
        
        /**
         * 阶段一：指纹识别类 POC
         * 用于识别目标技术栈，始终执行
         */
        public boolean isFingerprint() {
            switch (this) {
                case TECHNOLOGIES:      // 技术栈指纹
                case EXPOSED_PANELS:    // 暴露面板（也是一种资产发现）
                case HONEYPOT:          // 蜜罐检测
                case NETWORK_DETECTION: // 网络服务指纹
                    return true;
                default:
                    return false;
            }
        }
        
        /**
         * 阶段二：核心漏洞类 POC（需要指纹匹配）
         * 这些 POC 针对特定技术栈，智能模式下需要先识别出指纹才执行
         */
        public boolean requiresFingerprint() {
            switch (this) {
                case CVES:              // CVE 漏洞（针对特定产品）
                case CNVD:              // CNVD 漏洞
                case VULNERABILITIES:   // 通用漏洞（SQLi, XSS 等，但针对特定框架）
                case DEFAULT_LOGINS:    // 默认口令（针对特定产品）
                case IOT:               // IoT 漏洞（针对特定设备）
                case NETWORK_CVE:       // 网络服务 CVE
                case NETWORK_DEFAULT_LOGIN: // 网络默认口令
                    return true;
                default:
                    return false;
            }
        }
        
        /**
         * 阶段三：通用审计类 POC（不依赖指纹）
         * 配置错误、信息泄露等，任何目标都可以扫描
         */
        public boolean isGenericAudit() {
            switch (this) {
                case MISCONFIGURATION:  // 配置错误（CORS、安全头等）
                case EXPOSURES:         // 信息泄露（.git、.env 等）
                case MISCELLANEOUS:     // 杂项检测
                case SSL:               // SSL/TLS 检测
                case DNS_CONFIG:        // DNS 配置审计
                case DNS_TAKEOVER:      // DNS 接管
                case NETWORK_MISCONFIG: // 网络配置错误
                    return true;
                default:
                    return false;
            }
        }
        
        /**
         * 需要特殊输入或 flag 的 POC（默认排除）
         */
        public boolean requiresSpecialInput() {
            switch (this) {
                case OSINT:             // 纯信息收集（需要特定输入）
                case TOKEN_SPRAY:       // Token 验证（需要 API Key）
                case FUZZING:           // 模糊测试（需要 -fuzz flag）
                case CREDENTIAL_STUFFING: // 撞库（需要凭证）
                case TAKEOVERS:         // 子域名接管（针对域名记录）
                case HEADLESS:          // 无头浏览器（需要 -headless flag）
                case CODE:              // 本地代码执行（需要 -code flag）
                case JAVASCRIPT:        // JavaScript 协议（特殊协议）
                case FILE_KEYS:         // 本地文件扫描
                case FILE_MALWARE:      // 本地文件扫描
                case FILE_WEBSHELL:     // 本地文件扫描
                case FILE_AUDIT:        // 本地文件扫描
                    return true;
                default:
                    return false;
            }
        }

        /**
         * 获取该分类对应的协议层级
         */
        public PocProtocolLayer getProtocolLayer() {
            switch (this) {
                case CVES:
                case CNVD:
                case VULNERABILITIES:
                case DEFAULT_LOGINS:
                case IOT:
                case MISCONFIGURATION:
                case EXPOSURES:
                case EXPOSED_PANELS:
                case TECHNOLOGIES:
                case HONEYPOT:
                case OSINT:
                case TOKEN_SPRAY:
                case FUZZING:
                case CREDENTIAL_STUFFING:
                case TAKEOVERS:
                case MISCELLANEOUS:
                    return PocProtocolLayer.HTTP;

                case NETWORK_CVE:
                case NETWORK_MISCONFIG:
                case NETWORK_DEFAULT_LOGIN:
                case NETWORK_DETECTION:
                    return PocProtocolLayer.NETWORK;

                case FILE_KEYS:
                case FILE_MALWARE:
                case FILE_WEBSHELL:
                case FILE_AUDIT:
                    return PocProtocolLayer.FILE;

                case DNS_TAKEOVER:
                case DNS_CONFIG:
                    return PocProtocolLayer.DNS;

                case SSL:
                    return PocProtocolLayer.SSL;

                case HEADLESS:
                    return PocProtocolLayer.HEADLESS;

                case CODE:
                    return PocProtocolLayer.CODE;

                case JAVASCRIPT:
                    return PocProtocolLayer.JAVASCRIPT;

                default:
                    return PocProtocolLayer.HTTP;
            }
        }

        /**
         * 获取该分类对应的功能类型
         */
        public PocFunctionType getFunctionType() {
            switch (this) {
                case CVES:
                case CNVD:
                case VULNERABILITIES:
                case IOT:
                case NETWORK_CVE:
                case DNS_TAKEOVER:
                    return PocFunctionType.VULNERABILITY;

                case TECHNOLOGIES:
                case HONEYPOT:
                case EXPOSED_PANELS:
                case NETWORK_DETECTION:
                    return PocFunctionType.FINGERPRINT;

                case MISCONFIGURATION:
                case EXPOSURES:
                case NETWORK_MISCONFIG:
                case SSL:
                case DNS_CONFIG:
                case FILE_KEYS:
                case FILE_AUDIT:
                    return PocFunctionType.CONFIG_AUDIT;

                case OSINT:
                case TAKEOVERS:
                    return PocFunctionType.INFO_GATHERING;

                case DEFAULT_LOGINS:
                case NETWORK_DEFAULT_LOGIN:
                case TOKEN_SPRAY:
                case CREDENTIAL_STUFFING:
                    return PocFunctionType.CREDENTIAL;

                case FUZZING:
                case HEADLESS:
                case CODE:
                case JAVASCRIPT:
                case FILE_MALWARE:
                case FILE_WEBSHELL:
                default:
                    return PocFunctionType.SPECIAL;
            }
        }
    }

    /**
     * POC 协议层级枚举（一级分类）
     * 用于根据输入类型智能筛选 POC
     */
    public enum PocProtocolLayer {
        HTTP,       // Web 应用协议（HTTP/HTTPS）
        NETWORK,    // 网络层协议（TCP/UDP）
        DNS,        // DNS 协议
        FILE,       // 本地文件
        SSL,        // SSL/TLS
        HEADLESS,   // 无头浏览器
        CODE,       // 代码执行
        JAVASCRIPT; // JS 运行时

        /**
         * 判断该协议层级是否适用于给定的输入类型
         */
        public boolean isApplicableFor(InputType inputType) {
            switch (inputType) {
                case URL:
                    return this == HTTP || this == SSL || this == HEADLESS;
                case IP_PORT:
                    return this == NETWORK || this == SSL;
                case DOMAIN:
                    return this == DNS || this == HTTP;
                case LOCAL_PATH:
                case LOCAL_FILE:
                    return this == FILE || this == CODE || this == JAVASCRIPT;
                case ANY:
                default:
                    return true;
            }
        }
    }

    /**
     * POC 功能类型枚举（二级分类）
     * 用于按功能用途筛选 POC
     */
    public enum PocFunctionType {
        VULNERABILITY,  // 漏洞检测
        FINGERPRINT,    // 指纹识别
        CONFIG_AUDIT,   // 配置审计
        INFO_GATHERING, // 信息收集
        CREDENTIAL,     // 凭证相关
        SPECIAL;        // 特殊用途

        /**
         * 获取功能类型的中文描述
         */
        public String getDescription() {
            switch (this) {
                case VULNERABILITY: return "漏洞检测";
                case FINGERPRINT: return "指纹识别";
                case CONFIG_AUDIT: return "配置审计";
                case INFO_GATHERING: return "信息收集";
                case CREDENTIAL: return "凭证检测";
                case SPECIAL: return "特殊用途";
                default: return name();
            }
        }
    }

    /**
     * File 目标类型枚举
     */
    public enum FileTargetType {
        DIRECTORY,      // 扫描整个目录（递归）
        SPECIFIC_FILE   // 扫描特定文件（如 /etc/passwd）
    }
    
    /**
     * 指纹信息类
     * 用于存储目标的指纹识别结果，输出到报告中
     */
    @Data
    public static class FingerprintInfo {
        /**
         * 产品名称（如 "WordPress"、"Apache Tomcat"）
         */
        private String productName;
        
        /**
         * 版本号（如 "6.4.2"、"9.0.65"）
         */
        private String version;
        
        /**
         * 技术栈（如 "PHP/8.1"、"Java/11"）
         */
        private String technology;
        
        /**
         * 服务器信息（如 "nginx/1.24.0"、"Apache/2.4.52"）
         */
        private String server;
        
        /**
         * 操作系统（如 "Ubuntu"、"CentOS"）
         */
        private String os;
        
        /**
         * 检测到的所有标签（原始标签，未规范化）
         */
        private List<String> detectedTags = new ArrayList<>();
        
        /**
         * 规范化后的标签集合（用于 POC 匹配）
         */
        private Set<String> normalizedTags = new HashSet<>();
        
        /**
         * 服务 Banner（用于 Network 扫描）
         */
        private String banner;
        
        /**
         * 检测时间戳
         */
        private long detectedAt;
        
        /**
         * 指纹来源（nuclei/wappalyzer/custom）
         */
        private String source;
        
        /**
         * 置信度（0.0 - 1.0）
         */
        private double confidence = 1.0;
        
        /**
         * 添加检测到的标签
         */
        public void addDetectedTag(String tag) {
            if (tag != null && !tag.isEmpty()) {
                this.detectedTags.add(tag);
            }
        }
        
        /**
         * 批量添加检测到的标签
         */
        public void addDetectedTags(List<String> tags) {
            if (tags != null) {
                for (String tag : tags) {
                    addDetectedTag(tag);
                }
            }
        }
    }
}