package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslExpressionParser;

import java.util.HashMap;
import java.util.Map;

public class DslLenFunctionDebugTest {
    public static void main(String[] args) {
        System.out.println("=== DSL len函数调试测试 ===");
        
        // 创建测试数据
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        response.put("status_code", 200);
        
        Map<String, Object> request = new HashMap<>();
        request.put("method", "GET");
        
        String bodyContent = (String) response.get("body");
        System.out.println("实际body内容: \"" + bodyContent + "\"");
        System.out.println("实际body长度: " + bodyContent.length());
        System.out.println();
        
        // 测试len函数的各种用法
        testLenExpression("len(body)", response, request);
        testLenExpression("len(body) > 50", response, request);
        testLenExpression("len(body) < 10", response, request);
        testLenExpression("len(body) == 75", response, request);
        testLenExpression("len(body) >= 75", response, request);
        testLenExpression("len(body) <= 75", response, request);
        
        // 测试解析函数
        System.out.println("\n=== 解析函数测试 ===");
        testParseFunction("len(body)");
        testParseFunction("len(body) < 10");
        testParseFunction("len(body) > 50");
    }
    
    private static void testLenExpression(String expression, Map<String, Object> response, Map<String, Object> request) {
        try {
            boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            System.out.println(String.format("表达式: %s", expression));
            System.out.println(String.format("结果: %s", result));
            
            // 手动验证
            String bodyContent = (String) response.get("body");
            int actualLength = bodyContent.length();
            
            if (expression.equals("len(body)")) {
                System.out.println(String.format("预期: true (长度=%d > 0)", actualLength));
            } else if (expression.equals("len(body) > 50")) {
                boolean expected = actualLength > 50;
                System.out.println(String.format("预期: %s (%d > 50)", expected, actualLength));
            } else if (expression.equals("len(body) < 10")) {
                boolean expected = actualLength < 10;
                System.out.println(String.format("预期: %s (%d < 10)", expected, actualLength));
            } else if (expression.equals("len(body) == 75")) {
                boolean expected = actualLength == 75;
                System.out.println(String.format("预期: %s (%d == 75)", expected, actualLength));
            } else if (expression.equals("len(body) >= 75")) {
                boolean expected = actualLength >= 75;
                System.out.println(String.format("预期: %s (%d >= 75)", expected, actualLength));
            } else if (expression.equals("len(body) <= 75")) {
                boolean expected = actualLength <= 75;
                System.out.println(String.format("预期: %s (%d <= 75)", expected, actualLength));
            }
            
            System.out.println();
            
        } catch (Exception e) {
            System.out.println(String.format("表达式: %s", expression));
            System.out.println(String.format("错误: %s", e.getMessage()));
            System.out.println();
        }
    }
    
    private static void testParseFunction(String expression) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            System.out.println(String.format("表达式: %s", expression));
            System.out.println(String.format("解析参数数量: %d", args.length));
            for (int i = 0; i < args.length; i++) {
                System.out.println(String.format("  参数[%d]: \"%s\"", i, args[i]));
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println(String.format("解析失败: %s - %s", expression, e.getMessage()));
            System.out.println();
        }
    }
}