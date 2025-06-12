package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslExpressionParser;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslFunctionEvaluator;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.HashMap;
import java.util.Map;

public class DetailedNestedFunctionDebugTest {
    public static void main(String[] args) {
        // 创建测试上下文
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        context.put("method", "GET");
        
        System.out.println("=== 详细调试嵌套函数调用问题 ===");
        
        // 测试1: 详细调试to_lower函数
        System.out.println("\n--- 测试1: 详细调试to_lower函数 ---");
        String body = (String) context.get("body");
        System.out.println("原始body: " + body);
        System.out.println("原始body长度: " + body.length());
        
        String toLowerResult = evaluateToLowerForValue("to_lower(body)", context);
        System.out.println("to_lower(body) 结果: " + toLowerResult);
        System.out.println("to_lower(body) 长度: " + toLowerResult.length());
        
        System.out.println("手动转换: " + body.toLowerCase());
        System.out.println("手动转换长度: " + body.toLowerCase().length());
        
        // 测试2: 详细调试trim_space函数
        System.out.println("\n--- 测试2: 详细调试trim_space函数 ---");
        String testStr = "  test  ";
        System.out.println("原始字符串: [" + testStr + "]");
        System.out.println("原始字符串长度: " + testStr.length());
        
        String trimResult = evaluateTrimForValue("trim_space(\"  test  \")", context);
        System.out.println("trim_space(\"  test  \") 结果: [" + trimResult + "]");
        System.out.println("trim_space(\"  test  \") 长度: " + trimResult.length());
        
        System.out.println("手动trim: [" + testStr.trim() + "]");
        System.out.println("手动trim长度: " + testStr.trim().length());
        
        // 测试3: 详细调试contains(to_lower(body), "hello")
        System.out.println("\n--- 测试3: 详细调试contains(to_lower(body), \"hello\") ---");
        String lowerBody = body.toLowerCase();
        System.out.println("手动转换后的body: " + lowerBody);
        System.out.println("手动检查是否包含'hello': " + lowerBody.contains("hello"));
        
        // 使用DslFunctionEvaluator.evaluateContainsFunction
        boolean containsResult = DslFunctionEvaluator.evaluateContainsFunction("contains(to_lower(body), \"hello\")", context);
        System.out.println("evaluateContainsFunction结果: " + containsResult);
        
        // 分步调试
        String[] args1 = DslExpressionParser.parseContainsFunction("contains(to_lower(body), \"hello\")");
        System.out.println("parseContainsFunction结果:");
        for (int i = 0; i < args1.length; i++) {
            System.out.println("  args[" + i + "]: " + args1[i]);
        }
        
        String resolvedArg0 = DslFunctionEvaluator.resolveValue(args1[0], context);
        System.out.println("resolveValue(\"" + args1[0] + "\") 结果: " + resolvedArg0);
        System.out.println("resolveValue结果是否包含'hello': " + (resolvedArg0 != null && resolvedArg0.contains("hello")));
        
        // 测试4: 详细调试len(trim_space("  test  "))
        System.out.println("\n--- 测试4: 详细调试len(trim_space(\"  test  \")) ---");
        // 使用DslEvaluatorRefactored.evaluateFunctionForValue来获取len函数的值
        String lenTrimResult = DslEvaluatorRefactored.evaluateFunctionForValue("len(trim_space(\"  test  \"))", context);
        System.out.println("evaluateFunctionForValue(\"len(trim_space(\"  test  \"))\" 结果: " + lenTrimResult);
        
        // 分步调试
        String[] args2 = DslExpressionParser.parseContainsFunction("len(trim_space(\"  test  \"))");
        System.out.println("parseContainsFunction结果:");
        for (int i = 0; i < args2.length; i++) {
            System.out.println("  args[" + i + "]: " + args2[i]);
        }
        
        String resolvedArg2 = DslFunctionEvaluator.resolveValue(args2[0], context);
        System.out.println("resolveValue(\"" + args2[0] + "\") 结果: " + resolvedArg2);
        System.out.println("resolveValue结果长度: " + (resolvedArg2 != null ? resolvedArg2.length() : "null"));
    }
    
    // 复制必要的方法
    private static String evaluateToLowerForValue(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return "";
            }

            String target = resolveValue(args[0], context);
            if (target == null) {
                return "";
            }

            return target.toLowerCase();
        } catch (Exception e) {
            System.err.println("计算to_lower函数值失败: " + e.getMessage());
            return "";
        }
    }
    
    private static String evaluateTrimForValue(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return "";
            }

            String target = resolveValue(args[0], context);
            if (target == null) {
                return "";
            }

            return target.trim();
        } catch (Exception e) {
            System.err.println("计算trim函数值失败: " + e.getMessage());
            return "";
        }
    }
    
    private static String resolveValue(String value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }

        value = value.trim();
        
        // 如果是字符串字面值（被引号包围），直接返回内容
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        // 尝试从上下文中获取值
        Object contextValue = context.get(value);
        if (contextValue != null) {
            return contextValue.toString();
        }

        // 如果上下文中没有，返回原值
        return value;
    }
}