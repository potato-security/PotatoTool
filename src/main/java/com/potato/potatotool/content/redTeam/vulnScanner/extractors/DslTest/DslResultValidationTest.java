package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DslResultValidationTest {
    public static void main(String[] args) {
        System.out.println("=== DSL 判断结果正确性测试 ===");
        
        // 创建测试数据
        Map<String, Object> response = createTestResponse();
        Map<String, Object> request = createTestRequest();
        
        // 执行各类测试
        testBasicFunctionResults(response, request);
        testNegationResults(response, request);
        testLogicalOperatorResults(response, request);
        testComplexExpressionResults(response, request);
        testEdgeCases(response, request);
        
        System.out.println("\n=== 测试完成 ===");
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
    
    private static void testBasicFunctionResults(Object response, Object request) {
        System.out.println("\n--- 基本函数结果验证 ---");
        
        // contains 函数测试
        validateResult("contains(body, \"Hello\")", response, request, true, "应该找到Hello");
        validateResult("contains(body, \"Goodbye\")", response, request, false, "不应该找到Goodbye");
        validateResult("contains(body, \"hello\")", response, request, false, "大小写敏感，不应该找到hello");
        
        // startswith 函数测试
        validateResult("startswith(body, \"Hello\")", response, request, true, "应该以Hello开头");
        validateResult("startswith(body, \"World\")", response, request, false, "不应该以World开头");
        
        // len 函数测试
        validateResult("len(body) > 50", response, request, true, "body长度应该大于50");
        validateResult("len(body) < 10", response, request, false, "body长度不应该小于10");
        
        // regex 函数测试
        validateResult("regex(\"version \\\\d+\\.\\\\d+\\.\\\\d+\", body)", response, request, true, "应该匹配版本号格式");
        validateResult("regex(\"\\\\d{4}-\\\\d{2}-\\\\d{2}\", body)", response, request, false, "不应该匹配日期格式");
        
        // 字符串处理函数测试
        validateResult("contains(to_lower(body), \"hello\")", response, request, true, "转小写后应该找到hello");
        validateResult("len(trim_space(\"  test  \")) == 4", response, request, true, "去空格后长度应该是4");
    }
    
    private static void testNegationResults(Object response, Object request) {
        System.out.println("\n--- 否定函数结果验证 ---");
        
        // 基本否定测试
        validateResult("!contains(body, \"Goodbye\")", response, request, true, "否定：不包含Goodbye应该为true");
        validateResult("!contains(body, \"Hello\")", response, request, false, "否定：不包含Hello应该为false");
        
        // 复杂否定测试
        validateResult("!startswith(body, \"World\")", response, request, true, "否定：不以World开头应该为true");
        validateResult("!regex(\"\\\\d{4}-\\\\d{2}-\\\\d{2}\", body)", response, request, true, "否定：不匹配日期格式应该为true");
    }
    
    private static void testLogicalOperatorResults(Object response, Object request) {
        System.out.println("\n--- 逻辑运算符结果验证 ---");
        
        // AND 运算测试
        validateResult("contains(body, \"Hello\") and contains(body, \"World\")", response, request, true, "AND：包含Hello且包含World");
        validateResult("contains(body, \"Hello\") and contains(body, \"Goodbye\")", response, request, false, "AND：包含Hello但不包含Goodbye");
        
        // OR 运算测试
        validateResult("contains(body, \"Hello\") or contains(body, \"Goodbye\")", response, request, true, "OR：包含Hello或Goodbye");
        validateResult("contains(body, \"Goodbye\") or contains(body, \"Farewell\")", response, request, false, "OR：不包含Goodbye也不包含Farewell");
        
        // 混合逻辑运算
        validateResult("contains(body, \"Hello\") and !contains(body, \"Goodbye\")", response, request, true, "包含Hello且不包含Goodbye");
        validateResult("!contains(body, \"Hello\") or contains(body, \"World\")", response, request, true, "不包含Hello或包含World");
    }
    
    private static void testComplexExpressionResults(Object response, Object request) {
        System.out.println("\n--- 复杂表达式结果验证 ---");
        
        // 括号分组测试
        validateResult("(contains(body, \"Hello\") and contains(body, \"World\")) or contains(body, \"test\")", response, request, true, "括号分组：(Hello且World)或test");
        validateResult("(contains(body, \"Goodbye\") and contains(body, \"World\")) or contains(body, \"missing\")", response, request, false, "括号分组：(Goodbye且World)或missing");
        
        // 多层嵌套测试
        validateResult("contains(body, \"Hello\") and (contains(body, \"version\") or contains(body, \"status\"))", response, request, true, "嵌套：Hello且(version或status)");
        validateResult("!contains(body, \"error\") and (len(body) > 50 and startswith(body, \"Hello\"))", response, request, true, "复杂嵌套：不包含error且(长度>50且以Hello开头)");
        
        // 多函数组合测试
        validateResult("len(body) > 50 and contains(body, \"Hello\") and regex(\"version \\\\d+\\.\\\\d+\\.\\\\d+\", body)", response, request, true, "多函数：长度>50且包含Hello且匹配版本号");
    }
    
    private static void testEdgeCases(Object response, Object request) {
        System.out.println("\n--- 边界情况测试 ---");
        
        // 空字符串测试
        validateResult("contains(body, \"\")", response, request, true, "包含空字符串应该为true");
        
        // 特殊字符测试
        validateResult("contains(body, \"!\") or contains(body, \"?\") or contains(body, \".\")", response, request, true, "包含标点符号");
        
        // 数字比较测试
        validateResult("len(body) == len(body)", response, request, true, "长度自比较应该相等");
        validateResult("len(body) > 0", response, request, true, "长度应该大于0");
        
        // 函数嵌套测试
        validateResult("len(to_lower(body)) == len(body)", response, request, true, "转小写后长度应该不变");
        
        // 复杂正则测试
        validateResult("regex(\".*Hello.*World.*\", body)", response, request, true, "正则：Hello...World模式");
        validateResult("regex(\"^Hello.*success$\", body)", response, request, true, "正则：以Hello开头以success结尾");
    }
    
    private static void validateResult(String expression, Object response, Object request, boolean expected, String description) {
        try {
            boolean actual = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            String status = (actual == expected) ? "✓" : "✗";
            String result = (actual == expected) ? "通过" : "失败";
            
            System.out.println(String.format("%s [%s] %s", status, result, description));
            System.out.println(String.format("   表达式: %s", expression));
            System.out.println(String.format("   期望: %s, 实际: %s", expected, actual));
            
            if (actual != expected) {
                System.out.println("   ❌ 结果不匹配！");
            }
            System.out.println();
            
        } catch (Exception e) {
            System.out.println(String.format("✗ [错误] %s", description));
            System.out.println(String.format("   表达式: %s", expression));
            System.out.println(String.format("   异常: %s", e.getMessage()));
            System.out.println();
        }
    }
}