package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DslCorrectResultTest {
    public static void main(String[] args) {
        System.out.println("=== DSL 判断结果正确性测试 ===");
        
        // 创建测试数据
        Map<String, Object> response = createTestResponse();
        Map<String, Object> request = createTestRequest();
        
        int totalTests = 0;
        int passedTests = 0;
        
        // 基础函数测试
        System.out.println("\n--- 基础函数测试 ---");
        passedTests += testFunction("contains(body, \"Hello\")", true, response, request, ++totalTests);
        passedTests += testFunction("contains(body, \"NotFound\")", false, response, request, ++totalTests);
        passedTests += testFunction("startswith(body, \"Hello\")", true, response, request, ++totalTests);
        passedTests += testFunction("startswith(body, \"World\")", false, response, request, ++totalTests);
        passedTests += testFunction("len(body) > 10", true, response, request, ++totalTests);
        passedTests += testFunction("len(body) < 5", false, response, request, ++totalTests);
        passedTests += testFunction("status_code == 200", true, response, request, ++totalTests);
        passedTests += testFunction("status_code == 404", false, response, request, ++totalTests);
        
        // 字符串处理函数测试
        System.out.println("\n--- 字符串处理函数测试 ---");
        passedTests += testFunction("to_lower(method) == \"get\"", true, response, request, ++totalTests);
        passedTests += testFunction("to_lower(method) == \"post\"", false, response, request, ++totalTests);
        passedTests += testFunction("toupper(method) == \"GET\"", true, response, request, ++totalTests);
        passedTests += testFunction("tolower(method) == \"get\"", true, response, request, ++totalTests);
        
        // 否定操作测试
        System.out.println("\n--- 否定操作测试 ---");
        passedTests += testFunction("!contains(body, \"NotFound\")", true, response, request, ++totalTests);
        passedTests += testFunction("!contains(body, \"Hello\")", false, response, request, ++totalTests);
        passedTests += testFunction("!startswith(body, \"World\")", true, response, request, ++totalTests);
        passedTests += testFunction("!(status_code == 404)", true, response, request, ++totalTests);
        
        // 逻辑组合测试
        System.out.println("\n--- 逻辑组合测试 ---");
        passedTests += testFunction("contains(body, \"Hello\") AND status_code == 200", true, response, request, ++totalTests);
        passedTests += testFunction("contains(body, \"Hello\") AND status_code == 404", false, response, request, ++totalTests);
        passedTests += testFunction("contains(body, \"NotFound\") OR status_code == 200", true, response, request, ++totalTests);
        passedTests += testFunction("contains(body, \"NotFound\") OR status_code == 404", false, response, request, ++totalTests);
        
        // 括号分组测试
        System.out.println("\n--- 括号分组测试 ---");
        passedTests += testFunction("(contains(body, \"Hello\") OR contains(body, \"Hi\")) AND status_code == 200", true, response, request, ++totalTests);
        passedTests += testFunction("(contains(body, \"NotFound\") OR contains(body, \"Missing\")) AND status_code == 200", false, response, request, ++totalTests);
        passedTests += testFunction("!(contains(body, \"NotFound\") AND status_code == 404)", true, response, request, ++totalTests);
        
        // 复杂表达式测试
        System.out.println("\n--- 复杂表达式测试 ---");
        passedTests += testFunction("contains(body, \"Hello\") AND (status_code == 200 OR status_code == 201)", true, response, request, ++totalTests);
        passedTests += testFunction("len(body) > 10 AND startswith(body, \"Hello\") AND status_code == 200", true, response, request, ++totalTests);
        passedTests += testFunction("!contains(body, \"Error\") AND status_code == 200 AND to_lower(method) == \"get\"", true, response, request, ++totalTests);
        
        // 边界情况测试
        System.out.println("\n--- 边界情况测试 ---");
        passedTests += testFunction("len(body) == 33", true, response, request, ++totalTests); // "Hello World! This is a test response" 长度
        passedTests += testFunction("contains(body, \"\")", true, response, request, ++totalTests); // 空字符串包含
        passedTests += testFunction("startswith(body, \"\")", true, response, request, ++totalTests); // 空字符串开头
        
        // 输出测试结果
        System.out.println("\n=== 测试结果汇总 ===");
        System.out.println(String.format("总测试数: %d", totalTests));
        System.out.println(String.format("通过测试: %d", passedTests));
        System.out.println(String.format("失败测试: %d", totalTests - passedTests));
        System.out.println(String.format("通过率: %.1f%%", (double) passedTests / totalTests * 100));
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 所有测试通过！DSL 判断结果完全正确！");
        } else {
            System.out.println("\n⚠️  部分测试失败，需要进一步检查DSL评估逻辑。");
        }
    }
    
    private static Map<String, Object> createTestResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! This is a test response");
        response.put("status_code", 200);
        response.put("headers", "Content-Type: application/json");
        response.put("content_type", "application/json");
        response.put("content_length", 33);
        return response;
    }
    
    private static Map<String, Object> createTestRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("url", "https://example.com/api/test");
        request.put("method", "GET");
        request.put("headers", "User-Agent: TestClient");
        return request;
    }
    
    private static int testFunction(String expression, boolean expected, Object response, Object request, int testNumber) {
        try {
            boolean actual = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            boolean passed = (actual == expected);
            
            String status = passed ? "✓ PASS" : "✗ FAIL";
            System.out.println(String.format("%s [%02d] %s -> 期望: %s, 实际: %s", 
                status, testNumber, expression, expected, actual));
            
            return passed ? 1 : 0;
        } catch (Exception e) {
            System.out.println(String.format("✗ ERROR [%02d] %s -> 异常: %s", 
                testNumber, expression, e.getMessage()));
            return 0;
        }
    }
}