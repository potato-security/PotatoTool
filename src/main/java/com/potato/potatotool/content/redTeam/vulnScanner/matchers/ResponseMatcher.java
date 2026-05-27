package com.potato.potatotool.content.redTeam.vulnScanner.matchers;

import com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.XrayCelEvaluator;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.JsonExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.VariableExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.GobyFunctionProcessor;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.InteractshClient;
import com.potato.potatotool.content.redTeam.vulnScanner.util.RegexCompat;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

/**
 * 响应匹配器，用于匹配HTTP响应是否符合条件
 */
public class ResponseMatcher {

    /**
     * 匹配HTTP响应是否符合条件列表（无缓存版本）
     * @param response HTTP响应对象
     * @param matchers 匹配器列表
     * @param condition 匹配条件（AND/OR）
     * @return 是否匹配成功
     */
    public static boolean matchResponse(CustomHttpResponse response, List<PocObj.Matcher> matchers, PocObj.MatchersCondition condition) {
        return matchResponse(response, matchers, condition, null, null);
    }

    /**
     * 匹配HTTP响应是否符合条件列表（带缓存版本）
     * @param response HTTP响应对象
     * @param matchers 匹配器列表
     * @param condition 匹配条件（AND/OR）
     * @param responseCache 响应缓存（用于diff操作和多响应支持）
     * @return 是否匹配成功
     */
    public static boolean matchResponse(CustomHttpResponse response, List<PocObj.Matcher> matchers,
                                       PocObj.MatchersCondition condition, ResponseCache responseCache) {
        return matchResponse(response, matchers, condition, responseCache, null);
    }

    /**
     * 匹配HTTP响应是否符合条件列表（完整版本）
     * @param response HTTP响应对象
     * @param matchers 匹配器列表
     * @param condition 匹配条件（AND/OR）
     * @param responseCache 响应缓存（用于diff操作和多响应支持）
     * @param stepId 步骤ID（用于多响应场景，支持body_1, body_2等变量）
     * @return 是否匹配成功
     */
    public static boolean matchResponse(CustomHttpResponse response, List<PocObj.Matcher> matchers,
                                       PocObj.MatchersCondition condition, ResponseCache responseCache, String stepId) {
        return matchResponse(response, matchers, condition, responseCache, stepId, null);
    }
    
