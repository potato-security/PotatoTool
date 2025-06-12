package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

/**
 * DSL匹配器处理器
 * 负责执行匹配器并返回结果
 */
public class DslMatcherProcessor {

    /**
     * 处理匹配器列表
     */
    public static boolean processMatchers(List<DslExpressionParser.DslMatcher> matchers, Map<String, Object> context) {
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        // 如果只有一个匹配器，直接处理
        if (matchers.size() == 1) {
            return processSingleMatcher(matchers.get(0), context);
        }

        // 多个匹配器默认使用AND逻辑
        for (DslExpressionParser.DslMatcher matcher : matchers) {
            if (!processSingleMatcher(matcher, context)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 处理单个匹配器
     */
    public static boolean processSingleMatcher(DslExpressionParser.DslMatcher matcher, Map<String, Object> context) {
        if (matcher == null) {
            return false;
        }

        try {
            // 处理逻辑操作符
            if (matcher.getOperator() != null) {
                return processLogicalOperator(matcher, context);
            }

            // 处理具体的匹配器类型
            String type = matcher.getType();
            String expression = matcher.getExpression();

            if (type == null || expression == null) {
                return false;
            }

            switch (type.toLowerCase()) {
                case "contains":
                    return DslFunctionEvaluator.evaluateContainsFunction(expression, context);
                case "matches":
                    return DslFunctionEvaluator.evaluateMatchesFunction(expression, context);
                case "len":
                case "length":
                    return DslFunctionEvaluator.evaluateLenFunction(expression, context);
                case "startswith":
                    return DslFunctionEvaluator.evaluateStartsWithFunction(expression, context);
                case "endswith":
                    return DslFunctionEvaluator.evaluateEndsWithFunction(expression, context);
                case "ignorecase":
                    return processIgnoreCaseMatcher(expression, context);
                case "status":
                    return processStatusMatcher(expression, context);
                case "comparison":
                    return processComparisonMatcher(expression, context);
                case "function":
                    return processFunctionMatcher(expression, context);
                case "dsl":
                default:
                    return processDslMatcher(expression, context);
            }
            
        } catch (Exception e) {
            System.err.println("处理匹配器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理逻辑操作符
     */
    private static boolean processLogicalOperator(DslExpressionParser.DslMatcher matcher, Map<String, Object> context) {
        String operator = matcher.getOperator();
        List<DslExpressionParser.DslMatcher> subMatchers = matcher.getSubMatchers();

        if (subMatchers == null || subMatchers.isEmpty()) {
            return false;
        }

        switch (operator.toUpperCase()) {
            case "AND":
                for (DslExpressionParser.DslMatcher subMatcher : subMatchers) {
                    if (!processSingleMatcher(subMatcher, context)) {
                        return false;
                    }
                }
                return true;
                
            case "OR":
                for (DslExpressionParser.DslMatcher subMatcher : subMatchers) {
                    if (processSingleMatcher(subMatcher, context)) {
                        return true;
                    }
                }
                return false;
                
            case "NOT":
                if (subMatchers.size() > 0) {
                    return !processSingleMatcher(subMatchers.get(0), context);
                }
                return false;
                
            default:
                System.err.println("未知的逻辑操作符: " + operator);
                return false;
        }
    }

    /**
     * 处理忽略大小写匹配器
     */
    private static boolean processIgnoreCaseMatcher(String expression, Map<String, Object> context) {
        try {
            // 提取ignoreCase内部的函数调用
            int startIndex = expression.indexOf('(') + 1;
            int endIndex = expression.lastIndexOf(')');
            
            if (startIndex >= endIndex) {
                return false;
            }
            
            String innerExpression = expression.substring(startIndex, endIndex);
            
            // 创建临时上下文，将所有字符串值转换为小写
            Map<String, Object> tempContext = new java.util.HashMap<>(context);
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (entry.getValue() instanceof String) {
                    tempContext.put(entry.getKey(), ((String) entry.getValue()).toLowerCase());
                }
            }
            
            // 在临时上下文中评估内部表达式
            return DslFunctionEvaluator.evaluateContainsFunction(innerExpression, tempContext);
            
        } catch (Exception e) {
            System.err.println("处理ignoreCase匹配器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理状态码匹配器
     */
    private static boolean processStatusMatcher(String expression, Map<String, Object> context) {
        try {
            String[] parts = DslExpressionParser.parseComparisonExpression(expression);
            if (parts.length != 3 || parts[1].isEmpty()) {
                return false;
            }

            // 获取状态码
            Object statusObj = context.get("status");
            if (statusObj == null) {
                statusObj = context.get("status_code");
            }
            
            if (statusObj == null) {
                return false;
            }

            String statusValue = statusObj.toString();
            String operator = parts[1];
            String expectedValue = DslUtils.cleanStringValue(parts[2]);

            return compareValues(statusValue, operator, expectedValue);
            
        } catch (Exception e) {
            System.err.println("处理状态码匹配器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理比较匹配器
     */
    private static boolean processComparisonMatcher(String expression, Map<String, Object> context) {
        try {
            String[] parts = DslExpressionParser.parseComparisonExpression(expression);
            if (parts.length != 3 || parts[1].isEmpty()) {
                return false;
            }

            String leftValue = DslEvaluatorRefactored.resolveValueOrFunction(parts[0], context);
            String operator = parts[1];
            String rightValue = DslEvaluatorRefactored.resolveValueOrFunction(parts[2], context);

            return compareValues(leftValue, operator, rightValue);
            
        } catch (Exception e) {
            System.err.println("处理比较匹配器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理函数匹配器（优化版本 - 使用统一的函数类型管理器）
     */
    private static boolean processFunctionMatcher(String expression, Map<String, Object> context) {
        try {
            // 提取函数名
            String functionName = DslUtils.extractFunctionName(expression);
            if (functionName == null) {
                return false;
            }

            String normalizedFunctionName = functionName.toLowerCase();
            
            // 检查是否为已知函数
            if (!DslFunctionTypeManager.isKnownFunction(normalizedFunctionName)) {
                System.err.println("未知的函数: " + functionName);
                return false;
            }
            
            // 使用统一的函数评估接口
            return DslFunctionTypeManager.evaluateFunctionAsBoolean(normalizedFunctionName, expression, context);
            
        } catch (Exception e) {
            System.err.println("处理函数匹配器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理DSL匹配器（回退到原始DSL评估）
     */
    private static boolean processDslMatcher(String expression, Map<String, Object> context) {
        try {
            // 这里可以调用原始的DSL评估逻辑
            // 为了简化，这里只做基本的字符串匹配
            return evaluateBasicDslExpression(expression, context);
            
        } catch (Exception e) {
            System.err.println("处理DSL匹配器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 基本的DSL表达式评估
     */
    private static boolean evaluateBasicDslExpression(String expression, Map<String, Object> context) {
        // 简化的DSL评估逻辑
        // 检查表达式中是否包含上下文中的值
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (expression.contains(entry.getKey()) && entry.getValue() != null) {
                String value = entry.getValue().toString();
                if (expression.contains(value)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 比较两个值
     */
    private static boolean compareValues(String left, String operator, String right) {
        if (left == null || right == null) {
            return false;
        }

        // 尝试数值比较
        if (DslUtils.isNumeric(left) && DslUtils.isNumeric(right)) {
            double leftNum = DslUtils.safeParseDouble(left, 0);
            double rightNum = DslUtils.safeParseDouble(right, 0);
            
            switch (operator) {
                case "==": return leftNum == rightNum;
                case "!=": return leftNum != rightNum;
                case ">=": return leftNum >= rightNum;
                case "<=": return leftNum <= rightNum;
                case ">": return leftNum > rightNum;
                case "<": return leftNum < rightNum;
                default: return false;
            }
        }

        // 字符串比较
        switch (operator) {
            case "==": return left.equals(right);
            case "!=": return !left.equals(right);
            case ">=": return left.compareTo(right) >= 0;
            case "<=": return left.compareTo(right) <= 0;
            case ">": return left.compareTo(right) > 0;
            case "<": return left.compareTo(right) < 0;
            default: return false;
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private DslMatcherProcessor() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}