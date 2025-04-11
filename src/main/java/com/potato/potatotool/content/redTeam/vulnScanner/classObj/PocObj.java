package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2025/3/19 16:30
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
        private String flow;                // 流程表达式
        private String impact;              // 漏洞影响
        private String recommendation;      // 修复建议
        private String homepage;            // 主页

        // 搜索相关
        private Map<String, String> searchQueries = new HashMap<>();  // 搜索语句，如fofa、zoomeye等
        
        // 变量和载荷
        private Map<String, String> variables = new HashMap<>();      // 变量
        private Map<String, List<String>> payloads = new HashMap<>(); // 载荷
        
        // 漏洞信息
        private String cveId;               // CVE编号
        private String cweId;               // CWE编号
        private String vulType;             // 漏洞类型
        private String cvssScore;           // CVSS评分
        private String cvssMetrics;         // CVSS向量
        
        // 执行步骤
        private List<PocStep> verifySteps = new ArrayList<>();  // 验证步骤
        private List<PocStep> exploitSteps = new ArrayList<>(); // 利用步骤
        
        // 全局配置
        private GlobalConfig globalConfig = new GlobalConfig(); // 全局配置
        
        // 原始POC
        private Object originalPoc;         // 原始POC对象
        private String originalFormat;      // 原始POC格式
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
        private List<String> raw;           // 原始请求
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
    public static class TcpStep extends PocStep {
        private String host;                // 主机地址
        private String port;                // 端口
        private List<Input> inputs = new ArrayList<>(); // 输入列表
    }
    
    @Data
    public static class DnsStep extends PocStep {
        private String domain;              // 域名
        private String type;                // DNS记录类型
        private String resolver;            // DNS解析器
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
    }
    
    // 匹配器类型（修改为纯粹的匹配类型）
    public enum MatcherType {
        STATUS,     // 状态码
        SIZE,       // 大小
        WORD,       // 关键词
        REGEX,      // 正则表达式
        BINARY,     // 二进制
        DSL,        // 领域特定语言
        HASH,       // 哈希
        JSON,       // JSON
        XML,        // XML
        XPATH,      // XPath
        KVAL,       // 键值对
        GROUP,      // 组合匹配（嵌套匹配器）
        UNKNOWN     // 未知
, TIME
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
}