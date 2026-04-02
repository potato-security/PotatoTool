package com.potato.potatotool.content.classObj;

/**
 * 全局配置常量类 - 统一管理 config.json 中的配置键
 * @author Potato
 * @date 2025/10/10 10:24
 */
public interface ConfigConstants {
    // ========== 配置根节点 ==========
    String PROXY = "Proxy";
    String AI = "AI";
    String DECOMPILE = "Decompile";
    String EXTENSION = "Extension";
    String UPDATE = "UpDate";  // 注意：配置文件中拼写为 UpDate
    String START_PASSWORD = "StartPassword";
    String LANGUAGE = "Language";

    // ========== Proxy 代理配置相关 ==========
    String PROXY_ENABLE = "enable";
    String PROXY_ADDRESS = "address";
    String PROXY_SERVICES = "services";  // 代理服务配置对象

    // ========== AI 配置相关 ==========
    String AI_PROVIDER = "provider";
    String AI_BASE_URL = "base_url";
    String AI_API_KEY = "api_key";
    String AI_MODEL_NAME = "model_name";
    String AI_TIMEOUT_MS = "timeout_ms";

    String AI_THINKING = "thinking";
    String AI_THINKING_ENABLED = "enabled";
    String AI_THINKING_BUDGET_TOKENS = "budget_tokens";

    // 本地默认 AI 配置（加密存储）
    String AI_LOCAL_BASE_URL = "local_base_url";
    String AI_LOCAL_API_KEY = "local_api_key";
    String AI_LOCAL_MODEL_NAME = "local_model_name";

    // ========== Decompile 反编译配置相关 ==========
    String DECOMPILE_MODE = "decompileMode";
    String DECOMPILE_AI_OPTIMIZATION = "AI_optimization";

    // ========== Update 更新配置相关 ==========
    String UPDATE_WIN_KB_INFO = "winKbInfo";
    
    String UPDATE_APP_VERSION = "AppVersion";          // 软件当前版本（顶层配置）
    String UPDATE_RESOURCE_PATH = "ResourcePath";      // 自定义资源路径（顶层配置）
    String UPDATE_LAST_CHECK_TIME = "lastCheckTime";   // 上次检查更新时间
    String UPDATE_AUTO_CHECK = "autoCheck";            // 自动检查更新
    String UPDATE_AUTO_DOWNLOAD = "autoDownload";      // 自动下载更新
    String UPDATE_RESOURCES = "resources";             // 资源更新信息
    String UPDATE_VERSION = "version";                 // 版本号
    String UPDATE_FILE_NAME = "fileName";              // 资源文件名（不包含路径）
    String UPDATE_CHECKSUM = "checksum";               // 文件校验和对象
    String UPDATE_CHECKSUM_SHA256 = "sha256";          // SHA256哈希值
    String UPDATE_CHECKSUM_MD5 = "md5";                // MD5哈希值
    String UPDATE_SKIPPED_VERSIONS = "skippedVersions"; // 跳过的版本列表
    
    // ========== VulnScan 漏洞扫描配置相关 ==========
    String VULNSCAN = "VulnScan";
    String VULNSCAN_SERVICE = "VulnScan";                 // Proxy.services 下漏洞扫描服务键
    String VULNSCAN_POC_DIR = "pocDir";
    String VULNSCAN_REPORT_DIR = "reportDir";
    String VULNSCAN_THREADS = "threads";
    String VULNSCAN_TIMEOUT = "timeout";
    String VULNSCAN_RETRIES = "retries";
    String VULNSCAN_MAX_RESPONSE_SIZE = "maxResponseSize";
    String VULNSCAN_USER_AGENT = "userAgent";
    String VULNSCAN_DEBUG = "debug";
    String VULNSCAN_VERBOSE = "verbose";
    String VULNSCAN_MAX_CONNECTIONS = "maxConnections";
    String VULNSCAN_CONNECTION_TIMEOUT = "connectionTimeout";
    String VULNSCAN_MAX_PAYLOAD_COMBINATIONS = "maxPayloadCombinations";  // Payload 组合数限制

    // ========== VulnScan 变量字典配置 ==========
    String VULNSCAN_VARIABLES = "variables";           // 变量字典配置对象
    String VULNSCAN_VAR_USER_DICT = "userDict";        // {{user}} 用户名字典路径
    String VULNSCAN_VAR_PASS_DICT = "passDict";        // {{pass}} 密码字典路径
    String VULNSCAN_VAR_USERNAME_DICT = "usernameDict"; // {{username}} 用户名字典路径（别名）
    String VULNSCAN_VAR_PASSWORD_DICT = "passwordDict"; // {{password}} 密码字典路径（别名）
    String VULNSCAN_VAR_CUSTOM_DICTS = "customDicts";  // 自定义变量字典映射（Map<变量名, 字典路径>）

    // ========== VulnScan Headers 配置 ==========
    String VULNSCAN_CUSTOM_HEADERS = "customHeaders";      // 用户自定义 Headers 配置对象
    String VULNSCAN_HEADER_TEMPLATES = "headerTemplates";   // Header 模板配置对象（从配置文件读取，代码不修改）

    // ========== VulnScan 线程池配置 ==========
    String VULNSCAN_THREAD_POOL = "threadPool";             // 线程池配置对象
    String VULNSCAN_CORE_THREADS = "coreThreads";           // 核心线程数
    String VULNSCAN_MAX_THREADS = "maxThreads";             // 最大线程数
    String VULNSCAN_QUEUE_SIZE = "queueSize";               // 任务队列大小

    // ========== VulnScan 报告配置 ==========
    String VULNSCAN_REPORT = "report";                      // 报告配置对象
    String VULNSCAN_REPORT_DEFAULT_DIR = "defaultDir";      // 默认导出目录
    String VULNSCAN_REPORT_NAME_TEMPLATE = "nameTemplate";  // 报告命名模板
    String VULNSCAN_REPORT_AUTO_EXPORT = "autoExport";      // 是否自动导出
    String VULNSCAN_REPORT_DEFAULT_FORMAT = "defaultFormat"; // 默认报告格式

    // ========== VulnScan 数据库配置 ==========
    String VULNSCAN_DB_PATH = "dbPath";                     // 数据库路径（可选配置）
    String VULNSCAN_POC_DB_VERSION = "pocDatabaseVersion";  // POC 数据库版本
    
    // ========== VulnScan Headless 配置 ==========
    String VULNSCAN_HEADLESS = "headless";                 // Headless 配置对象
    String VULNSCAN_HEADLESS_BROWSER_PATH = "browserPath"; // 浏览器可执行文件路径（为空则自动探测）

    // ========== OOB 配置相关 ==========
    String OOB = "OOB";
    String OOB_HTTP = "http";
    String OOB_DNS = "dns";

    // HTTP OOB
    String OOB_HTTP_PLATFORM = "platform";
    String OOB_HTTP_INTERACTSH_SERVER = "interactshServer";
    String OOB_HTTP_INTERACTSH_TOKEN = "interactshToken";
    String OOB_HTTP_CUSTOM_SERVER = "customServer";
    String OOB_HTTP_CUSTOM_TOKEN = "customToken";
    String OOB_HTTP_CACHE_TTL_SECONDS = "cacheTtlSeconds";

    // DNS OOB
    String OOB_DNS_PLATFORM = "platform";
    String OOB_DNS_CEYE_IDENTIFIER = "ceyeIdentifier";
    String OOB_DNS_CEYE_TOKEN = "ceyeToken";

}
