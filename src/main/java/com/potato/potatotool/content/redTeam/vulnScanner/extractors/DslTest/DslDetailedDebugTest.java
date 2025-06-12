package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslContextBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslExpressionParser;

import java.util.*;

public class DslDetailedDebugTest {
    public static void main(String[] args) {
        System.out.println("=== DSL 详细调试测试 ===");
        
        // 创建测试数据
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! This is a test response");
        response.put("status_code", 200);
        response.put("headers", new HashMap<String, String>());
        
        Map<String, Object> request = new HashMap<>();
        request.put("method", "GET");
        request.put("url", "http://example.com");
        request.put("headers", new HashMap<String, String>());
        
        // 创建上下文
        Map<String, Object> context = DslContextBuilder.createDslContext(response, request);
        
        System.out.println("\n--- 上下文内容 ---");
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        
        // 手动测试函数值计算
        System.out.println("\n--- 手动函数值计算 ---");
        
        // 测试resolveValueOrFunction方法
        try {
            java.lang.reflect.Method resolveMethod = DslEvaluatorRefactored.class.getDeclaredMethod(
                "resolveValueOrFunction", String.class, Map.class);
            resolveMethod.setAccessible(true);
            
            String lenResult = (String) resolveMethod.invoke(null, "len(body)", context);
            System.out.println("len(body) 计算结果: " + lenResult);
            
            String toLowerResult = (String) resolveMethod.invoke(null, "to_lower(method)", context);
            System.out.println("to_lower(method) 计算结果: " + toLowerResult);
            
        } catch (Exception e) {
            System.out.println("反射调用失败: " + e.getMessage());
        }
        
        // 测试比较表达式解析
        System.out.println("\n--- 比较表达式解析测试 ---");
        
        String[] testExpressions = {
            "len(body) < 5",
            "to_lower(method) == \"post\""
        };
        
        for (String expr : testExpressions) {
            try {
                String[] parts = DslExpressionParser.parseComparisonExpression(expr);
                System.out.println("表达式: " + expr);
                System.out.println("  左侧: '" + parts[0] + "'");
                System.out.println("  操作符: '" + parts[1] + "'");
                System.out.println("  右侧: '" + parts[2] + "'");
                
                // 测试左侧值解析
                try {
                    java.lang.reflect.Method resolveMethod = DslEvaluatorRefactored.class.getDeclaredMethod(
                        "resolveValueOrFunction", String.class, Map.class);
                    resolveMethod.setAccessible(true);
                    
                    String leftValue = (String) resolveMethod.invoke(null, parts[0], context);
                    System.out.println("  左侧值: '" + leftValue + "'");
                    
                } catch (Exception e) {
                    System.out.println("  左侧值解析失败: " + e.getMessage());
                }
                
            } catch (Exception e) {
                System.out.println("表达式解析失败: " + expr + " -> " + e.getMessage());
            }
        }
        
        // 测试DSL表达式评估
        System.out.println("\n--- DSL表达式评估测试 ---");
        
        String[] dslTests = {
            "len(body)",
            "len(body) < 5",
            "len(body) == 36",
            "to_lower(method)",
            "to_lower(method) == \"get\"",
            "to_lower(method) == \"post\""
        };
        
        for (String expr : dslTests) {
            try {
                boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expr, response, request);
                System.out.println("表达式: " + expr + " -> 结果: " + result);
            } catch (Exception e) {
                System.out.println("表达式: " + expr + " -> 错误: " + e.getMessage());
            }
        }
    }
}