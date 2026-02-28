package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DNSLog 服务
 * 支持多个 DNSLog 平台：dnslog.cn 和 ceye.io
 * 
 * 平台特点：
 * - dnslog.cn: 免费，无需配置，但域名有效期较短（约1小时）
 * - ceye.io: 需要注册账号，功能更强大，域名有效期更长
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class DnsLogService {
    
    /**
     * DNSLog平台枚举
     */
    public enum Platform {
        DNSLOG_CN("dnslog.cn"),
        CEYE_IO("ceye.io");
        
        private final String name;
        
        Platform(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * DNSLog信息类 - 保存域名和关联信息
     */
    public static class DnsLogInfo {
        private String domain;           // 完整域名
        private String sessionId;        // Session ID（dnslog.cn使用）
        private long createTime;         // 创建时间
        private Platform platform;       // 平台类型
        
        public DnsLogInfo(String domain, String sessionId, Platform platform) {
            this.domain = domain;
            this.sessionId = sessionId;
            this.createTime = System.currentTimeMillis();
            this.platform = platform;
        }
        
        public String getDomain() { return domain; }
        public String getSessionId() { return sessionId; }
        public long getCreateTime() { return createTime; }
        public Platform getPlatform() { return platform; }
        
        /**
         * 检查域名是否过期
         * dnslog.cn: 50分钟有效期（预留缓冲）
         * ceye.io: 不过期
         */
        public boolean isExpired() {
            if (platform == Platform.DNSLOG_CN) {
                long elapsed = System.currentTimeMillis() - createTime;
                return elapsed > (50 * 60 * 1000); // 50分钟
            }
            return false; // ceye.io 不过期
        }
    }
    
    // 缓存生成的DNSLog信息，避免重复请求
    private static final ConcurrentHashMap<String, DnsLogInfo> DNSLOG_CACHE = new ConcurrentHashMap<>();
    
    // Mock support for testing
    private static boolean mockMode = false;
    
    public static void setMockMode(boolean mock) {
        mockMode = mock;
        if (mock) {
            System.out.println("DNSLog Service switched to MOCK mode");
        }
    }
    
    public static boolean isMockMode() {
        return mockMode;
    }
    
    // 当前使用的平台
    private static Platform currentPlatform = Platform.DNSLOG_CN;
    
    // dnslog.cn 配置
    private static volatile DnsLogInfo dnslogCnInfo = null;
    
    // ceye.io 配置（需要用户配置）
    private static String ceyeIdentifier = null;
    private static String ceyeToken = null;
    
    /**
     * 配置 ceye.io 平台
     * @param identifier ceye.io 的 identifier（注册后在个人设置中查看）
     * @param token ceye.io 的 API Token
     */
    public static void configureCeye(String identifier, String token) {
        if (identifier != null && !identifier.isEmpty() && token != null && !token.isEmpty()) {
            ceyeIdentifier = identifier;
            ceyeToken = token;
            System.out.println("已配置 ceye.io: " + identifier);
        }
    }
    
    /**
     * 设置当前使用的平台
     * @param platform 平台枚举
     */
    public static void setPlatform(Platform platform) {
        if (platform != null) {
            currentPlatform = platform;
            System.out.println("已切换DNSLog平台至: " + platform.getName());
        }
    }
    
    /**
     * 获取当前使用的平台
     */
    public static Platform getCurrentPlatform() {
        return currentPlatform;
    }
    
    // 记录 dnslog.cn 连续失败次数，用于快速失败（使用 AtomicInteger 保证原子性）
    private static final AtomicInteger dnslogCnFailCount = new AtomicInteger(0);
    private static final int MAX_FAIL_COUNT = 3; // 连续失败 3 次后快速失败
    private static volatile long lastFailTime = 0;
    private static final long FAIL_RESET_INTERVAL = 60000; // 60 秒后重置失败计数
    
    /**
     * 生成一个唯一的DNSLog子域名
     * 根据当前配置的平台自动选择
     * 
     * 注意：移除了 synchronized 以避免并发阻塞
     * 使用 volatile 变量和缓存实现线程安全
     * 
     * @return DNSLog完整域名
     */
    public static String generateDnsLogDomain() {
        try {
            // 快速失败检查：如果 dnslog.cn 连续失败多次，直接返回占位符
            if (dnslogCnFailCount.get() >= MAX_FAIL_COUNT) {
                long now = System.currentTimeMillis();
                if (now - lastFailTime < FAIL_RESET_INTERVAL) {
                    // 仍在失败冷却期，直接返回占位符，不阻塞
                    return generateFallbackDomain();
                } else {
                    // 冷却期过了，重置计数器
                    dnslogCnFailCount.set(0);
                }
            }
            
            // 如果当前使用 ceye.io，先尝试获取
            if (currentPlatform == Platform.CEYE_IO) {
                if (ceyeIdentifier != null && ceyeToken != null) {
                    try {
                        String domain = generateCeyeDomain();
                        if (domain != null && !domain.isEmpty()) {
                            return domain;
                        }
                    } catch (Exception e) {
                        System.err.println("ceye.io 获取域名失败: " + e.getMessage() + "，切换至 dnslog.cn");
                    }
                } else {
                    System.err.println("警告: ceye.io未配置，切换至dnslog.cn");
                }
                // ceye.io 失败，切换到 dnslog.cn
                currentPlatform = Platform.DNSLOG_CN;
            }

            // 使用 dnslog.cn
            return generateDnslogCnDomain();

        } catch (Exception e) {
            System.err.println("生成DNSLog域名失败: " + e.getMessage());
            return generateFallbackDomain();
        }
    }
    
    /**
     * 从 dnslog.cn 生成域名
     */
    private static String generateDnslogCnDomain() {
        try {
            // 如果缓存的域名还在有效期内，直接返回子域名（无需网络请求）
            if (dnslogCnInfo != null && !dnslogCnInfo.isExpired()) {
                String subDomain = generateSubDomain(dnslogCnInfo.getDomain());
                // 缓存子域名和session的关联
                DNSLOG_CACHE.put(subDomain, dnslogCnInfo);
                // 成功使用缓存，重置失败计数
                dnslogCnFailCount.set(0);
                return subDomain;
            }
            
            // 尝试获取新域名和session
            DnsLogInfo newInfo = getDomainFromDnslogCn();
            if (newInfo != null && newInfo.getDomain() != null) {
                dnslogCnInfo = newInfo;
                String subDomain = generateSubDomain(newInfo.getDomain());
                // 缓存子域名和session的关联
                DNSLOG_CACHE.put(subDomain, newInfo);
                // 成功获取，重置失败计数
                dnslogCnFailCount.set(0);
                return subDomain;
            }
            
            // 如果获取失败，记录失败并返回占位符
            recordFailure();
            System.err.println("警告: 无法从dnslog.cn获取域名，使用占位符 (失败次数: " + dnslogCnFailCount.get() + ")");
            return generateFallbackDomain();
            
        } catch (Exception e) {
            recordFailure();
            System.err.println("从dnslog.cn生成域名失败: " + e.getMessage() + " (失败次数: " + dnslogCnFailCount.get() + ")");
            return generateFallbackDomain();
        }
    }
    
    /**
     * 记录失败
     */
    private static void recordFailure() {
        dnslogCnFailCount.incrementAndGet();
        lastFailTime = System.currentTimeMillis();
    }
    
    /**
     * 从 ceye.io 生成域名
     */
    private static String generateCeyeDomain() {
        if (ceyeIdentifier == null || ceyeToken == null) {
            System.err.println("警告: ceye.io未配置");
            return generateFallbackDomain();
        }
        
        // ceye.io的域名格式: {random}.{identifier}.ceye.io
        String uniqueId = StrUtils.generateRandomString(8, 8).toLowerCase();
        return uniqueId + "." + ceyeIdentifier + ".ceye.io";
    }
    
    /**
     * 从 dnslog.cn 获取临时域名和session
     * @return DnsLogInfo对象（包含域名和session）
     */
    private static DnsLogInfo getDomainFromDnslogCn() {
        try {
            String url = "http://www.dnslog.cn/getdomain.php";

            RequestObj requestObj = new RequestObj()
                    .setUrl(url)
                    .setMethod("GET")
                    .setTimeOut(5)
                    .setReadTimeout(5);

            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    String domain = response.getTextStr();

                    if (domain != null && !domain.trim().isEmpty()) {
                        domain = domain.trim();

                        // 验证域名格式
                        if (domain.matches(".*\\.dnslog\\.(cn|io)$")) {
                            // 从响应中提取 session（PHPSESSID）
                            String sessionId = extractSessionFromResponse(response);

                            System.out.println("✓ 成功获取DNSLog域名: " + domain);
                            return new DnsLogInfo(domain, sessionId, Platform.DNSLOG_CN);
                        }
                    }
                }
            }

            System.err.println("警告: 无法从dnslog.cn获取域名");

        } catch (Exception e) {
            System.err.println("从dnslog.cn获取域名失败: " + e.getMessage());
        }

        return null;
    }
    
    /**
     * 从HTTP响应中提取 Session ID
     * @param response HTTP响应对象
     * @return Session ID（PHPSESSID）
     */
    private static String extractSessionFromResponse(CustomHttpResponse response) {
        try {
            // 从 Set-Cookie header 中提取 PHPSESSID
            List<String> setCookieHeaders = response.getHeaderField("Set-Cookie");
            if (setCookieHeaders != null) {
                for (String setCookie : setCookieHeaders) {
                    if (setCookie != null && setCookie.contains("PHPSESSID=")) {
                        Pattern pattern = Pattern.compile("PHPSESSID=([^;\\s]+)");
                        Matcher matcher = pattern.matcher(setCookie);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("提取Session失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 生成带有唯一标识的子域名
     * @param baseDomain 基础域名
     * @return 完整的子域名
     */
    private static String generateSubDomain(String baseDomain) {
        String uniqueId = StrUtils.generateRandomString(8, 8).toLowerCase();
        return uniqueId + "." + baseDomain;
    }
    
    /**
     * 生成回退域名（当无法连接到DNSLog平台时）
     * @return 占位符域名
     */
    private static String generateFallbackDomain() {
        String uniqueId = StrUtils.generateRandomString(12, 12).toLowerCase();
        return uniqueId + ".dnslog.example.com";
    }
    
    /**
     * 查询指定DNSLog域名的解析记录
     * 自动识别平台并调用对应的查询方法
     * @param domain DNSLog域名
     * @return 解析记录（JSON格式），如果无记录则返回null
     */
    public static String queryDnsLogRecords(String domain) {
        if (mockMode) {
            // 模拟模式下，假设只要查询就返回成功记录
            // 返回符合 dnslog.cn 或 ceye.io 格式的 JSON 记录
            long now = System.currentTimeMillis() / 1000;
            return String.format("[{\"domain\":\"%s\",\"ip\":\"127.0.0.1\",\"time\":\"%d\"}]", domain, now);
        }

        if (domain == null || domain.isEmpty()) {
            return null;
        }
        
        // 判断是哪个平台的域名
        if (domain.contains("ceye.io")) {
            return queryCeyeRecords(domain);
        } else if (domain.contains("dnslog.cn") || domain.contains("dnslog.io")) {
            return queryDnslogCnRecords(domain);
        }
        
        return null;
    }
    
    /**
     * 查询 dnslog.cn 的DNS记录
     */
    private static String queryDnslogCnRecords(String domain) {
        try {
            // 从缓存中查找对应的 DnsLogInfo
            DnsLogInfo info = DNSLOG_CACHE.get(domain);
            if (info == null) {
                // 如果缓存中没有，尝试使用当前的 dnslogCnInfo
                info = dnslogCnInfo;
            }
            
            RequestObj requestObj = new RequestObj()
                    .setUrl("http://www.dnslog.cn/getrecords.php")
                    .setMethod("GET")
                    .setTimeOut(5)
                    .setReadTimeout(5);
            
            // 如果有 session，添加 Cookie 到 headers
            if (info != null && info.getSessionId() != null) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", "PHPSESSID=" + info.getSessionId());
                requestObj.setHeaders(headers);
            }
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    String records = response.getTextStr();
                    if (records != null && !records.trim().isEmpty() && !records.equals("[]")) {
                        // 过滤出与当前域名相关的记录
                        return filterRecordsByDomain(records, domain);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查询dnslog.cn记录失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 过滤DNS记录，只返回与指定域名相关的记录
     * @param records 所有记录（JSON格式）
     * @param domain 要查询的域名
     * @return 过滤后的记录
     */
    private static String filterRecordsByDomain(String records, String domain) {
        try {
            // 简单的字符串包含检查，如果需要更精确的过滤可以使用JSON解析
            if (records.contains(domain)) {
                return records;
            }
        } catch (Exception e) {
            System.err.println("过滤DNS记录失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 查询 ceye.io 的DNS记录
     */
    private static String queryCeyeRecords(String domain) {
        if (ceyeToken == null || ceyeIdentifier == null) {
            System.err.println("ceye.io未配置，无法查询记录");
            return null;
        }
        
        try {
            // 提取子域名作为filter
            String filter = domain.replace("." + ceyeIdentifier + ".ceye.io", "");
            
            String apiUrl = "http://api.ceye.io/v1/records?token=" + ceyeToken + 
                           "&type=dns&filter=" + filter;
            
            RequestObj requestObj = new RequestObj()
                    .setUrl(apiUrl)
                    .setMethod("GET")
                    .setTimeOut(5)
                    .setReadTimeout(5);
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                if (response.getResponseCode() == 200) {
                    return response.getTextStr();
                }
            }
        } catch (Exception e) {
            System.err.println("查询ceye.io记录失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 清除DNSLog缓存
     */
    public static synchronized void clearCache() {
        dnslogCnInfo = null;
        DNSLOG_CACHE.clear();
        System.out.println("DNSLog缓存已清除");
    }
    
    /**
     * 检查DNSLog域名是否被解析
     * @param domain DNSLog域名
     * @return true表示有解析记录
     */
    public static boolean hasDnsResolution(String domain) {
        String records = queryDnsLogRecords(domain);
        return records != null && !records.trim().isEmpty() && !records.equals("[]");
    }
    
    // ==================== 连通性测试方法 ====================
    
    /**
     * 测试 dnslog.cn 平台连通性
     * @return 测试结果对象
     */
    public static TestResult testDnslogCnConnectivity() {
        TestResult result = new TestResult(Platform.DNSLOG_CN);
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 测试获取域名和session
            DnsLogInfo info = getDomainFromDnslogCn();
            if (info == null || info.getDomain() == null) {
                result.success = false;
                result.message = "无法从dnslog.cn获取域名";
                result.responseTime = System.currentTimeMillis() - startTime;
                return result;
            }
            
            result.domain = info.getDomain();
            
            // 2. 测试查询记录（应该返回空或成功响应）
            queryDnslogCnRecords(info.getDomain());
            // 能查询到结果或返回空数组都算成功
            
            result.success = true;
            result.message = "连接成功，域名: " + info.getDomain() + 
                           (info.getSessionId() != null ? ", Session已保存" : ", 未获取到Session");
            result.responseTime = System.currentTimeMillis() - startTime;
            
        } catch (Exception e) {
            result.success = false;
            result.message = "连接失败: " + e.getMessage();
            result.responseTime = System.currentTimeMillis() - startTime;
        }
        
        return result;
    }
    
    /**
     * 测试 ceye.io 平台连通性
     * @return 测试结果对象
     */
    public static TestResult testCeyeIoConnectivity() {
        TestResult result = new TestResult(Platform.CEYE_IO);
        long startTime = System.currentTimeMillis();
        
        if (ceyeIdentifier == null || ceyeToken == null) {
            result.success = false;
            result.message = "ceye.io 未配置（需要配置 identifier 和 token）";
            result.responseTime = 0;
            return result;
        }
        
        try {
            // 生成测试域名
            String testDomain = generateCeyeDomain();
            result.domain = testDomain;
            
            // 测试查询API
            String apiUrl = "http://api.ceye.io/v1/records?token=" + ceyeToken + "&type=dns";
            
            RequestObj requestObj = new RequestObj()
                    .setUrl(apiUrl)
                    .setMethod("GET")
                    .setTimeOut(5)
                    .setReadTimeout(5);
            
            try (CustomHttpResponse response = RequestUtils.requests(requestObj, null)) {
                int responseCode = response.getResponseCode();
                if (responseCode == 200) {
                    result.success = true;
                    result.message = "连接成功，identifier: " + ceyeIdentifier;
                } else if (responseCode == 401) {
                    result.success = false;
                    result.message = "认证失败，请检查 token 是否正确";
                } else {
                    result.success = false;
                    result.message = "API 返回错误码: " + responseCode;
                }
            }
            
            result.responseTime = System.currentTimeMillis() - startTime;
            
        } catch (Exception e) {
            result.success = false;
            result.message = "连接失败: " + e.getMessage();
            result.responseTime = System.currentTimeMillis() - startTime;
        }
        
        return result;
    }
    
    /**
     * 测试所有平台的连通性
     * @return 测试结果数组
     */
    public static TestResult[] testAllPlatforms() {
        return new TestResult[] {
            testDnslogCnConnectivity(),
            testCeyeIoConnectivity()
        };
    }
    
    /**
     * 测试结果类
     */
    public static class TestResult {
        public Platform platform;
        public boolean success;
        public String message;
        public String domain;
        public long responseTime; // 毫秒
        
        public TestResult(Platform platform) {
            this.platform = platform;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s - %s (耗时: %dms, 域名: %s)", 
                platform.getName(),
                success ? "✓ 成功" : "✗ 失败",
                message,
                responseTime,
                domain != null ? domain : "N/A"
            );
        }
    }
}

