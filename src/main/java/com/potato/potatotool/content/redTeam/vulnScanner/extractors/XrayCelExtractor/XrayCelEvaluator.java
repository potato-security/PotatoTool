package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import java.util.List;
import java.util.Map;

/**
 * Xray CEL (Common Expression Language) 表达式评估器
 * 专门处理 Xray YAML POC 的 CEL 表达式
 * 
 * 特点：
 * 1. 与 Nuclei DSL 完全解耦，独立实现
 * 2. 支持 Xray 特有的函数和语法
 * 3. 支持随机函数、高级编码、哈希等功能
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class XrayCelEvaluator {
    
    /**
     * 评估 Xray CEL 表达式
     * 
     * 示例表达式：
     * - response.status == 200
     * - response.body.contains("admin") && response.status == 200
     * - len(response.body) > 100
     * 
     * @param expression CEL 表达式字符串
     * @param response HTTP 响应对象
     * @param request HTTP 请求对象
     * @return 评估结果
     */
    public static boolean evaluate(String expression, Object response, Object request) {
        return evaluate(expression, response, request, null);
    }
    
    /**
     * 评估 Xray CEL 表达式（带 POC 变量）
     * 
     * @param expression CEL 表达式字符串
     * @param response HTTP 响应对象
     * @param request HTTP 请求对象
     * @param variables POC 变量映射（如 s1, s2）
     * @return 评估结果
     */
    public static boolean evaluate(String expression, Object response, Object request, 
                                   Map<String, ?> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 1. 创建 CEL 上下文
            Map<String, Object> context = XrayCelContext.createContext(response, request);
            
            // 2. 添加 POC 变量到上下文
            if (variables != null) {
                for (Map.Entry<String, ?> entry : variables.entrySet()) {
                    Object value = entry.getValue();
                    // 如果值是 List，取第一个元素（统一变量格式）
                    if (value instanceof List && !((List<?>) value).isEmpty()) {
                        value = ((List<?>) value).get(0);
                    }
                    context.put(entry.getKey(), value);
                }
            }
            
            // 3. 标准化表达式
            String normalizedExpression = normalizeExpression(expression);
            
            // 4. 解析并评估表达式
            return evaluateExpression(normalizedExpression, context);
            
        } catch (Exception e) {
            System.err.println("Xray CEL 评估失败: " + expression);
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 评估规则调用表达式
     * 
     * 示例：r0() && r1() || r2()
     * 
     * @param expression 规则表达式
     * @param ruleResults 规则结果映射 (规则名 -> 是否成功)
     * @return 评估结果
     */
    public static boolean evaluateRuleExpression(String expression, Map<String, Boolean> ruleResults) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 创建上下文，包含规则结果
            Map<String, Object> context = XrayCelContext.createRuleContext(ruleResults);
            
            // 评估表达式
            return evaluateExpression(expression, context);
            
        } catch (Exception e) {
            System.err.println("Xray 规则表达式评估失败: " + expression);
            System.err.println("错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 评估表达式并返回值（用于 output 提取）
     * 
     * @param expression CEL 表达式
     * @param response HTTP 响应对象
     * @param request HTTP 请求对象
     * @return 提取的值
     */
    public static Object evaluateForValue(String expression, Object response, Object request) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 创建上下文
            Map<String, Object> context = XrayCelContext.createContext(response, request);
            
            // 标准化表达式
            String normalizedExpression = normalizeExpression(expression);
            
            // 评估并返回值
            return evaluateForValueInternal(normalizedExpression, context);
            
        } catch (Exception e) {
            System.err.println("Xray CEL 值提取失败: " + expression);
            System.err.println("错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 标准化 CEL 表达式
     * 处理 Xray 特有的语法
     * 
     * 注意：bcontains 和 bmatches 是字节级操作函数，不能简单替换为字符串版本
     * 它们会在 XrayCelParser 中被正确处理，调用 BytesFunctions 中的实现
     */
    private static String normalizeExpression(String expression) {
        if (expression == null) {
            return "";
        }
        
        String normalized = expression.trim();
        
        // 不再替换 bcontains 和 bmatches，它们将被 XrayCelParser 正确处理
        // 这些是字节级操作函数，需要调用 BytesFunctions 中的实现
        
        return normalized;
    }
    
    /**
     * 评估表达式（内部实现）
     */
    private static boolean evaluateExpression(String expression, Map<String, Object> context) {
        // 处理逻辑运算符
        if (containsLogicalOperators(expression)) {
            return evaluateLogicalExpression(expression, context);
        }
        
        // 处理单个条件
        return evaluateSingleCondition(expression, context);
    }
    
    /**
     * 评估表达式并返回值（内部实现）
     */
    private static Object evaluateForValueInternal(String expression, Map<String, Object> context) {
        // 使用解析器评估
        return XrayCelParser.evaluateForValue(expression, context);
    }
    
    /**
     * 检查是否包含逻辑运算符
     */
    private static boolean containsLogicalOperators(String expression) {
        return expression.contains("&&") || 
               expression.contains("||") || 
               expression.contains(" and ") || 
               expression.contains(" or ") ||
               expression.startsWith("!") ||
               expression.contains(" not ");
    }
    
    /**
     * 评估包含逻辑运算符的表达式
     */
    private static boolean evaluateLogicalExpression(String expression, Map<String, Object> context) {
        return XrayCelParser.evaluateLogical(expression, context);
    }
    
    /**
     * 评估单个条件
     */
    private static boolean evaluateSingleCondition(String expression, Map<String, Object> context) {
        return XrayCelParser.evaluateSingle(expression, context);
    }
}

