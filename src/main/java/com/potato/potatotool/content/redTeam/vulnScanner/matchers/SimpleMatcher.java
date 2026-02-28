package com.potato.potatotool.content.redTeam.vulnScanner.matchers;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Matcher;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatcherType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatchersCondition;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 简化的通用匹配器
 * 用于 DNS、WebSocket、SSL、File、Headless、Code 等非 HTTP 协议的响应匹配
 * 提供统一的匹配逻辑，减少代码重复
 * 
 * @author Potato
 * @date 2025-11-26
 */
public class SimpleMatcher {
    
    /**
     * 匹配响应内容
     * 支持 WORD 和 REGEX 类型的匹配
     * 
     * @param content 响应内容（字符串）
     * @param step POC 步骤（包含匹配器列表和匹配条件）
     * @return 是否匹配成功
     */
    public static boolean match(String content, PocObj.PocStep step) {
        if (step == null || step.getMatchers() == null || step.getMatchers().isEmpty()) {
            return true; // 没有匹配器时默认成功
        }
        
        List<Matcher> matchers = step.getMatchers();
        MatchersCondition condition = step.getMatchersCondition();
        
        return matchWithCondition(content, matchers, condition);
    }
    
    /**
     * 根据条件匹配
     * 
     * @param content 响应内容
     * @param matchers 匹配器列表
     * @param condition 匹配条件（AND/OR）
     * @return 是否匹配成功
     */
    public static boolean matchWithCondition(String content, List<Matcher> matchers, MatchersCondition condition) {
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }
        
        if (content == null) {
            content = "";
        }
        
