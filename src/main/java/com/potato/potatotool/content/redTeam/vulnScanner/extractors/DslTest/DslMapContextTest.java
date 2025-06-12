package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslContextBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DslMapContextTest {
    public static void main(String[] args) {
        System.out.println("=== DSL Map上下文测试 ===");
        
        // 创建测试数据
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! This is a test response");
        response.put("status_code", 200);
        response.put("headers", "Content-Type: application/json");
        
        Map<String, Object> request = new HashMap<>();
        request.put("url", "https://example.com");
        request.put("method", "GET");
        
        // 测试上下文构建
        System.out.println("\n--- 上下文构建测试 ---");
        Map<String, Object> context = DslContextBuilder.createDslContext(response, request);
        
        System.out.println("构建的上下文内容:");
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        
        // 测试直接访问
        System.out.println("\n--- 直接访问测试 ---");
        System.out.println("原始response.body: " + response.get("body"));
        System.out.println("上下文中的body: " + context.get("body"));
        System.out.println("上下文中的response: " + context.get("response"));
        
        // 测试简单DSL表达式
        System.out.println("\n--- DSL表达式测试 ---");
        testDslExpression("contains(body, \"Hello\")", response, request);
        testDslExpression("len(body) > 10", response, request);
        testDslExpression("status_code == 200", response, request);
    }
    
    private static void testDslExpression(String expression, Object response, Object request) {
        try {
            boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            System.out.println(String.format("✓ %s -> %s", expression, result));
        } catch (Exception e) {
            System.out.println(String.format("✗ %s -> 错误: %s", expression, e.getMessage()));
            e.printStackTrace();
        }
    }
}