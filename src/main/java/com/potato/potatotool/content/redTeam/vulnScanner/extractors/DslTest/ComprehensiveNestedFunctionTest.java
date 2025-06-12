package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

/**
 * 综合嵌套函数测试
 * 测试各种复杂的嵌套函数调用场景
 */
public class ComprehensiveNestedFunctionTest {
    
    public static void main(String[] args) {
        System.out.println("=== 综合嵌套函数测试 ===");
        
        Map<String, Object> context = createTestContext();
        
        // 测试各种嵌套函数组合
        testNestedFunctionCombinations(context);
        
        // 测试复杂的嵌套场景
        testComplexNestedScenarios(context);
        
        // 测试substr函数的嵌套调用
        testSubstrNestedCalls(context);
        
        // 测试regex函数的嵌套调用
        testRegexNestedCalls(context);
        
        // 测试复杂逻辑表达式
        testComplexLogicalExpressions(context);
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    private static Map<String, Object> createTestContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        context.put("status", "success");
        context.put("version", "2.1.0");
        context.put("spaces", "  test data  ");
        return context;
    }
    
    private static void testNestedFunctionCombinations(Map<String, Object> context) {
        System.out.println("\n--- 测试嵌套函数组合 ---");
        
        String[] expressions = {
            "contains(to_lower(body), \"hello\")",
            "len(trim_space(spaces)) == 9",
            "len(to_lower(body)) == len(body)",
            "contains(to_upper(body), \"HELLO\")",
            "len(base64(\"test\")) > 0",
            "len(md5(\"test\")) == 32"
        };
        
        for (String expr : expressions) {
            try {
                boolean result = DslEvaluatorRefactored.evaluateDslExpression(expr, context);
                System.out.println("表达式: " + expr);
                System.out.println("结果: " + result);
                System.out.println();
            } catch (Exception e) {
                System.err.println("测试表达式失败: " + expr + ", 错误: " + e.getMessage());
            }
        }
    }
    
    private static void testComplexNestedScenarios(Map<String, Object> context) {
        System.out.println("\n--- 测试复杂嵌套场景 ---");
        
        String[] expressions = {
            "len(trim_space(to_lower(spaces))) == 9",
            "contains(trim_space(to_lower(body)), \"hello world\")",
            "len(to_upper(trim_space(spaces))) > 5"
        };
        
        for (String expr : expressions) {
            try {
                boolean result = DslEvaluatorRefactored.evaluateDslExpression(expr, context);
                System.out.println("复杂表达式: " + expr);
                System.out.println("结果: " + result);
                System.out.println();
            } catch (Exception e) {
                System.err.println("测试复杂表达式失败: " + expr + ", 错误: " + e.getMessage());
            }
        }
    }
    
    private static void testSubstrNestedCalls(Map<String, Object> context) {
        System.out.println("\n--- 测试substr嵌套调用 ---");
        
        // 测试substr函数的参数也可以是嵌套函数
        try {
            // 这里我们测试substr的基本功能，因为参数通常是数字
            String result1 = DslEvaluatorRefactored.resolveValueOrFunction("substr(to_lower(body), 0, 5)", context);
            System.out.println("substr(to_lower(body), 0, 5) = " + result1);
            
            String result2 = DslEvaluatorRefactored.resolveValueOrFunction("substr(trim_space(spaces), 1, 4)", context);
            System.out.println("substr(trim_space(spaces), 1, 4) = " + result2);
            
        } catch (Exception e) {
            System.err.println("测试substr嵌套调用失败: " + e.getMessage());
        }
    }
    
    private static void testRegexNestedCalls(Map<String, Object> context) {
        System.out.println("\n--- 测试regex嵌套调用 ---");
        
        String[] expressions = {
            "regex(\"hello\", to_lower(body))",
            "regex(\"TEST\", to_upper(spaces))"
        };
        
        for (String expr : expressions) {
            try {
                boolean result = DslEvaluatorRefactored.evaluateDslExpression(expr, context);
                System.out.println("Regex表达式: " + expr);
                System.out.println("结果: " + result);
                System.out.println();
            } catch (Exception e) {
                System.err.println("测试regex表达式失败: " + expr + ", 错误: " + e.getMessage());
            }
        }
    }
    
