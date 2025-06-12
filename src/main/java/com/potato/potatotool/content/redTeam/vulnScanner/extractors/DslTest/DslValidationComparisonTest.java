package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DslValidationComparisonTest {
    public static void main(String[] args) {
        System.out.println("=== DSL验证测试对比 ===");
        
        // 使用与DslResultValidationTest完全相同的数据创建方法
        Map<String, Object> response = createTestResponse();
        Map<String, Object> request = createTestRequest();
        
        String bodyContent = (String) response.get("body");
        System.out.println("Body内容: \"" + bodyContent + "\"");
        System.out.println("Body长度: " + bodyContent.length());
        System.out.println();
        
        // 测试问题表达式
        testExpression("len(body) < 10", response, request, false, "body长度不应该小于10");
        testExpression("len(body) > 50", response, request, true, "body长度应该大于50");
        
        // 测试嵌套函数
        testExpression("contains(to_lower(body), \"hello\")", response, request, true, "转小写后应该找到hello");
        
        // 测试regex函数
        testExpression("regex(\"version \\\\d+\\.\\\\d+\\.\\\\d+\", body)", response, request, true, "应该匹配版本号格式");
        
        System.out.println("\n=== 函数支持检查 ===");
        checkFunctionSupport();
    }
    
    private static Map<String, Object> createTestResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        response.put("headers", "Content-Type: application/json\nServer: nginx/1.18.0");
        response.put("status_code", 200);
        response.put("content_length", 75);
        response.put("content_type", "application/json");
        return response;
    }
    
    private static Map<String, Object> createTestRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("url", "https://api.example.com/test");
        request.put("method", "GET");
        request.put("headers", "User-Agent: TestClient/1.0");
        return request;
    }
    
    private static void testExpression(String expression, Map<String, Object> response, Map<String, Object> request, boolean expected, String description) {
        try {
            boolean actual = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            String status = (actual == expected) ? "✓" : "✗";
            String result = (actual == expected) ? "通过" : "失败";
            
            System.out.println(String.format("%s [%s] %s", status, result, description));
            System.out.println(String.format("   表达式: %s", expression));
            System.out.println(String.format("   期望: %s, 实际: %s", expected, actual));
            
            if (actual != expected) {
                System.out.println("   ❌ 结果不匹配！");
                
                // 额外调试信息
                if (expression.contains("len(body)")) {
                    String bodyContent = (String) response.get("body");
                    System.out.println(String.format("   调试: body长度=%d", bodyContent.length()));
                }
            }
            System.out.println();
            
        } catch (Exception e) {
            System.out.println(String.format("✗ [错误] %s", description));
            System.out.println(String.format("   表达式: %s", expression));
            System.out.println(String.format("   异常: %s", e.getMessage()));
            e.printStackTrace();
            System.out.println();
        }
    }
    
    private static void checkFunctionSupport() {
        // 检查各种函数是否被识别
        String[] functions = {"len", "contains", "to_lower", "regex", "startswith", "trim_space"};
        
        for (String func : functions) {
            try {
                String testExpr = func + "(\"test\")";
                if (func.equals("contains") || func.equals("startswith")) {
                    testExpr = func + "(\"test\", \"t\")";
                } else if (func.equals("regex")) {
                    testExpr = func + "(\"t.*\", \"test\")";
                }
                
                Map<String, Object> testContext = new HashMap<>();
                testContext.put("test", "test");
                
                boolean result = DslEvaluatorRefactored.evaluateSingleDsl(testExpr, testContext, new HashMap<>());
                System.out.println(String.format("函数 %s: 支持 (测试表达式: %s, 结果: %s)", func, testExpr, result));
                
            } catch (Exception e) {
                System.out.println(String.format("函数 %s: 不支持或有错误 (%s)", func, e.getMessage()));
            }
        }
    }
}