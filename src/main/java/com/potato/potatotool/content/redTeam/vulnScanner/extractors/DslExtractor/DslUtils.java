package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * DSL工具类
 * 包含所有通用的工具方法
 */
public class DslUtils {

    /**
     * 标准化Nuclei DSL表达式
     * 替换Nuclei特定语法为标准形式
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
     * 改进版：更智能地处理嵌套函数、取反操作等
     */
    public static boolean containsUnrecognizedFunctions(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // 清理表达式：移除取反符号、括号等，以便更好地识别
        String cleanedExpression = expression.trim();
        
        // 移除开头的取反符号
        if (cleanedExpression.startsWith("!")) {
            cleanedExpression = cleanedExpression.substring(1).trim();
        }
        
        // 移除最外层的括号
        if (cleanedExpression.startsWith("(") && cleanedExpression.endsWith(")")) {
            cleanedExpression = cleanedExpression.substring(1, cleanedExpression.length() - 1).trim();
        }

        // 使用改进的函数提取方法来检查所有函数调用
        return containsUnrecognizedFunctionsRecursive(cleanedExpression);
    }
    
    /**
     * 递归检查表达式中的未识别函数
     */
    private static boolean containsUnrecognizedFunctionsRecursive(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        // 如果是字符串字面量，直接跳过检查（字符串内容不应该被当作 DSL 代码检查）
        String trimmed = expression.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || 
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return false;
        }
        
        // 处理逻辑操作符分隔的表达式
        if (expression.contains("&&") || expression.contains("||")) {
            // 分割表达式并递归检查每个部分
            String[] parts = expression.split("(&&|\\|\\|)");
            for (String part : parts) {
                if (containsUnrecognizedFunctionsRecursive(part.trim())) {
                    return true;
                }
            }
            return false;
        }
        
        // 先移除字符串字面量，避免误检测字符串内部的模式
        String expressionWithoutStrings = removeStringLiterals(expression);
        
        // 从清理后的表达式中提取函数名
        String functionName = extractFunctionName(expressionWithoutStrings);
        
        if (functionName != null) {
            // 检查函数名是否已知
            if (!isLogicalOperator(functionName) && 
                !isKnownFunction(functionName) && 
                !isInternalResultObject(functionName) &&
                !DslFunctionTypeManager.isKnownFunction(functionName)) {
                System.err.println("未识别的函数: " + functionName + " 在表达式: " + expression);
                return true;
            }
            
            // 递归检查函数参数中的嵌套函数
            // 注意：只检查非字符串字面量的参数
            String[] args = extractFunctionArgs(expression);
            for (String arg : args) {
                String argTrimmed = arg.trim();
                // 跳过字符串字面量参数
                if ((argTrimmed.startsWith("\"") && argTrimmed.endsWith("\"")) || 
                    (argTrimmed.startsWith("'") && argTrimmed.endsWith("'"))) {
                    continue;
                }
                // 跳过纯数字或简单变量名（不含函数调用或对象访问）
                if (argTrimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    continue;
                }
                if (containsUnrecognizedFunctionsRecursive(argTrimmed)) {
                    return true;
                }
            }
        }
        
        // 检查对象属性（排除带下标的变量）
        Matcher objectMatcher = DslConstants.OBJECT_PATTERN.matcher(expressionWithoutStrings);
        while (objectMatcher.find()) {
            String objectName = objectMatcher.group(1);
            // 排除数字、已知对象和内部对象
            if (!objectName.matches("\\d+") && 
                !isKnownObject(objectName) &&
                !isInternalResultObject(objectName)) {
                // 只打印警告，不阻止执行（因为可能是自定义变量）
                // System.err.println("未识别的对象: " + objectName + " 在表达式: " + expressionWithoutStrings);
                // return true;  // 暂时不因未知对象而返回 true
            }
        }

