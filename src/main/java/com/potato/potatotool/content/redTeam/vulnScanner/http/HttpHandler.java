package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.utils.network.HeaderManager;
import com.potato.potatotool.utils.network.RequestObj;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
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

    private static final Pattern NUCLEI_EXPRESSION_PATTERN =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][\\w]*)\\s*\\((.*?)\\)\\s*\\}\\}");
    private static final Pattern RAW_CONFIG_LINE_PATTERN =
            Pattern.compile("^@(?i:(timeout|redirects?|followredirects?|proxy|retries?|retrywaittime|useragent|user-agent|host|tls-sni|tlssni|sni))(?:\\s*[:=\\s].*)?$");

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

        Map<String, String> configState = new HashMap<String, String>();
        List<String> effectiveRawLines = new ArrayList<String>();

        // 处理以@开头的特殊配置行（如 @timeout: 10s）
        int startIndex = 0;
        while (startIndex < rawLines.size() && isRawConfigLine(rawLines.get(startIndex))) {
            String configLine = rawLines.get(startIndex).trim();
            // 解析并应用配置
            parseAndApplyConfig(requestObj, configLine, variables, configState);
            startIndex++;
        }

        if (startIndex >= rawLines.size()) {
            return;
        }

        for (int i = startIndex; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            if (isRawConfigLine(line)) {
                parseAndApplyConfig(requestObj, line.trim(), variables, configState);
                continue;
            }
            effectiveRawLines.add(line);
        }

        if (effectiveRawLines.isEmpty()) {
            return;
        }

        String sanitizedRawBlock = joinRawLines(effectiveRawLines, 0);
        RawHttpRequestParser.ParsedRequest parsedRequest = RawHttpRequestParser.parse(sanitizedRawBlock);

        try {
            requestObj.setMethod(parsedRequest.getMethod());
        } catch (IllegalArgumentException e) {
            System.err.println("[错误] 无效的请求方法: " + parsedRequest.getMethod() + " - " + e.getMessage());
            requestObj.setMethod("GET");
        }

        String path = parsedRequest.getPath();
        if (variables != null && !variables.isEmpty() && path != null) {
            path = replaceVariables(path, variables);
        }

        String fullUrl;
        if (path == null || path.trim().isEmpty()) {
            fullUrl = target;
        } else if (path.toLowerCase().startsWith("http://") || path.toLowerCase().startsWith("https://")) {
            try {
                URL pathUrl = new URL(path);
                URL targetUrl = new URL(target);

                if (pathUrl.getHost().equalsIgnoreCase(targetUrl.getHost())) {
                    String extractedPath = pathUrl.getPath();
                    if (pathUrl.getQuery() != null && !pathUrl.getQuery().isEmpty()) {
                        extractedPath += "?" + pathUrl.getQuery();
                    }
                    fullUrl = buildUrl(target, extractedPath);
                } else {
                    fullUrl = path;
                }
            } catch (MalformedURLException e) {
                System.err.println("[警告] URL 解析失败，作为路径处理: " + path);
                fullUrl = buildUrl(target, path);
            }
        } else {
            fullUrl = buildUrl(target, path);
        }

        requestObj.setUrl(fullUrl);

        String hostOverride = configState.get("host");
        if (hostOverride != null && !hostOverride.trim().isEmpty()) {
            requestObj.setUrl(applyHostOverride(fullUrl, hostOverride));
        }

        Map<String, String> headers = new HashMap<String, String>();
        if (parsedRequest.getHeaders() != null) {
            for (Map.Entry<String, String> entry : parsedRequest.getHeaders().entrySet()) {
                String headerName = entry.getKey();
                String headerValue = entry.getValue();
                if (headerName == null || headerValue == null) {
                    continue;
                }
                if (variables != null && !variables.isEmpty()) {
                    headerValue = replaceVariables(headerValue, variables);
                }
                headers.put(headerName, headerValue);
            }
        }

        String tlsSniValue = configState.get("tls-sni");
        if (tlsSniValue != null && !tlsSniValue.trim().isEmpty()) {
            requestObj.setTlsSni(resolveTlsSniHost(tlsSniValue, requestObj.getUrl(), variables));
        }

        // 合并请求头：默认Headers < 自定义Headers < POC Headers
        // 使用HeaderManager进行headers合并
        Map<String, String> mergedHeaders = mergeWithDefaultHeaders(headers);
        requestObj.setHeaders(mergedHeaders);

        String bodyContent = parsedRequest.getBody() == null ? "" : parsedRequest.getBody().trim();
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
        String interactshUrl = resolveInteractshUrl(variables);
        
        // ========== 懒加载处理 ==========
        // 1. Interactsh URL：仅在当前变量上下文内复用，避免全局串扰
        result = materializeInteractshPlaceholders(result, variables);
        
        // 2. IP 地址：只有实际使用时才 DNS 解析
        if (result.contains("{{LAZY_IP:")) {
            result = resolveLazyIp(result, variables);
        }

        // 先处理 Goby 特殊函数（@@函数）
        // 这一步需要在变量替换之前进行，因为函数参数可能引用变量
        result = GobyFunctionProcessor.processGobyFunctions(result, variables);

        result = evaluateNucleiHelperExpressions(result, variables);

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
                        String gobyPlaceholder = "{{{" + varName + "}}}";
                        String placeholder = "{{" + varName + "}}";

                        // 只有当字符串包含该变量时才进行替换
                        if (result.contains(gobyPlaceholder)) {
                            result = result.replace(gobyPlaceholder, processedValue);
                            hasChanges = true;
                        }
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

        result = materializeInteractshPlaceholders(result, variables);
        result = evaluateNucleiHelperExpressions(result, variables);

        return result;
    }

    private static String materializeInteractshPlaceholders(String input, Map<String, String> variables) {
        if (input == null || (!input.contains("{{interactsh-url}}") && !input.contains("{{interactsh_url}}")
                && !input.contains("{{LAZY_INTERACTSH}}") && !input.contains("{{lazy_interactsh}}"))) {
            return input;
        }

        String interactshUrl = resolveInteractshUrl(variables);
        if (interactshUrl == null || interactshUrl.trim().isEmpty() || isLazyInteractshPlaceholder(interactshUrl)) {
            interactshUrl = getOrCreateInteractshHost();
        }
        interactshUrl = HttpLogService.toBareCallbackHost(interactshUrl);
        if (variables != null) {
            variables.put("interactsh-url", interactshUrl);
            variables.put("interactsh_url", interactshUrl);
        }
        return input.replace("{{interactsh-url}}", interactshUrl)
                .replace("{{interactsh_url}}", interactshUrl)
                .replace("{{LAZY_INTERACTSH}}", interactshUrl)
                .replace("{{lazy_interactsh}}", interactshUrl);
    }

    private static String resolveInteractshUrl(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        String interactshUrl = variables.get("interactsh-url");
        if (interactshUrl == null || interactshUrl.trim().isEmpty() || isLazyInteractshPlaceholder(interactshUrl)) {
            interactshUrl = variables.get("interactsh_url");
        }
        return interactshUrl;
    }

    private static boolean isLazyInteractshPlaceholder(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return normalized.toUpperCase(Locale.ROOT).contains("LAZY_INTERACTSH");
    }

    private static String getOrCreateInteractshHost() {
        return HttpLogService.toBareCallbackHost(getOrCreateInteractshUrl());
    }

    private static String evaluateNucleiHelperExpressions(String input, Map<String, String> variables) {
        if (input == null || input.indexOf("{{") < 0 || input.indexOf('(') < 0) {
            return input;
        }

        String result = input;
        int maxIterations = 8;
        for (int i = 0; i < maxIterations; i++) {
            Matcher matcher = NUCLEI_EXPRESSION_PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();
            boolean changed = false;

            while (matcher.find()) {
                String functionName = matcher.group(1);
                String args = matcher.group(2);
                String expression = functionName + "(" + args + ")";
                String evaluated = evaluateNucleiHelperExpression(expression, variables);
                if (evaluated == null) {
                    continue;
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(evaluated));
                changed = true;
            }

            matcher.appendTail(sb);
            result = sb.toString();
            if (!changed) {
                break;
            }
        }
        return result;
    }

    private static String evaluateNucleiHelperExpression(String expression, Map<String, String> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        String functionName = extractFunctionName(expression);
        if (functionName == null) {
            return null;
        }

        Map<String, Object> context = new HashMap<>();
        if (variables != null) {
            context.putAll(variables);
        }

        try {
            List<String> args = splitFunctionArgs(extractFunctionArgs(expression));
            List<String> resolvedArgs = new ArrayList<>();
            for (String arg : args) {
                resolvedArgs.add(resolveHelperArgument(arg, variables, context));
            }

            String lowerName = functionName.toLowerCase(Locale.ROOT);
            if ("url_encode".equals(lowerName) || "urlencode".equals(lowerName)) {
                if (resolvedArgs.isEmpty()) {
                    return null;
                }
                return URLEncoder.encode(resolvedArgs.get(0), "UTF-8");
            }

            String rebuiltExpression = rebuildExpression(functionName, resolvedArgs);
            String value = DslEvaluatorRefactored.evaluateFunctionForValue(rebuiltExpression, context);
            if (value != null) {
                return value;
            }
        } catch (Exception e) {
            System.err.println("[警告] Nuclei helper 表达式计算失败: " + expression + " - " + e.getMessage());
        }

        return null;
    }

    private static String extractFunctionName(String expression) {
        int paren = expression.indexOf('(');
        if (paren <= 0) {
            return null;
        }
        return expression.substring(0, paren).trim();
    }

    private static String extractFunctionArgs(String expression) {
        int start = expression.indexOf('(');
        int end = expression.lastIndexOf(')');
        if (start < 0 || end <= start) {
            return "";
        }
        return expression.substring(start + 1, end);
    }

    private static String rebuildExpression(String functionName, List<String> args) {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append('(');
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(escapeDslString(args.get(i))).append('"');
        }
        sb.append(')');
        return sb.toString();
    }

    private static String escapeDslString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String resolveHelperArgument(String arg, Map<String, String> variables, Map<String, Object> context) {
        if (arg == null) {
            return "";
        }

        String resolved = stripQuotes(arg.trim());
        resolved = replaceVariablesInArgument(resolved, variables);

        if (variables != null && variables.containsKey(resolved)) {
            return variables.get(resolved);
        }
        Object contextValue = context.get(resolved);
        if (contextValue != null) {
            return String.valueOf(contextValue);
        }
        return resolved;
    }

    private static String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String replaceVariablesInArgument(String value, Map<String, String> variables) {
        if (value == null || variables == null || variables.isEmpty() || value.indexOf("{{") < 0) {
            return value;
        }

        String result = value;
        List<Map.Entry<String, String>> entries = new ArrayList<>(variables.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) {
                int aLength = a.getKey() == null ? 0 : a.getKey().length();
                int bLength = b.getKey() == null ? 0 : b.getKey().length();
                return bLength - aLength;
            }
        });

        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return result;
    }

    private static List<String> splitFunctionArgs(String args) {
        List<String> result = new ArrayList<>();
        if (args == null || args.trim().isEmpty()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')' && parenDepth > 0) {
                    parenDepth--;
                } else if (c == ',' && parenDepth == 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }

            current.append(c);
        }

        result.add(current.toString().trim());
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
        parseAndApplyConfig(requestObj, configLine, variables, null);
    }

    private static void parseAndApplyConfig(RequestObj requestObj, String configLine, Map<String, String> variables,
                                            Map<String, String> configState) {
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
                    if (configState != null) {
                        configState.put("host", value);
                    }
                    break;
                    
                case "tls-sni":
                case "tlssni":
                case "sni":
                    // TLS SNI 配置（用于 SSRF 检测）
                    // 设置自定义 SNI 主机名，在 TLS 握手时使用
                    if (!value.isEmpty()) {
                        if (configState != null) {
                            configState.put("tls-sni", value);
                        }
                        requestObj.setTlsSni(resolveTlsSniValue(value, requestObj.getUrl(), variables));
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

    private static boolean isRawConfigLine(String line) {
        if (line == null) {
            return false;
        }
        return RAW_CONFIG_LINE_PATTERN.matcher(line.trim()).matches();
    }

    private static String joinRawLines(List<String> rawLines, int startIndex) {
        if (rawLines == null || rawLines.isEmpty() || startIndex >= rawLines.size()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < rawLines.size(); i++) {
            if (i > startIndex) {
                builder.append("\r\n");
            }
            builder.append(rawLines.get(i));
        }
        return builder.toString();
    }

    private static String applyHostOverride(String originalUrl, String hostOverride) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            return originalUrl;
        }
        if (hostOverride == null || hostOverride.trim().isEmpty()) {
            return originalUrl;
        }

        try {
            URL original = new URL(originalUrl);
            String normalizedOverride = hostOverride.trim();
            boolean protocolSpecified = normalizedOverride.contains("://");
            URL overrideUrl = protocolSpecified
                    ? new URL(normalizedOverride)
                    : new URL(original.getProtocol() + "://" + normalizedOverride);

            String protocol = protocolSpecified ? overrideUrl.getProtocol() : original.getProtocol();
            String host = overrideUrl.getHost();
            int port;
            if (overrideUrl.getPort() > 0) {
                port = overrideUrl.getPort();
            } else if (protocolSpecified) {
                port = overrideUrl.getDefaultPort();
            } else if (original.getPort() > 0) {
                port = original.getPort();
            } else {
                port = original.getDefaultPort();
            }
            String file = original.getFile();
            if (file == null || file.isEmpty()) {
                file = "/";
            }

            StringBuilder rebuilt = new StringBuilder();
            rebuilt.append(protocol).append("://").append(host);
            if (overrideUrl.getPort() > 0
                    || (!protocolSpecified && original.getPort() > 0)) {
                rebuilt.append(":").append(port);
            }
            rebuilt.append(file);
            return rebuilt.toString();
        } catch (Exception ignored) {
            return originalUrl;
        }
    }

    private static String resolveTlsSniHost(String value, String targetUrl, Map<String, String> variables) {
        String resolved = resolveTlsSniValue(value, targetUrl, variables);
        if (resolved == null || resolved.trim().isEmpty()) {
            return null;
        }
        return resolved;
    }

    private static String resolveTlsSniValue(String value, String targetUrl, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return new URL(trimmed).getHost();
            }
            if (trimmed.contains("://")) {
                return new URL(trimmed).getHost();
            }
            if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) {
                String placeholder = trimmed.substring(2, trimmed.length() - 2).trim();
                if ("Hostname".equalsIgnoreCase(placeholder) || "Host".equalsIgnoreCase(placeholder)) {
                    return resolveHostFromTargetUrl(targetUrl, "Hostname".equalsIgnoreCase(placeholder));
                }
                if ("interactsh-url".equalsIgnoreCase(placeholder) || "interactsh_url".equalsIgnoreCase(placeholder)) {
                    String interactsh = getOrCreateInteractshUrl();
                    if (interactsh.startsWith("http://") || interactsh.startsWith("https://")) {
                        return new URL(interactsh).getHost();
                    }
                    return interactsh;
                }
                if (variables != null) {
                    String variableValue = variables.get(placeholder);
                    if (variableValue != null && !variableValue.trim().isEmpty()) {
                        return resolveTlsSniValue(variableValue, targetUrl, variables);
                    }
                }
                return placeholder;
            }
            if ("interactsh-url".equalsIgnoreCase(trimmed) || "interactsh_url".equalsIgnoreCase(trimmed)) {
                String resolved = getOrCreateInteractshUrl();
                if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
                    return new URL(resolved).getHost();
                }
                return resolved;
            }
        } catch (Exception ignored) {
        }
        return trimmed;
    }

    private static String resolveHostFromTargetUrl(String targetUrl, boolean includePort) {
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            return null;
        }
        try {
            URL target = new URL(targetUrl);
            String host = target.getHost();
            int port = target.getPort() != -1 ? target.getPort() : target.getDefaultPort();
            if (!includePort || port <= 0 || port == 80 || port == 443) {
                return host;
            }
            return host + ":" + port;
        } catch (Exception e) {
            return targetUrl;
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

        // RequestObj 构造时会默认继承总代理，这里先清空，再按 VulnScan 配置显式应用。
        requestObj.setProxies(null);
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
