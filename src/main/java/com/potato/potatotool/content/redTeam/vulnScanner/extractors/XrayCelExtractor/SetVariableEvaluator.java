package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set 变量求值器
 * 在 POC 转换阶段执行 set 中定义的函数调用
 * 
 * 解决问题：
 * - 当前: set.randStr = "randomLowercase(8)" （字符串）
 * - 期望: set.randStr = "abcdefgh" （执行函数后的随机值）
 * 
 * 使用场景：
 * ```yaml
 * set:
 *   randStr: randomLowercase(8)
 *   randNum: randomInt(1000, 9999)
 *   encoded: base64("test")
 *   hash: md5("admin")
 * ```
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class SetVariableEvaluator {
    
    /**
     * 求值单个变量
     * 如果是函数调用则执行，否则返回原值
     * 
     * @param value 变量值（可能是函数调用字符串）
     * @return 求值后的结果
     */
    public static String evaluateVariable(String value) {
        return evaluateVariable(value, new HashMap<>());
    }
    
    /**
     * 求值单个变量（带上下文）
     * 支持函数调用和属性访问
     * 
     * @param value 变量值（可能是函数调用或属性访问）
     * @param context 变量上下文（包含已求值的对象）
     * @return 求值后的结果
     */
    public static String evaluateVariable(String value, Map<String, Object> context) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        String trimmedValue = value.trim();
        
        // 检测是否为属性访问: varName.property (如 reverse.url)
        if (isPropertyAccess(trimmedValue) && context != null) {
            String[] parts = trimmedValue.split("\\.", 2);
            String varName = parts[0];
            String propertyName = parts[1];
            
            Object obj = context.get(varName);
            if (obj instanceof ReverseObject) {
                ReverseObject reverse = (ReverseObject) obj;
                switch (propertyName.toLowerCase()) {
                    case "url":
                        String url = reverse.getUrl();
                        System.out.println("Set 变量属性访问: " + trimmedValue + " -> " + url);
                        return url;
                    case "domain":
                        String domain = reverse.getDomain();
                        System.out.println("Set 变量属性访问: " + trimmedValue + " -> " + domain);
                        return domain;
                    case "ip":
                        String ip = reverse.getIp();
                        System.out.println("Set 变量属性访问: " + trimmedValue + " -> " + ip);
                        return ip;
                }
            }
        }
        
        // 检测是否为函数调用: functionName(args)
        if (isFunctionCall(trimmedValue)) {
            try {
                // 使用传入的 context 或创建空的
                Map<String, Object> evalContext = context != null ? context : new HashMap<>();
                
                // 使用 XrayCelParser 执行函数
                Object result = XrayCelParser.evaluateForValue(trimmedValue, evalContext);
                
                if (result != null) {
                    String resultStr = result.toString();
                    System.out.println("Set 变量函数求值: " + trimmedValue + " -> " + resultStr);
                    return resultStr;
                } else {
                    System.err.println("Set 变量函数求值返回 null: " + trimmedValue);
                    return trimmedValue; // 失败时返回原值
                }
            } catch (Exception e) {
                System.err.println("Set 变量函数执行失败: " + trimmedValue + " - " + e.getMessage());
                return trimmedValue; // 失败时返回原值
            }
        }
        
        // 不是函数调用，直接返回
        return trimmedValue;
    }
    
    /**
     * 检测是否为属性访问
     * 格式: varName.property
     */
    private static boolean isPropertyAccess(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        // 匹配 varName.property 但不是函数调用
        return value.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z_][a-zA-Z0-9_]*$");
    }
    
    /**
     * 检测是否为函数调用
     * 
     * 匹配模式: functionName(...)
     * 示例:
     * - randomLowercase(8) true
     * - base64("test") true
     * - md5("admin") true
     * - "static value" false
     * - 123 false
     * 
     * @param value 待检测的值
     * @return 是否为函数调用
     */
    private static boolean isFunctionCall(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // 匹配: functionName(...)
        // 允许字母、数字、下划线开头，后面跟括号
        // 例如: randomInt(1,100), base64("test"), md5("admin")
        return value.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*\\)$");
    }
    
    /**
     * 求值变量列表
     * 批量处理多个变量值
     * 
     * @param values 变量值列表
     * @return 求值后的列表
     */
    public static List<String> evaluateVariableList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(evaluateVariable(value));
        }
        return result;
    }
    
    /**
     * 检测是否为表达式（不仅仅是函数调用）
     * 用于未来扩展，支持更复杂的表达式
     * 
     * 示例:
     * - url + "/admin" true
     * - string(unixtime()) true
     * - randomInt(10) + 100 true
     * 
     * @param value 待检测的值
     * @return 是否为表达式
     */
    @SuppressWarnings("unused")
    private static boolean isExpression(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // 包含运算符或函数调用
        return value.contains("+") || 
               value.contains("-") || 
               value.contains("*") || 
               value.contains("/") ||
               isFunctionCall(value);
    }
}