        return false;
    }
    
    /**
     * 查找表达式中所有未识别的函数名称
     * 改进版：返回未识别函数列表而非布尔值，便于更精细的处理
     * 
     * @param expression DSL表达式
     * @return 未识别的函数名称列表
     */
    public static List<String> findUnrecognizedFunctions(String expression) {
        List<String> unrecognizedFunctions = new ArrayList<>();
        
        if (expression == null || expression.trim().isEmpty()) {
            return unrecognizedFunctions;
        }
        
        // 移除字符串字面量
        String expressionWithoutStrings = removeStringLiterals(expression);
        
        // 提取所有函数调用
        Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(expressionWithoutStrings);
        
        while (matcher.find()) {
            String functionName = matcher.group(1);
            String lowerName = functionName.toLowerCase();
            
            // 跳过逻辑操作符和已知函数
            if (!isLogicalOperator(lowerName) && 
                !isKnownFunction(lowerName) && 
                !isInternalResultObject(lowerName) &&
                !DslFunctionTypeManager.isKnownFunction(lowerName) &&
                !lowerName.equals("true") && 
                !lowerName.equals("false")) {
                // 避免重复添加
                if (!unrecognizedFunctions.contains(functionName)) {
                    unrecognizedFunctions.add(functionName);
                }
            }
        }
        
        return unrecognizedFunctions;
    }

    /**
     * 移除字符串字面量，替换为占位符
     * 这样可以避免检查字符串内部的内容
     */
    public static String removeStringLiterals(String expression) {
        if (expression == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        boolean escaped = false;
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (escaped) {
                // 跳过转义字符
                escaped = false;
                if (!inDoubleQuote && !inSingleQuote) {
                    result.append(c);
                }
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                if (!inDoubleQuote && !inSingleQuote) {
                    result.append(c);
                }
                continue;
            }
            
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                result.append(c); // 保留引号
                continue;
            }
            
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                result.append(c); // 保留引号
                continue;
            }
            
            // 如果在字符串内部，跳过（不添加到结果中）
            if (!inDoubleQuote && !inSingleQuote) {
                result.append(c);
            }
        }
        
        return result.toString();
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
     * 检查比较操作符（只查找引号外的运算符）
     */
    public static String findComparisonOperator(String expression) {
        // 按优先级排序：先检查长运算符，再检查短运算符
        String[] sortedOperators = {"!=", "==", ">=", "<=", ">", "<"};
        
        for (String operator : sortedOperators) {
            int index = findOperatorOutsideQuotes(expression, operator);
            if (index >= 0) {
                return operator;
            }
        }
        return null;
    }
    
    /**
     * 在引号外查找运算符的位置
     * @return 运算符位置，如果不在引号外则返回 -1
     */
    private static int findOperatorOutsideQuotes(String expression, String operator) {
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        int parenDepth = 0;
        
        for (int i = 0; i <= expression.length() - operator.length(); i++) {
            char c = expression.charAt(i);
            
            // 跟踪引号状态
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            
            // 跟踪括号深度（只在函数参数内部时不匹配）
            if (!inDoubleQuote && !inSingleQuote) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')') {
                    parenDepth--;
                }
            }
            
            // 只在引号外且不在函数参数内部时匹配运算符
            if (!inDoubleQuote && !inSingleQuote && parenDepth == 0) {
                if (expression.substring(i).startsWith(operator)) {
                    // 确保不是更长运算符的一部分
                    if (operator.equals(">") && i > 0 && expression.charAt(i - 1) == '=') {
                        continue; // 是 >= 的一部分
                    }
                    if (operator.equals("<") && i > 0 && expression.charAt(i - 1) == '=') {
                        continue; // 是 <= 的一部分
                    }
                    if (operator.equals("=") && i + 1 < expression.length() && expression.charAt(i + 1) == '=') {
                        continue; // 是 == 的一部分
                    }
                    if (operator.equals("!") && i + 1 < expression.length() && expression.charAt(i + 1) == '=') {
                        continue; // 是 != 的一部分
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 提取函数名
     */
    public static String extractFunctionName(String expression) {
        Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(expression);
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
        List<String> args = new ArrayList<>();
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