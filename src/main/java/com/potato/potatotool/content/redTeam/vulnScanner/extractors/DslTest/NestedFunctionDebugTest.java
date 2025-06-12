package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.*;

import java.util.HashMap;
import java.util.Map;

public class NestedFunctionDebugTest {
    public static void main(String[] args) {
        // 创建测试上下文
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        context.put("method", "GET");
        
        System.out.println("=== 调试嵌套函数调用问题 ===");
        
        // 测试1: 单独的to_lower函数
        System.out.println("\n--- 测试1: 单独的to_lower函数 ---");
        String toLowerResult = evaluateToLowerForValue("to_lower(body)", context);
        System.out.println("to_lower(body) 结果: " + toLowerResult);
        
        // 测试2: 单独的trim_space函数
        System.out.println("\n--- 测试2: 单独的trim_space函数 ---");
        String trimResult = evaluateTrimForValue("trim_space(\"  test  \")", context);
        System.out.println("trim_space(\"  test  \") 结果: [" + trimResult + "]");
        System.out.println("长度: " + trimResult.length());
        
        // 测试3: 单独的len函数
        System.out.println("\n--- 测试3: 单独的len函数 ---");
        String lenResult = evaluateLenForValue("len(body)", context);
        System.out.println("len(body) 结果: " + lenResult);
        
        // 测试4: 嵌套函数调用 - len(to_lower(body))
        System.out.println("\n--- 测试4: 嵌套函数调用 len(to_lower(body)) ---");
        String nestedResult1 = evaluateLenForValue("len(to_lower(body))", context);
        System.out.println("len(to_lower(body)) 结果: " + nestedResult1);
        
        // 测试5: 嵌套函数调用 - len(trim_space(\"  test  \"))
        System.out.println("\n--- 测试5: 嵌套函数调用 len(trim_space(\"  test  \")) ---");
        String nestedResult2 = evaluateLenForValue("len(trim_space(\"  test  \"))", context);
        System.out.println("len(trim_space(\"  test  \")) 结果: " + nestedResult2);
        
        // 测试6: 嵌套函数调用 - contains(to_lower(body), \"hello\")
        System.out.println("\n--- 测试6: 嵌套函数调用 contains(to_lower(body), \"hello\") ---");
        boolean containsResult = DslFunctionEvaluator.evaluateContainsFunction("contains(to_lower(body), \"hello\")", context);
        System.out.println("contains(to_lower(body), \"hello\") 结果: " + containsResult);
        
        // 测试7: 调试resolveValueOrFunction
        System.out.println("\n--- 测试7: 调试resolveValueOrFunction ---");
        String resolvedValue = resolveValueOrFunction("to_lower(body)", context);
        System.out.println("resolveValueOrFunction(\"to_lower(body)\") 结果: " + resolvedValue);
        
        // 测试8: 调试DslExpressionParser.parseContainsFunction
        System.out.println("\n--- 测试8: 调试DslExpressionParser.parseContainsFunction ---");
        String[] args1 = DslExpressionParser.parseContainsFunction("len(to_lower(body))");
        System.out.println("parseContainsFunction(\"len(to_lower(body))\") 参数:");
        for (int i = 0; i < args1.length; i++) {
            System.out.println("  args[" + i + "]: " + args1[i]);
        }
        
        String[] args2 = DslExpressionParser.parseContainsFunction("contains(to_lower(body), \"hello\")");
        System.out.println("parseContainsFunction(\"contains(to_lower(body), \"hello\")\") 参数:");
        for (int i = 0; i < args2.length; i++) {
            System.out.println("  args[" + i + "]: " + args2[i]);
        }
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
    
    private static String evaluateLenForValue(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return "0";
            }

            String target = resolveValueOrFunction(args[0], context);
            if (target == null) {
                return "0";
            }

            return String.valueOf(target.length());
        } catch (Exception e) {
            System.err.println("计算len函数值失败: " + e.getMessage());
            return "0";
        }
    }
    
    private static String resolveValue(String value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }

        value = value.trim();
        
        // 如果是字符串字面值（被引号包围），直接返回内容
        String cleanValue = DslUtils.cleanStringValue(value);
        if (!cleanValue.equals(value)) {
            return cleanValue;
        }

        // 尝试从上下文中获取值
        Object contextValue = context.get(value);
        if (contextValue != null) {
            return contextValue.toString();
        }

        // 如果上下文中没有，返回原值
        return value;
    }
    
    private static String resolveValueOrFunction(String value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }

        value = value.trim();
        
        // 检查是否是函数调用
        if (DslConstants.FUNCTION_PATTERN.matcher(value).find()) {
            return evaluateFunctionForValue(value, context);
        }
        
        // 否则按普通值处理
        return resolveValue(value, context);
    }
    
    private static String evaluateFunctionForValue(String expression, Map<String, Object> context) {
        java.util.regex.Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(expression);
        if (matcher.find()) {
            String functionName = matcher.group(1);
            
            switch (functionName.toLowerCase()) {
                case "len":
                    return evaluateLenForValue(expression, context);
                case "to_lower":
                    return evaluateToLowerForValue(expression, context);
                case "trim":
                case "trim_space":
                    return evaluateTrimForValue(expression, context);
                default:
                    // 对于其他函数，仍然返回boolean结果的字符串表示
                    boolean result = DslEvaluatorRefactored.evaluateDslExpression(expression, context);
                    return String.valueOf(result);
            }
        }
        
        return null;
    }
}