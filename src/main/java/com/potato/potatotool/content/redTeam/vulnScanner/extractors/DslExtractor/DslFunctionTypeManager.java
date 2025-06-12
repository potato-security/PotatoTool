package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * DSL函数类型管理器
 * 统一管理所有DSL函数的类型分类和处理逻辑
 * 提供高性能的函数类型查询和统一的处理接口
 */
public class DslFunctionTypeManager {

    /**
     * 函数类型枚举
     */
    public enum FunctionType {
        BOOLEAN_FUNCTION,    // 返回boolean值的函数
        STRING_FUNCTION,     // 返回String值的函数
        NUMERIC_FUNCTION,    // 返回数值的函数
        UNKNOWN_FUNCTION     // 未知函数
    }

    // 预定义的函数类型集合，提高查询性能
    private static final Set<String> BOOLEAN_FUNCTIONS = new HashSet<>();
    private static final Set<String> STRING_FUNCTIONS = new HashSet<>();
    private static final Set<String> NUMERIC_FUNCTIONS = new HashSet<>();

    static {
        // 初始化boolean函数集合
        BOOLEAN_FUNCTIONS.add("contains");
        BOOLEAN_FUNCTIONS.add("bcontains");
        BOOLEAN_FUNCTIONS.add("startswith");
        BOOLEAN_FUNCTIONS.add("starts_with");
        BOOLEAN_FUNCTIONS.add("endswith");
        BOOLEAN_FUNCTIONS.add("ends_with");
        BOOLEAN_FUNCTIONS.add("matches");
        BOOLEAN_FUNCTIONS.add("bmatches");
        BOOLEAN_FUNCTIONS.add("regex");

        // 初始化字符串函数集合
        STRING_FUNCTIONS.add("tolowercase");
        STRING_FUNCTIONS.add("to_lower");
        STRING_FUNCTIONS.add("touppercase");
        STRING_FUNCTIONS.add("to_upper");
        STRING_FUNCTIONS.add("base64");
        STRING_FUNCTIONS.add("md5");
        STRING_FUNCTIONS.add("substr");
        STRING_FUNCTIONS.add("trim");
        STRING_FUNCTIONS.add("trim_space");
        STRING_FUNCTIONS.add("urldecode");

        // 初始化数值函数集合
        NUMERIC_FUNCTIONS.add("len");
        NUMERIC_FUNCTIONS.add("length");
        NUMERIC_FUNCTIONS.add("unixtime");
    }

    /**
     * 获取函数类型
     * @param functionName 函数名（已转换为小写）
     * @return 函数类型
     */
    public static FunctionType getFunctionType(String functionName) {
        if (functionName == null) {
            return FunctionType.UNKNOWN_FUNCTION;
        }

        String normalizedName = functionName.toLowerCase();

        if (BOOLEAN_FUNCTIONS.contains(normalizedName)) {
            return FunctionType.BOOLEAN_FUNCTION;
        }
        if (STRING_FUNCTIONS.contains(normalizedName)) {
            return FunctionType.STRING_FUNCTION;
        }
        if (NUMERIC_FUNCTIONS.contains(normalizedName)) {
            return FunctionType.NUMERIC_FUNCTION;
        }

        return FunctionType.UNKNOWN_FUNCTION;
    }

    /**
     * 检查是否为已知的DSL函数
     * @param functionName 函数名
     * @return 是否为已知函数
     */
    public static boolean isKnownFunction(String functionName) {
        return getFunctionType(functionName) != FunctionType.UNKNOWN_FUNCTION;
    }

    /**
     * 检查是否为返回boolean的函数
     * @param functionName 函数名
     * @return 是否为boolean函数
     */
    public static boolean isBooleanFunction(String functionName) {
        return getFunctionType(functionName) == FunctionType.BOOLEAN_FUNCTION;
    }

    /**
     * 检查是否为返回String的函数
     * @param functionName 函数名
     * @return 是否为String函数
     */
    public static boolean isStringFunction(String functionName) {
        return getFunctionType(functionName) == FunctionType.STRING_FUNCTION;
    }

    /**
     * 检查是否为返回数值的函数
     * @param functionName 函数名
     * @return 是否为数值函数
     */
    public static boolean isNumericFunction(String functionName) {
        return getFunctionType(functionName) == FunctionType.NUMERIC_FUNCTION;
    }

    /**
     * 统一的函数评估接口 - 返回boolean结果
     * @param functionName 函数名
     * @param expression 完整表达式
     * @param context 上下文
     * @return boolean结果
     */
    public static boolean evaluateFunctionAsBoolean(String functionName, String expression, Map<String, Object> context) {
        FunctionType type = getFunctionType(functionName);
        
        switch (type) {
            case BOOLEAN_FUNCTION:
                return evaluateBooleanFunction(functionName, expression, context);
            case STRING_FUNCTION:
                String stringResult = evaluateStringFunction(functionName, expression, context);
                return isValidStringForBoolean(stringResult);
            case NUMERIC_FUNCTION:
                String numericResult = evaluateNumericFunction(functionName, expression, context);
                return isValidNumericForBoolean(numericResult);
            default:
                return false;
        }
    }

