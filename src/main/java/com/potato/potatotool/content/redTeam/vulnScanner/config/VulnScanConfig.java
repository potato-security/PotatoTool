package com.potato.potatotool.content.redTeam.vulnScanner.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.http.PythonHandler;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.EnvPathConfig;
import com.potato.potatotool.utils.network.ProxyUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 漏洞扫描配置管理器
 * 从全局 config.json 中读取漏洞扫描相关配置
 *
 * @author Potato
 * @date 2024-10-28
 */
public class VulnScanConfig {
    private static VulnScanConfig instance;

    // 字典缓存（避免每次POC执行都重新加载）
    private volatile Map<String, List<String>> cachedDictionaries;

    // 默认配置值
    private static final class Defaults {
        static final String POC_DIR = "pocs";
        static final String REPORT_DIR = "reports";
        static final int DEFAULT_THREADS = 20;
        static final int DEFAULT_TIMEOUT = 15;
        static final int DEFAULT_RETRIES = 2;
        static final int MAX_RESPONSE_SIZE = 1024 * 1024; // 1MB
        static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        static final int MAX_PAYLOAD_COMBINATIONS = 1000; // Payload 组合数限制
    }
    
    private VulnScanConfig() {}
    
    /**
     * 获取配置管理器单例
     */
    public static synchronized VulnScanConfig getInstance() {
        if (instance == null) {
            instance = new VulnScanConfig();
        }
        return instance;
    }
    
