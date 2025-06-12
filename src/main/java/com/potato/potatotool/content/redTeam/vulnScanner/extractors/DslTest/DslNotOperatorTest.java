package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslExpressionParser;

import java.util.*;

/**
 * DSL NOT操作符测试类
 * 用于验证!操作符和not关键字的功能
 */
public class DslNotOperatorTest {

    public static void main(String[] args) {
        // 创建测试上下文
        Map<String, Object> context = new HashMap<>();
        context.put("body", "This is a test response body with some content");
        context.put("status_code", 200);
        context.put("header", "Content-Type: text/html");

        System.out.println("=== DSL NOT操作符测试 ===");
        System.out.println();

        // 测试用例
        String[] testExpressions = {
            // 使用!操作符的测试
            "!contains(body, \"Access Denied\")",
            "!contains(body, \"test\")",
            "!matches(body, \".*error.*\")",
            "!startsWith(body, \"Error\")",
            "!endsWith(body, \"failed\")",
            
            // 使用not关键字的测试
            "not contains(body, \"Access Denied\")",
            "not contains(body, \"test\")",
            "not matches(body, \".*error.*\")",
            
            // 复合表达式测试
            "!contains(body, \"Access Denied\") and status_code == 200",
            "!contains(body, \"error\") or contains(body, \"success\")",
                "contains(body, \"error\") or contains(body, \"success\")",
                "(contains(body, \"error\")) or contains(body, \"success\")",
                "contains(body, \"error\") or (contains(body, \"success\"))",
                "contains(body, \"error\") or !contains(body, \"success\")",
                "!contains(body, \"error\") or !contains(body, \"success\")",
            "status_code == 200 and !contains(body, \"forbidden\")"
        };

        // 创建模拟的response和request对象
        Object response = new Object() {
            public String getBody() { return (String) context.get("body"); }
            public int getStatusCode() { return (Integer) context.get("status_code"); }
            public String getHeader() { return (String) context.get("header"); }
        };
        Object request = new Object();

        for (String expression : testExpressions) {
            try {
                boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
                System.out.printf("表达式: %s%n", expression);
                System.out.printf("结果: %s%n", result ? "✓ 匹配" : "✗ 不匹配");
                System.out.println("---");
            } catch (Exception e) {
                System.out.printf("表达式: %s%n", expression);
                System.out.printf("错误: %s%n", e.getMessage());
                System.out.println("---");
            }
        }

        System.out.println();
        System.out.println("=== 解析器测试 ===");
        
        // 测试表达式解析
        String[] parseTestExpressions = {
            "!contains(body, \"test\")",
            "not contains(body, \"test\")",
            "!matches(body, \".*error.*\")"
        };

        for (String expression : parseTestExpressions) {
            try {
                List<String> expressions = Arrays.asList(expression);
                List<DslExpressionParser.DslMatcher> matchers = DslExpressionParser.parseDslToMatchers(expressions);
                
                System.out.printf("原始表达式: %s%n", expression);
                for (DslExpressionParser.DslMatcher matcher : matchers) {
                    if (matcher.getOperator() != null) {
                        System.out.printf("解析结果: 操作符=%s, 子匹配器数量=%d%n", 
                            matcher.getOperator(), matcher.getSubMatchers().size());
                    } else {
                        System.out.printf("解析结果: 类型=%s, 表达式=%s%n", 
                            matcher.getType(), matcher.getExpression());
                    }
                }
                System.out.println("---");
            } catch (Exception e) {
                System.out.printf("解析错误: %s - %s%n", expression, e.getMessage());
                System.out.println("---");
            }
        }

        System.out.println();
        System.out.println("=== 实际场景测试 ===");
        
        // 模拟实际的HTTP响应场景
        Map<String, Object> realContext = new HashMap<>();
        realContext.put("body", "<html><head><title>Welcome</title></head><body>Welcome to our website!</body></html>");
        realContext.put("status_code", 200);
        realContext.put("header", "Content-Type: text/html; charset=utf-8");
        
        String[] realTestExpressions = {
            "!contains(body, \"Access Denied\")",  // 应该返回true
            "!contains(body, \"Welcome\")",        // 应该返回false
            "!contains(body, \"error\") and status_code == 200",  // 应该返回true
            "status_code == 200 and !contains(body, \"forbidden\")", // 应该返回true
        };
        
        System.out.println("测试上下文:");
        System.out.println("body: " + realContext.get("body"));
        System.out.println("status_code: " + realContext.get("status_code"));
        System.out.println();
        
        // 创建模拟的response和request对象
        Object realResponse = new Object() {
            public String getBody() { return (String) realContext.get("body"); }
            public int getStatusCode() { return (Integer) realContext.get("status_code"); }
            public String getHeader() { return (String) realContext.get("header"); }
        };
        Object realRequest = new Object();
        
        for (String expression : realTestExpressions) {
            try {
                boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expression, realResponse, realRequest);
                System.out.printf("表达式: %s%n", expression);
                System.out.printf("结果: %s%n", result ? "✓ 匹配" : "✗ 不匹配");
                System.out.println("---");
            } catch (Exception e) {
                System.out.printf("表达式: %s%n", expression);
                System.out.printf("错误: %s%n", e.getMessage());
                System.out.println("---");
            }
        }
    }
}