package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DslDebugTest {
    public static void main(String[] args) {
        System.out.println("=== DSL 调试测试 ===");
        
        // 创建测试数据
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        response.put("headers", "Content-Type: application/json\nServer: nginx/1.18.0");
        response.put("status_code", 200);
        response.put("content_length", 75);
        
        Map<String, Object> request = new HashMap<>();
        request.put("url", "https://api.example.com/test");
        request.put("method", "GET");
        
        // 打印测试数据
        System.out.println("\n--- 测试数据 ---");
        System.out.println("Response body: " + response.get("body"));
        System.out.println("Body length: " + ((String)response.get("body")).length());
        System.out.println("Contains 'Hello': " + ((String)response.get("body")).contains("Hello"));
        System.out.println("Starts with 'Hello': " + ((String)response.get("body")).startsWith("Hello"));
        
        // 测试简单表达式
        System.out.println("\n--- 简单表达式测试 ---");
        testSimpleExpression("contains(body, \"Hello\")", response, request);
        testSimpleExpression("contains(body, \"World\")", response, request);
        testSimpleExpression("contains(body, \"test\")", response, request);
        testSimpleExpression("contains(body, \"missing\")", response, request);
        
        // 测试startswith
        System.out.println("\n--- startswith 测试 ---");
        testSimpleExpression("startswith(body, \"Hello\")", response, request);
        testSimpleExpression("startswith(body, \"World\")", response, request);
        
        // 测试len函数
        System.out.println("\n--- len 函数测试 ---");
        testSimpleExpression("len(body)", response, request);
        testSimpleExpression("len(body) > 50", response, request);
        testSimpleExpression("len(body) < 10", response, request);
        
        // 测试字符串处理函数
        System.out.println("\n--- 字符串处理函数测试 ---");
        testSimpleExpression("to_lower(\"HELLO\")", response, request);
        testSimpleExpression("contains(to_lower(body), \"hello\")", response, request);
        
        // 测试逻辑运算
        System.out.println("\n--- 逻辑运算测试 ---");
        testSimpleExpression("contains(body, \"Hello\") and contains(body, \"World\")", response, request);
        testSimpleExpression("contains(body, \"Hello\") or contains(body, \"missing\")", response, request);
        testSimpleExpression("!contains(body, \"missing\")", response, request);
        
        // 测试数字比较
        System.out.println("\n--- 数字比较测试 ---");
        testSimpleExpression("status_code == 200", response, request);
        testSimpleExpression("content_length > 50", response, request);
        
        // 测试特殊情况
        System.out.println("\n--- 特殊情况测试 ---");
        
        // 创建简单的测试数据
        Map<String, Object> simpleResponse = new HashMap<>();
        simpleResponse.put("body", "Hello");
        
        System.out.println("简单测试数据 body: " + simpleResponse.get("body"));
        testSimpleExpression("contains(body, \"Hello\")", simpleResponse, request);
        testSimpleExpression("startswith(body, \"Hello\")", simpleResponse, request);
        testSimpleExpression("len(body) == 5", simpleResponse, request);
    }
    
    private static void testSimpleExpression(String expression, Object response, Object request) {
        try {
            boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            System.out.println(String.format("✓ %s -> %s", expression, result));
        } catch (Exception e) {
            System.out.println(String.format("✗ %s -> 错误: %s", expression, e.getMessage()));
            e.printStackTrace();
        }
    }
}