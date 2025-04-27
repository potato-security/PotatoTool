package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.utils.CustomHttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DSL表达式评估器，用于解析和评估各种DSL表达式
 */
public class DslEvaluator {

    /**
     * 创建DSL表达式的上下文
     * @param response HTTP响应
     * @return 上下文映射
     */
    private static Map<String, Object> createDslContext(CustomHttpResponse response) {
        // 创建顶级上下文对象
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> responseMap = new HashMap<>();

        int status = 0;
        try {
            status = response.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 添加响应相关的核心字段
        String bodyContent = response.getTextStr();
        responseMap.put("body", bodyContent);
        responseMap.put("body_string", bodyContent); // 别名，兼容性
        responseMap.put("status", status);
        responseMap.put("status_code", status); // 别名，兼容性

        // 设置Content-Type和Content-Length
        String contentType = getResponseHeader(response.getHeaderFields(), "Content-Type");
        responseMap.put("content_type", contentType);

        int contentLength = response.getContentLength();
        responseMap.put("content_length", contentLength);

        // 设置头部信息
        Map<String, List<String>> headers = response.getHeaderFields();
        responseMap.put("headers", headers);
        responseMap.put("all_headers", formatAllHeaders(headers)); // 格式化的所有头

        // 原始响应
        responseMap.put("raw", bodyContent);

        // 响应时间
        long responseTime = response.getResponseTime();
        responseMap.put("time", responseTime);
        responseMap.put("latency", responseTime); // 别名，兼容性

        // 添加常见头部作为响应对象的直接属性，方便访问
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (header.getKey() != null) {
                String headerValue = String.join(", ", header.getValue());
                responseMap.put(header.getKey().toLowerCase().replace("-", "_"), headerValue);
            }
        }

        context.put("response", responseMap);

        // 添加请求相关的变量
        Map<String, Object> requestMap = new HashMap<>();
        try {
            URL url = response.getURL();
            String urlString = url.toString();
            requestMap.put("url", urlString);
            requestMap.put("path", url.getPath());
            requestMap.put("host", url.getHost());
            requestMap.put("scheme", url.getProtocol());
            int port = url.getPort();
            requestMap.put("port", port == -1 ? url.getDefaultPort() : port);

            // 添加查询参数
            String query = url.getQuery();
            if (query != null && !query.isEmpty()) {
                requestMap.put("query", query);

                // 解析查询参数
                Map<String, String> queryParams = new HashMap<>();
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        queryParams.put(
                                pair.substring(0, idx),
                                idx < pair.length() - 1 ? pair.substring(idx + 1) : ""
                        );
                    }
                }
                requestMap.put("query_params", queryParams);
            }
        } catch (Exception e) {
            System.err.println("解析URL失败: " + e.getMessage());
        }

        context.put("request", requestMap);

        return context;
    }


    /**
     * 使用嵌套Matcher结构处理DSL表达式
     * @param response HTTP响应
     * @param dslExpressions DSL表达式列表
     * @return 是否匹配成功
     */
    public static boolean matchDslWithNestedMatchers(CustomHttpResponse response, List<String> dslExpressions) {
        if (dslExpressions == null || dslExpressions.isEmpty()) {
            return false;
        }

        try {
            // 创建上下文环境
            Map<String, Object> context = createDslContext(response);

            // 处理每个DSL表达式
            for (String dslExpr : dslExpressions) {
                try {
                    // 解析DSL表达式为Matcher结构
                    List<PocObj.Matcher> dslMatchers = parseDslToMatchers(dslExpr, context);
                    if (dslMatchers != null && !dslMatchers.isEmpty()) {
                        // 如果只有一个匹配器，直接处理
                        if (dslMatchers.size() == 1) {
                            if (matchSingleDslMatcher(context, dslMatchers.get(0))) {
                                return true;
                            }
                        } else {
                            // 多个匹配器，默认使用AND条件
                            if (matchMultipleDslMatchers(context, dslMatchers, PocObj.MatchersCondition.AND)) {
                                return true;
                            }
                        }
                    } else {
                        // 降级：如果无法解析为Matcher结构，尝试使用原始方式
                        if (evaluateUnifiedDsl(context, dslExpr)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("DSL表达式解析失败: " + dslExpr + ", 错误: " + e.getMessage());
                    // 尝试原始方式作为退路
                    if (evaluateUnifiedDsl(context, dslExpr)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("DSL匹配失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 将DSL表达式解析为匹配器结构
     * @param dslExpr DSL表达式
     * @param context 上下文
     * @return 匹配器列表
     */
    private static List<PocObj.Matcher> parseDslToMatchers(String dslExpr, Map<String, Object> context) {
        List<PocObj.Matcher> matchers = new ArrayList<>();

        // 标准化表达式
        String normalizedExpr = normalizeExpression(dslExpr);

        // 处理逻辑运算符分割表达式
        if (normalizedExpr.contains(" && ")) {
            // AND 逻辑
            String[] parts = normalizedExpr.split(" && ");
            for (String part : parts) {
                PocObj.Matcher subMatcher = createDslMatcher(part.trim(), context);
                if (subMatcher != null) {
                    matchers.add(subMatcher);
                }
            }
            // 如果有多个匹配器，创建一个GROUP类型的主匹配器
            if (matchers.size() > 1) {
                PocObj.Matcher groupMatcher = new PocObj.Matcher();
                groupMatcher.setType(PocObj.MatcherType.GROUP);
                groupMatcher.setSubMatchers(matchers);
                groupMatcher.setCondition("AND");

                List<PocObj.Matcher> result = new ArrayList<>();
                result.add(groupMatcher);
                return result;
            }
        } else if (normalizedExpr.contains(" || ")) {
            // OR 逻辑
            String[] parts = normalizedExpr.split(" \\|\\| ");
            for (String part : parts) {
                PocObj.Matcher subMatcher = createDslMatcher(part.trim(), context);
                if (subMatcher != null) {
                    matchers.add(subMatcher);
                }
            }
            // 如果有多个匹配器，创建一个GROUP类型的主匹配器
            if (matchers.size() > 1) {
                PocObj.Matcher groupMatcher = new PocObj.Matcher();
                groupMatcher.setType(PocObj.MatcherType.GROUP);
                groupMatcher.setSubMatchers(matchers);
                groupMatcher.setCondition("OR");

                List<PocObj.Matcher> result = new ArrayList<>();
                result.add(groupMatcher);
                return result;
            }
        } else {
            // 单个表达式
            PocObj.Matcher matcher = createDslMatcher(normalizedExpr, context);
            if (matcher != null) {
                matchers.add(matcher);
            }
        }

        return matchers;
    }

    /**
     * 创建单个DSL匹配器
     * @param expr 表达式
     * @param context 上下文
     * @return 匹配器对象
     */
    private static PocObj.Matcher createDslMatcher(String expr, Map<String, Object> context) {
        PocObj.Matcher matcher = new PocObj.Matcher();

        try {
            // 处理包含函数的表达式
            if (expr.contains("contains(")) {
                matcher.setType(PocObj.MatcherType.WORD);
                parseContainsFunction(expr, matcher, context);
            } else if (expr.contains("ignoreCase(")) {
                matcher.setType(PocObj.MatcherType.WORD);
                matcher.setCaseInsensitive(true);
                parseIgnoreCaseFunction(expr, matcher, context);
            } else if (expr.contains("matches(") || expr.contains("bmatches(")) {
                matcher.setType(PocObj.MatcherType.REGEX);
                parseRegexFunction(expr, matcher, context);
            } else if (expr.contains("len(")) {
                matcher.setType(PocObj.MatcherType.SIZE);
                parseLengthFunction(expr, matcher, context);
            } else if (expr.contains("status")) {
                matcher.setType(PocObj.MatcherType.STATUS);
                parseStatusExpression(expr, matcher, context);
            } else if (isComparisonExpression(expr)) {
                // 处理比较表达式
                parseComparisonExpression(expr, matcher, context);
            } else {
                // 无法识别的表达式，返回原始DSL类型
                matcher.setType(PocObj.MatcherType.DSL);
                List<String> values = new ArrayList<>();
                values.add(expr);
                matcher.setValues(values);
            }

            return matcher;
        } catch (Exception e) {
            System.err.println("创建DSL匹配器失败: " + expr + ", 错误: " + e.getMessage());

            // 创建一个降级的DSL匹配器
            matcher.setType(PocObj.MatcherType.DSL);
            List<String> values = new ArrayList<>();
            values.add(expr);
            matcher.setValues(values);
            return matcher;
        }
    }

    /**
     * 解析contains函数
     */
    private static void parseContainsFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("contains(");
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String argsStr = expr.substring(openBracket + 1, closeBracket);
        String[] args = splitFunctionArgs(argsStr);

        if (args.length >= 2) {
            // 设置匹配的部分（body, header等）
            String sourcePart = stripQuotes(args[0]);
            matcher.setPart(determinePart(sourcePart, context));

            // 设置匹配值
            List<String> values = new ArrayList<>();
            values.add(stripQuotes(args[1]));
            matcher.setValues(values);

            // 设置操作类型为包含
            matcher.setOperation(PocObj.OperationType.CONTAINS);
        }
    }

    /**
     * 解析ignoreCase函数
     */
    private static void parseIgnoreCaseFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("ignoreCase(");
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String innerExpr = expr.substring(openBracket + 1, closeBracket);

        // 如果内部是contains函数，递归解析
        if (innerExpr.contains("contains(")) {
            parseContainsFunction(innerExpr, matcher, context);
            matcher.setCaseInsensitive(true);
        }
    }

    /**
     * 解析正则表达式函数
     */
    private static void parseRegexFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("matches(");
        if (funcIndex < 0) {
            funcIndex = expr.indexOf("bmatches(");
        }
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String argsStr = expr.substring(openBracket + 1, closeBracket);
        String[] args = splitFunctionArgs(argsStr);

        if (args.length >= 2) {
            // 设置匹配的部分
            String sourcePart = stripQuotes(args[0]);
            matcher.setPart(determinePart(sourcePart, context));

            // 设置匹配值（正则表达式）
            List<String> values = new ArrayList<>();
            values.add(stripQuotes(args[1]));
            matcher.setValues(values);
        }
    }

    /**
     * 解析长度函数
     */
    private static void parseLengthFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("len(");
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String argStr = expr.substring(openBracket + 1, closeBracket);

        // 找出比较操作符
        PocObj.OperationType opType = PocObj.OperationType.DEFAULT;
        String valueStr = null;

        if (expr.contains("==")) {
            opType = PocObj.OperationType.EQUAL;
            valueStr = expr.substring(expr.indexOf("==") + 2).trim();
        } else if (expr.contains("!=")) {
            opType = PocObj.OperationType.NOT_EQUAL;
            valueStr = expr.substring(expr.indexOf("!=") + 2).trim();
        } else if (expr.contains(">=")) {
            opType = PocObj.OperationType.GREATER_EQUAL;
            valueStr = expr.substring(expr.indexOf(">=") + 2).trim();
        } else if (expr.contains("<=")) {
            opType = PocObj.OperationType.LESS_EQUAL;
            valueStr = expr.substring(expr.indexOf("<=") + 2).trim();
        } else if (expr.contains(">")) {
            opType = PocObj.OperationType.GREATER;
            valueStr = expr.substring(expr.indexOf(">") + 1).trim();
        } else if (expr.contains("<")) {
            opType = PocObj.OperationType.LESS;
            valueStr = expr.substring(expr.indexOf("<") + 1).trim();
        }

        // 设置匹配的部分
        matcher.setPart(determinePart(stripQuotes(argStr), context));

        // 设置操作类型
        matcher.setOperation(opType);

        // 设置匹配值
        if (valueStr != null) {
            List<String> values = new ArrayList<>();
            values.add(valueStr);
            matcher.setValues(values);
        }
    }

    /**
     * 解析状态码表达式
     */
    private static void parseStatusExpression(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        matcher.setPart("status");

        // 找出比较操作符
        PocObj.OperationType opType = PocObj.OperationType.DEFAULT;
        String valueStr = null;

        if (expr.contains("==")) {
            opType = PocObj.OperationType.EQUAL;
            valueStr = expr.substring(expr.indexOf("==") + 2).trim();
        } else if (expr.contains("!=")) {
            opType = PocObj.OperationType.NOT_EQUAL;
            valueStr = expr.substring(expr.indexOf("!=") + 2).trim();
        } else if (expr.contains(">=")) {
            opType = PocObj.OperationType.GREATER_EQUAL;
            valueStr = expr.substring(expr.indexOf(">=") + 2).trim();
        } else if (expr.contains("<=")) {
            opType = PocObj.OperationType.LESS_EQUAL;
            valueStr = expr.substring(expr.indexOf("<=") + 2).trim();
        } else if (expr.contains(">")) {
            opType = PocObj.OperationType.GREATER;
            valueStr = expr.substring(expr.indexOf(">") + 1).trim();
        } else if (expr.contains("<")) {
            opType = PocObj.OperationType.LESS;
            valueStr = expr.substring(expr.indexOf("<") + 1).trim();
        }

        // 设置操作类型
        matcher.setOperation(opType);

        // 设置匹配值
        if (valueStr != null) {
            List<String> values = new ArrayList<>();
            values.add(valueStr);
            matcher.setValues(values);
        }
    }

    /**
     * 解析比较表达式
     */
    private static void parseComparisonExpression(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        String leftPart = null;
        String rightPart = null;
        PocObj.OperationType opType = PocObj.OperationType.DEFAULT;

        if (expr.contains("==")) {
            String[] parts = expr.split("==");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.EQUAL;
            }
        } else if (expr.contains("!=")) {
            String[] parts = expr.split("!=");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.NOT_EQUAL;
            }
        } else if (expr.contains(">=")) {
            String[] parts = expr.split(">=");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.GREATER_EQUAL;
            }
        } else if (expr.contains("<=")) {
            String[] parts = expr.split("<=");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.LESS_EQUAL;
            }
        } else if (expr.contains(">")) {
            String[] parts = expr.split(">");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.GREATER;
            }
        } else if (expr.contains("<")) {
            String[] parts = expr.split("<");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.LESS;
            }
        }

        if (leftPart != null && rightPart != null) {
            // 判断左侧是什么类型的表达式
            if (leftPart.contains("status") || leftPart.contains("response.status_code")) {
                matcher.setType(PocObj.MatcherType.STATUS);
                matcher.setPart("status");
            } else if (leftPart.contains("content_length") || leftPart.contains("response.content_length")) {
                matcher.setType(PocObj.MatcherType.SIZE);
                matcher.setPart("body");
            } else if (leftPart.contains("time") || leftPart.contains("response.time") || leftPart.contains("latency")) {
                matcher.setType(PocObj.MatcherType.TIME);
                matcher.setPart("time");
            } else {
                // 默认处理为WORD类型
                matcher.setType(PocObj.MatcherType.WORD);
                matcher.setPart(determinePart(leftPart, context));
            }

            // 设置操作类型
            matcher.setOperation(opType);

            // 设置匹配值
            List<String> values = new ArrayList<>();
            values.add(rightPart);
            matcher.setValues(values);
        }
    }

    /**
     * 确定表达式引用的部分（body, header等）
     */
    private static String determinePart(String reference, Map<String, Object> context) {
        if (reference == null) return "body";

        reference = reference.trim();

        // 处理常见的引用方式
        if (reference.equals("body") || reference.equals("response.body") || reference.equals("response.body_string")) {
            return "body";
        } else if (reference.equals("headers") || reference.equals("response.headers") || reference.equals("all_headers") || reference.equals("response.all_headers")) {
            return "headers";
        } else if (reference.equals("status") || reference.equals("status_code") || reference.equals("response.status") || reference.equals("response.status_code")) {
            return "status";
        } else if (reference.startsWith("response.") && context.containsKey("response")) {
            // 判断是否引用具体的响应头
            Map<String, Object> responseObj = (Map<String, Object>) context.get("response");
            if (responseObj != null) {
                String headerName = reference.substring("response.".length());
                if (responseObj.containsKey(headerName)) {
                    return headerName;
                }
            }
        }

        // 默认返回body
        return "body";
    }

    /**
     * 判断是否为比较表达式
     */
    private static boolean isComparisonExpression(String expr) {
        return expr.contains("==") || expr.contains("!=") ||
                expr.contains(">=") || expr.contains("<=") ||
                expr.contains(">") || expr.contains("<");
    }

    /**
     * 匹配单个DSL匹配器
     */
    private static boolean matchSingleDslMatcher(Map<String, Object> context, PocObj.Matcher matcher) {
        // 根据匹配器类型判断如何处理
        switch (matcher.getType()) {
            case DSL:
                // 原始DSL表达式
                List<String> values = matcher.getValues();
                if (values != null && !values.isEmpty()) {
                    for (String expr : values) {
                        try {
                            if (evaluateUnifiedDsl(context, expr)) {
                                return true;
                            }
                        } catch (Exception e) {
                            System.err.println("DSL表达式解析失败: " + expr + ", 错误: " + e.getMessage());
                        }
                    }
                }
                return false;

            case GROUP:
                // 处理组匹配器
                if (matcher.getSubMatchers() != null && !matcher.getSubMatchers().isEmpty()) {
                    String condition = matcher.getCondition();
                    PocObj.MatchersCondition matchersCondition = PocObj.MatchersCondition.AND;
                    if (condition != null && condition.equalsIgnoreCase("OR")) {
                        matchersCondition = PocObj.MatchersCondition.OR;
                    }
                    return matchMultipleDslMatchers(context, matcher.getSubMatchers(), matchersCondition);
                }
                return false;

            default:
                // 其他类型的匹配器，直接返回false
                System.err.println("不支持在DSL中使用的匹配器类型: " + matcher.getType());
                return false;
        }
    }

    /**
     * 匹配多个DSL匹配器
     */
    private static boolean matchMultipleDslMatchers(Map<String, Object> context, List<PocObj.Matcher> matchers, PocObj.MatchersCondition condition) {
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        if (condition == PocObj.MatchersCondition.OR) {
            // OR逻辑：任一匹配成功即返回true
            for (PocObj.Matcher matcher : matchers) {
                if (matchSingleDslMatcher(context, matcher)) {
                    return true;
                }
            }
            return false;
        } else {
            // AND逻辑：全部匹配成功才返回true
            for (PocObj.Matcher matcher : matchers) {
                if (!matchSingleDslMatcher(context, matcher)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 从响应头集合中获取指定头的值
     * @param headers 响应头集合
     * @param headerName 头名称
     * @return 头值或空字符串
     */
    private static String getResponseHeader(Map<String, List<String>> headers, String headerName) {
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (headerName != null && entry.getKey() != null && headerName.equalsIgnoreCase(entry.getKey())) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        return String.join(", ", values);
                    }
                    break;
                }
            }
        }
        return "";
    }

    /**
     * 格式化所有HTTP头为单个字符串
     * @param headers HTTP头集合
     * @return 格式化的HTTP头字符串
     */
    private static String formatAllHeaders(Map<String, List<String>> headers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            if (headerName != null) { // 跳过null键（状态行）
                List<String> values = entry.getValue();
                for (String value : values) {
                    sb.append(headerName).append(": ").append(value).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 评估统一的DSL表达式
     * @param context DSL上下文
     * @param expression DSL表达式
     * @return 表达式评估结果
     */
    private static boolean evaluateUnifiedDsl(Map<String, Object> context, String expression) {
        // 预处理表达式，标准化语法差异
        String normalizedExpr = normalizeExpression(expression);

        // 解析并评估表达式
        return evaluateDslExpression(context, normalizedExpr);
    }

    /**
     * 标准化DSL表达式
     * @param expression 原始表达式
     * @return 标准化后的表达式
     */
    private static String normalizeExpression(String expression) {
        String originalExpression = expression.trim();
        String normalized = originalExpression;

        // 替换Xray特有的语法为统一格式
        // 替换bcontains为contains
        normalized = normalized.replaceAll("bcontains\\s*\\(", "contains(");

        // 替换b"字符串"或b'字符串'为普通字符串
        normalized = normalized.replaceAll("b([\"'])", "$1");

        // 替换icontains为ignoreCase(contains( 形式
        normalized = normalized.replaceAll("icontains\\s*\\(", "ignoreCase(contains(");

        // 替换ibcontains为ignoreCase(contains( 形式 - Xray特有
        normalized = normalized.replaceAll("ibcontains\\s*\\(", "ignoreCase(contains(");

        // 替换bmatches为matches - Xray特有
        normalized = normalized.replaceAll("bmatches\\s*\\(", "matches(");

        // 替换Nuclei特有的语法
        // to_lower()替换为toLowerCase()
        normalized = normalized.replaceAll("to_lower\\s*\\(", "toLowerCase(");

        // to_upper()替换为toUpperCase()
        normalized = normalized.replaceAll("to_upper\\s*\\(", "toUpperCase(");

        // 检查处理前后表达式是否有变化，如果没有变化但包含特殊函数，可能是无法识别的表达式
        System.out.println("----------------------------------------------------");
        if (originalExpression.equals(normalized)) {
            // 检查是否包含可能未被处理的特殊函数
            if (containsUnrecognizedFunctions(originalExpression)) {
                logUnrecognizedExpression(originalExpression);
            }
        }

        return normalized;
    }

    /**
     * 检查表达式中是否包含未被识别的特殊函数
     * @param expression DSL表达式
     * @return 是否包含未识别的函数
     */
    private static boolean containsUnrecognizedFunctions(String expression) {
        // 已知可以处理的函数列表
        String[] knownFunctions = {
                "contains", "bcontains", "icontains", "ibcontains", "matches", "bmatches",
                "to_lower", "toLowerCase", "to_upper", "toUpperCase", "ignoreCase",
                "base64", "md5", "sha1", "sha256", "substr", "len", "substr", "regex", "rand",
                "string", "bytes", "reverse", "wait", "sleep", "submatch", "all_headers",
                "body", "body_string", "status_code", "content_length", "content_type", "latency",
                "header", "html_element", "html_attribute", "raw", "request", "response",
                "htmlelement", "jsonpath", "contains_all", "contains_any", "compare_versions",
                "startswith", "endswith", "mmh3", "base64_py", "base64_decode", "hex_encode",
                "hex_decode", "replace", "tolower", "toupper", "interactsh_protocol", "tostring",
                "json_minify"
        };

        // 已知的对象/属性列表
        String[] knownObjects = {
                "response", "request", "status", "headers", "body", "content_type",
                "content_length", "raw", "time", "latency", "path", "host", "scheme", "port", "url",
                "header", "location", "body_1", "body_2", "body_3", "body_4", "body_5",
                "header_1", "header_2", "header_3", "header_4", "header_5", "status_code_1",
                "status_code_2", "location_1", "location_2", "content_type", "server", "set_cookie",
                "all_headers", "data", "BaseURL", "version", "internal_detected_version", "last_version"
        };

        // 查找可能的函数调用
        Pattern pattern = Pattern.compile("\\b(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(expression);

        while (matcher.find()) {
            String foundFunction = matcher.group(1);
            boolean isKnown = false;

            for (String knownFunction : knownFunctions) {
                if (foundFunction.equals(knownFunction)) {
                    isKnown = true;
                    break;
                }
            }

            if (!isKnown) {
                System.err.println("未识别的函数: " + foundFunction);
                return true; // 发现未知函数
            }
        }

        // 查找可能的对象/属性引用
        Pattern objPattern = Pattern.compile("\\b(\\w+)\\.(\\w+)");
        Matcher objMatcher = objPattern.matcher(expression);

        while (objMatcher.find()) {
            String object = objMatcher.group(1);
            // 检查对象是否在已知列表中
            boolean isKnownObj = false;
            for (String knownObj : knownObjects) {
                if (object.equals(knownObj)) {
                    isKnownObj = true;
                    break;
                }
            }

            if (!isKnownObj && !object.equals("__length_result") &&
                    !object.equals("__lower_case_result") && !object.equals("__upper_case_result") &&
                    !object.equals("__hash_result") && !object.equals("__base64_result") &&
                    !object.equals("__header_result") && !object.equals("__substr_result")) {
                System.err.println("未识别的对象/属性: " + object);
                return true; // 发现未知对象
            }
        }

        return false;
    }

    /**
     * 记录未能识别的DSL表达式到文件
     * @param expression 未识别的DSL表达式
     */
    private static void logUnrecognizedExpression(String expression) {
        try {
            File logFile = new File("errorPoc.txt");
            boolean fileExists = logFile.exists();

            java.io.FileWriter writer = new java.io.FileWriter(logFile, true); // 追加模式
            java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(writer);

            // 如果是新文件，添加标题
            if (!fileExists) {
                bufferedWriter.write("未识别的DSL表达式记录:\n");
                bufferedWriter.write("===================\n\n");
            }

            // 记录时间戳和表达式
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());

            bufferedWriter.write("[" + timestamp + "] " + expression + "\n");
            bufferedWriter.close();

            System.out.println("[!] 发现未识别的DSL表达式: " + expression);
        } catch (IOException e) {
            System.err.println("[×] 记录未识别的DSL表达式时出错: " + e.getMessage());
        }
    }

    /**
     * 评估标准化后的DSL表达式
     * @param context DSL上下文
     * @param expression DSL表达式
     * @return 表达式评估结果
     */
    private static boolean evaluateDslExpression(Map<String, Object> context, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        expression = expression.trim();

        // 查找"and"、"or"和"not"
        Pattern andPattern = Pattern.compile("\\band\\b", Pattern.CASE_INSENSITIVE);
        Pattern orPattern = Pattern.compile("\\bor\\b", Pattern.CASE_INSENSITIVE);
        Pattern notPattern = Pattern.compile("\\bnot\\b", Pattern.CASE_INSENSITIVE);

        Matcher andMatcher = andPattern.matcher(expression);
        Matcher orMatcher = orPattern.matcher(expression);
        Matcher notMatcher = notPattern.matcher(expression);

        if (andMatcher.find()) {
            String[] parts = expression.split("\\band\\b", 2);
            return evaluateDslExpression(context, parts[0]) && evaluateDslExpression(context, parts[1]);
        } else if (orMatcher.find()) {
            String[] parts = expression.split("\\bor\\b", 2);
            return evaluateDslExpression(context, parts[0]) || evaluateDslExpression(context, parts[1]);
        } else if (notMatcher.find()) {
            String remainingExpr = expression.substring(notMatcher.end()).trim();
            return !evaluateDslExpression(context, remainingExpr);
        }

        // 先检查是否有未被识别的函数或对象
        if (containsUnrecognizedFunctions(expression)) {
            System.err.println("警告: 发现未识别的函数或对象在表达式: " + expression);
        }

        // 评估特定函数
        if (expression.contains("contains_all(")) {
            return evaluateContainsAllFunction(context, expression);
        } else if (expression.contains("contains_any(")) {
            return evaluateContainsAnyFunction(context, expression);
        } else if (expression.contains("startswith(")) {
            return evaluateStartsWithFunction(context, expression);
        } else if (expression.contains("endswith(")) {
            return evaluateEndsWithFunction(context, expression);
        } else if (expression.contains("base64_decode(")) {
            return evaluateBase64DecodeFunction(context, expression);
        } else if (expression.contains("version_compare(")) {
            return evaluateVersionCompareFunction(context, expression);
        } else if (expression.contains("md5(") || expression.contains("sha1(") || expression.contains("sha256(")) {
            if (expression.contains("md5(")) {
                return evaluateHashFunction(context, expression, "md5");
            } else if (expression.contains("sha1(")) {
                return evaluateHashFunction(context, expression, "sha1");
            } else if (expression.contains("sha256(")) {
                return evaluateHashFunction(context, expression, "sha256");
            }
        } else if (expression.contains("base64(")) {
            return evaluateBase64Function(context, expression);
        } else if (expression.contains("header(")) {
            return evaluateHeaderFunction(context, expression);
        } else if (expression.contains("substr(")) {
            return evaluateSubstrFunction(context, expression);
        } else if (expression.contains("ignoreCase(")) {
            return evaluateIgnoreCaseFunction(context, expression);
        } else if (expression.contains("toLowerCase(") || expression.contains("to_lower(") || expression.contains("tolower(")) {
            return evaluateLowerCaseFunction(context, expression);
        } else if (expression.contains("toUpperCase(") || expression.contains("to_upper(") || expression.contains("toupper(")) {
            return evaluateUpperCaseFunction(context, expression);
        } else if (expression.contains("len(")) {
            return evaluateLengthFunction(context, expression);
        }

        // 评估比较表达式
        return evaluateComparisonExpression(context, expression);
    }

    /**
     * 查找对应的右括号位置
     * @param expression 表达式
     * @param openBracketPos 左括号位置
     * @return 右括号位置，未找到返回-1
     */
    private static int findClosingBracket(String expression, int openBracketPos) {
        if (openBracketPos < 0 || openBracketPos >= expression.length()) {
            return -1;
        }

        int count = 1;
        for (int i = openBracketPos + 1; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * 分割函数参数，考虑引号内的逗号
     * @param argsStr 参数字符串
     * @return 分割后的参数数组
     */
    private static String[] splitFunctionArgs(String argsStr) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = '"';
        int nestedBrackets = 0;

        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);

            if (c == '"' || c == '\'') {
                if (!inQuote) {
                    inQuote = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuote = false;
                }
                currentArg.append(c);
            } else if (c == '(' && !inQuote) {
                nestedBrackets++;
                currentArg.append(c);
            } else if (c == ')' && !inQuote) {
                nestedBrackets--;
                currentArg.append(c);
            } else if (c == ',' && !inQuote && nestedBrackets == 0) {
                args.add(currentArg.toString());
                currentArg = new StringBuilder();
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }

        return args.toArray(new String[0]);
    }

    /**
     * 去除字符串两端的引号
     * @param str 输入字符串
     * @return 去除引号后的字符串
     */
    private static String stripQuotes(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }

        if ((str.startsWith("\"") && str.endsWith("\"")) ||
                (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length() - 1);
        }

        return str;
    }

    /**
     * 评估contains_all函数，检查字段是否包含所有指定的值
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateContainsAllFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("contains_all(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length < 2) {
            System.err.println("contains_all函数需要至少两个参数: " + expression);
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 检查是否包含所有值
        for (int i = 1; i < args.length; i++) {
            Object valueObj = resolveValue(context, args[i].trim());
            if (valueObj == null) {
                return false;
            }

            String value = valueObj.toString();

            // 如果有任何一个值不包含，则返回false
            if (!sourceStr.contains(value)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 评估contains_any函数，检查字段是否包含任意一个指定的值
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateContainsAnyFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("contains_any(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length < 2) {
            System.err.println("contains_any函数需要至少两个参数: " + expression);
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 检查是否包含任意一个值
        for (int i = 1; i < args.length; i++) {
            Object valueObj = resolveValue(context, args[i].trim());
            if (valueObj == null) {
                continue;
            }

            String value = valueObj.toString();

            // 如果包含任意一个值，则返回true
            if (sourceStr.contains(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 评估startswith函数，检查字段是否以指定字符串开头
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateStartsWithFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("startswith(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length != 2) {
            System.err.println("startswith函数需要两个参数: " + expression);
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 解析前缀
        Object prefixObj = resolveValue(context, args[1].trim());
        if (prefixObj == null) {
            return false;
        }
        String prefix = prefixObj.toString();

        return sourceStr.startsWith(prefix);
    }

    /**
     * 评估endswith函数，检查字段是否以指定字符串结尾
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateEndsWithFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("endswith(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length != 2) {
            System.err.println("endswith函数需要两个参数: " + expression);
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 解析后缀
        Object suffixObj = resolveValue(context, args[1].trim());
        if (suffixObj == null) {
            return false;
        }
        String suffix = suffixObj.toString();

        return sourceStr.endsWith(suffix);
    }

    /**
     * 评估base64_decode函数，将base64编码的字符串解码
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateBase64DecodeFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("base64_decode(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }

        try {
            // 解码base64
            String base64Str = innerValue.toString();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Str);
            String decodedStr = new String(decodedBytes, StandardCharsets.UTF_8);

            // 创建一个新上下文，包含解码后的结果
            Map<String, Object> decodedContext = new HashMap<>(context);
            decodedContext.put("__base64_decoded_result", decodedStr);

            // 处理后续表达式
            String remainingExpr = expression.substring(closeBracket + 1).trim();
            if (remainingExpr.startsWith(".")) {
                return evaluateDslExpression(decodedContext, "__base64_decoded_result" + remainingExpr);
            } else if (!remainingExpr.isEmpty()) {
                return evaluateComparisonExpression(decodedContext, "__base64_decoded_result " + remainingExpr);
            }

            // 如果没有后续操作，返回解码结果（非空为真）
            return !decodedStr.isEmpty();
        } catch (Exception e) {
            System.err.println("base64解码失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估比较表达式
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateComparisonExpression(Map<String, Object> context, String expression) {
        // 检查各种比较操作符
        if (expression.contains("==")) {
            return processComparisonOperator(context, expression, "==");
        } else if (expression.contains("!=")) {
            return processComparisonOperator(context, expression, "!=");
        } else if (expression.contains(">=")) {
            return processComparisonOperator(context, expression, ">=");
        } else if (expression.contains("<=")) {
            return processComparisonOperator(context, expression, "<=");
        } else if (expression.contains(">")) {
            return processComparisonOperator(context, expression, ">");
        } else if (expression.contains("<")) {
            return processComparisonOperator(context, expression, "<");
        }

        return false;
    }

    /**
     * 评估哈希函数 (md5/sha1/sha256)
     * @param context DSL上下文
     * @param expression 表达式
     * @param hashType 哈希类型
     * @return 评估结果
     */
    private static boolean evaluateHashFunction(Map<String, Object> context, String expression, String hashType) {
        try {
            // 解析函数参数
            int startIndex = expression.indexOf(hashType + "(") + hashType.length() + 1;
            int endIndex = findClosingBracket(expression, startIndex);

            if (endIndex == -1) {
                System.err.println("无法解析哈希函数: " + expression);
                return false;
            }

            String functionArgs = expression.substring(startIndex, endIndex);
            String[] args = splitFunctionArgs(functionArgs);

            if (args.length < 1) {
                System.err.println("哈希函数参数不足: " + expression);
                return false;
            }

            // 解析第一个参数
            String input = resolveStringValue(context, args[0].trim());
            if (input == null) {
                return false;
            }

            // 计算哈希值
            String hashValue;
            MessageDigest digest;

            switch (hashType.toLowerCase()) {
                case "md5":
                    digest = MessageDigest.getInstance("MD5");
                    break;
                case "sha1":
                    digest = MessageDigest.getInstance("SHA-1");
                    break;
                case "sha256":
                    digest = MessageDigest.getInstance("SHA-256");
                    break;
                default:
                    System.err.println("不支持的哈希类型: " + hashType);
                    return false;
            }

            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            hashValue = sb.toString();

            // 如果有变量名作为第二个参数，将结果存储到上下文中
            if (args.length > 1) {
                String varName = args[1].trim();
                varName = stripQuotes(varName); // 移除可能的引号
                context.put(varName, hashValue);
            }

            return true;
        } catch (Exception e) {
            System.err.println("评估哈希函数时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估Base64编码函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateBase64Function(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("base64(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }

        // 计算Base64编码
        String base64Value;
        try {
            base64Value = Base64.getEncoder().encodeToString(
                    innerValue.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Base64编码失败: " + e.getMessage());
            return false;
        }

        // 将计算结果存入上下文
        Map<String, Object> base64Context = new HashMap<>(context);
        base64Context.put("__base64_result", base64Value);

        // 处理编码后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(base64Context, "__base64_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(base64Context, "__base64_result " + remainingExpr);
        }

        return !base64Value.isEmpty();
    }

    /**
     * 评估header函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateHeaderFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("header(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 移除引号
        innerExpr = stripQuotes(innerExpr);

        // 获取头部值 - 使用getResponseHeaderFromContext代替getResponseHeader
        String headerValue = getResponseHeaderFromContext(context, innerExpr);
        if (headerValue == null) {
            return false;
        }

        // 将头部值存入上下文
        Map<String, Object> headerContext = new HashMap<>(context);
        headerContext.put("__header_result", headerValue);

        // 处理头部值的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(headerContext, "__header_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(headerContext, "__header_result " + remainingExpr);
        }

        return !headerValue.isEmpty();
    }

    /**
     * 评估substr函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateSubstrFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("substr(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length < 2) {
            System.err.println("substr函数参数不足: " + argsStr);
            return false;
        }

        // 解析参数
        Object strObj = resolveValue(context, args[0].trim());
        if (strObj == null) {
            return false;
        }
        String str = strObj.toString();

        try {
            int startPos = Integer.parseInt(stripQuotes(args[1].trim()));
            String result;

            if (args.length > 2) {
                // 如果有长度参数
                int length = Integer.parseInt(stripQuotes(args[2].trim()));
                if (startPos < 0) {
                    startPos = Math.max(0, str.length() + startPos);
                }
                if (startPos >= str.length()) {
                    result = "";
                } else {
                    int endPos = Math.min(startPos + length, str.length());
                    result = str.substring(startPos, endPos);
                }
            } else {
                // 如果只有起始位置
                if (startPos < 0) {
                    startPos = Math.max(0, str.length() + startPos);
                }
                if (startPos >= str.length()) {
                    result = "";
                } else {
                    result = str.substring(startPos);
                }
            }

            // 将结果存入上下文
            Map<String, Object> substrContext = new HashMap<>(context);
            substrContext.put("__substr_result", result);

            // 处理子字符串的比较
            String remainingExpr = expression.substring(closeBracket + 1).trim();
            if (remainingExpr.startsWith(".")) {
                return evaluateDslExpression(substrContext, "__substr_result" + remainingExpr);
            } else if (!remainingExpr.isEmpty()) {
                return evaluateComparisonExpression(substrContext, "__substr_result " + remainingExpr);
            }

            return !result.isEmpty();

        } catch (NumberFormatException e) {
            System.err.println("substr函数解析数字参数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估忽略大小写函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateIgnoreCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("ignoreCase(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 解析表达式并忽略大小写
        Map<String, Object> ignoreCaseContext = new HashMap<>(context);
        return evaluateDslExpression(ignoreCaseContext, innerExpr);
    }

    /**
     * 评估toLowerCase/to_lower函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateLowerCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex;
        if (expression.contains("toLowerCase(")) {
            funcIndex = expression.indexOf("toLowerCase(");
        } else if (expression.contains("to_lower(")) {
            funcIndex = expression.indexOf("to_lower(");
        } else if (expression.contains("tolower(")) {
            funcIndex = expression.indexOf("tolower(");
        } else {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }

        // 转换为小写
        String lowerCaseValue = innerValue.toString().toLowerCase();

        // 将转换结果存入上下文
        Map<String, Object> lowerCaseContext = new HashMap<>(context);
        lowerCaseContext.put("__lower_case_result", lowerCaseValue);

        // 处理转换后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(lowerCaseContext, "__lower_case_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(lowerCaseContext, "__lower_case_result " + remainingExpr);
        }

        return !lowerCaseValue.isEmpty();
    }

    /**
     * 评估toUpperCase/to_upper函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateUpperCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex;
        if (expression.contains("toUpperCase(")) {
            funcIndex = expression.indexOf("toUpperCase(");
        } else if (expression.contains("to_upper(")) {
            funcIndex = expression.indexOf("to_upper(");
        } else if (expression.contains("toupper(")) {
            funcIndex = expression.indexOf("toupper(");
        } else {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }

        // 转换为大写
        String upperCaseValue = innerValue.toString().toUpperCase();

        // 将转换结果存入上下文
        Map<String, Object> upperCaseContext = new HashMap<>(context);
        upperCaseContext.put("__upper_case_result", upperCaseValue);

        // 处理转换后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(upperCaseContext, "__upper_case_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(upperCaseContext, "__upper_case_result " + remainingExpr);
        }

        return !upperCaseValue.isEmpty();
    }

    /**
     * 评估length函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateLengthFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("len(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }

        // 计算长度
        int length = innerValue.toString().length();

        // 将长度结果存入上下文
        Map<String, Object> lengthContext = new HashMap<>(context);
        lengthContext.put("__length_result", length);

        // 处理长度的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return false; // 长度是一个整数，不能再访问属性
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(lengthContext, "__length_result " + remainingExpr);
        }

        return length > 0;
    }


    /**
     * 从上下文中获取响应头部值
     * @param context DSL上下文
     * @param headerName 头部名称
     * @return 头部值
     */
    private static String getResponseHeaderFromContext(Map<String, Object> context, String headerName) {
        // 尝试从RESP_HEADERS中获取
        Object headers = context.get("RESP_HEADERS");
        if (headers instanceof Map) {
            Map<?, ?> headersMap = (Map<?, ?>) headers;
            // 不区分大小写查找头部
            for (Map.Entry<?, ?> entry : headersMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().toString().equalsIgnoreCase(headerName)) {
                    Object value = entry.getValue();
                    if (value instanceof List) {
                        List<?> values = (List<?>) value;
                        if (!values.isEmpty()) {
                            return values.get(0).toString();
                        }
                    } else if (value != null) {
                        return value.toString();
                    }
                }
            }
        }

        // 如果找不到，返回null
        return null;
    }

    /**
     * 处理比较操作符
     * @param context DSL上下文
     * @param expression 表达式
     * @param operator 操作符
     * @return 比较结果
     */
    private static boolean processComparisonOperator(Map<String, Object> context, String expression, String operator) {
        String[] parts = expression.split(operator, 2);
        if (parts.length != 2) {
            return false;
        }

        String leftExpr = parts[0].trim();
        String rightExpr = parts[1].trim();

        Object leftValue = resolveValue(context, leftExpr);
        Object rightValue = resolveValue(context, rightExpr);

        if (leftValue == null || rightValue == null) {
            return false;
        }

        // 检查是否忽略大小写
        boolean ignoreCase = context.containsKey("__case_insensitive") &&
                (Boolean)context.get("__case_insensitive");

        // 尝试数值比较
        try {
            double leftNum = Double.parseDouble(leftValue.toString());
            double rightNum = Double.parseDouble(rightValue.toString());

            switch (operator) {
                case "==": return leftNum == rightNum;
                case "!=": return leftNum != rightNum;
                case ">": return leftNum > rightNum;
                case "<": return leftNum < rightNum;
                case ">=": return leftNum >= rightNum;
                case "<=": return leftNum <= rightNum;
                default: return false;
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，按字符串比较
            String leftStr = leftValue.toString();
            String rightStr = rightValue.toString();

            if (ignoreCase) {
                leftStr = leftStr.toLowerCase();
                rightStr = rightStr.toLowerCase();
            }

            int comparison = leftStr.compareTo(rightStr);

            switch (operator) {
                case "==": return comparison == 0;
                case "!=": return comparison != 0;
                case ">": return comparison > 0;
                case "<": return comparison < 0;
                case ">=": return comparison >= 0;
                case "<=": return comparison <= 0;
                default: return false;
            }
        }
    }

    /**
     * 评估version_compare函数，比较两个版本号
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private static boolean evaluateVersionCompareFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("version_compare(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length != 3) {
            System.err.println("version_compare函数需要三个参数: " + expression);
            return false;
        }

        // 解析两个版本号和比较操作符
        Object version1Obj = resolveValue(context, args[0].trim());
        Object version2Obj = resolveValue(context, args[1].trim());
        String operator = stripQuotes(args[2].trim());

        if (version1Obj == null || version2Obj == null) {
            return false;
        }

        String version1 = version1Obj.toString();
        String version2 = version2Obj.toString();

        // 分割版本号为组件
        String[] v1Components = version1.split("\\.");
        String[] v2Components = version2.split("\\.");

        // 比较每个组件
        int maxLength = Math.max(v1Components.length, v2Components.length);
        for (int i = 0; i < maxLength; i++) {
            int v1Comp = (i < v1Components.length) ? parseInt(v1Components[i]) : 0;
            int v2Comp = (i < v2Components.length) ? parseInt(v2Components[i]) : 0;

            if (v1Comp < v2Comp) {
                return operator.equals("<") || operator.equals("<=") || operator.equals("!=");
            } else if (v1Comp > v2Comp) {
                return operator.equals(">") || operator.equals(">=") || operator.equals("!=");
            }
        }

        // 如果版本号完全相等
        return operator.equals("==") || operator.equals("<=") || operator.equals(">=");
    }

    /**
     * 辅助方法：尝试将字符串解析为整数，失败则返回0
     */
    private static int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 解析字符串值
     * @param context DSL上下文
     * @param reference 引用或字面值
     * @return 解析后的字符串值
     */
    private static String resolveStringValue(Map<String, Object> context, String reference) {
        Object value = resolveValue(context, reference);
        return value != null ? value.toString() : null;
    }

    /**
     * 解析上下文中的值
     * @param context 上下文
     * @param reference 引用路径
     * @return 解析后的值
     */
    private static Object resolveValue(Map<String, Object> context, String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }

        // 检查是否是字面量
        if (reference.startsWith("\"") && reference.endsWith("\"")) {
            return reference.substring(1, reference.length() - 1);
        }
        if (reference.startsWith("'") && reference.endsWith("'")) {
            return reference.substring(1, reference.length() - 1);
        }

        // 尝试解析数字
        try {
            return Double.parseDouble(reference);
        } catch (NumberFormatException ignored) {
            // 不是数字，继续
        }

        // 解析字段引用
        return getFieldValue(context, reference);
    }

    /**
     * 根据字段路径获取字段值
     * @param context 上下文映射
     * @param fieldPath 字段路径
     * @return 字段值或null
     */
    private static Object getFieldValue(Map<String, Object> context, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        String[] parts = fieldPath.split("\\.");
        Object current = context;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * 使用DSL表达式提取内容
     * @param response HTTP响应
     * @param expressions DSL表达式列表
     * @return 提取的内容
     */
    public static String extractDsl(CustomHttpResponse response, List<String> expressions) {
        if (response == null || expressions == null || expressions.isEmpty()) {
            return null;
        }

        // 创建上下文环境
        Map<String, Object> context = createDslContext(response);

        for (String expr : expressions) {
            try {
                if (expr.contains(" = ")) {
                    // 提取赋值表达式右侧的值
                    String[] parts = expr.split(" = ", 2);
                    if (parts.length == 2) {
                        String valueExpr = parts[1];
                        Object value = resolveValue(context, valueExpr);
                        if (value != null) {
                            return value.toString();
                        }
                    }
                } else {
                    // 直接解析表达式
                    Object value = resolveValue(context, expr);
                    if (value != null) {
                        return value.toString();
                    }
                }
            } catch (Exception e) {
                System.err.println("DSL表达式提取失败: " + expr + ", 错误: " + e.getMessage());
            }
        }
        return null;
    }
} 