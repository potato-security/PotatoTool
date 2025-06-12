package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslExpressionParser;

import java.util.HashMap;
import java.util.Map;

/**
 * 最终验证测试 - 检查所有DSL函数的修复情况
 */
public class DslFinalValidationTest {
    
    public static void main(String[] args) {
        System.out.println("=== DSL 最终验证测试 ===");
        
        // 创建测试上下文
        Map<String, Object> context = createTestContext();
        
        // 测试regex函数参数顺序
        testRegexParameterOrder(context);
        
        // 测试嵌套函数调用
        testNestedFunctionCalls(context);
        
        // 测试字面值处理
        testLiteralValueHandling(context);
        
        // 运行原始失败的测试
        testOriginalFailures(context);
    }
    
    private static Map<String, Object> createTestContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        context.put("status_code", 200);
        context.put("content_type", "application/json");
        return context;
    }
    
    private static void testRegexParameterOrder(Map<String, Object> context) {
        System.out.println("\n--- 测试regex函数参数顺序 ---");
        
        String bodyContent = (String) context.get("body");
        System.out.println("Body内容: " + bodyContent);
        
        // 测试不同的参数顺序
        String[] regexTests = {
            "regex(\"version \\d+\\.\\d+\\.\\d+\", body)",  // pattern, target
            "regex(body, \"version \\d+\\.\\d+\\.\\d+\")",  // target, pattern
        };
        
        for (String test : regexTests) {
            try {
                System.out.println("\n测试表达式: " + test);
                
                // 解析参数
                String[] args = DslExpressionParser.parseContainsFunction(test);
                System.out.println("解析的参数数量: " + args.length);
                for (int i = 0; i < args.length; i++) {
                    System.out.println("  参数" + i + ": " + args[i]);
                }
                
                // 尝试两种参数顺序
                boolean result1 = testRegexWithOrder(args[0], args[1], context, "pattern, target");
                boolean result2 = testRegexWithOrder(args[1], args[0], context, "target, pattern");
                
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }
    
    private static boolean testRegexWithOrder(String arg1, String arg2, Map<String, Object> context, String order) {
        try {
            String pattern = arg1.trim();
            String target = resolveValue(arg2, context);
            
            System.out.println("  " + order + ": pattern=" + pattern + ", target=" + target);
            
            if (target == null) {
                System.out.println("  结果: false (target为null)");
                return false;
            }
            
            // 移除引号
            if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
                pattern = pattern.substring(1, pattern.length() - 1);
            }
            
            java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
            boolean matches = regexPattern.matcher(target).find();
            
            System.out.println("  结果: " + matches);
            return matches;
            
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
            return false;
        }
    }
    
    private static void testNestedFunctionCalls(Map<String, Object> context) {
        System.out.println("\n--- 测试嵌套函数调用 ---");
        
        String[] nestedTests = {
            "contains(to_lower(body), \"hello\")",
            "len(to_lower(body))",
            "len(trim_space(\"  test  \"))"
        };
        
        for (String test : nestedTests) {
            System.out.println("\n测试表达式: " + test);
            try {
                // 手动解析嵌套函数
                if (test.startsWith("contains(")) {
                    testNestedContains(test, context);
                } else if (test.startsWith("len(")) {
                    testNestedLen(test, context);
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }
    
    private static void testNestedContains(String expression, Map<String, Object> context) {
        String[] args = DslExpressionParser.parseContainsFunction(expression);
        System.out.println("解析的参数: " + java.util.Arrays.toString(args));
        
        if (args.length >= 2) {
            String firstArg = args[0].trim();
            String secondArg = args[1].trim();
            
            System.out.println("第一个参数: " + firstArg);
            System.out.println("第二个参数: " + secondArg);
            
            // 处理第一个参数（可能是函数调用）
            String resolvedFirst;
            if (firstArg.startsWith("to_lower(")) {
                String[] toLowerArgs = DslExpressionParser.parseContainsFunction(firstArg);
                if (toLowerArgs.length > 0) {
                    String target = resolveValue(toLowerArgs[0], context);
                    resolvedFirst = target != null ? target.toLowerCase() : "";
                    System.out.println("to_lower结果: " + resolvedFirst);
                } else {
                    resolvedFirst = "";
                }
            } else {
                resolvedFirst = resolveValue(firstArg, context);
            }
            
            String resolvedSecond = resolveValue(secondArg, context);
            System.out.println("最终比较: \"" + resolvedFirst + "\".contains(\"" + resolvedSecond + "\")");
            
            boolean result = resolvedFirst != null && resolvedSecond != null && resolvedFirst.contains(resolvedSecond);
            System.out.println("结果: " + result);
        }
    }
    
    private static void testNestedLen(String expression, Map<String, Object> context) {
        String[] args = DslExpressionParser.parseContainsFunction(expression);
        System.out.println("解析的参数: " + java.util.Arrays.toString(args));
        
        if (args.length >= 1) {
            String arg = args[0].trim();
            System.out.println("参数: " + arg);
            
            String resolvedArg;
            if (arg.startsWith("to_lower(")) {
                String[] toLowerArgs = DslExpressionParser.parseContainsFunction(arg);
                if (toLowerArgs.length > 0) {
                    String target = resolveValue(toLowerArgs[0], context);
                    resolvedArg = target != null ? target.toLowerCase() : "";
                    System.out.println("to_lower结果: " + resolvedArg);
                } else {
                    resolvedArg = "";
                }
            } else if (arg.startsWith("trim_space(")) {
                String[] trimArgs = DslExpressionParser.parseContainsFunction(arg);
                if (trimArgs.length > 0) {
                    String target = resolveValue(trimArgs[0], context);
                    resolvedArg = target != null ? target.trim() : "";
                    System.out.println("trim_space结果: " + resolvedArg);
                } else {
                    resolvedArg = "";
                }
            } else {
                resolvedArg = resolveValue(arg, context);
            }
            
            int length = resolvedArg != null ? resolvedArg.length() : 0;
            System.out.println("长度: " + length);
        }
    }
    
    private static void testLiteralValueHandling(Map<String, Object> context) {
        System.out.println("\n--- 测试字面值处理 ---");
        
        String[] literals = {
            "\"  test  \"",
            "\"hello\"",
            "body",
            "\"version \\d+\\.\\d+\\.\\d+\""
        };
        
        for (String literal : literals) {
            String resolved = resolveValue(literal, context);
            System.out.println(literal + " -> " + resolved);
        }
    }
    
    private static void testOriginalFailures(Map<String, Object> context) {
        System.out.println("\n--- 测试原始失败的表达式 ---");
        
        String[] failedTests = {
            "regex(\"version \\d+\\.\\d+\\.\\d+\", body)",
            "contains(to_lower(body), \"hello\")",
            "len(trim_space(\"  test  \")) == 4",
            "len(to_lower(body)) == len(body)"
        };
        
        for (String test : failedTests) {
            System.out.println("\n测试: " + test);
            try {
                // 调用实际的DSL评估器
                boolean result = DslEvaluatorRefactored.evaluateDslExpression(test, context);
                System.out.println("结果: " + result);
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }
    
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