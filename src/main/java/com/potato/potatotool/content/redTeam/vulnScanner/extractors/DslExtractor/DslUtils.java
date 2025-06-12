package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslConstants;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * DSL工具类
 * 包含所有通用的工具方法
 */
public class DslUtils {

    /**
     * 标准化DSL表达式
     * 替换Xray和Nuclei特定语法为标准形式
     */
    public static String normalizeExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return expression;
        }

        String normalized = expression;
        
        // 应用语法替换规则
        for (String[] replacement : DslConstants.SYNTAX_REPLACEMENTS) {
            normalized = normalized.replaceAll(replacement[0], replacement[1]);
        }

        // 检查并记录未识别的函数
        if (containsUnrecognizedFunctions(normalized)) {
            logUnrecognizedExpression(expression);
        }

        return normalized;
    }

    /**
     * 检查表达式是否包含未识别的函数
     */
    public static boolean containsUnrecognizedFunctions(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // 检查函数调用
        Matcher functionMatcher = DslConstants.FUNCTION_PATTERN.matcher(expression);
        while (functionMatcher.find()) {
            String functionName = functionMatcher.group(1);
            
            // 过滤逻辑操作符，避免将 "or (" 或 "and (" 误识别为函数
            if (isLogicalOperator(functionName)) {
                continue;
            }
            
            if (!isKnownFunction(functionName) && !isInternalResultObject(functionName)) {
                System.err.println("未识别的函数: " + functionName + " 在表达式: " + expression);
                return true;
            }
        }

        // 检查对象属性
        Matcher objectMatcher = DslConstants.OBJECT_PATTERN.matcher(expression);
        while (objectMatcher.find()) {
            String objectName = objectMatcher.group(1);
            if (!isKnownObject(objectName) && !isInternalResultObject(objectName)) {
                System.err.println("未识别的对象: " + objectName + " 在表达式: " + expression);
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否为逻辑操作符
     */
    public static boolean isLogicalOperator(String operatorName) {
        if (operatorName == null) {
            return false;
        }
        String lowerName = operatorName.toLowerCase();
        return "and".equals(lowerName) || "or".equals(lowerName) || "not".equals(lowerName);
    }

    /**
     * 检查是否为已知函数
     */
    public static boolean isKnownFunction(String functionName) {
        for (String knownFunction : DslConstants.KNOWN_FUNCTIONS) {
            if (knownFunction.equalsIgnoreCase(functionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为已知对象
     */
    public static boolean isKnownObject(String objectName) {
        for (String knownObject : DslConstants.KNOWN_OBJECTS) {
            if (knownObject.equalsIgnoreCase(objectName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为内部结果对象
     */
    public static boolean isInternalResultObject(String objectName) {
        for (String internalObject : DslConstants.INTERNAL_RESULT_OBJECTS) {
            if (internalObject.equalsIgnoreCase(objectName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 记录未识别的DSL表达式到错误日志文件
     */
    public static void logUnrecognizedExpression(String expression) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DslConstants.ERROR_LOG_FILE, true))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            writer.println("[" + timestamp + "] 未识别的DSL表达式: " + expression);
        } catch (IOException e) {
            System.err.println("无法写入错误日志文件: " + e.getMessage());
        }
    }

    /**
     * 提取函数参数
     * 从函数调用字符串中提取参数列表
     */
    public static String[] extractFunctionArguments(String functionCall) {
        if (functionCall == null || !functionCall.contains("(")) {
            return new String[0];
        }

        int startIndex = functionCall.indexOf('(') + 1;
        int endIndex = functionCall.lastIndexOf(')');
        
        if (startIndex >= endIndex) {
            return new String[0];
        }

        String argsString = functionCall.substring(startIndex, endIndex).trim();
        if (argsString.isEmpty()) {
            return new String[0];
        }

        // 简单的参数分割，不处理嵌套括号
        return argsString.split(",\\s*");
    }

    /**
     * 清理字符串值（移除引号）
     */
    public static String cleanStringValue(String value) {
        if (value == null) {
            return null;
        }
        
        String cleaned = value.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            return cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    /**
     * 检查字符串是否为数字
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 安全地转换字符串为整数
     */
    public static int safeParseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全地转换字符串为双精度浮点数
     */
    public static double safeParseDouble(String str, double defaultValue) {
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 检查比较操作符
     */
    public static String findComparisonOperator(String expression) {
        for (String operator : DslConstants.COMPARISON_OPERATORS) {
            if (expression.contains(operator)) {
                return operator;
            }
        }
        return null;
    }

    /**
     * 提取函数名
     */
    public static String extractFunctionName(String expression) {
        java.util.regex.Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 提取函数参数
     */
    public static String[] extractFunctionArgs(String expression) {
        // 手动解析函数参数，处理嵌套括号
        String functionName = extractFunctionName(expression);
        if (functionName == null) {
            return new String[0];
        }
        
        int startIndex = expression.indexOf('(');
        int endIndex = findMatchingCloseParen(expression, startIndex);
        
        if (startIndex == -1 || endIndex == -1) {
            return new String[0];
        }
        
        String argsString = expression.substring(startIndex + 1, endIndex);
        if (argsString.trim().isEmpty()) {
            return new String[0];
        }
        
        // 智能参数分割，处理嵌套函数
        return splitFunctionArgs(argsString);
    }
    
    /**
     * 找到匹配的右括号位置
     */
    private static int findMatchingCloseParen(String expression, int openParenIndex) {
        if (openParenIndex == -1) {
            return -1;
        }
        
        int parenCount = 1;
        boolean inQuotes = false;
        char quoteChar = '\0';
        
        for (int i = openParenIndex + 1; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                quoteChar = '\0';
            } else if (!inQuotes) {
                if (c == '(') {
                    parenCount++;
                } else if (c == ')') {
                    parenCount--;
                    if (parenCount == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1; // 没有找到匹配的右括号
    }
    
    /**
     * 智能分割函数参数，处理嵌套括号
     */
    private static String[] splitFunctionArgs(String argsString) {
        java.util.List<String> args = new java.util.ArrayList<>();
        int start = 0;
        int parenCount = 0;
        boolean inQuotes = false;
        char quoteChar = '\0';
        
        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);
            
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                quoteChar = '\0';
            } else if (!inQuotes) {
                if (c == '(') {
                    parenCount++;
                } else if (c == ')') {
                    parenCount--;
                } else if (c == ',' && parenCount == 0) {
                    args.add(argsString.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        
        // 添加最后一个参数
        if (start < argsString.length()) {
            args.add(argsString.substring(start).trim());
        }
        
        return args.toArray(new String[0]);
    }

    /**
     * 解析值（从上下文中获取或直接返回字面值）
     */
    public static String resolveValue(String value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }

        value = value.trim();
        
        // 如果是字符串字面值（被引号包围），直接返回内容
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        // 检查是否为函数调用
        String functionName = extractFunctionName(value);
        if (functionName != null && DslFunctionTypeManager.isKnownFunction(functionName)) {
            // 如果是函数调用，评估函数并返回结果
            return DslFunctionTypeManager.evaluateFunctionForStringValue(functionName.toLowerCase(), value, context);
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
     * 私有构造函数，防止实例化
     */
    private DslUtils() {
        // 工具类不应该被实例化
    }
}