    /**
     * 匹配HTTP响应是否符合条件列表（带 POC 变量版本）
     * @param response HTTP响应对象
     * @param matchers 匹配器列表
     * @param condition 匹配条件（AND/OR）
     * @param responseCache 响应缓存（用于diff操作和多响应支持）
     * @param stepId 步骤ID（用于多响应场景，支持body_1, body_2等变量）
     * @param pocVariables POC 变量映射（用于 CEL 表达式中的变量替换，如 s1, s2）
     * @return 是否匹配成功
     */
    public static boolean matchResponse(CustomHttpResponse response, List<PocObj.Matcher> matchers,
                                       PocObj.MatchersCondition condition, ResponseCache responseCache,
                                       String stepId, Map<String, Object> pocVariables) {
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        // AND条件：所有匹配器都必须匹配成功
        if (condition == PocObj.MatchersCondition.AND) {
            for (PocObj.Matcher matcher : matchers) {
                if (!matchSingleMatcher(response, matcher, responseCache, stepId, pocVariables)) {
                    return false;
                }
            }
            return true;
        }

        // OR条件：任一匹配器匹配成功即可（官方语义）
        if (condition == PocObj.MatchersCondition.OR) {
            for (PocObj.Matcher matcher : matchers) {
                if (matchSingleMatcher(response, matcher, responseCache, stepId, pocVariables)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }
    
    /**
     * 匹配单个匹配器（无缓存版本）
     */
    private static boolean matchSingleMatcher(CustomHttpResponse response, PocObj.Matcher matcher) {
        return matchSingleMatcher(response, matcher, null, null);
    }

    /**
     * 匹配单个匹配器（带缓存版本）
     */
    private static boolean matchSingleMatcher(CustomHttpResponse response, PocObj.Matcher matcher, ResponseCache responseCache) {
        return matchSingleMatcher(response, matcher, responseCache, null);
    }

    /**
     * 匹配单个匹配器
     * 
     * 匹配器类型分类：
     * - 响应级别（不需要 content）：STATUS, TIME, DSL, CEL, DIFF
     * - 内容级别（需要 content）：WORD, REGEX, SIZE, JSON
     * - 字节级别（需要 byte[]）：BINARY, HASH
     * - 组合类型：GROUP
     * 
     * @param response HTTP响应对象
     * @param matcher 匹配器
     * @param responseCache 响应缓存
     * @param stepId 步骤ID（用于多响应场景）
     * @return 是否匹配成功
     */
    private static boolean matchSingleMatcher(CustomHttpResponse response, PocObj.Matcher matcher,
                                             ResponseCache responseCache, String stepId) {
        return matchSingleMatcher(response, matcher, responseCache, stepId, null);
    }
    
    /**
     * 匹配单个匹配器（带 POC 变量）
     */
    private static boolean matchSingleMatcher(CustomHttpResponse response, PocObj.Matcher matcher,
                                             ResponseCache responseCache, String stepId,
                                             Map<String, Object> pocVariables) {
        // 1. 基础校验
        if (matcher == null || matcher.getType() == null) {
            return false;
        }
        
        PocObj.MatcherType type = matcher.getType();
        List<String> values = matcher.getValues();
        PocObj.OperationType operation = matcher.getOperation();
        boolean negative = matcher.isNegative();
        ResponseCache.CachedResponse indexedResponse = getIndexedResponse(matcher, responseCache, stepId);

        boolean matched;

        // 2. 特殊操作：DIFF 比较（用于盲注检测）
        if (operation == PocObj.OperationType.DIFF && responseCache != null) {
            matched = matchDiff(response, values, matcher.getPart(), responseCache);
            return negative ? !matched : matched;
        }

        switch (type) {
            // ========== 响应级别匹配（不需要提取 content）==========
            case STATUS:
                matched = indexedResponse != null
                        ? matchStatus(indexedResponse.getStatusCode(), values, matcher.getOperation(), matcher.getCondition())
                        : matchStatusCode(response, values, matcher.getOperation(), matcher.getCondition());
                break;

            case TIME:
                matched = matchTime(
                        indexedResponse != null ? indexedResponse.getResponseTimeMs() : response.getResponseTime(),
                        values,
                        operation,
                        matcher.getTimeUnit()
                );
                break;

            case DSL:
                // Nuclei DSL 表达式匹配
                matched = matchDsl(values, response, responseCache, stepId, pocVariables, matcher.getCondition());
                break;

            case CEL:
                // Xray CEL 表达式匹配（传递 POC 变量用于表达式求值）
                matched = matchCel(values, response, pocVariables);
                break;

            // ========== 内容级别匹配（需要提取 content）==========
            case WORD:
            case REGEX:
            case SIZE:
            case JSON:
            case XPATH:
                // 特殊处理：DNSLog 验证 ($reserver)
                if (matcher.getPart() != null &&
                   ("$reserver".equalsIgnoreCase(matcher.getPart()) || "reserver".equalsIgnoreCase(matcher.getPart()))) {
                    matched = checkDnsLog(values, pocVariables);
                } else if (matcher.getPart() != null && "interactsh_protocol".equalsIgnoreCase(matcher.getPart())) {
                    matched = checkHttpInteraction(values, pocVariables);
                } else {
                    matched = matchContentBased(type, matcher, response, indexedResponse, pocVariables);
                }
                break;

            // ========== 字节级别匹配（需要 byte[]）==========
            case BINARY:
            case HASH:
                matched = matchBytesBased(type, matcher, response, indexedResponse);
                break;

            // ========== 组合类型 ==========
            case GROUP:
                matched = matchGroup(matcher, response, responseCache, stepId, pocVariables);
                break;

            default:
                matched = false;
                break;
        }

        return negative ? !matched : matched;
    }
    
    /**
     * 状态码匹配
     */
    private static boolean matchStatusCode(CustomHttpResponse response, List<String> values) {
        return matchStatusCode(response, values, null);
    }

    private static boolean matchStatusCode(CustomHttpResponse response, List<String> values, String condition) {
        return matchStatusCode(response, values, null, condition);
    }

    private static boolean matchStatusCode(CustomHttpResponse response, List<String> values,
                                           PocObj.OperationType operation, String condition) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        try {
            return matchStatus(resolveStatusCodeForMatcher(response), values, operation, condition);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * DSL 表达式匹配（Nuclei）
     */
    private static boolean matchDsl(List<String> values, CustomHttpResponse response, 
                                    ResponseCache responseCache, String stepId) {
        return matchDsl(values, response, responseCache, stepId, null, null);
    }

    private static boolean matchDsl(List<String> values, CustomHttpResponse response,
                                    ResponseCache responseCache, String stepId,
                                    Map<String, Object> pocVariables) {
        return matchDsl(values, response, responseCache, stepId, pocVariables, null);
    }

    private static boolean matchDsl(List<String> values, CustomHttpResponse response,
                                    ResponseCache responseCache, String stepId,
                                    Map<String, Object> pocVariables,
                                    String condition) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        if (isOrCondition(condition)) {
            for (String value : values) {
                if (DslEvaluatorRefactored.matchDslWithNestedMatchers(
                        java.util.Collections.singletonList(value), response, response, responseCache, stepId, pocVariables)) {
                    return true;
                }
            }
            return false;
        }
        return DslEvaluatorRefactored.matchDslWithNestedMatchers(values, response, response, responseCache, stepId, pocVariables);
    }
    
    /**
     * CEL 表达式匹配（Xray）
     */
    private static boolean matchCel(List<String> values, CustomHttpResponse response, 
                                    Map<String, Object> pocVariables) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return XrayCelEvaluator.evaluate(values.get(0), response, null, pocVariables);
    }
    
    /**
     * 基于内容的匹配（WORD, REGEX, SIZE, JSON）
     */
    private static boolean matchContentBased(PocObj.MatcherType type, PocObj.Matcher matcher, 
                                             CustomHttpResponse response,
                                             ResponseCache.CachedResponse indexedResponse,
                                             Map<String, Object> pocVariables) {
        List<String> values = matcher.getValues();
        if (values == null || values.isEmpty()) {
            return false;
        }
        
        // 替换 Goby 风格的变量引用 {{{varName}}}
        if (pocVariables != null && !pocVariables.isEmpty()) {
            values = substituteGobyVariables(values, pocVariables);
        }
        
        String part = matcher.getPart();
        String content = indexedResponse != null
                ? getCachedResponsePart(indexedResponse, part)
                : VariableExtractor.getResponsePart(response, part, shouldUseInitialRedirectResponse(matcher));
        if (content == null) {
            return false;
        }
        
        boolean caseInsensitive = matcher.isCaseInsensitive();
        String basePart = VariableExtractor.stripIndexedPart(part);
        
        PocObj.OperationType operation = matcher.getOperation();
        
        switch (type) {
            case WORD:
                // HTTP 头名称是大小写不敏感的，对 header 匹配默认忽略大小写
                boolean effectiveCaseInsensitive = caseInsensitive || "header".equalsIgnoreCase(basePart);
                boolean wordMatch = matchWord(content, values, effectiveCaseInsensitive, matcher.getCondition());
                // 处理 NOT_CONTAINS 操作
                if (operation == PocObj.OperationType.NOT_CONTAINS) {
                    return !wordMatch;
                }
                return wordMatch;
            case REGEX:
                return matchRegex(content, values, caseInsensitive, matcher.getCondition());
            case SIZE:
                return matchSize(content.length(), values, matcher.getOperation(), matcher.getCondition());
            case JSON:
                return matchJson(content, values, matcher.getCondition());
            case XPATH:
                return matchXpath(content, values, matcher.getCondition());
            default:
                return false;
        }
    }

    private static int resolveStatusCodeForMatcher(CustomHttpResponse response) {
        if (response == null) {
            return 0;
        }
        return response.getResponseCode();
    }

    private static boolean shouldUseInitialRedirectResponse(PocObj.Matcher matcher) {
        if (matcher == null) {
            return false;
        }
        String part = matcher.getPart();
        if (part == null) {
            return false;
        }
        String normalizedPart = VariableExtractor.stripIndexedPart(part);
        return "header".equalsIgnoreCase(normalizedPart) || "location".equalsIgnoreCase(normalizedPart);
    }
    
    /**
     * 基于字节的匹配（BINARY, HASH）
     */
    private static boolean matchBytesBased(PocObj.MatcherType type, PocObj.Matcher matcher,
                                           CustomHttpResponse response,
                                           ResponseCache.CachedResponse indexedResponse) {
        List<String> values = matcher.getValues();
        if (values == null || values.isEmpty()) {
            return false;
        }
        
        byte[] contentBytes = indexedResponse != null ? indexedResponse.getRawBytes() : response.getByteArray();
        if (contentBytes == null) {
            return false;
        }
        
        switch (type) {
            case BINARY:
                return matchBinary(contentBytes, values, matcher.getCondition());
            case HASH:
                return matchHash(contentBytes, values, matcher.getOperation());
            default:
                return false;
        }
    }
    
    /**
     * 匹配器组匹配（GROUP）
     */
    private static boolean matchGroup(PocObj.Matcher matcher, CustomHttpResponse response,
                                      ResponseCache responseCache, String stepId,
                                      Map<String, Object> pocVariables) {
        List<PocObj.Matcher> subMatchers = matcher.getSubMatchers();
        if (subMatchers == null || subMatchers.isEmpty()) {
            return false;
        }
        
        // 解析组合条件（默认 AND）
        PocObj.MatchersCondition condition = PocObj.MatchersCondition.AND;
        if ("OR".equalsIgnoreCase(matcher.getCondition())) {
            condition = PocObj.MatchersCondition.OR;
        }
        
        boolean groupResult = matchResponse(response, subMatchers, condition, responseCache, stepId, pocVariables);
        return groupResult;
    }

    private static ResponseCache.CachedResponse getIndexedResponse(PocObj.Matcher matcher,
                                                                   ResponseCache responseCache,
                                                                   String stepId) {
        if (matcher == null || responseCache == null || stepId == null || stepId.isEmpty()) {
            return null;
        }
        Integer index = VariableExtractor.getIndexedPartNumber(matcher.getPart());
        if (index == null) {
            return null;
        }
        return responseCache.get(stepId + "_" + index);
    }

    private static String getCachedResponsePart(ResponseCache.CachedResponse cachedResponse, String part) {
        if (cachedResponse == null) {
            return null;
        }

        String normalizedPart = VariableExtractor.stripIndexedPart(part);
        if (normalizedPart == null || normalizedPart.trim().isEmpty()) {
            normalizedPart = "body";
        }

        switch (normalizedPart.toLowerCase(Locale.ROOT)) {
            case "body":
                return cachedResponse.getBody();
            case "header":
                return cachedResponse.getHeader();
            case "status":
                return String.valueOf(cachedResponse.getStatusCode());
            case "all":
                return (cachedResponse.getHeader() == null ? "" : cachedResponse.getHeader())
                        + "\n\n"
                        + (cachedResponse.getBody() == null ? "" : cachedResponse.getBody());
            case "raw":
                return cachedResponse.getRawBytes() == null
                        ? null
                        : new String(cachedResponse.getRawBytes(), StandardCharsets.ISO_8859_1);
            case "content_length":
                return String.valueOf(cachedResponse.getBody() == null ? 0 : cachedResponse.getBody().length());
            default:
                return cachedResponse.getHeader();
        }
    }
    
    /**
     * 匹配差异对比
     * 用于布尔盲注检测，对比当前响应与之前缓存的响应
     * 
     * @param currentResponse 当前响应
     * @param values 匹配值（格式：cacheKey 或 cacheKey1:cacheKey2）
     * @param compareType 对比类型（body, header, status, length, time）
     * @param responseCache 响应缓存
     * @return 是否存在差异
     */
    private static boolean matchDiff(CustomHttpResponse currentResponse, List<String> values, 
                                    String compareType, ResponseCache responseCache) {
        if (values == null || values.isEmpty() || responseCache == null) {
            return false;
        }
        
        String value = values.get(0);
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // 解析缓存键
        // 格式1: "key1" - 对比当前响应与缓存key1
        // 格式2: "key1:key2" - 对比缓存key1与缓存key2
        String[] keys = value.split(":");
        
        if (keys.length == 1) {
            // 对比当前响应与缓存的响应
            String cacheKey = keys[0].trim();
            ResponseCache.CachedResponse cachedResp = responseCache.get(cacheKey);
            
            if (cachedResp == null) {
                System.err.println("未找到缓存的响应: " + cacheKey);
                return false;
            }
            
            // 将当前响应转换为CachedResponse进行对比
            ResponseCache.CachedResponse currentCached = new ResponseCache.CachedResponse(
                currentResponse.getResponseCode(),
                currentResponse.getTextStr(),
                currentResponse.getHeaderFieldsText(),
                currentResponse.getByteArray(),
                currentResponse.getResponseTime(),
                0, 0
            );
            
            ResponseCache.DiffResult result = ResponseCache.diff(currentCached, cachedResp, compareType);
            return result.isDifferent();
            
        } else if (keys.length == 2) {
            // 对比两个缓存的响应
            String cacheKey1 = keys[0].trim();
            String cacheKey2 = keys[1].trim();
            
            ResponseCache.DiffResult result = responseCache.diff(cacheKey1, cacheKey2, compareType);
            if (result == null) {
                return false;
            }
            
            return result.isDifferent();
        } else {
            System.err.println("无效的diff格式: " + value);
            return false;
        }
    }


    private static boolean checkDnsLog(List<String> values, Map<String, Object> pocVariables) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        List<String> resolvedValues = values;
        if (pocVariables != null && !pocVariables.isEmpty()) {
            resolvedValues = substituteGobyVariables(values, pocVariables);
        }

        for (String value : resolvedValues) {
            if (value == null || value.isEmpty()) {
                continue;
            }

            try {
                if (isHttpOobTarget(value) && HttpLogService.hasInteraction(value)) {
                    return true;
                }
            } catch (Exception e) {
                // HTTP OOB 查询失败，继续尝试 DNS OOB
            }

            try {
                if (DnsLogService.hasDnsResolution(value)) {
                    return true;
                }
            } catch (Exception e) {
                // DNS OOB 查询失败
            }
        }

        return false;
    }

    private static boolean checkHttpInteraction(List<String> values, Map<String, Object> pocVariables) {
        String interactshUrl = resolveInteractshUrl(pocVariables);
        if (interactshUrl == null || interactshUrl.trim().isEmpty() || interactshUrl.contains("LAZY_INTERACTSH")) {
            return false;
        }

        try {
            InteractshClient.Interaction interaction = HttpLogService.waitForConfiguredInteraction(interactshUrl);
            if (interaction != null) {
                return valuesContainProtocol(values, interaction.getProtocol());
            }
        } catch (Exception ignored) {
        }

        try {
            String records = HttpLogService.queryHttpLogRecords(interactshUrl);
            if (records == null || records.trim().isEmpty() || "[]".equals(records.trim())) {
                return false;
            }
            if (values == null || values.isEmpty()) {
                return true;
            }
            String normalizedRecords = records.toLowerCase(Locale.ROOT);
            for (String value : values) {
                if (value != null && normalizedRecords.contains("\"protocol\":\"" + value.toLowerCase(Locale.ROOT) + "\"")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private static String resolveInteractshUrl(Map<String, Object> pocVariables) {
        if (pocVariables == null || pocVariables.isEmpty()) {
            return null;
        }
        Object value = pocVariables.get("interactsh-url");
        if (value == null) {
            value = pocVariables.get("interactsh_url");
        }
        String interactshUrl = value == null ? null : String.valueOf(value);
        if (interactshUrl == null || interactshUrl.trim().isEmpty()) {
            return interactshUrl;
        }
        if (interactshUrl.startsWith("http://") || interactshUrl.startsWith("https://")) {
            return interactshUrl;
        }
        return "http://" + interactshUrl;
    }

    private static boolean valuesContainProtocol(List<String> values, String protocol) {
        if (values == null || values.isEmpty()) {
            return protocol != null && !protocol.trim().isEmpty();
        }
        String normalizedProtocol = protocol == null ? "" : protocol.trim().toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.trim().equalsIgnoreCase(normalizedProtocol)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHttpOobTarget(String value) {
        return value.contains("oast.") || value.contains("interactsh.");
    }

    private static boolean isAndCondition(String condition) {
        return "AND".equalsIgnoreCase(condition);
    }

    private static boolean isOrCondition(String condition) {
        return "OR".equalsIgnoreCase(condition);
    }

    /**
     * 匹配HTTP状态码
     * @param statusCode 实际状态码
     * @param values 期望的状态码列表
     * @return 是否匹配成功
     */
    private static boolean matchStatus(int statusCode, List<String> values) {
        return matchStatus(statusCode, values, null);
    }

    private static boolean matchStatus(int statusCode, List<String> values, String condition) {
        return matchStatus(statusCode, values, null, condition);
    }

    private static boolean matchStatus(int statusCode, List<String> values,
                                       PocObj.OperationType operation, String condition) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        if (operation == null) {
            operation = PocObj.OperationType.EQUAL;
        }

        boolean requireAll = isAndCondition(condition);
        for (String value : values) {
            boolean valueMatched = false;
            try {
                int expectedStatus = Integer.parseInt(value.trim());
                switch (operation) {
                    case NOT_EQUAL:
                        valueMatched = statusCode != expectedStatus;
                        break;
                    case GREATER:
                        valueMatched = statusCode > expectedStatus;
                        break;
                    case LESS:
                        valueMatched = statusCode < expectedStatus;
                        break;
                    case GREATER_EQUAL:
                        valueMatched = statusCode >= expectedStatus;
                        break;
                    case LESS_EQUAL:
                        valueMatched = statusCode <= expectedStatus;
                        break;
                    default:
                        valueMatched = statusCode == expectedStatus;
                        break;
                }
            } catch (NumberFormatException e) {
                // 忽略非法值
            }

            if (requireAll && !valueMatched) {
                return false;
            }
            if (!requireAll && valueMatched) {
                return true;
            }
        }
        
        return requireAll;
    }

    /**
     * 匹配响应大小
     * @param size 实际大小
     * @param values 期望的大小列表
     * @param operation 比较操作类型
     * @return 是否匹配成功
     */
    private static boolean matchSize(int size, List<String> values, PocObj.OperationType operation) {
        return matchSize(size, values, operation, null);
    }

    private static boolean matchSize(int size, List<String> values, PocObj.OperationType operation, String condition) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        if (operation == null) {
            operation = PocObj.OperationType.EQUAL;
        }

        boolean requireAll = isAndCondition(condition);
        for (String value : values) {
            boolean valueMatched = false;
            try {
                int targetSize = Integer.parseInt(value.trim());

                switch (operation) {
                    case NOT_EQUAL:
                        valueMatched = size != targetSize;
                        break;
                    case GREATER:
                        valueMatched = size > targetSize;
                        break;
                    case LESS:
                        valueMatched = size < targetSize;
                        break;
                    case GREATER_EQUAL:
                        valueMatched = size >= targetSize;
                        break;
                    case LESS_EQUAL:
                        valueMatched = size <= targetSize;
                        break;
                    default:
                        valueMatched = size == targetSize;
                        break;
                }
            } catch (NumberFormatException e) {
                // 忽略非法值
            }

            if (requireAll && !valueMatched) {
                return false;
            }
            if (!requireAll && valueMatched) {
                return true;
            }
        }
        
        return requireAll;
    }

    /**
     * 匹配字符串是否包含指定单词
     * @param content 内容
     * @param values 期望包含的单词列表
     * @param caseInsensitive 是否忽略大小写
     * @return 是否匹配成功
     */
    private static boolean matchWord(String content, List<String> values, boolean caseInsensitive) {
        return matchWord(content, values, caseInsensitive, null);
    }

    private static boolean matchWord(String content, List<String> values, boolean caseInsensitive, String condition) {
        if (content == null || values == null || values.isEmpty()) {
            return false;
        }

        String contentToMatch = caseInsensitive ? content.toLowerCase() : content;
        boolean requireAll = isAndCondition(condition);
        
        for (String value : values) {
            String valueToMatch = caseInsensitive ? value.toLowerCase() : value;
            boolean valueMatched = contentToMatch.contains(valueToMatch);
            if (requireAll && !valueMatched) {
                return false;
            }
            if (!requireAll && valueMatched) {
                return true;
            }
        }
        
        return requireAll;
    }

    /**
     * 匹配字符串是否符合正则表达式
     * @param content 内容
     * @param values 正则表达式列表
     * @param caseInsensitive 是否忽略大小写
     * @return 是否匹配成功
     */
    private static boolean matchRegex(String content, List<String> values, boolean caseInsensitive) {
        return matchRegex(content, values, caseInsensitive, null);
    }

    private static boolean matchRegex(String content, List<String> values, boolean caseInsensitive, String condition) {
        if (content == null || values == null || values.isEmpty()) {
            return false;
        }

        boolean requireAll = isAndCondition(condition);
        for (String regex : values) {
            boolean valueMatched = false;
            try {
                Pattern pattern = caseInsensitive
                        ? RegexCompat.compile(regex, Pattern.CASE_INSENSITIVE)
                        : RegexCompat.compile(regex);
                
                if (pattern.matcher(content).find()) {
                    valueMatched = true;
                }
            } catch (PatternSyntaxException e) {
                // 忽略非法正则表达式
            }

            if (requireAll && !valueMatched) {
                return false;
            }
            if (!requireAll && valueMatched) {
                return true;
            }
        }
        
        return requireAll;
    }

    /**
     * 匹配二进制数据是否存在
     * @param contentByte 内容
     * @param values 十六进制字符串列表
     * @return 是否匹配成功
     */
    private static boolean matchBinary(byte[] contentByte, List<String> values) {
        return matchBinary(contentByte, values, null);
    }

    private static boolean matchBinary(byte[] contentByte, List<String> values, String condition) {
        if (contentByte == null || values == null || values.isEmpty()) {
            return false;
        }

        boolean requireAll = isAndCondition(condition);
        for (String hexString : values) {
            boolean valueMatched = false;
            try {
                String hexContent = bytesToHexString(contentByte);
                String normalizedHex = hexString == null ? "" : hexString.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
                if (!normalizedHex.isEmpty() && hexContent.contains(normalizedHex)) {
                    valueMatched = true;
                }
            } catch (Exception e) {
                // 忽略非法十六进制字符串
            }

            if (requireAll && !valueMatched) {
                return false;
            }
            if (!requireAll && valueMatched) {
                return true;
            }
        }
        
        return requireAll;
    }

    private static boolean matchJson(String content, List<String> values, String condition) {
        if (content == null || values == null || values.isEmpty()) {
            return false;
        }

        boolean requireAll = isAndCondition(condition);
        for (String value : values) {
            boolean valueMatched = JsonExtractor.matchJson(content, java.util.Collections.singletonList(value));
            if (requireAll && !valueMatched) {
                return false;
            }
            if (!requireAll && valueMatched) {
                return true;
            }
        }

        return requireAll;
    }

    private static boolean matchXpath(String content, List<String> values, String condition) {
        if (content == null || values == null || values.isEmpty()) {
            return false;
        }

        boolean requireAll = isAndCondition(condition);
        for (String value : values) {
            boolean valueMatched = matchSingleXpath(content, value);
            if (requireAll && !valueMatched) {
                return false;
            }
            if (!requireAll && valueMatched) {
                return true;
            }
        }

        return requireAll;
    }

    private static boolean matchSingleXpath(String content, String xpathExpr) {
        if (xpathExpr == null || xpathExpr.trim().isEmpty()) {
            return false;
        }

        try {
            org.jsoup.nodes.Document jsoupDocument = Jsoup.parse(content);
            Document document = new W3CDom().namespaceAware(false).fromJsoup(jsoupDocument);
            XPath xpath = new XPathFactoryImpl().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(xpathExpr, document, XPathConstants.NODESET);
            return nodes != null && nodes.getLength() > 0;
        } catch (Exception ignored) {
        }

        try {
            org.jsoup.nodes.Document jsoupDocument = Jsoup.parse(content);
            Document document = new W3CDom().namespaceAware(false).fromJsoup(jsoupDocument);
            XPath xpath = new XPathFactoryImpl().newXPath();
            String result = xpath.evaluate(xpathExpr, document);
            return result != null && !result.trim().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 匹配内容的哈希值
     * @param contentByte 内容
     * @param values 期望的哈希值列表
     * @param operation 哈希算法类型
     * @return 是否匹配成功
     */
    private static boolean matchHash(byte[] contentByte, List<String> values, PocObj.OperationType operation) {
        if (contentByte == null || values == null || values.isEmpty()) {
            return false;
        }

        String md5Hash = calculateMD5(contentByte);
        String sha1Hash = calculateSHA1(contentByte);

        for (String value : values) {
            if (value.equalsIgnoreCase(md5Hash) || value.equalsIgnoreCase(sha1Hash)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 匹配响应时间
     * 
     * @param responseTime 实际响应时间（毫秒）- 内部统一使用毫秒作为标准单位
     * @param values 期望的响应时间列表（原始值，单位由 timeUnit 决定）
     * @param operation 比较操作类型
     * @param timeUnit 时间单位：
     *                 - "ms": 毫秒（Goby、内部标准）
     *                 - "s": 秒（Pocsuite、默认值）
     *                 - null: 默认为秒（向后兼容）
     * @return 是否匹配成功
     * 
     * 说明：
     * - Pocsuite JsonPoc: time 字段单位为秒 (s)
     * - Goby JsonPoc: $time 变量单位为毫秒 (ms)
     * - 内部统一使用毫秒进行比较
     */
    private static boolean matchTime(long responseTime, List<String> values, 
                                     PocObj.OperationType operation, String timeUnit) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        for (String value : values) {
            try {
                double targetTime = Double.parseDouble(value.trim());
                long targetTimeMs;
                
                // 根据时间单位转换为毫秒
                if ("ms".equals(timeUnit)) {
                    // Goby: 已经是毫秒，直接使用
                    targetTimeMs = (long) targetTime;
                } else {
                    // Pocsuite 或默认: 秒转毫秒
                    targetTimeMs = (long) (targetTime * 1000);
                }

                switch (operation) {
                    case NOT_EQUAL:
                        if (responseTime != targetTimeMs) {
                            return true;
                        }
                        break;
                    case GREATER:
                        if (responseTime > targetTimeMs) {
                            return true;
                        }
                        break;
                    case LESS:
                        if (responseTime < targetTimeMs) {
                            return true;
                        }
                        break;
                    case GREATER_EQUAL:
                        if (responseTime >= targetTimeMs) {
                            return true;
                        }
                        break;
                    case LESS_EQUAL:
                        if (responseTime <= targetTimeMs) {
                            return true;
                        }
                        break;
                    default:
                        // 默认使用大于等于（符合 Pocsuite 官方语义）
                        if (responseTime >= targetTimeMs) {
                            return true;
                        }
                }
            } catch (NumberFormatException e) {
                // 忽略非法值
                System.err.println("Invalid time value: " + value);
            }
        }
        
        return false;
    }


    /**
     * 计算字符串的MD5哈希值
     * @param contentByte 内容
     * @return MD5哈希值
     */
    private static String calculateMD5(byte[] contentByte) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(contentByte);
            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * 计算字符串的SHA1哈希值
     * @param contentByte 内容
     * @return SHA1哈希值
     */
    private static String calculateSHA1(byte[] contentByte) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(contentByte);
            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 替换 Goby 风格的变量引用 {{{varName}}}
     * @param values 原始值列表
     * @param pocVariables POC 变量映射
     * @return 替换后的值列表
     */
    private static List<String> substituteGobyVariables(List<String> values, Map<String, Object> pocVariables) {
        if (values == null || values.isEmpty() || pocVariables == null || pocVariables.isEmpty()) {
            return values;
        }
        
        List<String> result = new ArrayList<>(values.size());
        Map<String, String> stringVariables = new HashMap<>();
        for (Map.Entry<String, Object> entry : pocVariables.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                stringVariables.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        // Goby 变量格式: {{{varName}}}
        Pattern pattern = Pattern.compile("\\{\\{\\{([^}]+)\\}\\}\\}");
        
        for (String value : values) {
            if (value == null) {
                result.add(null);
                continue;
            }
            
            Matcher m = pattern.matcher(value);
            StringBuffer sb = new StringBuffer();
            
            while (m.find()) {
                String varName = m.group(1);
                Object varValue = pocVariables.get(varName);
                if (varValue != null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(varValue.toString()));
                } else {
                    // 变量未找到，保留原样
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                }
            }
            m.appendTail(sb);
            result.add(GobyFunctionProcessor.processGobyFunctions(sb.toString(), stringVariables));
        }

        return result;
    }
}
