package com.potato.potatotool.utils.ui.highlighters;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 * 提供创建和管理高亮器正则表达式的通用方法
 * @author Potato
 */
public class PatternUtils {
    
    /**
     * 从关键字数组创建关键字模式
     * @param keywords 关键字数组
     * @return 关键字正则表达式模式
     */
    public static String createKeywordPattern(String[] keywords) {
        return "\\b(" + String.join("|", keywords) + ")\\b";
    }
    
    /**
     * 创建常见的括号和分隔符模式映射
     * @return 括号和分隔符模式映射
     */
    public static Map<String, String> createCommonPatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("PAREN", HighlighterConstants.PAREN_PATTERN);
        patterns.put("BRACE", HighlighterConstants.BRACE_PATTERN);
        patterns.put("BRACKET", HighlighterConstants.BRACKET_PATTERN);
        patterns.put("SEMICOLON", HighlighterConstants.SEMICOLON_PATTERN);
        return patterns;
    }
    
    // 缓存已编译的Pattern对象，避免重复创建
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 构建组合模式
     * 将多个命名捕获组模式组合成一个Pattern
     * @param patterns 命名捕获组模式映射
     * @return 编译好的Pattern对象
     */
    public static Pattern buildPattern(Map<String, String> patterns) {
        StringBuilder patternBuilder = new StringBuilder();
        
        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            if (patternBuilder.length() > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append("(?<").append(entry.getKey()).append(">").append(entry.getValue()).append(")");
        }
        
        String patternString = patternBuilder.toString();
        return PATTERN_CACHE.computeIfAbsent(patternString, Pattern::compile);
    }
    
    /**
     * 创建Java语言的模式映射
     * @return Java语言模式映射
     */
    public static Map<String, String> createJavaPatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("KEYWORD", createKeywordPattern(HighlighterConstants.JAVA_KEYWORDS));
        patterns.put("PAREN", HighlighterConstants.PAREN_PATTERN);
        patterns.put("BRACE", HighlighterConstants.BRACE_PATTERN);
        patterns.put("BRACKET", HighlighterConstants.BRACKET_PATTERN);
        patterns.put("SEMICOLON", HighlighterConstants.SEMICOLON_PATTERN);
        patterns.put("STRING", HighlighterConstants.DOUBLE_QUOTE_STRING_PATTERN);
        patterns.put("COMMENT", HighlighterConstants.SINGLE_LINE_COMMENT_PATTERN + "|" + HighlighterConstants.MULTI_LINE_COMMENT_PATTERN);
        return patterns;
    }
    
    /**
     * 创建Java语言的Pattern对象
     * @return 编译好的Java语言Pattern对象
     */
    public static Pattern createJavaPattern() {
        return buildPattern(createJavaPatterns());
    }
    
    /**
     * 创建Python语言的模式映射
     * @return Python语言模式映射
     */
    public static Map<String, String> createPythonPatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("KEYWORD", createKeywordPattern(HighlighterConstants.PYTHON_KEYWORDS));
        patterns.put("PAREN", HighlighterConstants.PAREN_PATTERN);
        patterns.put("BRACE", HighlighterConstants.BRACE_PATTERN);
        patterns.put("BRACKET", HighlighterConstants.BRACKET_PATTERN);
        patterns.put("SEMICOLON", HighlighterConstants.SEMICOLON_PATTERN);
        patterns.put("STRING", HighlighterConstants.DOUBLE_QUOTE_STRING_PATTERN + "|" + HighlighterConstants.SINGLE_QUOTE_STRING_PATTERN);
        patterns.put("COMMENT", HighlighterConstants.PYTHON_COMMENT_PATTERN);
        return patterns;
    }
    
    /**
     * 创建Python语言的Pattern对象
     * @return 编译好的Python语言Pattern对象
     */
    public static Pattern createPythonPattern() {
        return buildPattern(createPythonPatterns());
    }
    
    /**
     * 创建JavaScript语言的模式映射
     * @return JavaScript语言模式映射
     */
    public static Map<String, String> createJavaScriptPatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("KEYWORD", createKeywordPattern(HighlighterConstants.JAVASCRIPT_KEYWORDS));
        patterns.put("PAREN", HighlighterConstants.PAREN_PATTERN);
        patterns.put("BRACE", HighlighterConstants.BRACE_PATTERN);
        patterns.put("BRACKET", HighlighterConstants.BRACKET_PATTERN);
        patterns.put("SEMICOLON", HighlighterConstants.SEMICOLON_PATTERN);
        patterns.put("STRING", HighlighterConstants.DOUBLE_QUOTE_STRING_PATTERN + "|" + HighlighterConstants.SINGLE_QUOTE_STRING_PATTERN);
        patterns.put("COMMENT", HighlighterConstants.SINGLE_LINE_COMMENT_PATTERN + "|" + HighlighterConstants.MULTI_LINE_COMMENT_PATTERN);
        return patterns;
    }
    
    /**
     * 创建JavaScript语言的Pattern对象
     * @return 编译好的JavaScript语言Pattern对象
     */
    public static Pattern createJavaScriptPattern() {
        return buildPattern(createJavaScriptPatterns());
    }
    
    /**
     * 创建JSON语言的模式映射
     * @return JSON语言模式映射
     */
    public static Map<String, String> createJsonPatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("PAREN", HighlighterConstants.PAREN_PATTERN);
        patterns.put("BRACE", HighlighterConstants.BRACE_PATTERN);
        patterns.put("BRACKET", HighlighterConstants.BRACKET_PATTERN);
        patterns.put("DOUBLEQUOTES", HighlighterConstants.DOUBLE_QUOTE_STRING_PATTERN);
        patterns.put("SINGLEQUOTES", HighlighterConstants.SINGLE_QUOTE_STRING_PATTERN);
        return patterns;
    }
    
    /**
     * 创建JSON语言的Pattern对象
     * @return 编译好的JSON语言Pattern对象
     */
    public static Pattern createJsonPattern() {
        return buildPattern(createJsonPatterns());
    }
    
    /**
     * 创建JavaScript语言的模式映射
     * @return JavaScript语言模式映射
     */
    public static Map<String, String> createJavaScriptDetailedPatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("KEYWORD", "\\b(" + String.join("|", HighlighterConstants.JAVASCRIPT_KEYWORDS) + ")\\b");
        patterns.put("BUILTIN", "\\b(" + String.join("|", HighlighterConstants.JAVASCRIPT_BUILTINS) + ")\\b");
        patterns.put("PAREN", HighlighterConstants.PAREN_PATTERN);
        patterns.put("BRACE", HighlighterConstants.BRACE_PATTERN);
        patterns.put("BRACKET", HighlighterConstants.BRACKET_PATTERN);
        patterns.put("SEMICOLON", HighlighterConstants.SEMICOLON_PATTERN);
        patterns.put("STRING", HighlighterConstants.JAVASCRIPT_STRING_PATTERN);
        patterns.put("COMMENT", HighlighterConstants.JAVASCRIPT_COMMENT_PATTERN);
        patterns.put("NUMBER", "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b");
        patterns.put("FUNCTION", "\\b[a-zA-Z]\\w*\\s*(?=\\()");
        patterns.put("TEMPLATE", "`([^`\\\\]|\\\\.)*`");
        return patterns;
    }
    
    /**
     * 创建JavaScript语言的详细Pattern对象
     * @return 编译好的JavaScript语言Pattern对象
     */
    public static Pattern createJavaScriptDetailedPattern() {
        return buildPattern(createJavaScriptDetailedPatterns());
    }
    
    /**
     * 创建通用的字符串模式
     * @return 字符串模式
     */
    public static String createStringPattern() {
        return HighlighterConstants.DOUBLE_QUOTE_STRING_PATTERN;
    }
    
    /**
     * 创建通用的单行注释模式
     * @param commentPrefix 注释前缀，如 // 或 #
     * @return 单行注释模式
     */
    public static String createSingleLineCommentPattern(String commentPrefix) {
        return commentPrefix + "[^\n]*";
    }
    
    /**
     * 创建通用的多行注释模式
     * @param startDelimiter 开始分隔符，如 /*
     * @param endDelimiter 结束分隔符，如 *\/
     * @return 多行注释模式
     */
    public static String createMultiLineCommentPattern(String startDelimiter, String endDelimiter) {
        return startDelimiter + "(.|\\R)*?" + endDelimiter;
    }
}