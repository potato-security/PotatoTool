package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslExpressionParser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 测试regex函数的实现和修复
 */
public class DslRegexFunctionTest {
    
    public static void main(String[] args) {
        System.out.println("=== DSL Regex函数测试 ===");
        
        // 创建测试上下文
        Map<String, Object> context = createTestContext();
        
        // 测试regex函数
        testRegexFunction(context);
        
        // 测试修复后的contains + to_lower组合
        testNestedFunctions(context);
    }
    
    private static Map<String, Object> createTestContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        context.put("status_code", 200);
        context.put("content_type", "application/json");
        return context;
    }
    
    private static void testRegexFunction(Map<String, Object> context) {
        System.out.println("\n--- 测试regex函数 ---");
        
        String[] regexTests = {
            "regex(\"version \\d+\\.\\d+\\.\\d+\", body)",
            "regex(\"Hello\", body)",
            "regex(\"test\", body)",
            "regex(\"notfound\", body)"
        };
        
        for (String test : regexTests) {
            try {
                boolean result = evaluateRegexFunction(test, context);
                System.out.println(test + " = " + result);
            } catch (Exception e) {
                System.out.println(test + " = ERROR: " + e.getMessage());
            }
        }
    }
    
    private static void testNestedFunctions(Map<String, Object> context) {
        System.out.println("\n--- 测试嵌套函数 ---");
        
        String[] nestedTests = {
            "contains(to_lower(body), \"hello\")",
            "contains(to_lower(body), \"world\")",
            "contains(to_lower(body), \"test\")"
        };
        
        for (String test : nestedTests) {
            try {
                boolean result = evaluateNestedFunction(test, context);
                System.out.println(test + " = " + result);
            } catch (Exception e) {
                System.out.println(test + " = ERROR: " + e.getMessage());
            }
        }
    }
    
    /**
     * 实现regex函数
     */
    public static boolean evaluateRegexFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                System.err.println("regex函数需要至少2个参数: pattern, target");
                return false;
            }

            String pattern = args[0].trim();
            String target = resolveValue(args[1], context);
            
            if (target == null) {
                return false;
            }

            // 移除引号
            if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
                pattern = pattern.substring(1, pattern.length() - 1);
            }
            
            // 编译并匹配正则表达式
            Pattern regexPattern = Pattern.compile(pattern);
            boolean matches = regexPattern.matcher(target).find();
            
            System.out.println("Regex匹配: pattern=" + pattern + ", target=" + target + ", result=" + matches);
            
            return matches;
            
        } catch (Exception e) {
            System.err.println("评估regex函数失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理嵌套函数调用
     */
    public static boolean evaluateNestedFunction(String expression, Map<String, Object> context) {
        try {
            // 解析contains函数
            if (expression.startsWith("contains(")) {
                String[] args = DslExpressionParser.parseContainsFunction(expression);
                if (args.length < 2) {
                    return false;
                }
                
                String firstArg = args[0].trim();
                String secondArg = args[1].trim();
                
                // 如果第一个参数是函数调用，先评估它
                String resolvedFirst;
                if (firstArg.contains("(") && firstArg.contains(")")) {
                    resolvedFirst = evaluateFunctionForValue(firstArg, context);
                } else {
                    resolvedFirst = resolveValue(firstArg, context);
                }
                
                String resolvedSecond = resolveValue(secondArg, context);
                
                if (resolvedFirst == null || resolvedSecond == null) {
                    return false;
                }
                
                boolean result = resolvedFirst.contains(resolvedSecond);
                System.out.println("Contains检查: \"" + resolvedFirst + "\".contains(\"" + resolvedSecond + "\") = " + result);
                return result;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("评估嵌套函数失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 评估函数并返回字符串值
     */
    private static String evaluateFunctionForValue(String expression, Map<String, Object> context) {
        if (expression.startsWith("to_lower(")) {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length > 0) {
                String target = resolveValue(args[0], context);
                return target != null ? target.toLowerCase() : "";
            }
        }
        return "";
    }
    
    /**
     * 解析值
     */
    private static String resolveValue(String value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }
        
        value = value.trim();
        
        // 移除引号
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        
        // 从上下文获取值
        Object contextValue = context.get(value);
        if (contextValue != null) {
            return contextValue.toString();
        }
        
        return value;
    }
}