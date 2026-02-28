package com.potato.potatotool.content.redTeam.vulnScanner.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串操作工具类
 * 支持Goby POC中的高级字符串操作
 * 
 * 功能:
 * 1. sub操作 - 子字符串提取（支持索引范围）
 * 2. add操作 - 字符串拼接（支持多变量模板）
 * 
 * @author Potato
 * @date 2025-11-01
 */
public class StringOperations {

    // 变量引用模式: {{{varName}}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\{([^}]+)\\}\\}\\}");

    /**
     * 子字符串提取（sub操作）
     * 
     * 支持的格式:
     * - sub|text       简单文本匹配
     * - sub|5:10       索引5到10
     * - sub|:10        前10个字符
     * - sub|5:         从索引5到结尾
     * - sub|-5:        最后5个字符
     * - sub|:-5        除了最后5个字符
     * 
     * @param source 源字符串
     * @param pattern 提取模式
     * @return 提取的子字符串，失败返回null
     */
    public static String substring(String source, String pattern) {
        if (source == null || pattern == null) {
            return null;
        }

        // 检查是否是索引范围格式
        if (pattern.contains(":")) {
            return substringByRange(source, pattern);
        } else {
            // 简单文本匹配（原有功能）
            return substringByText(source, pattern);
        }
    }

    /**
     * 按索引范围提取子字符串
     * 
     * @param source 源字符串
     * @param range 范围表达式 (如 "5:10", ":10", "5:", "-5:")
     * @return 提取的子字符串
     */
    private static String substringByRange(String source, String range) {
        try {
            String[] parts = range.split(":", -1);  // -1保留空字符串
            int sourceLen = source.length();

            // 解析起始索引
            int start;
            if (parts[0].isEmpty()) {
                start = 0;
            } else {
                start = parseIndex(parts[0], sourceLen);
            }

            // 解析结束索引
            int end;
            if (parts.length < 2 || parts[1].isEmpty()) {
                end = sourceLen;
            } else {
                end = parseIndex(parts[1], sourceLen);
            }

            // 边界检查
            start = Math.max(0, Math.min(start, sourceLen));
            end = Math.max(0, Math.min(end, sourceLen));

            if (start > end) {
                return "";
            }

            return source.substring(start, end);

        } catch (Exception e) {
            System.err.println("子字符串范围提取失败: " + range + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析索引（支持负数）
     * 
     * @param indexStr 索引字符串
     * @param length 字符串长度
     * @return 实际索引位置
     */
    private static int parseIndex(String indexStr, int length) {
        int index = Integer.parseInt(indexStr.trim());
        if (index < 0) {
            // 负数索引：从末尾开始计算
            return length + index;
        }
        return index;
    }

    /**
     * 按文本匹配提取子字符串（原有功能，保持兼容）
     * 
     * @param source 源字符串
     * @param text 要匹配的文本
     * @return 匹配的文本或null
     */
    private static String substringByText(String source, String text) {
        if (source.contains(text)) {
            return text;
        }
        return null;
    }

    /**
     * 字符串拼接（add操作）
     * 
     * 支持的格式:
     * - add|simple_value             简单值
     * - add|{{{var1}}}_{{{var2}}}   多变量模板
     * - add|prefix_{{{var}}}_suffix  混合模板
     * - add|{{{v1}}}-{{{v2}}}-{{{v3}}} 多个变量
     * 
     * @param template 拼接模板
     * @param variables 变量映射
     * @return 拼接结果
     */
    public static String concatenate(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }

        // 如果没有变量引用，直接返回
        if (!template.contains("{{{")) {
            return template;
        }

        // 如果没有提供变量映射，返回原模板
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        // 替换所有变量引用
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = variables.get(varName);

            if (varValue != null) {
                // 转义特殊字符
                matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
            } else {
                // 变量未找到，保留原引用
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                System.err.println("警告: 变量 {{{" + varName + "}}} 未找到");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 字符串拼接（从variables List中获取第一个值）
     * 适配统一的variables格式 Map<String, List<String>>
     * 
     * @param template 拼接模板
     * @param variables 变量映射（List格式）
     * @return 拼接结果
     */
    public static String concatenateFromList(String template, Map<String, List<String>> variables) {
        if (template == null || variables == null) {
            return template;
        }

        // 将List格式转换为String格式（取第一个值）
        Map<String, String> stringVars = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                stringVars.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        return concatenate(template, stringVars);
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        字符串操作工具测试                                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // 测试sub操作
        System.out.println("=== sub操作测试 ===");
        String testStr = "0123456789";
        System.out.println("源字符串: " + testStr);
        System.out.println("sub|5:10 => " + substring(testStr, "5:10"));       // "56789"
        System.out.println("sub|:5   => " + substring(testStr, ":5"));         // "01234"
        System.out.println("sub|5:   => " + substring(testStr, "5:"));         // "56789"
        System.out.println("sub|-5:  => " + substring(testStr, "-5:"));        // "56789"
        System.out.println("sub|:-5  => " + substring(testStr, ":-5"));        // "01234"
        System.out.println("sub|-3:-1 => " + substring(testStr, "-3:-1"));     // "78"

        // 测试add操作
        System.out.println("\n=== add操作测试 ===");
        Map<String, String> vars = new HashMap<>();
        vars.put("scheme", "https");
        vars.put("host", "example.com");
        vars.put("port", "443");
        vars.put("path", "/api/v1");

        String template1 = "{{{scheme}}}://{{{host}}}:{{{port}}}";
        System.out.println("模板: " + template1);
        System.out.println("结果: " + concatenate(template1, vars));

        String template2 = "{{{scheme}}}://{{{host}}}{{{path}}}";
        System.out.println("\n模板: " + template2);
        System.out.println("结果: " + concatenate(template2, vars));

        String template3 = "prefix_{{{host}}}_suffix";
        System.out.println("\n模板: " + template3);
        System.out.println("结果: " + concatenate(template3, vars));
    }
}


