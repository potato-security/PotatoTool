package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslContextBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.*;

public class DslSimpleValueTest {
    public static void main(String[] args) {
        System.out.println("=== DSL 函数值计算测试 ===");
        
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
        
        // 测试函数值计算
        System.out.println("\n--- 函数值计算测试 ---");
        
        // 测试len函数
        String bodyValue = (String) context.get("body");
        System.out.println("body原始值: '" + bodyValue + "'");
        System.out.println("body长度: " + (bodyValue != null ? bodyValue.length() : 0));
        
        // 测试to_lower函数
        String methodValue = (String) context.get("method");
        System.out.println("method原始值: '" + methodValue + "'");
        System.out.println("method小写: '" + (methodValue != null ? methodValue.toLowerCase() : "") + "'");
        
        // 测试DSL表达式
        System.out.println("\n--- DSL表达式测试 ---");
        
        String[] testExpressions = {
            "len(body)",
            "len(body) == 36",
            "len(body) < 5",
            "to_lower(method)",
            "to_lower(method) == \"get\"",
            "to_lower(method) == \"post\""
        };
        
        for (String expr : testExpressions) {
            try {
                boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expr, response, request);
                System.out.println("表达式: " + expr + " -> 结果: " + result);
            } catch (Exception e) {
                System.out.println("表达式: " + expr + " -> 错误: " + e.getMessage());
            }
        }
    }
}