    /**
     * 统一的函数评估接口 - 返回字符串值
     * @param functionName 函数名
     * @param expression 完整表达式
     * @param context 上下文
     * @return 字符串结果
     */
    public static String evaluateFunctionForStringValue(String functionName, String expression, Map<String, Object> context) {
        FunctionType type = getFunctionType(functionName);
        
        switch (type) {
            case BOOLEAN_FUNCTION:
                boolean boolResult = evaluateBooleanFunction(functionName, expression, context);
                return String.valueOf(boolResult);
            case STRING_FUNCTION:
                return evaluateStringFunction(functionName, expression, context);
            case NUMERIC_FUNCTION:
                return evaluateNumericFunction(functionName, expression, context);
            default:
                return null;
        }
    }

    /**
     * 统一的函数评估接口 - 返回String结果
     * @param functionName 函数名
     * @param expression 完整表达式
     * @param context 上下文
     * @return String结果
     */
    public static String evaluateFunctionAsString(String functionName, String expression, Map<String, Object> context) {
        FunctionType type = getFunctionType(functionName);
        
        switch (type) {
            case BOOLEAN_FUNCTION:
                boolean boolResult = evaluateBooleanFunction(functionName, expression, context);
                return String.valueOf(boolResult);
            case STRING_FUNCTION:
                return evaluateStringFunction(functionName, expression, context);
            case NUMERIC_FUNCTION:
                return evaluateNumericFunction(functionName, expression, context);
            default:
                return null;
        }
    }

    /**
     * 评估boolean函数
     */
    private static boolean evaluateBooleanFunction(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            case "contains":
            case "bcontains":
                return DslFunctionEvaluator.evaluateContainsFunction(expression, context);
            case "startswith":
            case "starts_with":
                return DslFunctionEvaluator.evaluateStartsWithFunction(expression, context);
            case "endswith":
            case "ends_with":
                return DslFunctionEvaluator.evaluateEndsWithFunction(expression, context);
            case "matches":
            case "bmatches":
                return DslFunctionEvaluator.evaluateMatchesFunction(expression, context);
            case "regex":
                return DslFunctionEvaluator.evaluateRegexFunction(expression, context);
            default:
                return false;
        }
    }

    /**
     * 评估String函数
     */
    private static String evaluateStringFunction(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            case "tolowercase":
            case "to_lower":
                return DslFunctionEvaluator.evaluateToLowerCaseFunction(expression, context);
            case "touppercase":
            case "to_upper":
                return DslFunctionEvaluator.evaluateToUpperCaseFunction(expression, context);
            case "base64":
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
            default:
                return null;
        }
    }

    /**
     * 评估数值函数并返回boolean结果
     */
    private static boolean evaluateNumericFunctionAsBoolean(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            case "len":
            case "length":
                return DslFunctionEvaluator.evaluateLenFunction(expression, context);
            case "unixtime":
                return DslFunctionEvaluator.evaluateUnixtimeFunction(expression, context);
            default:
                return false;
        }
    }

    /**
     * 评估数值函数并返回字符串结果
     */
    private static String evaluateNumericFunction(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            case "len":
            case "length":
                // 计算实际长度值
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String arg = args[0].trim();
                        String value = DslUtils.resolveValue(arg, context);
                        if (value != null) {
                            return String.valueOf(value.length());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("计算长度失败: " + e.getMessage());
                }
                return "0";
            case "unixtime":
                long unixTime = System.currentTimeMillis() / 1000;
                return String.valueOf(unixTime);
            default:
                return null;
        }
    }

    /**
     * 检查字符串结果是否可以转换为true
     */
    private static boolean isValidStringForBoolean(String result) {
        return result != null && !result.isEmpty();
    }

    /**
     * 检查数值结果是否可以转换为true
     */
    private static boolean isValidNumericForBoolean(String result) {
        if (result == null) {
            return false;
        }
        try {
            double value = Double.parseDouble(result);
            return value != 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 获取所有支持的函数名称
     * @return 所有支持的函数名称集合
     */
    public static Set<String> getAllSupportedFunctions() {
        Set<String> allFunctions = new HashSet<>();
        allFunctions.addAll(BOOLEAN_FUNCTIONS);
        allFunctions.addAll(STRING_FUNCTIONS);
        allFunctions.addAll(NUMERIC_FUNCTIONS);
        return allFunctions;
    }
}