    /**
     * 从config.json获取漏洞扫描配置
     */
    private JsonObject getVulnScanConfig() {
        try {
            JsonElement element = (JsonElement) Constants.getOutsideConfig(ConfigConstants.VULNSCAN);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (Exception e) {
            // 配置不存在或读取失败，使用默认值
        }
        return new JsonObject();
    }
    
    /**
     * 从配置中获取字符串值
     */
    private String getStringValue(String key, String defaultValue) {
        JsonObject config = getVulnScanConfig();
        if (config.has(key) && !config.get(key).isJsonNull()) {
            return config.get(key).getAsString();
        }
        return defaultValue;
    }
    
    /**
     * 从配置中获取整数值
     */
    private int getIntValue(String key, int defaultValue) {
        JsonObject config = getVulnScanConfig();
        if (config.has(key) && !config.get(key).isJsonNull()) {
            try {
                return config.get(key).getAsInt();
            } catch (Exception e) {
                // 类型转换失败，使用默认值
            }
        }
        return defaultValue;
    }
    
    /**
     * 从配置中获取布尔值
     */
    private boolean getBooleanValue(String key, boolean defaultValue) {
        JsonObject config = getVulnScanConfig();
        if (config.has(key) && !config.get(key).isJsonNull()) {
            try {
                return config.get(key).getAsBoolean();
            } catch (Exception e) {
                // 类型转换失败，使用默认值
            }
        }
        return defaultValue;
    }
    
    // ==================== POC相关配置 ====================
    
    /**
     * 获取POC目录路径
     */
    public String getPocDirectory() {
        return getStringValue(ConfigConstants.VULNSCAN_POC_DIR, Defaults.POC_DIR);
    }
    
    /**
     * 获取Nuclei POC目录
     */
    public String getNucleiPocDirectory() {
        return getPocDirectory() + "/nuclei";
    }
    
    /**
     * 获取Xray POC目录
     */
    public String getXrayPocDirectory() {
        return getPocDirectory() + "/xray";
    }
    
    /**
     * 获取Goby POC目录
     */
    public String getGobyPocDirectory() {
        return getPocDirectory() + "/goby";
    }
    
    /**
     * 获取Pocsuite POC目录
     */
    public String getPocsuitePocDirectory() {
        return getPocDirectory() + "/pocsuite";
    }
    
    // ==================== 报告相关配置 ====================
    
    /**
     * 获取报告输出目录
     */
    public String getReportDirectory() {
        return getStringValue(ConfigConstants.VULNSCAN_REPORT_DIR, Defaults.REPORT_DIR);
    }
    
    /**
     * 获取HTML报告模板路径
     */
    public String getHtmlReportTemplate() {
        return "templates/report.html";
    }
    
    // ==================== 扫描相关配置 ====================
    
    /**
     * 获取默认线程数
     */
    public int getDefaultThreads() {
        return getIntValue(ConfigConstants.VULNSCAN_THREADS, Defaults.DEFAULT_THREADS);
    }
    
    /**
     * 获取默认超时时间（秒）
     */
    public int getDefaultTimeout() {
        return getIntValue(ConfigConstants.VULNSCAN_TIMEOUT, Defaults.DEFAULT_TIMEOUT);
    }
    
    /**
     * 获取默认重试次数
     */
    public int getDefaultRetries() {
        return getIntValue(ConfigConstants.VULNSCAN_RETRIES, Defaults.DEFAULT_RETRIES);
    }
    
    /**
     * 获取最大响应大小（字节）
     */
    public int getMaxResponseSize() {
        return getIntValue(ConfigConstants.VULNSCAN_MAX_RESPONSE_SIZE, Defaults.MAX_RESPONSE_SIZE);
    }
    
    /**
     * 获取默认User-Agent
     */
    public String getDefaultUserAgent() {
        return getStringValue(ConfigConstants.VULNSCAN_USER_AGENT, Defaults.USER_AGENT);
    }
    
    /**
     * 是否启用调试模式
     */
    public boolean isDebugEnabled() {
        return getBooleanValue(ConfigConstants.VULNSCAN_DEBUG, false);
    }
    
    /**
     * 是否启用详细日志
     */
    public boolean isVerboseEnabled() {
        return getBooleanValue(ConfigConstants.VULNSCAN_VERBOSE, false);
    }
    
    // ==================== 连接池相关配置 ====================
    
    /**
     * 获取最大连接数
     */
    public int getMaxConnections() {
        return getIntValue(ConfigConstants.VULNSCAN_MAX_CONNECTIONS, 100);
    }
    
    /**
     * 获取连接超时时间（毫秒）
     */
    public int getConnectionTimeout() {
        return getIntValue(ConfigConstants.VULNSCAN_CONNECTION_TIMEOUT, 30000);
    }

    /**
     * 获取最大 Payload 组合数限制
     */
    public int getMaxPayloadCombinations() {
        return getIntValue(ConfigConstants.VULNSCAN_MAX_PAYLOAD_COMBINATIONS, Defaults.MAX_PAYLOAD_COMBINATIONS);
    }

    // ==================== 代理相关配置 ====================
    
    /**
     * 是否启用代理（主代理与漏洞扫描服务代理需同时开启）
     */
    public boolean isProxyEnabled() {
        return ProxyUtils.isMainProxyEnabled()
                && ProxyUtils.isServiceProxyEnabled(ConfigConstants.VULNSCAN_SERVICE);
    }

    /**
     * 获取代理地址（读取全局代理配置）
     */
    public String getProxyAddress() {
        return ProxyUtils.getMainProxyAddress();
    }
    
    /**
     * 重新加载配置（清除缓存）
     */
    public void reload() {
        // 清除 Constants 的缓存配置
        Constants.cachedConfig = null;
    }

    // ==================== 变量字典相关配置 ====================

    /**
     * 获取变量字典配置对象
     */
    private JsonObject getVariablesConfig() {
        try {
            JsonObject vulnScanConfig = getVulnScanConfig();
            if (vulnScanConfig.has(ConfigConstants.VULNSCAN_VARIABLES) &&
                vulnScanConfig.get(ConfigConstants.VULNSCAN_VARIABLES).isJsonObject()) {
                return vulnScanConfig.get(ConfigConstants.VULNSCAN_VARIABLES).getAsJsonObject();
            }
        } catch (Exception e) {
            // 配置不存在或读取失败
        }
        return new JsonObject();
    }

    /**
     * 获取用户名字典路径（{{user}}）
     */
    public String getUserDictPath() {
        JsonObject variablesConfig = getVariablesConfig();
        if (variablesConfig.has(ConfigConstants.VULNSCAN_VAR_USER_DICT)) {
            return variablesConfig.get(ConfigConstants.VULNSCAN_VAR_USER_DICT).getAsString();
        }
        return null;
    }

    /**
     * 获取密码字典路径（{{pass}}）
     */
    public String getPassDictPath() {
        JsonObject variablesConfig = getVariablesConfig();
        if (variablesConfig.has(ConfigConstants.VULNSCAN_VAR_PASS_DICT)) {
            return variablesConfig.get(ConfigConstants.VULNSCAN_VAR_PASS_DICT).getAsString();
        }
        return null;
    }

    /**
     * 获取自定义变量字典路径映射
     * @return Map<变量名, 字典路径>
     */
    public Map<String, String> getCustomDicts() {
        Map<String, String> customDicts = new HashMap<>();
        try {
            JsonObject variablesConfig = getVariablesConfig();
            if (variablesConfig.has(ConfigConstants.VULNSCAN_VAR_CUSTOM_DICTS) &&
                variablesConfig.get(ConfigConstants.VULNSCAN_VAR_CUSTOM_DICTS).isJsonObject()) {
                JsonObject customDictsObj = variablesConfig.get(ConfigConstants.VULNSCAN_VAR_CUSTOM_DICTS).getAsJsonObject();
                for (String key : customDictsObj.keySet()) {
                    customDicts.put(key, customDictsObj.get(key).getAsString());
                }
            }
        } catch (Exception e) {
            // 读取失败，返回空Map
        }
        return customDicts;
    }

    /**
     * 从字典文件加载变量值列表
     * @param dictPath 字典文件路径
     * @return 变量值列表
     */
    public List<String> loadDictionaryValues(String dictPath) {
        List<String> values = new ArrayList<>();
        if (dictPath == null || dictPath.trim().isEmpty()) {
            return values;
        }

        try {
            Path path = Paths.get(dictPath);
            if (!Files.exists(path) || !Files.isReadable(path)) {
                System.err.println("字典文件不存在或不可读: " + dictPath);
                return values;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(dictPath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) { // 跳过空行和注释行
                        values.add(line);
                    }
                }
            }

            if (values.isEmpty()) {
                System.err.println("警告: 字典文件为空: " + dictPath);
            }

        } catch (IOException e) {
            System.err.println("读取字典文件失败: " + dictPath + ", 错误: " + e.getMessage());
        }

        return values;
    }

