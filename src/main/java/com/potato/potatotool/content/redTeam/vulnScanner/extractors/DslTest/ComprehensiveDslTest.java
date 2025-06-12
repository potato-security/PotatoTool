package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.*;
import java.util.*;

/**
 * DSL系统综合测试
 */
public class ComprehensiveDslTest {
    
    public static void main(String[] args) {
        System.out.println("=== DSL系统综合测试 ===");
        
        // 创建测试上下文
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World Test");
        context.put("status", "200");
        context.put("header", "Content-Type: application/json");
        context.put("url", "https://example.com/api/test");
        
        // 测试基本函数
        testBasicFunctions(context);
        
        // 测试嵌套函数
        testNestedFunctions(context);
        
        // 测试字符串函数
        testStringFunctions(context);
        
        // 测试数值函数
        testNumericFunctions(context);
        
        // 测试布尔函数
        testBooleanFunctions(context);
        
        // 测试复杂表达式
        testComplexExpressions(context);
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    private static void testBasicFunctions(Map<String, Object> context) {
        System.out.println("\n--- 基本函数测试 ---");
        
        // 测试contains函数
        testFunction("contains(body, \"Hello\")", context, "true");
        testFunction("contains(body, \"xyz\")", context, "false");
        
        // 测试len函数
        testFunction("len(body)", context, "16");
        testFunction("len(\"test\")", context, "4");
    }
    
    private static void testNestedFunctions(Map<String, Object> context) {
        System.out.println("\n--- 嵌套函数测试 ---");
        
        // 测试嵌套函数
        testFunction("len(trim_space(\"  test  \"))", context, "4");
        testFunction("contains(to_lower(body), \"hello\")", context, "true");
        testFunction("len(to_upper(\"hello\"))", context, "5");
    }
    
    private static void testStringFunctions(Map<String, Object> context) {
        System.out.println("\n--- 字符串函数测试 ---");
        
        // 测试字符串处理函数
        testFunction("to_lower(\"HELLO\")", context, "hello");
        testFunction("to_upper(\"hello\")", context, "HELLO");
        testFunction("trim_space(\"  test  \")", context, "test");
        testFunction("substr(\"hello\", 1, 3)", context, "ell");
    }
    
    private static void testNumericFunctions(Map<String, Object> context) {
        System.out.println("\n--- 数值函数测试 ---");
        
        // 测试数值函数
        testFunction("len(\"hello\")", context, "5");
        testFunction("length(\"world\")", context, "5");
    }
    
    private static void testBooleanFunctions(Map<String, Object> context) {
        System.out.println("\n--- 布尔函数测试 ---");
        
        // 测试布尔函数
        testFunction("contains(body, \"Hello\")", context, "true");
        testFunction("starts_with(body, \"Hello\")", context, "true");
        testFunction("ends_with(body, \"Test\")", context, "true");
        testFunction("matches(status, \"200\")", context, "true");
    }
    
    private static void testComplexExpressions(Map<String, Object> context) {
        System.out.println("\n--- 复杂表达式测试 ---");
        
        // 测试复杂嵌套
        testFunction("contains(to_lower(trim_space(\"  HELLO WORLD  \")), \"hello\")", context, "true");
        testFunction("len(to_upper(trim_space(\"  test  \")))", context, "4");
    }
    
    private static void testFunction(String expression, Map<String, Object> context, String expected) {
        try {
            String result = DslEvaluatorRefactored.evaluateFunctionForValue(expression, context);
            boolean passed = Objects.equals(result, expected);
            
            System.out.printf("%-50s => %-10s [%s]%n", 
                expression, 
                result != null ? result : "null", 
                passed ? "PASS" : "FAIL"
            );
            
            if (!passed) {
                System.out.printf("  期望: %s, 实际: %s%n", expected, result);
            }
        } catch (Exception e) {
            System.out.printf("%-50s => ERROR: %s%n", expression, e.getMessage());
        }
    }
}