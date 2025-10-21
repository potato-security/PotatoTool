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
    String AI_API_BASE = "AI_API_Base";
    String AI_API_KEY = "AI_API_Key";
    String AI_MODEL = "AI_Model";
    
    // 本地默认 AI 配置（加密存储）
    String LOCAL_AI_API_BASE = "Local_AI_API_Base";
    String LOCAL_AI_API_KEY = "Local_AI_API_Key";
    String LOCAL_AI_MODEL = "Local_AI_Model";

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
    
}
