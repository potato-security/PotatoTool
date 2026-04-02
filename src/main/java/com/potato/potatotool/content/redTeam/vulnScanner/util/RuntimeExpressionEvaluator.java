package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.ReverseObject;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.XrayCelEvaluator;

import java.util.*;

/**
 * 运行时表达式评估器
 * 用于处理 POC 中需要在运行时求值的表达式（如 newReverse()、reverse.url 等）
 * 
 * 设计目的：
 * 1. 将运行时表达式处理逻辑从 PocExecutor 中解耦
 * 2. 统一处理 @@expression: 标记的变量
 * 3. 支持对象创建和属性访问
 */
public class RuntimeExpressionEvaluator {
    
    /** 运行时表达式标记前缀 */
    public static final String EXPRESSION_PREFIX = "@@expression:";
    
    /**
     * 检查值是否为运行时表达式
     */
    public static boolean isRuntimeExpression(String value) {
        return value != null && value.startsWith(EXPRESSION_PREFIX);
    }
    
    /**
     * 获取表达式内容（去除前缀）
     */
    public static String getExpressionContent(String value) {
        if (!isRuntimeExpression(value)) {
            return value;
        }
        return value.substring(EXPRESSION_PREFIX.length());
    }
    
    /**
     * 检查变量是否需要保留（不被变量过滤器过滤）
     * 运行时表达式变量可能被其他变量依赖，需要保留
     */
    public static boolean shouldPreserveVariable(String value) {
        if (!isRuntimeExpression(value)) {
            return false;
        }
        String expr = getExpressionContent(value);
        // 检查是否是对象创建或属性引用表达式
        return isObjectCreationExpression(expr) || isPropertyAccessExpression(expr);
    }
    
    /**
     * 检查是否为对象创建表达式（如 newReverse()）
     */
    public static boolean isObjectCreationExpression(String expr) {
        return expr != null && (
            expr.contains("newReverse()") ||
            expr.contains("newHTTPRequest()") ||
            expr.startsWith("new")
        );
    }
    
    /**
     * 检查是否为属性访问表达式（如 reverse.url）
     */
    public static boolean isPropertyAccessExpression(String expr) {
        return expr != null && expr.contains(".");
    }
    
    /**
     * 检查是否为 Goby 函数格式 (@@functionName(args))
     * 注意：不是 @@expression: 格式
     */
    public static boolean isGobyFunction(String value) {
        return value != null && value.startsWith("@@") && !value.startsWith(EXPRESSION_PREFIX) && value.contains("(");
    }
    
    /**
     * 评估 POC 变量，处理运行时表达式
     * 
     * @param pocVariables POC 变量映射 (变量名 -> 值列表)
     * @return 评估后的变量映射 (变量名 -> 对象值)
     */
    public static Map<String, Object> evaluateVariables(Map<String, List<String>> pocVariables) {
        Map<String, Object> result = new HashMap<>();
        if (pocVariables == null || pocVariables.isEmpty()) {
            return result;
        }

        // 第一阶段：处理对象创建表达式
        for (Map.Entry<String, List<String>> entry : pocVariables.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String val = entry.getValue().get(0);
            if (isRuntimeExpression(val)) {
                String expr = getExpressionContent(val);
                if (isObjectCreationExpression(expr)) {
                    Object evalResult = XrayCelEvaluator.evaluateForValue(expr, null, null);
                    result.put(entry.getKey(), evalResult != null ? evalResult : val);
                }
            } else {
                result.put(entry.getKey(), val);
            }
        }

        // 第二阶段：处理属性访问表达式
        for (Map.Entry<String, List<String>> entry : pocVariables.entrySet()) {
            if (result.containsKey(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String val = entry.getValue().get(0);
            if (isRuntimeExpression(val)) {
                String expr = getExpressionContent(val);
                if (isPropertyAccessExpression(expr)) {
                    Object propValue = evaluatePropertyAccess(expr, result);
                    result.put(entry.getKey(), propValue != null ? propValue : expr);
                } else {
                    Object evalResult = XrayCelEvaluator.evaluateForValue(expr, null, null);
                    result.put(entry.getKey(), evalResult != null ? evalResult : val);
                }
            }
        }

        return result;
    }

    public static Map<String, Object> evaluateRuntimeExpressions(Map<String, List<String>> pocVariables) {
        Map<String, Object> result = new HashMap<>();
        if (pocVariables == null || pocVariables.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, List<String>> entry : pocVariables.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String val = entry.getValue().get(0);
            if (isRuntimeExpression(val)) {
                String expr = getExpressionContent(val);
                if (isObjectCreationExpression(expr)) {
                    Object evalResult = XrayCelEvaluator.evaluateForValue(expr, null, null);
                    result.put(entry.getKey(), evalResult != null ? evalResult : val);
                }
            } else {
                result.put(entry.getKey(), val);
            }
        }

        for (Map.Entry<String, List<String>> entry : pocVariables.entrySet()) {
            if (result.containsKey(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String val = entry.getValue().get(0);
            if (isRuntimeExpression(val)) {
                String expr = getExpressionContent(val);
                if (isPropertyAccessExpression(expr)) {
                    Object propValue = evaluatePropertyAccess(expr, result);
                    result.put(entry.getKey(), propValue != null ? propValue : expr);
                } else {
                    Object evalResult = XrayCelEvaluator.evaluateForValue(expr, null, null);
                    result.put(entry.getKey(), evalResult != null ? evalResult : val);
                }
            }
        }

        return result;
    }
    
    /**
     * 评估属性访问表达式
     */
    private static Object evaluatePropertyAccess(String expr, Map<String, Object> context) {
        if (expr == null || !expr.contains(".")) {
            return null;
        }
        String[] parts = expr.split("\\.", 2);
        String varName = parts[0];
        String propName = parts[1];
        
        Object varObj = context.get(varName);
        if (varObj == null) {
            return null;
        }
        
        return getObjectProperty(varObj, propName);
    }
    
    /**
     * 获取对象属性值
     */
    public static Object getObjectProperty(Object obj, String propName) {
        if (obj == null || propName == null) {
            return null;
        }
        
        // 特殊处理 ReverseObject
        if (obj instanceof ReverseObject) {
            ReverseObject reverse = (ReverseObject) obj;
            switch (propName.toLowerCase()) {
                case "url":
                    return reverse.getUrl();
                case "domain":
                    return reverse.getDomain();
                default:
                    return null;
            }
        }
        
        // 通用反射获取属性（作为后备）
        try {
            java.lang.reflect.Method getter = obj.getClass().getMethod(
                "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1)
            );
            return getter.invoke(obj);
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField(propName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception ignored) {}
        }
        
        return null;
    }
}