    private static void testComplexLogicalExpressions(Map<String, Object> context) {
        System.out.println("\n--- 测试复杂逻辑表达式 ---");
        
        // 用户指定的复杂测试表达式
        String complexExpression = "len(trim_space(to_lower(spaces))) == 2 or (contains(trim_space(to_lower(body)), \"hello world\") and len(to_upper(trim_space(spaces))) > 110)";
        
        try {
            System.out.println("测试表达式: " + complexExpression);
            
            // 分析表达式的各个部分
            System.out.println("\n--- 表达式分析 ---");
            
            // 第一部分: len(trim_space(to_lower(spaces))) == 2
            String part1 = "len(trim_space(to_lower(spaces))) == 2";
            boolean result1 = DslEvaluatorRefactored.evaluateDslExpression(part1, context);
            String value1 = DslEvaluatorRefactored.resolveValueOrFunction("len(trim_space(to_lower(spaces)))", context);
            System.out.println("部分1: " + part1);
            System.out.println("  trim_space(to_lower(spaces)) = \"" + DslEvaluatorRefactored.resolveValueOrFunction("trim_space(to_lower(spaces))", context) + "\"");
            System.out.println("  len(trim_space(to_lower(spaces))) = " + value1);
            System.out.println("  结果: " + result1);
            
            // 第二部分: contains(trim_space(to_lower(body)), "hello world")
            String part2a = "contains(trim_space(to_lower(body)), \"hello world\")";
            boolean result2a = DslEvaluatorRefactored.evaluateDslExpression(part2a, context);
            String value2a = DslEvaluatorRefactored.resolveValueOrFunction("trim_space(to_lower(body))", context);
            System.out.println("\n部分2a: " + part2a);
            System.out.println("  trim_space(to_lower(body)) = \"" + value2a + "\"");
            System.out.println("  结果: " + result2a);
            
            // 第三部分: len(to_upper(trim_space(spaces))) > 110
            String part2b = "len(to_upper(trim_space(spaces))) > 110";
            boolean result2b = DslEvaluatorRefactored.evaluateDslExpression(part2b, context);
            String value2b = DslEvaluatorRefactored.resolveValueOrFunction("len(to_upper(trim_space(spaces)))", context);
            System.out.println("\n部分2b: " + part2b);
            System.out.println("  to_upper(trim_space(spaces)) = \"" + DslEvaluatorRefactored.resolveValueOrFunction("to_upper(trim_space(spaces))", context) + "\"");
            System.out.println("  len(to_upper(trim_space(spaces))) = " + value2b);
            System.out.println("  结果: " + result2b);
            
            // 第二部分整体: (contains(...) and len(...) > 110)
            boolean part2Result = result2a && result2b;
            System.out.println("\n部分2整体 (" + part2a + " and " + part2b + "): " + part2Result);
            
            // 整个表达式的逻辑分析
            boolean expectedResult = result1 || part2Result;
            System.out.println("\n--- 逻辑分析 ---");
            System.out.println("表达式结构: (部分1) or (部分2a and 部分2b)");
            System.out.println("逻辑计算: " + result1 + " or (" + result2a + " and " + result2b + ")");
            System.out.println("逻辑计算: " + result1 + " or " + part2Result);
            System.out.println("预期结果: " + expectedResult);
            
            // 实际测试完整表达式
            boolean actualResult = DslEvaluatorRefactored.evaluateDslExpression(complexExpression, context);
            System.out.println("\n--- 实际测试结果 ---");
            System.out.println("实际结果: " + actualResult);
            
            // 验证结果正确性
            if (actualResult == expectedResult) {
                System.out.println("✅ 测试通过！实际结果与预期结果一致");
            } else {
                System.out.println("❌ 测试失败！实际结果与预期结果不一致");
                System.out.println("预期: " + expectedResult + ", 实际: " + actualResult);
            }
            
        } catch (Exception e) {
            System.err.println("测试复杂逻辑表达式失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}