        // AND 条件：所有匹配器都必须成功
        if (condition == MatchersCondition.AND) {
            for (Matcher matcher : matchers) {
                if (!matchSingle(content, matcher)) {
                    return false;
                }
            }
            return true;
        }
        // OR 条件：任一匹配器成功即可
        else {
            for (Matcher matcher : matchers) {
                if (matchSingle(content, matcher)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * 匹配单个匹配器
     * 
     * @param content 响应内容
     * @param matcher 匹配器
     * @return 是否匹配成功
     */
    public static boolean matchSingle(String content, Matcher matcher) {
        if (matcher == null || matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return true;
        }
        
        MatcherType type = matcher.getType();
        List<String> values = matcher.getValues();
        boolean caseInsensitive = matcher.isCaseInsensitive();
        boolean negative = matcher.isNegative();
        
        boolean result;
        
        switch (type) {
            case WORD:
                result = matchWord(content, values, caseInsensitive, matcher.getCondition());
                break;
            case REGEX:
                result = matchRegex(content, values, caseInsensitive, matcher.getCondition());
                break;
            case STATUS:
                // 状态匹配 - 通常用于 HTTP，简化协议可能不需要
                result = values.stream().anyMatch(v -> content.contains(v));
                break;
            case DSL:
                // DSL 匹配 - 评估 DSL 表达式
                result = matchDsl(content, values, matcher.getCondition());
                break;
            case BINARY:
                // 二进制匹配 - 将内容转为十六进制后匹配
                result = matchBinary(content, values, matcher.getCondition());
                break;
            default:
                // DSL/CEL/XPATH等类型由ResponseMatcher处理，不会走到SimpleMatcher
                System.err.println("[SimpleMatcher] 不支持的匹配类型: " + type);
                result = false;
        }
        
        // 处理反向匹配
        return negative ? !result : result;
    }
    
    /**
     * 关键词匹配
     * 
     * @param content 内容
     * @param words 关键词列表
     * @param caseInsensitive 是否忽略大小写
     * @param condition 内部条件（AND/OR）
     * @return 是否匹配
     */
    private static boolean matchWord(String content, List<String> words, boolean caseInsensitive, String condition) {
        if (content == null || words == null || words.isEmpty()) {
            return false;
        }
        
        String searchContent = caseInsensitive ? content.toLowerCase() : content;
        boolean isAnd = "AND".equalsIgnoreCase(condition);
        
        for (String word : words) {
            String searchWord = caseInsensitive ? word.toLowerCase() : word;
            boolean contains = searchContent.contains(searchWord);
            
            if (isAnd && !contains) {
                return false; // AND 条件下，有一个不包含就失败
            }
            if (!isAnd && contains) {
                return true; // OR 条件下，有一个包含就成功
            }
        }
        
        return isAnd; // AND 条件全部包含返回 true，OR 条件全部不包含返回 false
    }
    
    /**
     * 正则表达式匹配
     * 
     * @param content 内容
     * @param patterns 正则表达式列表
     * @param caseInsensitive 是否忽略大小写
     * @param condition 内部条件（AND/OR）
     * @return 是否匹配
     */
    private static boolean matchRegex(String content, List<String> patterns, boolean caseInsensitive, String condition) {
        if (content == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        
        boolean isAnd = "AND".equalsIgnoreCase(condition);
        
        for (String regex : patterns) {
            try {
                Pattern pattern = caseInsensitive 
                        ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                        : Pattern.compile(regex, Pattern.DOTALL);
                
                boolean matches = pattern.matcher(content).find();
                
                if (isAnd && !matches) {
                    return false;
                }
                if (!isAnd && matches) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                System.err.println("[SimpleMatcher] 正则表达式语法错误: " + regex + " - " + e.getMessage());
                if (isAnd) {
                    return false; // AND 条件下，正则错误视为不匹配
                }
            }
        }
        
        return isAnd;
    }
    
    /**
     * DSL 表达式匹配
     * 
     * @param content 内容
     * @param dslExpressions DSL 表达式列表
     * @param condition 内部条件（AND/OR）
     * @return 是否匹配
     */
    private static boolean matchDsl(String content, List<String> dslExpressions, String condition) {
        if (content == null || dslExpressions == null || dslExpressions.isEmpty()) {
            return false;
        }
        
        boolean isAnd = "AND".equalsIgnoreCase(condition);
        
        // 创建上下文，将 content 作为 response 变量
        Map<String, Object> context = new HashMap<>();
        context.put("response", content);
        context.put("body", content);
        
        for (String dslExpression : dslExpressions) {
            try {
                // 评估 DSL 表达式（返回布尔值）
                boolean matches = DslEvaluatorRefactored.evaluateDslExpression(dslExpression, context);
                
                if (isAnd && !matches) {
                    return false;
                }
                if (!isAnd && matches) {
                    return true;
                }
            } catch (Exception e) {
                System.err.println("[SimpleMatcher] DSL 表达式评估失败: " + dslExpression + " - " + e.getMessage());
                if (isAnd) {
                    return false;
                }
            }
        }
        
        return isAnd;
    }
    
    /**
     * 二进制匹配
     * 将内容转为字节后匹配十六进制模式
     * 
     * @param content 内容
     * @param hexPatterns 十六进制模式列表
     * @param condition 内部条件（AND/OR）
     * @return 是否匹配
     */
    private static boolean matchBinary(String content, List<String> hexPatterns, String condition) {
        if (content == null || hexPatterns == null || hexPatterns.isEmpty()) {
            return false;
        }
        
        boolean isAnd = "AND".equalsIgnoreCase(condition);
        
        // 将内容转为十六进制字符串
        String hexContent = bytesToHex(content.getBytes());
        
        for (String hexPattern : hexPatterns) {
            // 清理十六进制模式（移除空格、0x 前缀等）
            String cleanPattern = hexPattern.replaceAll("\\s+", "")
                    .replaceAll("(?i)0x", "")
                    .toUpperCase();
            
            boolean matches = hexContent.contains(cleanPattern);
            
            if (isAnd && !matches) {
                return false;
            }
            if (!isAnd && matches) {
                return true;
            }
        }
        
        return isAnd;
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
