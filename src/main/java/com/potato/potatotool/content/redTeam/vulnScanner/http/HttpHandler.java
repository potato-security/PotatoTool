package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.utils.network.RequestObj;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP请求处理类，负责解析和处理HTTP请求
 */
public class HttpHandler {

    /**
     * 处理原始HTTP请求（支持 Object 类型的变量）
     * @param requestObj 请求对象
     * @param rawBlocks 原始请求块列表（每个元素可能是多行文本块）
     * @param target 目标URL
     * @param variables 变量映射
     */
    public static void processRawRequestObj(RequestObj requestObj, List<String> rawBlocks, String target, Map<String, Object> variables) {
        // 将 Object map 转换为 String map
        Map<String, String> stringVariables = convertToStringMap(variables);
        processRawRequest(requestObj, rawBlocks, target, stringVariables);
    }

    /**
     * 处理原始HTTP请求
     * @param requestObj 请求对象
     * @param rawBlocks 原始请求块列表（每个元素可能是多行文本块）
     * @param target 目标URL
     * @param variables 变量映射
     */
    public static void processRawRequest(RequestObj requestObj, List<String> rawBlocks, String target, Map<String, String> variables) {
        if (requestObj == null || rawBlocks == null || rawBlocks.isEmpty()) {
            return;
        }

        // 注意：Nuclei 的 raw 字段可能包含多个请求块，每个块代表一个独立的请求
        // 多个块在 NucleiPocConverter.processHttpRequests() 中已被拆分为多个 PocStep
        // 因此这里应该只有一个块
        String firstBlock = rawBlocks.get(0);
        if (firstBlock == null || firstBlock.isEmpty()) {
            return;
        }

        // 验证：如果仍有多个块，说明转换器有问题
        if (rawBlocks.size() > 1) {
            System.err.println("[错误] raw 字段仍包含多个请求块，转换器未正确拆分！将只使用第一个块。");
        }

        // 将文本块拆分成单行列表
        // firstBlock 格式："POST /path HTTP/1.1\nHost: xxx\n\nbody"
        // 需要转换为：["POST /path HTTP/1.1", "Host: xxx", "", "body"]
        List<String> rawLines = new ArrayList<>();
        String[] lines = firstBlock.split("\\r\\n|\\r|\\n");
        for (String line : lines) {
            // 即使是空行也添加（空行用于分隔 headers 和 body）
            rawLines.add(line);
        }
        
        if (rawLines.isEmpty()) {
            return;
        }

        // 处理以@开头的特殊配置行（如 @timeout: 10s）
        int startIndex = 0;
        while (startIndex < rawLines.size() && rawLines.get(startIndex).trim().startsWith("@")) {
            String configLine = rawLines.get(startIndex).trim();
            // 解析并应用配置
            parseAndApplyConfig(requestObj, configLine, variables);
            startIndex++;
        }
        
        if (startIndex >= rawLines.size()) {
            return;
        }

        // 解析第一行以获取请求方法和路径
        String firstLine = rawLines.get(startIndex);
        String[] parts = firstLine.split("\\s+");
        if (parts.length >= 2) {
            // 设置请求方法
            try {
                requestObj.setMethod(parts[0]);
            } catch (IllegalArgumentException e) {
                System.err.println("[错误] 无效的请求方法: " + parts[0] + " - " + e.getMessage());
                // 使用默认方法
                requestObj.setMethod("GET");
            }
            
            // 设置请求路径（可能需要替换变量）
            String path = parts[1];
            if (variables != null && !variables.isEmpty()) {
                path = replaceVariables(path, variables);
            }
            
            String fullUrl;
            
            // 判断是否为完整URL（代理请求或SSRF测试）
            if (path.toLowerCase().startsWith("http://") || path.toLowerCase().startsWith("https://")) {
                try {
                    URL pathUrl = new URL(path);
                    URL targetUrl = new URL(target);
                    
                    // 比较 host 是否相同
                    if (pathUrl.getHost().equalsIgnoreCase(targetUrl.getHost())) {
                        // 同一个 host，提取路径部分并组合
                        String extractedPath = pathUrl.getPath();
                        if (pathUrl.getQuery() != null && !pathUrl.getQuery().isEmpty()) {
                            extractedPath += "?" + pathUrl.getQuery();
                        }
                        fullUrl = buildUrl(target, extractedPath);
                    } else {
                        // 不同 host，这是代理请求或 SSRF 测试，保持完整 URL
                        fullUrl = path;
                    }
                } catch (MalformedURLException e) {
                    // URL 解析失败，尝试作为路径处理
                    System.err.println("[警告] URL 解析失败，作为路径处理: " + path);
                    fullUrl = buildUrl(target, path);
                }
            } else {
                // 普通路径，组合 target 和 path
                fullUrl = buildUrl(target, path);
            }
            
            requestObj.setUrl(fullUrl);
        }

        Map<String, String> headers = new HashMap<>();
        StringBuilder body = new StringBuilder();
        boolean isBody = false;

        // 解析其余行以获取请求头和请求体（从 startIndex + 1 开始）
        for (int i = startIndex + 1; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            
            // 处理以@开头的特殊配置行（继续应用配置）
            if (line.trim().startsWith("@")) {
                parseAndApplyConfig(requestObj, line.trim(), variables);
                continue;
            }
            
            // 空行标志着请求头的结束和请求体的开始
            if (line.trim().isEmpty()) {
                isBody = true;
                continue;
            }
            
            if (!isBody) {
                // 解析请求头
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex).trim();
                    String headerValue = line.substring(colonIndex + 1).trim();
                    
                    // 替换变量
                    if (variables != null && !variables.isEmpty()) {
                        headerValue = replaceVariables(headerValue, variables);
                    }
                    
                    headers.put(headerName, headerValue);
                }
            } else {
                // 累积请求体
                body.append(line).append("\n");
            }
        }

        // 合并请求头：默认Headers < 自定义Headers < POC Headers
        // 使用HeaderManager进行headers合并
        Map<String, String> mergedHeaders = mergeWithDefaultHeaders(headers);
        requestObj.setHeaders(mergedHeaders);
        
        String bodyContent = body.toString().trim();
        if (!bodyContent.isEmpty()) {
            // 替换请求体中的变量
            if (variables != null && !variables.isEmpty()) {
                bodyContent = replaceVariables(bodyContent, variables);
            }
            requestObj.setPostData(bodyContent);
        }

        // postMethod 统一在 RequestUtils.setRequestBody 阶段决策，避免业务层重复覆盖
    }

    /**
     * 替换字符串中的变量引用（支持 Object 类型的变量）
     * @param input 输入字符串
     * @param variables 变量映射
     * @return 替换后的字符串
     */
    public static String replaceVariablesObj(String input, Map<String, Object> variables) {
        Map<String, String> stringVariables = convertToStringMap(variables);
        return replaceVariables(input, stringVariables);
    }

    /**
     * 将 Object map 转换为 String map
     */
    private static Map<String, String> convertToStringMap(Map<String, Object> variables) {
        Map<String, String> result = new HashMap<>();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (entry.getValue() != null) {
                    result.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        return result;
    }

    /**
     * 替换字符串中的变量引用
     * 支持嵌套变量解析（如 FILENAME: "{{FQDN}}"）
     *
     * @param input 输入字符串
     * @param variables 变量映射
     * @return 替换后的字符串
     */
    public static String replaceVariables(String input, Map<String, String> variables) {
        if (input == null) {
            return input;
        }

        String result = input;
        
        // ========== 懒加载处理 ==========
        // 1. Interactsh URL：仅在当前变量上下文内复用，避免全局串扰
        if (result.contains("{{interactsh-url}}") || result.contains("{{LAZY_INTERACTSH}}")) {
            String interactshUrl = variables == null ? null : variables.get("interactsh-url");
            if (interactshUrl == null || interactshUrl.trim().isEmpty()) {
                interactshUrl = getOrCreateInteractshUrl();
                if (variables != null) {
                    variables.put("interactsh-url", interactshUrl);
                }
            }
            result = result.replace("{{LAZY_INTERACTSH}}", interactshUrl);
        }
        
        // 2. IP 地址：只有实际使用时才 DNS 解析
        if (result.contains("{{LAZY_IP:")) {
            result = resolveLazyIp(result, variables);
        }

        // 先处理 Goby 特殊函数（@@函数）
        // 这一步需要在变量替换之前进行，因为函数参数可能引用变量
        result = GobyFunctionProcessor.processGobyFunctions(result, variables);

        // 多轮替换，支持嵌套变量解析（如 {{FILENAME}} → "{{FQDN}}" → "example.com"）
        if (variables != null && !variables.isEmpty()) {
            int maxIterations = 10; // 最大替换轮数，防止循环引用导致死循环
            boolean hasChanges = true;
            int iteration = 0;

            while (hasChanges && iteration < maxIterations) {
                hasChanges = false;
                String previousResult = result;

                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    String varName = entry.getKey();
                    String varValue = entry.getValue();

                    if (varName != null && varValue != null) {
                        // 变量值本身可能也包含 @@ 函数，需要先处理
                        String processedValue = GobyFunctionProcessor.processGobyFunctions(varValue, variables);
                        String placeholder = "{{" + varName + "}}";

                        // 只有当字符串包含该变量时才进行替换
                        if (result.contains(placeholder)) {
                            result = result.replace(placeholder, processedValue);
                            hasChanges = true;
                        }
                    }
                }

                iteration++;

                // 检测循环引用：如果有变化但结果相同，说明存在循环引用
                if (hasChanges && result.equals(previousResult)) {
                    System.err.println("[警告] 检测到变量循环引用，停止替换");
                    break;
                }
            }

            // 如果达到最大轮数仍有未替换的变量，输出警告
            if (iteration >= maxIterations && result.contains("{{") && result.contains("}}")) {
                System.err.println("[警告] 变量替换达到最大轮数（" + maxIterations + "），可能存在循环引用或嵌套层级过深");
            }
        }

        return result;
    }

    /**
     * 处理特殊变量
     * @param requestObj 请求对象
     * @param specialLine 特殊变量行
     */
    public void processSpecialVariable(RequestObj requestObj, String specialLine) {
        if (requestObj == null || specialLine == null || specialLine.isEmpty()) {
            return;
        }

        // 分割特殊变量行，格式如：@timeout=10 @proxy=127.0.0.1:8080 @followRedirect=true
        String[] variables = specialLine.split("\\s+");

        for (String variable : variables) {
            if (!variable.startsWith("@")) {
                continue;
            }

            // 去掉@前缀
            String varWithoutPrefix = variable.substring(1);

            // 分割变量名和值
            String[] parts = varWithoutPrefix.split("=", 2);
            if (parts.length != 2) {
                System.err.println("特殊变量格式错误: " + variable + "，应为 @name=value 格式");
                continue;
            }

            String name = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            // 根据变量名设置请求对象的属性
            switch (name) {
                case "timeout":
                    try {
                        int timeout = Integer.parseInt(value);
                        if (timeout > 0) {
                            requestObj.setTimeOut(timeout);
                        } else {
                            System.err.println("超时值必须大于0: " + value);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("无效的超时值: " + value);
                    }
                    break;

                case "proxy":
                    requestObj.setProxies(value);
                    break;

                case "followredirect":
                case "followredirects":
                    requestObj.setFollowRedirects(Boolean.parseBoolean(value));
                    break;

                case "retries":
                    try {
                        int retries = Integer.parseInt(value);
                        requestObj.setRetries(retries);
                    } catch (NumberFormatException e) {
                        System.err.println("无效的重试次数: " + value);
                    }
                    break;

                case "retrywaittime":
                    try {
                        int retryWaitTime = Integer.parseInt(value);
                        requestObj.setRetryWaitTime(retryWaitTime);
                    } catch (NumberFormatException e) {
                        System.err.println("无效的重试等待时间: " + value);
                    }
                    break;

                case "randomuseragent":
                    requestObj.setRandomUserAgent(Boolean.parseBoolean(value));
                    break;

                case "nouseragent":
                    requestObj.setNoUserAgent(Boolean.parseBoolean(value));
                    break;

                case "proxiestype":
                    try {
                        requestObj.setProxiesType(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.err.println(e.getMessage());
                    }
                    break;

                default:
                    System.out.println("未知的特殊变量: " + name + "=" + value);
                    break;
            }
        }
    }
    
    /**
     * 解析并应用配置行（如 @timeout: 10s 或 @timeout 10s）
     * @param requestObj 请求对象
     * @param configLine 配置行（如 "@timeout: 10s" 或 "@timeout 10s"）
     * @param variables 变量映射
     */
    private static void parseAndApplyConfig(RequestObj requestObj, String configLine, Map<String, String> variables) {
        if (configLine == null || !configLine.startsWith("@")) {
            return;
        }
        
        // 移除 @ 前缀
        configLine = configLine.substring(1).trim();
        
        // 分割配置名和值（支持 :、= 或 空格 分隔符）
        String[] parts;
        if (configLine.contains(":")) {
            // 优先使用冒号分隔（如 @timeout: 10s）
            parts = configLine.split(":", 2);
        } else if (configLine.contains("=")) {
            // 其次使用等号分隔（如 @timeout=10s）
            parts = configLine.split("=", 2);
        } else {
            // 最后使用空格分隔（如 @timeout 10s）
            parts = configLine.split("\\s+", 2);
        }
        
        if (parts.length != 2) {
            return;
        }
        
        String name = parts[0].trim().toLowerCase();
        String value = parts[1].trim();
        
        // 替换变量
        if (variables != null && !variables.isEmpty()) {
            value = replaceVariables(value, variables);
        }
        
        // 应用配置
        try {
            switch (name) {
                case "timeout":
                    // 解析超时时间（支持 10s, 10ms 等格式）
                    int timeout = parseTimeValue(value);
                    if (timeout > 0) {
                        requestObj.setTimeOut(timeout);
                    }
                    break;
                    
                case "redirects":
                case "redirect":
                case "followredirect":
                case "followredirects":
                    // 跟随重定向
                    requestObj.setFollowRedirects(parseBooleanValue(value));
                    break;
                    
                case "proxy":
                    // 代理设置
                    if (!value.isEmpty()) {
                        requestObj.setProxies(value);
                    }
                    break;
                    
                case "retries":
                case "retry":
                    // 重试次数
                    try {
                        int retries = Integer.parseInt(value);
                        if (retries > 0) {
                            requestObj.setRetries(retries);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                    break;
                    
                case "useragent":
                case "user-agent":
                    // 设置 User-Agent
                    if (requestObj.getHeaders() == null) {
                        requestObj.setHeaders(new HashMap<>());
                    }
                    requestObj.getHeaders().put("User-Agent", value);
                    break;
                    
                case "host":
                    // 设置 Host 头（用于 HTTP/1.0 请求）
                    if (requestObj.getHeaders() == null) {
                        requestObj.setHeaders(new HashMap<>());
                    }
                    requestObj.getHeaders().put("Host", value);
                    break;
                    
                case "tls-sni":
                case "tlssni":
                case "sni":
                    // TLS SNI 配置（用于 SSRF 检测）
                    // 设置自定义 SNI 主机名，在 TLS 握手时使用
                    if (!value.isEmpty()) {
                        requestObj.setTlsSni(value);
                    }
                    break;
                    
                default:
                    // 未知配置，记录日志但不报错
                    System.out.println("[DEBUG] 未知的配置项: @" + name + ": " + value);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 解析配置失败 @" + name + ": " + value + " - " + e.getMessage());
        }
    }
    
    /**
     * 解析时间值（支持 10s, 100ms, 10 等格式）
     * @param value 时间字符串
     * @return 秒数
     */
    private static int parseTimeValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        
        value = value.trim().toLowerCase();
        
        try {
            // 解析带单位的时间值
            if (value.endsWith("ms")) {
                // 毫秒转秒
                int ms = Integer.parseInt(value.substring(0, value.length() - 2).trim());
                return Math.max(1, ms / 1000);
            } else if (value.endsWith("s")) {
                // 秒
                return Integer.parseInt(value.substring(0, value.length() - 1).trim());
            } else if (value.endsWith("m")) {
                // 分钟转秒
                return Integer.parseInt(value.substring(0, value.length() - 1).trim()) * 60;
            } else {
                // 默认为秒
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            System.err.println("[WARN] 无法解析时间值: " + value);
            return 0;
        }
    }
    
    /**
     * 解析布尔值（支持 true/false, yes/no, 1/0 等格式）
     * @param value 布尔字符串
     * @return 布尔值
     */
    private static boolean parseBooleanValue(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        value = value.trim().toLowerCase();
        return value.equals("true") || value.equals("yes") || value.equals("1") || value.equals("on");
    }
    
    /**
     * 构建完整URL
     * @param target 目标URL（如 https://example.com）
     * @param path 请求路径（如 /api/test）
     * @return 完整的URL
     */
    private static String buildUrl(String target, String path) {
        if (path == null || path.isEmpty()) {
            return target;
        }
        
        // 如果路径已经是完整URL，直接返回
        if (path.toLowerCase().startsWith("http://") || path.toLowerCase().startsWith("https://")) {
            return path;
        }
        
        // 规范化 target 和 path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (target.endsWith("/")) {
            target = target.substring(0, target.length() - 1);
        }
        
        return target + path;
    }
    
    /**
     * 合并请求头与默认Headers
     * 优先级：defaultHeaders < configHeaders < pocHeaders
     *
     * @param pocHeaders POC中定义的Headers
     * @return 合并后的Headers
     */
    private static Map<String, String> mergeWithDefaultHeaders(Map<String, String> pocHeaders) {
        // 使用HeaderManager进行合并
        return HeaderManager.getInstance().mergeHeaders(pocHeaders);
    }
    
    /**
     * 检查并应用代理设置
     * 结合全局代理和VulnScan代理开关
     * 
     * @param requestObj 请求对象
     */
    public static void applyProxySettings(RequestObj requestObj) {
        if (requestObj == null) {
            return;
        }
        
        VulnScanConfig config = VulnScanConfig.getInstance();
        
        // 检查是否启用代理
        if (config.isProxyEnabled()) {
            String proxy = config.getProxyAddress();
            if (proxy != null && !proxy.isEmpty()) {
                requestObj.setProxies(proxy);
                // 默认使用HTTP代理类型
                requestObj.setProxiesType("HTTP");
            }
        }
    }
    
    /**
     * 创建带有默认配置的请求对象
     * 自动应用默认Headers和代理设置
     * 
     * @return 配置好的RequestObj
     */
    public static RequestObj createConfiguredRequest() {
        RequestObj requestObj = new RequestObj();
        
        // 设置默认Headers
        Map<String, String> defaultHeaders = HeaderManager.getInstance().getCustomHeaders();
        requestObj.setHeaders(defaultHeaders);
        
        // 应用代理设置
        applyProxySettings(requestObj);
        
        // 设置超时和重试
        VulnScanConfig config = VulnScanConfig.getInstance();
        requestObj.setTimeOut(config.getDefaultTimeout());
        requestObj.setRetries(config.getDefaultRetries());
        
        return requestObj;
    }
    
    // ========== 懒加载缓存 ==========
    private static final Pattern LAZY_IP_PATTERN = Pattern.compile("\\{\\{LAZY_IP:([^}]+)\\}\\}");
    private static final Map<String, String> ipCache = new HashMap<>();

    /**
     * 获取 Interactsh URL
     * 每次按需生成，避免不同扫描上下文共享全局 URL
     */
    private static String getOrCreateInteractshUrl() {
        try {
            String interactshUrl = HttpLogService.generateHttpLogUrl();
            if (interactshUrl == null || interactshUrl.trim().isEmpty()) {
                return "http://placeholder.oast.invalid";
            }
            return interactshUrl;
        } catch (Exception e) {
            System.err.println("[Interactsh] 初始化失败: " + e.getMessage());
            return "http://placeholder.oast.invalid";
        }
    }
    
    /**
     * 懒加载解析 IP 地址
     * 处理 {{LAZY_IP:hostname}} 格式的占位符
     */
    private static String resolveLazyIp(String input, Map<String, String> variables) {
        Matcher matcher = LAZY_IP_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String hostname = matcher.group(1);
            String ip = resolveIp(hostname);
            matcher.appendReplacement(sb, ip);
            
            // 更新变量映射
            if (variables != null) {
                variables.put("ip", ip);
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * DNS 解析（带缓存）
     */
    private static synchronized String resolveIp(String hostname) {
        // 先查缓存
        if (ipCache.containsKey(hostname)) {
            return ipCache.get(hostname);
        }
        
        try {
            InetAddress address = InetAddress.getByName(hostname);
            String ip = address.getHostAddress();
            ipCache.put(hostname, ip);
            return ip;
        } catch (UnknownHostException e) {
            // DNS 解析失败，返回主机名
            ipCache.put(hostname, hostname);
            return hostname;
        }
    }
} 