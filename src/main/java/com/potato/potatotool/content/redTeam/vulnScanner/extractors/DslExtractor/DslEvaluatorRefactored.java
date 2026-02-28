package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 重构后的DSL评估器
 * 作为核心接口，整合所有拆分的组件
 * 提供简洁的API用于DSL表达式评估
 */
public class DslEvaluatorRefactored {

    /**
     * 表达式解析缓存 - 避免重复解析相同表达式
     * Key: DSL表达式字符串
     * Value: 解析后的匹配器列表
     */
    private static final Map<String, List<DslExpressionParser.DslMatcher>> EXPRESSION_CACHE = 
        new ConcurrentHashMap<>(256);
    
    /**
     * 最大缓存条目数
     */
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * 未知函数警告记录 - 避免重复警告
     */
    private static final Map<String, Boolean> UNKNOWN_FUNCTION_WARNED = new ConcurrentHashMap<>();

    /**
     * 验证DSL表达式语法
     * 在转换阶段调用，提前发现语法错误
     * 
     * @param dslExpression DSL表达式
     * @throws IllegalArgumentException 如果语法错误
     */
    public static void validateDslSyntax(String dslExpression) {
        if (dslExpression == null || dslExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("DSL表达式不能为空");
        }
        
        try {
            // 使用空上下文进行语法检查
            Map<String, Object> emptyContext = new HashMap<>();
            emptyContext.put("body", "");
            emptyContext.put("header", "");
            emptyContext.put("status", 200);
            
            // 尝试解析表达式
            DslExpressionParser.parseDslToMatchers(Arrays.asList(dslExpression));
            
            // 验证函数名称
            validateFunctionNames(dslExpression);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("DSL表达式语法错误: " + dslExpression + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证DSL表达式中的函数名称
     */
    private static void validateFunctionNames(String expression) {
        // 先移除字符串字面量，避免误判字符串内容为函数调用
        String expressionWithoutStrings = DslUtils.removeStringLiterals(expression);

        // 提取所有函数调用
        Pattern pattern = Pattern.compile("(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(expressionWithoutStrings);

        while (matcher.find()) {
            String functionName = matcher.group(1).toLowerCase();
            if (!DslFunctionTypeManager.isKnownFunction(functionName)) {
                // 排除比较运算符和逻辑运算符
                if (!functionName.matches("(and|or|not|true|false)")) {
                    throw new IllegalArgumentException("未知的DSL函数: " + functionName);
                }
            }
        }
    }

    /**
     * 使用嵌套匹配器匹配DSL表达式（支持多响应）
     * 这是主要的入口方法，支持Nuclei多块raw请求场景
     *
     * @param dslExpressions DSL表达式列表
     * @param response 当前响应对象
     * @param request 请求对象
     * @param responseCache 响应缓存（用于访问body_1, body_2等索引变量）
     * @param stepId 步骤ID（用于从缓存中查找多个响应）
     * @return 是否匹配成功
     */
    public static boolean matchDslWithNestedMatchers(List<String> dslExpressions, Object response, Object request,
                                                     Object responseCache, String stepId) {
        try {
            // 创建DSL上下文（支持多响应）
            Map<String, Object> context;
            if (responseCache instanceof com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache && stepId != null) {
                // 使用多响应版本，提取body_1, body_2, status_code_2等索引变量
                context = DslContextBuilder.createDslContext(response, request,
                    (com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache) responseCache, stepId);
            } else {
                // 回退到原有版本
                context = DslContextBuilder.createDslContext(response, request);
            }

            // 解析DSL表达式为匹配器（使用缓存）
            List<DslExpressionParser.DslMatcher> matchers = getCachedMatchers(dslExpressions);

            // 处理匹配器并返回结果
            return DslMatcherProcessor.processMatchers(matchers, context);

        } catch (Exception e) {
            System.err.println("DSL匹配失败: " + e.getMessage());
            e.printStackTrace();

            // 回退到统一DSL评估
            return evaluateUnifiedDslFallback(dslExpressions, response, request);
        }
    }

    /**
     * 使用嵌套匹配器匹配DSL表达式（兼容原有接口）
     * 这是主要的入口方法
     */
    public static boolean matchDslWithNestedMatchers(List<String> dslExpressions, Object response, Object request) {
        try {
            // 创建DSL上下文
            Map<String, Object> context = DslContextBuilder.createDslContext(response, request);

            // 解析DSL表达式为匹配器（使用缓存）
            List<DslExpressionParser.DslMatcher> matchers = getCachedMatchers(dslExpressions);

            // 处理匹配器并返回结果
            return DslMatcherProcessor.processMatchers(matchers, context);

        } catch (Exception e) {
            System.err.println("DSL匹配失败: " + e.getMessage());
            e.printStackTrace();

            // 回退到统一DSL评估
            return evaluateUnifiedDslFallback(dslExpressions, response, request);
        }
    }

    /**
     * 评估单个DSL表达式
     */
    public static boolean evaluateSingleDsl(String dslExpression, Object response, Object request) {
        try {
            // 创建DSL上下文
            Map<String, Object> context = DslContextBuilder.createDslContext(response, request);
            
            // 直接评估表达式
            return evaluateDslExpression(dslExpression, context);
            
        } catch (Exception e) {
            System.err.println("单个DSL评估失败: " + e.getMessage());
            // 回退到原有方法
            return matchDslWithNestedMatchers(Arrays.asList(dslExpression), response, request);
        }
    }

    /**
     * 评估统一DSL表达式（回退方法）
     */
    public static boolean evaluateUnifiedDslFallback(List<String> dslExpressions, Object response, Object request) {
        try {
            // 创建DSL上下文
            Map<String, Object> context = DslContextBuilder.createDslContext(response, request);
            
            // 逐个评估表达式
            for (String expression : dslExpressions) {
                if (!evaluateDslExpression(expression, context)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("统一DSL评估失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估单个DSL表达式
     */
    public static boolean evaluateDslExpression(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }

        try {
            // 标准化表达式
            String normalizedExpression = DslUtils.normalizeExpression(expression);
            
            // 检查是否包含未识别的函数 - 改进策略：警告但继续尝试评估
            List<String> unrecognizedFunctions = DslUtils.findUnrecognizedFunctions(normalizedExpression);
            if (!unrecognizedFunctions.isEmpty()) {
                // 只警告一次，避免重复日志
                for (String func : unrecognizedFunctions) {
                    if (!UNKNOWN_FUNCTION_WARNED.containsKey(func)) {
                        System.err.println("警告: 发现未识别的DSL函数 '" + func + "'，将尝试继续评估表达式");
                        UNKNOWN_FUNCTION_WARNED.put(func, true);
                    }
                }
                // 不再直接返回false，继续尝试评估已知部分
            }
            
            // 处理逻辑操作符
            if (containsLogicalOperators(normalizedExpression)) {
                return evaluateLogicalExpression(normalizedExpression, context);
            }
            
            // 评估单个条件
            return evaluateSingleCondition(normalizedExpression, context);
            
        } catch (Exception e) {
            System.err.println("评估DSL表达式失败: " + expression + ", 错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查表达式是否包含逻辑操作符
     */
    private static boolean containsLogicalOperators(String expression) {
        return DslConstants.AND_PATTERN.matcher(expression).find() ||
               DslConstants.OR_PATTERN.matcher(expression).find() ||
               DslConstants.NOT_PATTERN.matcher(expression).find();
    }

    /**
     * 评估包含逻辑操作符的表达式
     */
    private static boolean evaluateLogicalExpression(String expression, Map<String, Object> context) {
        // 处理NOT操作符
        if (DslConstants.NOT_PATTERN.matcher(expression).find()) {
            String withoutNot = DslConstants.NOT_PATTERN.matcher(expression).replaceFirst("").trim();
            return !evaluateDslExpression(withoutNot, context);
        }
        
        // 处理OR操作符
        if (DslConstants.OR_PATTERN.matcher(expression).find()) {
            String[] orParts = DslConstants.OR_PATTERN.split(expression);
            for (String part : orParts) {
                if (evaluateDslExpression(part.trim(), context)) {
                    return true;
                }
            }
            return false;
        }
        
        // 处理AND操作符
        if (DslConstants.AND_PATTERN.matcher(expression).find()) {
            String[] andParts = DslConstants.AND_PATTERN.split(expression);
            for (String part : andParts) {
                if (!evaluateDslExpression(part.trim(), context)) {
                    return false;
                }
            }
            return true;
        }
        
        return evaluateSingleCondition(expression, context);
    }

    /**
     * 评估单个条件
     */
    private static boolean evaluateSingleCondition(String expression, Map<String, Object> context) {
        expression = expression.trim();
        
        // 首先检查是否是比较表达式
        String operator = DslUtils.findComparisonOperator(expression);
        if (operator != null) {
            String[] parts = DslExpressionParser.parseComparisonExpression(expression);
            if (parts.length == 3) {
                String leftValue = resolveValueOrFunction(parts[0], context);
                String rightValue = resolveValueOrFunction(parts[2], context);
                return compareValues(leftValue, operator, rightValue);
            }
        }
        
        // 检查函数调用（没有比较操作符的情况）
        if (DslConstants.FUNCTION_PATTERN.matcher(expression).find()) {
            return evaluateFunctionCall(expression, context);
        }
        
        // 默认处理
        return evaluateBasicExpression(expression, context);
    }

    /**
     * 评估函数调用（返回boolean结果）
     */
    private static boolean evaluateFunctionCall(String expression, Map<String, Object> context) {
        Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(expression);
        if (matcher.find()) {
            String functionName = matcher.group(1).toLowerCase();
            
            switch (functionName) {
                // 返回boolean的函数
                case "contains":
                case "bcontains":
                    return DslFunctionEvaluator.evaluateContainsFunction(expression, context);
                case "contains_any":
                    return DslFunctionEvaluator.evaluateContainsAnyFunction(expression, context);
                case "contains_all":
                    return DslFunctionEvaluator.evaluateContainsAllFunction(expression, context);
                case "matches":
                case "bmatches":
                    return DslFunctionEvaluator.evaluateMatchesFunction(expression, context);
                case "startswith":
                    return DslFunctionEvaluator.evaluateStartsWithFunction(expression, context);
                case "endswith":
                    return DslFunctionEvaluator.evaluateEndsWithFunction(expression, context);
                case "regex":
                    return DslFunctionEvaluator.evaluateRegexFunction(expression, context);
                case "compare_versions":
                    return DslFunctionEvaluator.evaluateCompareVersionsFunction(expression, context);
                
                // 返回值的函数，需要在比较上下文中使用
                case "len":
                case "length":
                    // len函数应该返回数值，这里检查是否有比较操作
                    try {
                        String[] args = DslExpressionParser.parseContainsFunction(expression);
                        if (args.length >= 1) {
                            String target = resolveValueOrFunction(args[0], context);
                            if (target != null) {
                                String lenValue = String.valueOf(target.length());
                                context.put("__temp_len_result", lenValue);
                                return !lenValue.equals("0"); // 非零长度返回true
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("计算len函数值失败: " + e.getMessage());
                    }
                    return false;
                    
                case "tolowercase":
                case "to_lower":
                case "tolower":  // 添加tolower别名
                case "touppercase":
                case "to_upper":
                case "toupper":  // 添加toupper别名
                case "base64":
                case "base64_py":
                case "md5":
                case "substr":
                case "trim":
                case "trim_space":
                case "urldecode":
                    // 这些函数返回字符串值，在单独调用时返回是否非空
                    String result = evaluateFunctionForValue(expression, context);
                    return result != null && !result.isEmpty();
                    
                case "unixtime":
                    return DslFunctionEvaluator.evaluateUnixtimeFunction(expression, context);
                    
                default:
                    System.err.println("未知的函数: " + functionName);
                    return false;
            }
        }
        
        return false;
    }

    /**
     * 基本表达式评估
     */
    private static boolean evaluateBasicExpression(String expression, Map<String, Object> context) {
        // 简化的表达式评估
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
     * 解析值（从上下文中获取或直接返回字面值）
     */
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
    
    /**
     * 解析值或函数调用结果
     */
    public static String resolveValueOrFunction(String value, Map<String, Object> context) {
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
    
    /**
     * 评估函数并返回字符串值（优化版本 - 使用统一的函数类型管理器）
     */
    public static String evaluateFunctionForValue(String expression, Map<String, Object> context) {
        try {
            String functionName = DslUtils.extractFunctionName(expression);
            if (functionName == null) {
                return null;
            }

            String normalizedFunctionName = functionName.toLowerCase();
            
            // 检查是否为已知函数
            if (!DslFunctionTypeManager.isKnownFunction(normalizedFunctionName)) {
                System.err.println("未知的函数: " + functionName);
                return null;
            }
            
            // 先处理嵌套函数参数
            String processedExpression = preprocessNestedFunctions(expression, context);
            
            // 使用统一的函数评估接口获取字符串值
            String result = DslFunctionTypeManager.evaluateFunctionForStringValue(normalizedFunctionName, processedExpression, context);
            return result;
            
        } catch (Exception e) {
            System.err.println("函数值评估失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
      * 预处理嵌套函数，将嵌套函数调用替换为其结果
      */
     private static String preprocessNestedFunctions(String expression, Map<String, Object> context) {
         try {
             // 直接检查参数中是否包含函数调用
             String functionName = DslUtils.extractFunctionName(expression);
             if (functionName == null) {
                 return expression;
             }
             
             // 提取函数参数
             String[] args = DslUtils.extractFunctionArgs(expression);
             if (args.length == 0) {
                 return expression;
             }
             
             // 处理每个参数，检查是否包含嵌套函数
             for (int i = 0; i < args.length; i++) {
                 String arg = args[i].trim();
                 
                 // 检查参数是否是函数调用
                 if (arg.contains("(") && arg.contains(")")) {
                     String nestedFunctionName = DslUtils.extractFunctionName(arg);
                     
                     if (nestedFunctionName != null && DslFunctionTypeManager.isKnownFunction(nestedFunctionName.toLowerCase())) {
                         // 递归评估嵌套函数
                         String nestedResult = DslUtils.resolveValue(arg, context);
                         
                         if (nestedResult != null) {
                             // 构建新的表达式，替换嵌套函数为其结果
                             String newExpression = expression.replace(arg, "\"" + nestedResult + "\"");
                             return newExpression;
                         }
                     }
                 }
             }
         } catch (Exception e) {
             // 静默处理异常，返回原始表达式
         }
         
         return expression;
     }
     
    /**
     * 统一的函数评估接口获取字符串值（保留原有逻辑作为备用）
     */
    private static String evaluateFunctionForStringValueLegacy(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            // 字符串函数
            case "tolowercase":
            case "to_lower":
            case "tolower":  // 添加tolower别名
                return DslFunctionEvaluator.evaluateToLowerCaseFunction(expression, context);
            case "touppercase":
            case "to_upper":
            case "toupper":  // 添加toupper别名
                return DslFunctionEvaluator.evaluateToUpperCaseFunction(expression, context);
            case "base64":
            case "base64_py":
                return DslFunctionEvaluator.evaluateBase64Function(expression, context);
            case "md5":
                return DslFunctionEvaluator.evaluateMd5Function(expression, context);
            case "substr":
                return DslFunctionEvaluator.evaluateSubstrFunction(expression, context);
            case "trim":
            case "trim_space":
                return DslFunctionEvaluator.evaluateTrimFunction(expression, context);
            case "urldecode":
                return DslFunctionEvaluator.evaluateUrlDecodeFunction(expression, context);
                
            // 布尔函数转字符串
            case "contains":
            case "bcontains":
                return String.valueOf(DslFunctionEvaluator.evaluateContainsFunction(expression, context));
            case "contains_any":
                return String.valueOf(DslFunctionEvaluator.evaluateContainsAnyFunction(expression, context));
            case "contains_all":
                return String.valueOf(DslFunctionEvaluator.evaluateContainsAllFunction(expression, context));
            case "matches":
            case "bmatches":
                return String.valueOf(DslFunctionEvaluator.evaluateMatchesFunction(expression, context));
            case "startswith":
                return String.valueOf(DslFunctionEvaluator.evaluateStartsWithFunction(expression, context));
            case "endswith":
                return String.valueOf(DslFunctionEvaluator.evaluateEndsWithFunction(expression, context));
            case "regex":
                return String.valueOf(DslFunctionEvaluator.evaluateRegexFunction(expression, context));
            case "compare_versions":
                return String.valueOf(DslFunctionEvaluator.evaluateCompareVersionsFunction(expression, context));
                
            // 数值函数转字符串
            case "len":
            case "length":
                try {
                    String[] args = DslExpressionParser.parseContainsFunction(expression);
                    if (args.length >= 1) {
                        String target = resolveValueOrFunction(args[0], context);
                        if (target != null) {
                            return String.valueOf(target.length());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("计算len函数值失败: " + e.getMessage());
                }
                return "0";
                
            case "unixtime":
                long unixTime = System.currentTimeMillis() / 1000;
                context.put("__unixtime_result", unixTime);
                return String.valueOf(unixTime);
                
            default:
                return null;
        }
    }
    




    /**
     * 比较两个值
     */
    private static boolean compareValues(String left, String operator, String right) {
        if (left == null || right == null) {
            return false;
        }

        try {
            // 尝试数值比较
            if (DslUtils.isNumeric(left) && DslUtils.isNumeric(right)) {
                double leftNum = DslUtils.safeParseDouble(left, 0);
                double rightNum = DslUtils.safeParseDouble(right, 0);
                
                switch (operator) {
                    case "==": return Math.abs(leftNum - rightNum) < 1e-9; // 浮点数比较
                    case "!=": return Math.abs(leftNum - rightNum) >= 1e-9;
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
        } catch (Exception e) {
            System.err.println("比较值时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从缓存获取或解析匹配器
     * 
     * @param dslExpressions DSL表达式列表
     * @return 解析后的匹配器列表
     */
    private static List<DslExpressionParser.DslMatcher> getCachedMatchers(List<String> dslExpressions) {
        // 生成缓存键
        String cacheKey = String.join("|||", dslExpressions);
        
        // 尝试从缓存获取
        List<DslExpressionParser.DslMatcher> cached = EXPRESSION_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 缓存未命中，解析表达式
        List<DslExpressionParser.DslMatcher> matchers = DslExpressionParser.parseDslToMatchers(dslExpressions);
        
        // 缓存结果（检查大小限制）
        if (EXPRESSION_CACHE.size() < MAX_CACHE_SIZE) {
            EXPRESSION_CACHE.put(cacheKey, matchers);
        } else {
            // 缓存满了，清除部分旧条目（简单策略：清除一半）
            int removeCount = MAX_CACHE_SIZE / 2;
            EXPRESSION_CACHE.keySet().stream()
                .limit(removeCount)
                .forEach(EXPRESSION_CACHE::remove);
            EXPRESSION_CACHE.put(cacheKey, matchers);
        }
        
        return matchers;
    }
    
    /**
     * 清除表达式缓存
     */
    public static void clearCache() {
        EXPRESSION_CACHE.clear();
        UNKNOWN_FUNCTION_WARNED.clear();
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        return String.format("DSL缓存: %d条表达式, %d个未知函数已警告", 
            EXPRESSION_CACHE.size(), UNKNOWN_FUNCTION_WARNED.size());
    }
    
    /**
     * 获取DSL评估统计信息
     */
    public static String getEvaluationStats() {
        return "DSL评估器已优化，支持表达式缓存和改进的未知函数处理策略";
    }


    /**
     * 获取支持的函数列表
     */
    public static String[] getSupportedFunctions() {
        return DslConstants.KNOWN_FUNCTIONS.clone();
    }

    /**
     * 获取支持的对象列表
     */
    public static String[] getSupportedObjects() {
        return DslConstants.KNOWN_OBJECTS.clone();
    }

    /**
     * 获取函数的返回值类型
     * @param functionName 函数名
     * @return "boolean", "string", "number" 或 "unknown"
     */
    public static String getFunctionReturnType(String functionName) {
        if (functionName == null) {
            return "unknown";
        }
        
        String lowerName = functionName.toLowerCase();
        switch (lowerName) {
            case "contains":
            case "bcontains":
            case "matches":
            case "bmatches":
            case "startswith":
            case "endswith":
            case "regex":
                return "boolean";
                
            case "len":
            case "length":
            case "unixtime":
                return "number";
                
            case "tolowercase":
            case "to_lower":
            case "touppercase":
            case "to_upper":
            case "base64":
            case "base64_py":
            case "md5":
            case "substr":
            case "trim":
            case "trim_space":
            case "urldecode":
                return "string";
                
            default:
                return "unknown";
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private DslEvaluatorRefactored() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}