    /**
     * 加载所有配置的变量字典到变量映射（带缓存）
     * 只支持 {{user}} 和 {{pass}} 两个内置字典变量
     * @return Map<变量名, 变量值列表>
     */
    public Map<String, List<String>> loadAllDictionaries() {
        if (cachedDictionaries != null) {
            return cachedDictionaries;
        }

        Map<String, List<String>> variables = new HashMap<>();

        // 1. 加载内置默认 user 字典
        try {
            String defaultUserContent = Constants.getResourceString("defaultUserDict");
            if (defaultUserContent != null && !defaultUserContent.isEmpty()) {
                List<String> userValues = parseContentToList(defaultUserContent);
                if (!userValues.isEmpty()) {
                    variables.put("user", userValues);
                }
            }
        } catch (Exception e) {
            // 默认字典加载失败，忽略
        }

        // 2. 加载内置默认 pass 字典
        try {
            String defaultPassContent = Constants.getResourceString("defaultPassDict");
            if (defaultPassContent != null && !defaultPassContent.isEmpty()) {
                List<String> passValues = parseContentToList(defaultPassContent);
                if (!passValues.isEmpty()) {
                    variables.put("pass", passValues);
                }
            }
        } catch (Exception e) {
            // 默认字典加载失败，忽略
        }

        // 3. 加载用户配置的 user 字典（会覆盖默认字典）
        String userDictPath = getUserDictPath();
        if (userDictPath != null) {
            List<String> userValues = loadDictionaryValues(userDictPath);
            if (!userValues.isEmpty()) {
                variables.put("user", userValues);
            }
        }

        // 4. 加载用户配置的 pass 字典（会覆盖默认字典）
        String passDictPath = getPassDictPath();
        if (passDictPath != null) {
            List<String> passValues = loadDictionaryValues(passDictPath);
            if (!passValues.isEmpty()) {
                variables.put("pass", passValues);
            }
        }

        // 5. 加载自定义字典（允许用户自定义其他变量）
        Map<String, String> customDicts = getCustomDicts();
        for (Map.Entry<String, String> entry : customDicts.entrySet()) {
            String varName = entry.getKey();
            String dictPath = entry.getValue();
            List<String> values = loadDictionaryValues(dictPath);
            if (!values.isEmpty()) {
                variables.put(varName, values);
            }
        }

        cachedDictionaries = Collections.unmodifiableMap(variables);
        return cachedDictionaries;
    }

