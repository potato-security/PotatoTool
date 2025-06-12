package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslContextBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DslDebugFailedTest {
    public static void main(String[] args) {
        System.out.println("=== DSL 失败测试调试 ===");
        
        // 创建测试数据
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! This is a test response");
        response.put("status_code", 200);
        
        Map<String, Object> request = new HashMap<>();
        request.put("method", "GET");
        
        // 构建上下文并查看实际值
        Map<String, Object> context = DslContextBuilder.createDslContext(response, request);
        
        System.out.println("\n--- 上下文调试信息 ---");
        System.out.println("body值: '" + context.get("body") + "'");
        System.out.println("body长度: " + (context.get("body") != null ? context.get("body").toString().length() : "null"));
        System.out.println("method值: '" + context.get("method") + "'");
        
        // 测试失败的表达式
        System.out.println("\n--- 失败测试调试 ---");
        
        // 测试06: len(body) < 5 应该返回false
        debugExpression("len(body)", response, request);
        debugExpression("len(body) < 5", response, request);
        
        // 测试10: to_lower(method) == \"post\" 应该返回false
        debugExpression("to_lower(method)", response, request);
        debugExpression("to_lower(method) == \"post\"", response, request);
        
        // 额外的调试测试
        System.out.println("\n--- 额外调试测试 ---");
        debugExpression("method", response, request);
        debugExpression("body", response, request);
        debugExpression("len(\"Hello World! This is a test response\")", response, request);
        debugExpression("to_lower(\"GET\")", response, request);
    }
    
    private static void debugExpression(String expression, Object response, Object request) {
        try {
            boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            System.out.println(String.format("表达式: %s -> 结果: %s", expression, result));
        } catch (Exception e) {
            System.out.println(String.format("表达式: %s -> 错误: %s", expression, e.getMessage()));
            e.printStackTrace();
        }
    }
}