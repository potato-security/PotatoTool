package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DslRegexDebugTest {
    public static void main(String[] args) {
        // 创建测试上下文
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        
        // 测试不同的DSL表达式
        String[] expressions = {
            "regex(\"version \\d+\\.\\d+\\.\\d+\", body)",
            "regex(\"version \\\\d+\\\\.\\\\d+\\\\.\\\\d+\", body)",
            "regex(\".*Hello.*\", body)",
            "regex(\"version [0-9]+\\.[0-9]+\\.[0-9]+\", body)"
        };
        
        System.out.println("测试上下文:");
        System.out.println("body = " + context.get("body"));
        System.out.println();
        
        for (String expression : expressions) {
            System.out.println("测试表达式: " + expression);
            
            try {
                // 解析函数参数
                String[] funcArgs = parseRegexFunction(expression);
                if (funcArgs.length >= 2) {
                    String pattern = funcArgs[0];
                    String target = resolveValue(funcArgs[1], context);
                    
                    System.out.println("  解析的模式: " + pattern);
                    System.out.println("  目标文本: " + target);
                    
                    // 移除引号
                    if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
                        pattern = pattern.substring(1, pattern.length() - 1);
                    }
                    
                    System.out.println("  处理后模式: " + pattern);
                    
                    // 测试正则匹配
                    java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
                    boolean matches = regexPattern.matcher(target).find();
                    
                    System.out.println("  匹配结果: " + matches);
                    
                    if (matches) {
                        java.util.regex.Matcher matcher = regexPattern.matcher(target);
                        if (matcher.find()) {
                            System.out.println("  匹配内容: " + matcher.group());
                        }
                    }
                } else {
                    System.out.println("  错误: 参数不足");
                }
            } catch (Exception e) {
                System.out.println("  错误: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
        
        // 测试实际的DSL评估器
        System.out.println("=== 测试实际DSL评估器 ===");
        for (String expression : expressions) {
            System.out.println("表达式: " + expression);
            try {
                boolean result = DslEvaluatorRefactored.evaluateDslExpression(expression, context);
                System.out.println("结果: " + result);
            } catch (Exception e) {
                System.out.println("错误: " + e.getMessage());
            }
            System.out.println();
        }
    }
    
    // 简化的函数参数解析
    private static String[] parseRegexFunction(String expression) {
        // 移除regex(和最后的)
        String content = expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')'));
        
        // 简单的参数分割（这里假设没有嵌套的括号或复杂的引号）
        String[] parts = content.split(",", 2);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
    
    // 简化的值解析
    private static String resolveValue(String value, Map<String, Object> context) {
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        Object contextValue = context.get(value);
        return contextValue != null ? contextValue.toString() : value;
    }
}