    /**
     * 解析字典内容为列表
     * @param content 字典文件内容
     * @return 字典值列表
     */
    private List<String> parseContentToList(String content) {
        List<String> values = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return values;
        }

        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                values.add(line);
            }
        }

        return values;
    }

    /**
     * 验证字典文件路径
     * @param dictPath 字典文件路径
     * @return 验证结果 {valid: boolean, message: String}
     */
    public ValidationResult validateDictPath(String dictPath) {
        if (dictPath == null || dictPath.trim().isEmpty()) {
            return new ValidationResult(false, "字典路径不能为空");
        }

        try {
            Path path = Paths.get(dictPath);

            if (!Files.exists(path)) {
                return new ValidationResult(false, "字典文件不存在: " + dictPath);
            }

            if (!Files.isRegularFile(path)) {
                return new ValidationResult(false, "路径不是文件: " + dictPath);
            }

            if (!Files.isReadable(path)) {
                return new ValidationResult(false, "字典文件不可读: " + dictPath);
            }

            // 检查文件大小（不超过 10MB）
            long fileSize = Files.size(path);
            if (fileSize > 10 * 1024 * 1024) {
                return new ValidationResult(false, "字典文件过大（超过10MB）: " + dictPath);
            }

            // 检查是否至少包含一行有效数据
            try (BufferedReader reader = new BufferedReader(new FileReader(dictPath))) {
                String line;
                boolean hasValidLine = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        hasValidLine = true;
                        break;
                    }
                }

                if (!hasValidLine) {
                    return new ValidationResult(false, "字典文件为空或没有有效数据: " + dictPath);
                }
            }

            return new ValidationResult(true, "字典文件验证通过");

        } catch (IOException e) {
            return new ValidationResult(false, "读取字典文件失败: " + e.getMessage());
        } catch (Exception e) {
            return new ValidationResult(false, "验证失败: " + e.getMessage());
        }
    }

    /**
     * 字典路径验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    /**
     * 保存用户名字典路径（带验证）
     */
    public boolean saveUserDictPath(String dictPath) {
        if (dictPath != null && !dictPath.trim().isEmpty()) {
            ValidationResult result = validateDictPath(dictPath);
            if (!result.isValid()) {
                System.err.println("字典路径验证失败: " + result.getMessage());
                return false;
            }
        }
        return saveVariableConfig(ConfigConstants.VULNSCAN_VAR_USER_DICT, dictPath);
    }

    /**
     * 保存密码字典路径（带验证）
     */
    public boolean savePassDictPath(String dictPath) {
        if (dictPath != null && !dictPath.trim().isEmpty()) {
            ValidationResult result = validateDictPath(dictPath);
            if (!result.isValid()) {
                System.err.println("字典路径验证失败: " + result.getMessage());
                return false;
            }
        }
        return saveVariableConfig(ConfigConstants.VULNSCAN_VAR_PASS_DICT, dictPath);
    }

    /**
     * 保存自定义变量字典映射（带验证）
     */
    public boolean saveCustomDicts(Map<String, String> customDicts) {
        try {
            // 验证所有字典路径
            if (customDicts != null && !customDicts.isEmpty()) {
                for (Map.Entry<String, String> entry : customDicts.entrySet()) {
                    String dictPath = entry.getValue();
                    if (dictPath != null && !dictPath.trim().isEmpty()) {
                        ValidationResult result = validateDictPath(dictPath);
                        if (!result.isValid()) {
                            System.err.println("自定义字典验证失败 [" + entry.getKey() + "]: " + result.getMessage());
                            return false;
                        }
                    }
                }
            }

            // 构造variables配置对象
            Map<String, Object> variablesConfig = new HashMap<>();

            // 保留现有的user和pass配置
            String existingUserDict = getUserDictPath();
            if (existingUserDict != null) {
                variablesConfig.put(ConfigConstants.VULNSCAN_VAR_USER_DICT, existingUserDict);
            }

            String existingPassDict = getPassDictPath();
            if (existingPassDict != null) {
                variablesConfig.put(ConfigConstants.VULNSCAN_VAR_PASS_DICT, existingPassDict);
            }

            // 添加customDicts
            if (customDicts != null && !customDicts.isEmpty()) {
                variablesConfig.put(ConfigConstants.VULNSCAN_VAR_CUSTOM_DICTS, customDicts);
            }

            // 保存到VulnScan.variables
            Map<String, Object> vulnScanMap = new HashMap<>();
            vulnScanMap.put(ConfigConstants.VULNSCAN_VARIABLES, variablesConfig);

            return Constants.saveConfig(vulnScanMap, ConfigConstants.VULNSCAN);
        } catch (Exception e) {
            System.err.println("保存自定义字典配置失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 保存单个变量字典配置项
     */
    private boolean saveVariableConfig(String key, String value) {
        try {
            // 获取现有的variables配置
            JsonObject existingVariables = getVariablesConfig();
            Map<String, Object> variablesMap = new HashMap<>();

            // 复制现有配置
            for (String existingKey : existingVariables.keySet()) {
                variablesMap.put(existingKey, existingVariables.get(existingKey).getAsString());
            }

            // 更新或添加新值
            if (value != null && !value.trim().isEmpty()) {
                variablesMap.put(key, value);
            } else {
                // 如果值为空，删除该配置
                variablesMap.remove(key);
            }

            // 保存到VulnScan.variables
            Map<String, Object> vulnScanMap = new HashMap<>();
            vulnScanMap.put(ConfigConstants.VULNSCAN_VARIABLES, variablesMap);

            return Constants.saveConfig(vulnScanMap, ConfigConstants.VULNSCAN);
        } catch (Exception e) {
            System.err.println("保存变量字典配置失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 保存完整的变量字典配置（带验证）
     * @param userDictPath 用户名字典路径
     * @param passDictPath 密码字典路径
     * @param customDicts 自定义变量字典映射
     */
    public boolean saveAllDictionaries(String userDictPath, String passDictPath, Map<String, String> customDicts) {
        try {
            Map<String, Object> variablesConfig = new HashMap<>();

            // 验证并保存user字典
            if (userDictPath != null && !userDictPath.trim().isEmpty()) {
                ValidationResult result = validateDictPath(userDictPath);
                if (!result.isValid()) {
                    System.err.println("用户名字典验证失败: " + result.getMessage());
                    return false;
                }
                variablesConfig.put(ConfigConstants.VULNSCAN_VAR_USER_DICT, userDictPath);
            }

            // 验证并保存pass字典
            if (passDictPath != null && !passDictPath.trim().isEmpty()) {
                ValidationResult result = validateDictPath(passDictPath);
                if (!result.isValid()) {
                    System.err.println("密码字典验证失败: " + result.getMessage());
                    return false;
                }
                variablesConfig.put(ConfigConstants.VULNSCAN_VAR_PASS_DICT, passDictPath);
            }

            // 验证并保存自定义字典
            if (customDicts != null && !customDicts.isEmpty()) {
                for (Map.Entry<String, String> entry : customDicts.entrySet()) {
                    String dictPath = entry.getValue();
                    if (dictPath != null && !dictPath.trim().isEmpty()) {
                        ValidationResult result = validateDictPath(dictPath);
                        if (!result.isValid()) {
                            System.err.println("自定义字典验证失败 [" + entry.getKey() + "]: " + result.getMessage());
                            return false;
                        }
                    }
                }
                variablesConfig.put(ConfigConstants.VULNSCAN_VAR_CUSTOM_DICTS, customDicts);
            }

            // 保存到VulnScan.variables
            Map<String, Object> vulnScanMap = new HashMap<>();
            vulnScanMap.put(ConfigConstants.VULNSCAN_VARIABLES, variablesConfig);

            boolean success = Constants.saveConfig(vulnScanMap, ConfigConstants.VULNSCAN);

            // 保存成功后清除缓存
            if (success) {
                reload();
            }

            return success;
        } catch (Exception e) {
            System.err.println("保存所有字典配置失败: " + e.getMessage());
            return false;
        }
    }

    // ==================== 线程池配置管理 ====================

    /**
     * 获取线程池配置对象
     */
    private JsonObject getThreadPoolConfig() {
        try {
            JsonObject vulnScanConfig = getVulnScanConfig();
            if (vulnScanConfig.has(ConfigConstants.VULNSCAN_THREAD_POOL) &&
                vulnScanConfig.get(ConfigConstants.VULNSCAN_THREAD_POOL).isJsonObject()) {
                return vulnScanConfig.get(ConfigConstants.VULNSCAN_THREAD_POOL).getAsJsonObject();
            }
        } catch (Exception e) {
            // 配置不存在或读取失败
        }
        return new JsonObject();
    }

    /**
     * 获取核心线程数
     */
    public int getCoreThreads() {
        JsonObject config = getThreadPoolConfig();
        if (config.has(ConfigConstants.VULNSCAN_CORE_THREADS)) {
            try {
                return config.get(ConfigConstants.VULNSCAN_CORE_THREADS).getAsInt();
            } catch (Exception e) {
                // 类型转换失败
            }
        }
        return 10; // 默认值
    }

    /**
     * 获取最大线程数
     */
    public int getMaxThreads() {
        JsonObject config = getThreadPoolConfig();
        if (config.has(ConfigConstants.VULNSCAN_MAX_THREADS)) {
            try {
                return config.get(ConfigConstants.VULNSCAN_MAX_THREADS).getAsInt();
            } catch (Exception e) {
                // 类型转换失败
            }
        }
        return 50; // 默认值
    }

    /**
     * 获取任务队列大小
     */
    public int getQueueSize() {
        JsonObject config = getThreadPoolConfig();
        if (config.has(ConfigConstants.VULNSCAN_QUEUE_SIZE)) {
            try {
                return config.get(ConfigConstants.VULNSCAN_QUEUE_SIZE).getAsInt();
            } catch (Exception e) {
                // 类型转换失败
            }
        }
        return 1000; // 默认值
    }

    // ==================== 报告配置管理 ====================

    /**
     * 获取报告配置对象
     */
    private JsonObject getReportConfig() {
        try {
            JsonObject vulnScanConfig = getVulnScanConfig();
            if (vulnScanConfig.has(ConfigConstants.VULNSCAN_REPORT) &&
                vulnScanConfig.get(ConfigConstants.VULNSCAN_REPORT).isJsonObject()) {
                return vulnScanConfig.get(ConfigConstants.VULNSCAN_REPORT).getAsJsonObject();
            }
        } catch (Exception e) {
            // 配置不存在或读取失败
        }
        return new JsonObject();
    }

    /**
     * 获取报告默认目录
     */
    public String getReportDefaultDirectory() {
        JsonObject config = getReportConfig();
        if (config.has(ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR)) {
            return config.get(ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR).getAsString();
        }
        return Defaults.REPORT_DIR; // 使用现有的默认值
    }

    /**
     * 获取报告命名模板
     */
    public String getReportNameTemplate() {
        JsonObject config = getReportConfig();
        if (config.has(ConfigConstants.VULNSCAN_REPORT_NAME_TEMPLATE)) {
            return config.get(ConfigConstants.VULNSCAN_REPORT_NAME_TEMPLATE).getAsString();
        }
        return "scan_report_{timestamp}"; // 默认模板
    }

    /**
     * 是否自动导出报告
     */
    public boolean isAutoExportEnabled() {
        JsonObject config = getReportConfig();
        if (config.has(ConfigConstants.VULNSCAN_REPORT_AUTO_EXPORT)) {
            try {
                return config.get(ConfigConstants.VULNSCAN_REPORT_AUTO_EXPORT).getAsBoolean();
            } catch (Exception e) {
                // 类型转换失败
            }
        }
        return false; // 默认不自动导出
    }

    /**
     * 获取默认报告格式
     */
    public String getDefaultReportFormat() {
        JsonObject config = getReportConfig();
        if (config.has(ConfigConstants.VULNSCAN_REPORT_DEFAULT_FORMAT)) {
            return config.get(ConfigConstants.VULNSCAN_REPORT_DEFAULT_FORMAT).getAsString();
        }
        return "HTML"; // 默认HTML格式
    }

    // ==================== 数据库配置管理 ====================

    /**
     * 获取数据库路径（可选配置）
     * 如果未配置，则使用默认路径 ResourcePath/vulnscan/vulnscan.db
     */
    public String getDatabasePath() {
        String dbPath = getStringValue(ConfigConstants.VULNSCAN_DB_PATH, null);
        if (dbPath != null && !dbPath.trim().isEmpty()) {
            return dbPath;
        }
        // 默认路径将由 VulnScanDatabase 根据 PathManager 计算
        return null;
    }

    // ==================== Python 配置管理 ====================

    /**
     * 获取 Python 路径配置
     * 配置项: EnvPath.python
     * 
     * @return Python 可执行文件路径，如果未配置返回 null（将自动检测）
     */
    public String getPythonPath() {
        String pythonPath = EnvPathConfig.getPythonPath();
        return pythonPath == null || pythonPath.trim().isEmpty() ? null : pythonPath.trim();
    }

    /**
     * 刷新 Python 运行时缓存
     */
    public void initializePython() {
        String configuredPath = getPythonPath();
        PythonHandler.resetPythonPath();
        if (configuredPath != null && !configuredPath.isEmpty()) {
            PythonHandler.setPythonPath(configuredPath);
        }
    }

    /**
     * 获取 Python 版本信息
     * 
     * @return Python 版本字符串
     */
    public String getPythonVersion() {
        return PythonHandler.getPythonVersion();
    }

    /**
     * 检查 Python 是否可用
     * 
     * @return 是否可用
     */
    public boolean isPythonAvailable() {
        return PythonHandler.isPythonAvailable();
    }
}
