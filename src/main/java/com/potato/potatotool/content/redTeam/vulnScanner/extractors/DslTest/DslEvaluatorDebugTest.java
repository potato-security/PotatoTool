package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.*;

import java.util.HashMap;
import java.util.Map;

public class DslEvaluatorDebugTest {
    public static void main(String[] args) {
        // 创建测试上下文
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        
        System.out.println("=== 调试DslEvaluatorRefactored.evaluateDslExpression ===");
        
        String expression = "contains(to_lower(body), \"hello\")";
        System.out.println("测试表达式: " + expression);
        
        // 测试表达式标准化
        String normalizedExpression = DslUtils.normalizeExpression(expression);
        System.out.println("标准化后的表达式: " + normalizedExpression);
        
        // 检查是否包含未识别的函数
        boolean hasUnrecognizedFunctions = DslUtils.containsUnrecognizedFunctions(normalizedExpression);
        System.out.println("是否包含未识别的函数: " + hasUnrecognizedFunctions);
        
        // 检查是否包含逻辑操作符
        boolean hasLogicalOperators = containsLogicalOperators(normalizedExpression);
        System.out.println("是否包含逻辑操作符: " + hasLogicalOperators);
        
        // 测试单个条件评估
        System.out.println("\n--- 测试evaluateSingleCondition ---");
        boolean singleConditionResult = evaluateSingleCondition(normalizedExpression, context);
        System.out.println("evaluateSingleCondition结果: " + singleConditionResult);
        
        // 测试比较操作符检查
        String operator = DslUtils.findComparisonOperator(normalizedExpression);
        System.out.println("找到的比较操作符: " + operator);
        
        // 测试函数调用检查
        boolean isFunctionCall = DslConstants.FUNCTION_PATTERN.matcher(normalizedExpression).find();
        System.out.println("是否是函数调用: " + isFunctionCall);
        
        if (isFunctionCall) {
            System.out.println("\n--- 测试evaluateFunctionCall ---");
            boolean functionCallResult = evaluateFunctionCall(normalizedExpression, context);
            System.out.println("evaluateFunctionCall结果: " + functionCallResult);
            
            // 测试函数名提取
            java.util.regex.Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(normalizedExpression);
            if (matcher.find()) {
                String functionName = matcher.group(1).toLowerCase();
                System.out.println("提取的函数名: " + functionName);
                
                if ("contains".equals(functionName)) {
                    System.out.println("\n--- 直接调用DslFunctionEvaluator.evaluateContainsFunction ---");
                    boolean directResult = DslFunctionEvaluator.evaluateContainsFunction(normalizedExpression, context);
                    System.out.println("直接调用结果: " + directResult);
                }
            }
        }
        
        // 最终测试
        System.out.println("\n--- 最终测试DslEvaluatorRefactored.evaluateDslExpression ---");
        boolean finalResult = DslEvaluatorRefactored.evaluateDslExpression(expression, context);
        System.out.println("最终结果: " + finalResult);
    }
    
    // 复制必要的方法
    private static boolean containsLogicalOperators(String expression) {
        return DslConstants.AND_PATTERN.matcher(expression).find() ||
               DslConstants.OR_PATTERN.matcher(expression).find() ||
               DslConstants.NOT_PATTERN.matcher(expression).find();
    }
    
    private static boolean evaluateSingleCondition(String expression, Map<String, Object> context) {
        expression = expression.trim();
        
        // 首先检查是否是比较表达式
        String operator = DslUtils.findComparisonOperator(expression);
        if (operator != null) {
            System.out.println("检测到比较操作符: " + operator);
            String[] parts = DslExpressionParser.parseComparisonExpression(expression);
            if (parts.length == 3) {
                String leftValue = DslEvaluatorRefactored.resolveValueOrFunction(parts[0], context);
                String rightValue = DslEvaluatorRefactored.resolveValueOrFunction(parts[2], context);
                System.out.println("左值: " + leftValue + ", 右值: " + rightValue);
                return compareValues(leftValue, operator, rightValue);
            }
        }
        
        // 检查函数调用（没有比较操作符的情况）
        if (DslConstants.FUNCTION_PATTERN.matcher(expression).find()) {
            System.out.println("检测到函数调用，调用evaluateFunctionCall");
            return evaluateFunctionCall(expression, context);
        }
        
        // 默认处理
        System.out.println("使用默认处理，调用evaluateBasicExpression");
        return evaluateBasicExpression(expression, context);
    }
    
    private static boolean evaluateFunctionCall(String expression, Map<String, Object> context) {
        java.util.regex.Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(expression);
        if (matcher.find()) {
            String functionName = matcher.group(1).toLowerCase();
            System.out.println("函数名: " + functionName);
            
            switch (functionName) {
                case "contains":
                case "bcontains":
                    System.out.println("调用DslFunctionEvaluator.evaluateContainsFunction");
                    return DslFunctionEvaluator.evaluateContainsFunction(expression, context);
                default:
                    System.out.println("未处理的函数: " + functionName);
                    return false;
            }
        }
        return false;
    }
    
    private static boolean compareValues(String leftValue, String operator, String rightValue) {
        // 简化的比较实现
        if ("==".equals(operator)) {
            return leftValue != null && leftValue.equals(rightValue);
        }
        return false;
    }
    
    private static boolean evaluateBasicExpression(String expression, Map<String, Object> context) {
        System.out.println("evaluateBasicExpression被调用，表达式: " + expression);
        return false;
    }
}