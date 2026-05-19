package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions.AdvancedFunctions;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions.BytesFunctions;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions.EncodingFunctions;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions.HashFunctions;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions.RandomFunctions;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions.StringFunctions;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions.UtilityFunctions;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.XrayCelContext.ResponseWrapper;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.XrayCelContext.RequestWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xray CEL 表达式解析器
 * 解析和评估 Xray CEL 表达式
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class XrayCelParser {
    
    // 函数调用模式
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\s*\\(([^)]*)\\)");
    
    // 比较运算符模式
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("(.+?)\\s*(==|!=|>=|<=|>|<)\\s*(.+)");
    
    /**
     * 验证CEL表达式语法
     * 在转换阶段调用，提前发现语法错误
     * 
     * @param celExpression CEL表达式
     * @throws IllegalArgumentException 如果语法错误
     */
    public static void validateCelSyntax(String celExpression) {
        if (celExpression == null || celExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("CEL表达式不能为空");
        }
        
        try {
            // 使用空上下文进行语法检查
            Map<String, Object> emptyContext = new HashMap<>();
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("status", 200);
            emptyResponse.put("body", "");
            emptyResponse.put("headers", new HashMap<>());
            emptyContext.put("response", emptyResponse);
            
            // 尝试解析表达式（不执行）
            validateExpressionStructure(celExpression);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("CEL表达式语法错误: " + celExpression + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证表达式结构
     */
    private static void validateExpressionStructure(String expression) {
        // 检查括号匹配
        int parenthesisCount = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') parenthesisCount++;
            if (c == ')') parenthesisCount--;
            if (parenthesisCount < 0) {
                throw new IllegalArgumentException("括号不匹配");
            }
        }
        if (parenthesisCount != 0) {
            throw new IllegalArgumentException("括号不匹配");
        }
        
        // 检查基本语法
        if (expression.contains("===") || expression.contains("!==")) {
            throw new IllegalArgumentException("不支持的运算符: === 或 !==，请使用 == 或 !=");
        }
    }
    
    /**
     * 智能解析函数参数（支持引号内的逗号）
     * 
     * 示例:
     * - "arg1, arg2" -> ["arg1", "arg2"]
     * - "'hello, world', 123" -> ["'hello, world'", "123"]
     * - "\"test, abc\", func(1, 2)" -> ["\"test, abc\"", "func(1, 2)"]
     * 
     * @param argsStr 参数字符串
     * @return 参数数组
     */
    private static String[] parseArguments(String argsStr) {
        if (argsStr == null || argsStr.trim().isEmpty()) {
            return new String[0];
        }
        
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int parenthesisCount = 0;
        
        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            
            // 处理转义字符
            if (c == '\\' && i + 1 < argsStr.length()) {
                currentArg.append(c);
                currentArg.append(argsStr.charAt(i + 1));
                i++;
                continue;
            }
            
            // 处理引号
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                currentArg.append(c);
                continue;
            }
            
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                currentArg.append(c);
                continue;
            }
            
            // 处理括号（函数嵌套）
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenthesisCount++;
                } else if (c == ')') {
                    parenthesisCount--;
                }
            }
            
            // 处理逗号分隔符
            if (c == ',' && !inSingleQuote && !inDoubleQuote && parenthesisCount == 0) {
                args.add(currentArg.toString().trim());
                currentArg = new StringBuilder();
                continue;
            }
            
            currentArg.append(c);
        }
        
        // 添加最后一个参数
        if (currentArg.length() > 0) {
            args.add(currentArg.toString().trim());
        }
        
        return args.toArray(new String[0]);
    }
    
    /**
     * 评估逻辑表达式
     * 处理 &&, ||, and, or 等逻辑运算符
     */
    public static boolean evaluateLogical(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 处理括号优先级
            expression = expression.trim();
            
            // 移除最外层的括号（如果存在）
            expression = removeOuterParentheses(expression);
            
            // 处理 OR 运算符（优先级最低）- 使用智能分割
            List<String> orParts = splitByOperator(expression, "||");
            if (orParts.size() > 1) {
                for (String part : orParts) {
                    if (evaluateLogical(part.trim(), context)) {
                        return true;
                    }
                }
                return false;
            }
            
            // 处理 AND 运算符 - 使用智能分割
            List<String> andParts = splitByOperator(expression, "&&");
            if (andParts.size() > 1) {
                for (String part : andParts) {
                    if (!evaluateLogical(part.trim(), context)) {
                        return false;
                    }
                }
                return true;
            }
            
            // 处理 NOT 运算符
            if (expression.startsWith("!")) {
                String innerExpr = expression.substring(1).trim();
                // 移除NOT后面的括号（如果有）
                innerExpr = removeOuterParentheses(innerExpr);
                return !evaluateLogical(innerExpr, context);
            }
            
            // 单个条件
            return evaluateSingle(expression, context);
            
        } catch (Exception e) {
            System.err.println("逻辑表达式评估失败: " + expression);
            System.err.println("错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 移除表达式最外层的括号（如果存在且配对）
     */
    private static String removeOuterParentheses(String expression) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }
        
        expression = expression.trim();
        
        // 检查是否以括号开始和结束
        if (!expression.startsWith("(") || !expression.endsWith(")")) {
            return expression;
        }
        
        // 验证最外层括号是否配对
        int count = 0;
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '(') {
                count++;
            } else if (expression.charAt(i) == ')') {
                count--;
            }
            // 如果在结束前count变为0，说明不是最外层括号
            if (count == 0 && i < expression.length() - 1) {
                return expression;
            }
        }
        
        // 移除最外层括号
        return expression.substring(1, expression.length() - 1).trim();
    }
    
    /**
     * 智能分割表达式（考虑括号、引号）
     * 
     * @param expression 表达式
     * @param operator 运算符（如 "||", "&&"）
     * @return 分割后的部分
     */
    private static List<String> splitByOperator(String expression, String operator) {
        List<String> parts = new ArrayList<>();
        if (expression == null || expression.isEmpty()) {
            parts.add(expression);
            return parts;
        }
        
        StringBuilder current = new StringBuilder();
        int parenCount = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            // 处理转义字符
            if (i > 0 && expression.charAt(i - 1) == '\\') {
                current.append(c);
                continue;
            }
            
            // 处理引号
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                continue;
            }
            
            // 处理括号
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenCount++;
                } else if (c == ')') {
                    parenCount--;
                }
            }
            
            // 检查是否遇到运算符（只在括号外且引号外）
            if (!inSingleQuote && !inDoubleQuote && parenCount == 0) {
                boolean isOperator = false;
                if (operator.equals("||") && i + 1 < expression.length() && 
                    c == '|' && expression.charAt(i + 1) == '|') {
                    isOperator = true;
                    i++; // 跳过第二个 |
                } else if (operator.equals("&&") && i + 1 < expression.length() && 
                           c == '&' && expression.charAt(i + 1) == '&') {
                    isOperator = true;
                    i++; // 跳过第二个 &
                }
                
                if (isOperator) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        // 添加最后一部分
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        // 如果没有找到运算符，返回原表达式
        if (parts.isEmpty()) {
            parts.add(expression);
        }
        
        return parts;
    }
    
    /**
     * 评估单个条件
     */
    public static boolean evaluateSingle(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            expression = expression.trim();
            
            // 处理三元运算符（如 a ? b : c）
            // 注意：排除字符串字面量方法调用模式（如 "pattern".bmatches(...)）
            if (containsTernaryOperator(expression)) {
                Object result = evaluateTernary(expression, context);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return result != null;
            }
            
            // 处理规则调用（如 r0(), r1()）
            if (expression.matches("\\w+\\s*\\(\\s*\\)")) {
                String ruleName = expression.replaceAll("\\s*\\(\\s*\\)", "");
                Object result = context.get(ruleName);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return false;
            }
            
            // 处理比较表达式
            Matcher compMatcher = COMPARISON_PATTERN.matcher(expression);
            if (compMatcher.matches()) {
                String left = compMatcher.group(1).trim();
                String operator = compMatcher.group(2).trim();
                String right = compMatcher.group(3).trim();
                
                return evaluateComparison(left, operator, right, context);
            }
            
            // 处理直接的布尔值
            if ("true".equalsIgnoreCase(expression)) {
                return true;
            }
            if ("false".equalsIgnoreCase(expression)) {
                return false;
            }
            
            // 尝试从上下文获取值
            Object value = resolveValue(expression, context);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value != null && !(value instanceof String && value.equals(expression))) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("单个条件评估失败: " + expression);
            System.err.println("错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 评估比较表达式
     */
    private static boolean evaluateComparison(String left, String operator, String right, Map<String, Object> context) {
        try {
            // 解析左右值
            Object leftValue = resolveValue(left, context);
            Object rightValue = resolveValue(right, context);
            
            // 数值比较
            if (isNumeric(leftValue) && isNumeric(rightValue)) {
                double leftNum = toDouble(leftValue);
                double rightNum = toDouble(rightValue);
                
                switch (operator) {
                    case "==": return leftNum == rightNum;
                    case "!=": return leftNum != rightNum;
                    case ">=": return leftNum >= rightNum;
                    case "<=": return leftNum <= rightNum;
                    case ">": return leftNum > rightNum;
                    case "<": return leftNum < rightNum;
                }
            }
            
            // 字符串比较
            String leftStr = toString(leftValue);
            String rightStr = toString(rightValue);
            
            switch (operator) {
                case "==": return leftStr.equals(rightStr);
                case "!=": return !leftStr.equals(rightStr);
                case ">=": return leftStr.compareTo(rightStr) >= 0;
                case "<=": return leftStr.compareTo(rightStr) <= 0;
                case ">": return leftStr.compareTo(rightStr) > 0;
                case "<": return leftStr.compareTo(rightStr) < 0;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("比较表达式评估失败: " + left + " " + operator + " " + right);
            return false;
        }
    }
    
    /**
     * 解析值或表达式
     */
    private static Object resolveValue(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        
        expression = expression.trim();
        
        // 字符串字面值（优先处理，避免被误认为其他类型）
        if ((expression.startsWith("\"") && expression.endsWith("\"")) ||
            (expression.startsWith("'") && expression.endsWith("'"))) {
            String str = expression.substring(1, expression.length() - 1);
            // 处理转义字符
            return unescapeString(str);
        }
        
        // 数字字面值
        if (expression.matches("-?\\d+")) {
            return Integer.parseInt(expression);
        }
        if (expression.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(expression);
        }
        
        // 三元运算符（如 a ? b : c）
        // 注意：需要在切片和函数之前检查，避免误判
        if (containsTernaryOperator(expression)) {
            return evaluateTernary(expression, context);
        }

        // Xray 特有的链式/字面量方法调用（如 response.body.contains(...)、"regex".bmatches(...)）
        if (isStringLiteralMethodCall(expression) || isChainedMethodCall(expression, context)) {
            Object value = evaluateFunctionCall(expression, context);
            if (value != null) {
                return value;
            }
        }
        
        // 函数调用（允许包含空格，因为参数可能包含空格）
        // 注意：需要在切片之前检查，因为函数参数可能包含切片
        if (expression.contains("(") && expression.contains(")")) {
            // 检查是否为函数调用格式：functionName(...)
            if (expression.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*\\)$")) {
                return evaluateFunctionCall(expression, context);
            }
        }
        
        // 数组索引或切片访问（如 list[0], list[1:3]）
        if (expression.contains("[") && expression.contains("]")) {
            return resolveIndexAccess(expression, context);
        }
        
        // 数学表达式（包含算术运算符）
        if (MathExpressionEvaluator.isMathExpression(expression)) {
            try {
                return MathExpressionEvaluator.evaluate(expression, context);
            } catch (Exception e) {
                // 如果数学表达式评估失败，继续尝试其他方式
                System.err.println("数学表达式评估失败: " + expression + ", 尝试其他解析方式");
            }
        }
        
        // 属性访问（如 response.status）
        if (isPropertyAccessCandidate(expression, context)) {
            return resolvePropertyAccess(expression, context);
        }
        
        // 从上下文获取变量
        if (context.containsKey(expression)) {
            return context.get(expression);
        }
        
        // 返回原始字符串
        return expression;
    }

    private static boolean isChainedMethodCall(String expression, Map<String, Object> context) {
        if (expression == null || !expression.contains(".") || !expression.contains("(") || !expression.endsWith(")")) {
            return false;
        }

        int firstParen = expression.indexOf('(');
        int lastDotBeforeParen = expression.lastIndexOf('.', firstParen);
        if (firstParen <= 0 || lastDotBeforeParen <= 0) {
            return false;
        }

        String objectPath = expression.substring(0, lastDotBeforeParen).trim();
        if (objectPath.isEmpty()) {
            return false;
        }

        if (objectPath.contains(".")) {
            String root = objectPath.substring(0, objectPath.indexOf('.')).trim();
            return context != null && context.containsKey(root);
        }

        return context != null && context.containsKey(objectPath);
    }

    private static boolean isPropertyAccessCandidate(String expression, Map<String, Object> context) {
        if (expression == null || !expression.contains(".")) {
            return false;
        }

        int firstDot = expression.indexOf('.');
        if (firstDot <= 0) {
            return false;
        }

        String root = expression.substring(0, firstDot).trim();
        return context != null && context.containsKey(root);
    }
    
    /**
     * 检测表达式是否包含三元运算符
     * 需要排除字符串字面值内的 ? 和 :
     * 同时需要排除字符串字面量方法调用模式（如 "pattern".bmatches(...)）
     * 
     * @param expression 表达式
     * @return 是否包含三元运算符
     */
    private static boolean containsTernaryOperator(String expression) {
        if (!expression.contains("?") || !expression.contains(":")) {
            return false;
        }
        
        // 特殊处理：字符串字面量方法调用模式 "xxx".method(...) 不是三元运算符
        // 例如 "root:.*?:".bmatches(response.body) 中的 ? 和 : 在正则模式中
        if (isStringLiteralMethodCall(expression)) {
            return false;
        }
        
        boolean inString = false;
        char stringChar = 0;
        int questionCount = 0;
        int colonCount = 0;
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            // 处理字符串
            if (c == '"' || c == '\'') {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    // 检查是否为转义引号
                    if (i > 0 && expression.charAt(i - 1) != '\\') {
                        inString = false;
                    }
                }
            }
            
            // 在字符串外检测 ? 和 :
            if (!inString) {
                if (c == '?') questionCount++;
                if (c == ':') colonCount++;
            }
        }
        
        // 真正的三元运算符需要在字符串外有 ? 和 :
        return questionCount > 0 && colonCount > 0;
    }
    
    /**
     * 检测表达式是否是字符串字面量方法调用
     * 例如: "pattern".bmatches(response.body)
     * 这种语法是 Xray 特有的，正则模式作为对象调用 bmatches/bcontains 等方法
     */
    private static boolean isStringLiteralMethodCall(String expression) {
        String trimmed = expression.trim();
        // 检查是否以引号开头
        if ((trimmed.startsWith("\"") || trimmed.startsWith("'")) && 
            trimmed.contains(".") && trimmed.contains("(")) {
            // 找到第一个引号和匹配的结束引号
            char quote = trimmed.charAt(0);
            int endQuote = -1;
            for (int i = 1; i < trimmed.length(); i++) {
                if (trimmed.charAt(i) == quote && trimmed.charAt(i - 1) != '\\') {
                    endQuote = i;
                    break;
                }
            }
            // 如果结束引号后面紧跟着 .method(
            if (endQuote > 0 && endQuote + 1 < trimmed.length() && trimmed.charAt(endQuote + 1) == '.') {
                return true;
            }
        }
        // 也检查 r'pattern'.bmatches(...) 形式（raw string）
        if (trimmed.startsWith("r\"") || trimmed.startsWith("r'")) {
            char quote = trimmed.charAt(1);
            int endQuote = -1;
            for (int i = 2; i < trimmed.length(); i++) {
                if (trimmed.charAt(i) == quote) {
                    endQuote = i;
                    break;
                }
            }
            if (endQuote > 0 && endQuote + 1 < trimmed.length() && trimmed.charAt(endQuote + 1) == '.') {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 解析属性访问（如 response.status, response.body）
     * 支持：
     * - 普通属性：response.status
     * - 数组索引：list[0], headers["Server"]
     * - 链式访问：response.headers["Server"]
     */
    private static Object resolvePropertyAccess(String expression, Map<String, Object> context) {
        // 先检查是否包含数组索引访问
        if (expression.contains("[") && expression.contains("]")) {
            return resolveIndexAccess(expression, context);
        }
        
        String[] parts = expression.split("\\.", 2);
        if (parts.length < 2) {
            return null;
        }
        
        String objectName = parts[0].trim();
        String propertyPath = parts[1].trim();
        
        Object obj = context.get(objectName);
        if (obj == null) {
            return null;
        }
        
        // 处理 ReverseObject
        if (obj instanceof ReverseObject) {
            ReverseObject reverse = (ReverseObject) obj;
            
            // 处理方法调用（如 reverse.wait(10)）
            if (propertyPath.contains("(") && propertyPath.contains(")")) {
                return evaluateReverseMethod(reverse, propertyPath, context);
            }
            
            // 处理属性访问
            return getReverseProperty(reverse, propertyPath);
        }
        
        // 处理 ResponseWrapper
        if (obj instanceof ResponseWrapper) {
            ResponseWrapper response = (ResponseWrapper) obj;
            
            // 处理方法调用（如 response.body.contains("admin") 或 response.body.bcontains(b"uid=")）
            if (propertyPath.contains("(") && propertyPath.contains(")")) {
                // 先获取属性，再调用方法
                String[] methodParts = propertyPath.split("\\.", 2);
                if (methodParts.length == 2) {
                    Object propValue = getResponseProperty(response, methodParts[0]);
                    return evaluateMethodCall(propValue, methodParts[1], context);
                } else {
                    return evaluateMethodCall(response, propertyPath, context);
                }
            }
            
            // 处理数组索引访问（如 response.headers["Server"]）
            if (propertyPath.contains("[") && propertyPath.contains("]")) {
                Object propValue = getResponseProperty(response, propertyPath.split("\\[")[0].trim());
                if (propValue != null) {
                    return resolveIndexAccess(propertyPath, Collections.singletonMap("_temp", propValue));
                }
            }
            
            return getResponseProperty(response, propertyPath);
        }
        
        // 处理 RequestWrapper
        if (obj instanceof RequestWrapper) {
            RequestWrapper request = (RequestWrapper) obj;
            
            // 处理方法调用（如 request.url.contains("admin")）
            if (propertyPath.contains("(") && propertyPath.contains(")")) {
                // 先获取属性，再调用方法
                String[] methodParts = propertyPath.split("\\.", 2);
                if (methodParts.length == 2) {
                    Object propValue = getRequestProperty(request, methodParts[0]);
                    return evaluateMethodCall(propValue, methodParts[1], context);
                } else {
                    return evaluateMethodCall(request, propertyPath, context);
                }
            }
            
            // 处理数组索引访问（如 request.headers["Content-Type"]）
            if (propertyPath.contains("[") && propertyPath.contains("]")) {
                Object propValue = getRequestProperty(request, propertyPath.split("\\[")[0].trim());
                if (propValue != null) {
                    return resolveIndexAccess(propertyPath, Collections.singletonMap("_temp", propValue));
                }
            }
            
            return getRequestProperty(request, propertyPath);
        }
        
        // 处理 Map
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).get(propertyPath);
        }
        
        return null;
    }
    
    /**
     * 解析索引访问
     * 支持：
     * - 数组索引：list[0], list[1]
     * - Map 键访问：map["key"], headers["Server"]
     * - 链式访问：response.headers["Server"]
     * - 切片语法：list[start:end]
     * 
     * @param expression 表达式
     * @param context 上下文
     * @return 索引访问的结果
     */
    private static Object resolveIndexAccess(String expression, Map<String, Object> context) {
        try {
            // 解析：objectName[index] 或 object.property[index] 或 list[start:end]
            int bracketStart = expression.indexOf('[');
            int bracketEnd = expression.lastIndexOf(']');
            
            if (bracketStart == -1 || bracketEnd == -1 || bracketStart >= bracketEnd) {
                return null;
            }
            
            String objectPath = expression.substring(0, bracketStart).trim();
            String indexStr = expression.substring(bracketStart + 1, bracketEnd).trim();
            
            // 获取对象
            Object obj;
            if (objectPath.contains(".")) {
                obj = resolvePropertyAccess(objectPath, context);
            } else {
                obj = context.get(objectPath);
                if (obj == null && objectPath.equals("_temp")) {
                    obj = context.get("_temp");
                }
            }
            
            if (obj == null) {
                return null;
            }
            
            // 检查是否为切片语法 [start:end]
            if (indexStr.contains(":")) {
                return resolveSlice(obj, indexStr, context);
            }
            
            // 解析索引
            Object index = resolveValue(indexStr, context);
            
            // 处理 List 数组索引
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                if (index instanceof Number) {
                    int idx = ((Number) index).intValue();
                    if (idx >= 0 && idx < list.size()) {
                        return list.get(idx);
                    }
                }
            }
            
            // 处理 Map 键访问
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                return map.get(index != null ? index.toString() : indexStr);
            }
            
            // 处理数组
            if (obj instanceof Object[]) {
                Object[] array = (Object[]) obj;
                if (index instanceof Number) {
                    int idx = ((Number) index).intValue();
                    if (idx >= 0 && idx < array.length) {
                        return array[idx];
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("索引访问解析失败: " + expression + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 解析切片语法 [start:end]
     * 
     * 示例：
     * - list[1:3] -> [list[1], list[2]]
     * - list[:3] -> [list[0], list[1], list[2]]
     * - list[1:] -> [list[1], list[2], ...]
     * - list[-2:] -> 最后两个元素
     * 
     * @param obj 对象（List 或数组）
     * @param sliceStr 切片字符串（如 "1:3"）
     * @param context 上下文
     * @return 切片结果
     */
    private static Object resolveSlice(Object obj, String sliceStr, Map<String, Object> context) {
        String[] parts = sliceStr.split(":", 2);
        String startStr = parts[0].trim();
        String endStr = parts.length > 1 ? parts[1].trim() : "";
        
        int start = 0;
        int end = Integer.MAX_VALUE;
        
        // 解析起始索引
        if (!startStr.isEmpty()) {
            Object startObj = resolveValue(startStr, context);
            if (startObj instanceof Number) {
                start = ((Number) startObj).intValue();
            } else {
                try {
                    start = Integer.parseInt(startStr);
                } catch (NumberFormatException e) {
                    start = 0;
                }
            }
        }
        
        // 解析结束索引
        if (!endStr.isEmpty()) {
            Object endObj = resolveValue(endStr, context);
            if (endObj instanceof Number) {
                end = ((Number) endObj).intValue();
            } else {
                try {
                    end = Integer.parseInt(endStr);
                } catch (NumberFormatException e) {
                    end = Integer.MAX_VALUE;
                }
            }
        }
        
        // 处理 List
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            int size = list.size();
            
            // 处理负索引
            if (start < 0) start = size + start;
            if (end < 0) end = size + end;
            
            // 边界检查
            start = Math.max(0, Math.min(start, size));
            end = Math.max(0, Math.min(end, size));
            
            if (start >= end) {
                return new ArrayList<>();
            }
            
            return list.subList(start, end);
        }
        
        // 处理数组
        if (obj instanceof Object[]) {
            Object[] array = (Object[]) obj;
            int size = array.length;
            
            // 处理负索引
            if (start < 0) start = size + start;
            if (end < 0) end = size + end;
            
            // 边界检查
            start = Math.max(0, Math.min(start, size));
            end = Math.max(0, Math.min(end, size));
            
            if (start >= end) {
                return new ArrayList<>();
            }
            
            List<Object> result = new ArrayList<>();
            for (int i = start; i < end; i++) {
                result.add(array[i]);
            }
            return result;
        }
        
        // 处理字符串切片
        if (obj instanceof String) {
            String str = (String) obj;
            int size = str.length();
            
            // 处理负索引
            if (start < 0) start = size + start;
            if (end < 0) end = size + end;
            
            // 边界检查
            start = Math.max(0, Math.min(start, size));
            end = Math.max(0, Math.min(end, size));
            
            if (start >= end) {
                return "";
            }
            
            return str.substring(start, end);
        }
        
        return null;
    }
    
    /**
     * 评估三元运算符
     * 
     * 语法：condition ? trueValue : falseValue
     * 示例：
     * - response.status == 200 ? "success" : "failed"
     * - len(response.body) > 100 ? true : false
     * - a ? b : (c ? d : e)  // 嵌套三元运算符
     * - len(items) > 3 ? items[0:3] : items  // 包含切片语法
     * 
     * @param expression 三元表达式
     * @param context 上下文
     * @return 求值结果
     */
    private static Object evaluateTernary(String expression, Map<String, Object> context) {
        if (expression == null || !expression.contains("?") || !expression.contains(":")) {
            return null;
        }
        
        try {
            // 查找 ? 和 : 的位置（需要考虑嵌套、括号和方括号）
            int questionPos = -1;
            int colonPos = -1;
            int parenDepth = 0;
            int bracketDepth = 0; // 方括号深度（用于切片语法）
            int ternaryDepth = 0; // 三元运算符嵌套深度
            boolean inString = false;
            char stringChar = 0;
            
            for (int i = 0; i < expression.length(); i++) {
                char c = expression.charAt(i);
                
                // 处理字符串
                if (c == '"' || c == '\'') {
                    if (!inString) {
                        inString = true;
                        stringChar = c;
                    } else if (c == stringChar && (i == 0 || expression.charAt(i - 1) != '\\')) {
                        inString = false;
                    }
                    continue;
                }
                
                if (inString) {
                    continue;
                }
                
                // 跟踪括号和方括号深度
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')') {
                    parenDepth--;
                } else if (c == '[') {
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
                }
                
                // 只在最外层括号、方括号和最外层三元运算符中查找 ? 和 :
                if (parenDepth == 0 && bracketDepth == 0) {
                    if (c == '?') {
                        if (ternaryDepth == 0 && questionPos == -1) {
                            questionPos = i;
                        }
                        ternaryDepth++;
                    } else if (c == ':') {
                        ternaryDepth--;
                        if (ternaryDepth == 0 && questionPos != -1 && colonPos == -1) {
                            colonPos = i;
                            break;
                        }
                    }
                }
            }
            
            if (questionPos == -1 || colonPos == -1) {
                System.err.println("三元运算符语法错误: " + expression);
                return null;
            }
            
            // 提取三部分
            String condition = expression.substring(0, questionPos).trim();
            String trueValue = expression.substring(questionPos + 1, colonPos).trim();
            String falseValue = expression.substring(colonPos + 1).trim();
            
            // 移除 trueValue 和 falseValue 外层的括号（如果有）
            trueValue = removeOuterParentheses(trueValue);
            falseValue = removeOuterParentheses(falseValue);
            
            // 评估条件
            boolean conditionResult = evaluateLogical(condition, context);
            
            // 返回对应的值
            return conditionResult ? resolveValue(trueValue, context) : resolveValue(falseValue, context);
            
        } catch (Exception e) {
            System.err.println("三元运算符评估失败: " + expression + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取 Reverse 对象属性
     */
    private static Object getReverseProperty(ReverseObject reverse, String property) {
        switch (property.toLowerCase()) {
            case "domain":
                return reverse.getDomain();
            case "url":
                return reverse.getUrl();
            case "ip":
                return reverse.getIp();
            default:
                return null;
        }
    }
    
    /**
     * 评估 Reverse 对象方法调用
     */
    private static Object evaluateReverseMethod(ReverseObject reverse, String methodCall, Map<String, Object> context) {
        // 解析方法名和参数
        Matcher matcher = FUNCTION_PATTERN.matcher(methodCall);
        if (!matcher.find()) {
            return null;
        }
        
        String methodName = matcher.group(1).toLowerCase();
        String argsStr = matcher.group(2).trim();
        
        try {
            switch (methodName) {
                case "wait":
                case "waitfor":
                    if (argsStr.isEmpty()) {
                        // 无参数，使用默认超时
                        return reverse.waitFor();
                    } else {
                        // 有参数，解析超时时间
                        int timeout = parseIntArg(argsStr, context);
                        return reverse.waitFor(timeout);
                    }
                    
                case "hascallback":
                    return reverse.hasCallback();
                    
                case "getrecords":
                    return reverse.getRecords();
                    
                default:
                    System.err.println("未知的 Reverse 方法: " + methodName);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Reverse 方法调用失败: " + methodCall + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取 Response 属性
     */
    private static Object getResponseProperty(ResponseWrapper response, String property) {
        switch (property.toLowerCase()) {
            case "status":
            case "status_code":
                return response.getStatus();
            case "body":
            case "body_string":
                return response.getBody();
            case "headers":
                return response.getHeaders();
            case "latency":
            case "time":
                return response.getLatency();
            case "content_type":
                return response.getContentType();
            case "url":
                return response.getUrl();
            case "url_path":
                return response.getUrlPath();
            case "raw":
                return response.getRaw();
            default:
                return response.get(property);
        }
    }
    
    /**
     * 获取 Request 属性
     */
    private static Object getRequestProperty(RequestWrapper request, String property) {
        switch (property.toLowerCase()) {
            case "method":
                return request.getMethod();
            case "url":
                return request.getUrl();
            case "path":
                return request.getPath();
            case "query":
                return request.getQuery();
            case "proto":
            case "protocol":
                return request.getProto();
            case "host":
                return request.getHost();
            case "port":
                return request.getPort();
            case "headers":
                return request.getHeaders();
            case "body":
            case "body_string":
                return request.getBody();
            case "content_type":
                return request.getContentType();
            case "user_agent":
                return request.getUserAgent();
            case "raw":
                return request.getRaw();
            default:
                return request.get(property);
        }
    }
    
    /**
     * 评估方法调用（如 contains("admin")）
     */
    private static Object evaluateMethodCall(Object target, String methodCall, Map<String, Object> context) {
        // 使用智能括号匹配
        int firstParen = methodCall.indexOf('(');
        if (firstParen == -1) {
            return null;
        }
        
        String methodName = methodCall.substring(0, firstParen).trim();
        String argsStr = extractFunctionArguments(methodCall, firstParen);
        if (argsStr == null) {
            return null;
        }
        
        // 智能解析参数（支持引号内逗号）
        String[] args = parseArguments(argsStr);
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].trim();
            // 检查是否是需要求值的表达式（如 string(s2 - s1)）
            if (args[i].matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*\\)$") && !args[i].startsWith("\"")) {
                // 这是一个函数调用，需要求值
                try {
                    Object evaluated = evaluateFunctionCall(args[i], context);
                    if (evaluated != null) {
                        args[i] = toString(evaluated);
                        continue;
                    }
                } catch (Exception e) {
                    // 求值失败，继续正常处理
                }
            }
            // 移除引号
            if ((args[i].startsWith("\"") && args[i].endsWith("\"")) ||
                (args[i].startsWith("'") && args[i].endsWith("'"))) {
                args[i] = args[i].substring(1, args[i].length() - 1);
            }
            // 处理转义字符（如 \" -> "）
            args[i] = args[i].replace("\\\"", "\"").replace("\\'", "'");
        }
        
        String targetStr = toString(target);
        
        // 执行方法
        switch (methodName.toLowerCase()) {
            case "contains":
                return args.length > 0 && targetStr.contains(args[0]);
            case "icontains":
                // 忽略大小写的包含检查
                if (args.length > 0) {
                    String pattern = args[0];
                    // 移除 b"..." 或 b'...' 包装
                    if (pattern.startsWith("b\"") && pattern.endsWith("\"")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    } else if (pattern.startsWith("b'") && pattern.endsWith("'")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    }
                    return targetStr.toLowerCase().contains(pattern.toLowerCase());
                }
                return false;
            case "ibcontains":
                // 忽略大小写的字节级包含检查
                if (args.length > 0) {
                    String pattern = args[0];
                    // 移除 b"..." 或 b'...' 包装
                    if (pattern.startsWith("b\"") && pattern.endsWith("\"")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    } else if (pattern.startsWith("b'") && pattern.endsWith("'")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    } else if (pattern.startsWith("b")) {
                        // 兼容: 引号已被移除的情况 (b"xxx" -> bxxx)
                        pattern = pattern.substring(1);
                    }
                    return targetStr.toLowerCase().contains(pattern.toLowerCase());
                }
                return false;
            case "bcontains":
                // 字节级包含检查 - 处理 b"..." 语法
                if (args.length > 0) {
                    String pattern = args[0];
                    // 移除 b"..." 或 b'...' 包装
                    if (pattern.startsWith("b\"") && pattern.endsWith("\"")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    } else if (pattern.startsWith("b'") && pattern.endsWith("'")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    } else if (pattern.startsWith("b")) {
                        // 兼容: 引号已被移除的情况 (b"xxx" -> bxxx)
                        pattern = pattern.substring(1);
                    }
                    return BytesFunctions.bcontains(targetStr, pattern);
                }
                return false;
            case "bmatches":
                // 字节级正则匹配 - 处理 b"..." 语法
                if (args.length > 0) {
                    String pattern = args[0];
                    // 移除 b"..." 包装
                    if (pattern.startsWith("b\"") && pattern.endsWith("\"")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    } else if (pattern.startsWith("b'") && pattern.endsWith("'")) {
                        pattern = pattern.substring(2, pattern.length() - 1);
                    }
                    return BytesFunctions.bmatches(targetStr, pattern);
                }
                return false;
            case "startswith":
            case "starts_with":
                return args.length > 0 && targetStr.startsWith(args[0]);
            case "endswith":
            case "ends_with":
                return args.length > 0 && targetStr.endsWith(args[0]);
            case "len":
            case "length":
                return targetStr.length();
            case "tolowercase":
            case "to_lower":
                return targetStr.toLowerCase();
            case "touppercase":
            case "to_upper":
                return targetStr.toUpperCase();
            default:
                return null;
        }
    }
    
    /**
     * 评估函数调用
     */
    private static Object evaluateFunctionCall(String expression, Map<String, Object> context) {
        // 使用更智能的括号匹配，支持嵌套函数调用
        int firstParen = expression.indexOf('(');
        if (firstParen == -1) {
            return null;
        }
        
        String functionName = expression.substring(0, firstParen).trim().toLowerCase();
        
        // 检测链式方法调用（如 response.body.bcontains(...)）
        if (functionName.contains(".")) {
            // 分割成对象路径和方法名
            int lastDot = functionName.lastIndexOf('.');
            String objectPath = expression.substring(0, lastDot);
            String methodCall = expression.substring(lastDot + 1);
            
            // 特殊处理：字符串字面量调用方法（如 "pattern".bmatches(response.body)）
            // 这种语法是 Xray 特有的，正则模式作为对象调用 bmatches
            if ((objectPath.startsWith("\"") && objectPath.endsWith("\"")) ||
                (objectPath.startsWith("'") && objectPath.endsWith("'"))) {
                // 字符串字面量调用方法 - 需要特殊处理
                String pattern = objectPath.substring(1, objectPath.length() - 1);
                String argsStr = extractFunctionArguments(expression, firstParen);
                String methodNameLower = methodCall.toLowerCase();
                
                // 提取方法名（去除括号及参数部分）
                int parenIdx = methodNameLower.indexOf('(');
                if (parenIdx > 0) {
                    methodNameLower = methodNameLower.substring(0, parenIdx);
                }
                
                if (argsStr != null && "bmatches".equals(methodNameLower)) {
                    // "regex".bmatches(data) -> bmatches(data, regex)
                    Object data = resolveValue(argsStr.trim(), context);
                    String dataStr = toString(data);
                    boolean result = BytesFunctions.bmatches(dataStr, pattern);
                    System.out.println("[DEBUG] CEL bmatches: pattern='" + pattern + "', data(前50字符)='" + 
                        (dataStr.length() > 50 ? dataStr.substring(0, 50) + "..." : dataStr) + "', result=" + result);
                    return result;
                }
            }
            
            // 解析对象
            Object target = resolveValue(objectPath, context);
            if (target != null) {
                return evaluateMethodCall(target, methodCall, context);
            }
        }
        
        // 智能提取参数（匹配配对的括号）
        String argsStr = extractFunctionArguments(expression, firstParen);
        if (argsStr == null) {
            return null;
        }
        
        // 智能解析参数（支持引号内逗号）
        String[] args = parseArguments(argsStr);
        
        // 执行函数
        try {
            switch (functionName) {
                // ==================== 随机函数 ====================
                case "randomint":
                case "rand_int":
                    if (args.length >= 2) {
                        int min = parseIntArg(args[0].trim(), context);
                        int max = parseIntArg(args[1].trim(), context);
                        return RandomFunctions.randomInt(min, max);
                    }
                    break;
                    
                case "randomlowercase":
                    if (args.length >= 1) {
                        int length = parseIntArg(args[0].trim(), context);
                        return RandomFunctions.randomLowercase(length);
                    }
                    break;
                    
                case "randomuppercase":
                    if (args.length >= 1) {
                        int length = parseIntArg(args[0].trim(), context);
                        return RandomFunctions.randomUppercase(length);
                    }
                    break;
                    
                case "rand_text_alpha":
                    if (args.length >= 1) {
                        int length = parseIntArg(args[0].trim(), context);
                        return RandomFunctions.rand_text_alpha(length);
                    }
                    break;
                    
                case "rand_text_numeric":
                    if (args.length >= 1) {
                        int length = parseIntArg(args[0].trim(), context);
                        return RandomFunctions.rand_text_numeric(length);
                    }
                    break;
                    
                case "rand_text_alphanumeric":
                    if (args.length >= 1) {
                        int length = parseIntArg(args[0].trim(), context);
                        return RandomFunctions.rand_text_alphanumeric(length);
                    }
                    break;
                    
                case "randomuuid":
                    return RandomFunctions.randomUUID();
                    
                // ==================== 编码函数 ====================
                case "base64":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return EncodingFunctions.base64(arg);
                    }
                    break;
                    
                case "base64decode":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return EncodingFunctions.base64Decode(arg);
                    }
                    break;
                    
                case "urlencode":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return EncodingFunctions.urlencode(arg);
                    }
                    break;
                    
                case "urldecode":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return EncodingFunctions.urldecode(arg);
                    }
                    break;
                    
                case "hexencode":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return EncodingFunctions.hexEncode(arg);
                    }
                    break;
                    
                case "hexdecode":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return EncodingFunctions.hexDecode(arg);
                    }
                    break;
                    
                // ==================== 哈希函数 ====================
                case "md5":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return HashFunctions.md5(arg);
                    }
                    break;
                    
                case "sha1":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return HashFunctions.sha1(arg);
                    }
                    break;
                    
                case "sha256":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return HashFunctions.sha256(arg);
                    }
                    break;
                    
                case "sha512":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return HashFunctions.sha512(arg);
                    }
                    break;
                    
                case "mmh3":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return HashFunctions.mmh3(arg);
                    }
                    break;
                    
                // ==================== 字符串函数 ====================
                case "len":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return StringFunctions.len(arg);
                    }
                    break;
                    
                case "substr":
                    if (args.length >= 3) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        int start = parseIntArg(args[1].trim(), context);
                        int length = parseIntArg(args[2].trim(), context);
                        return StringFunctions.substr(str, start, length);
                    }
                    break;
                    
                case "replaceall":
                    if (args.length >= 3) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        String oldStr = toString(resolveValue(args[1].trim(), context));
                        String newStr = toString(resolveValue(args[2].trim(), context));
                        return StringFunctions.replaceAll(str, oldStr, newStr);
                    }
                    break;
                    
                case "toupper":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return StringFunctions.toUpper(arg);
                    }
                    break;
                    
                case "tolower":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return StringFunctions.toLower(arg);
                    }
                    break;
                    
                case "repeat":
                    if (args.length >= 2) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        int times = parseIntArg(args[1].trim(), context);
                        return StringFunctions.repeat(str, times);
                    }
                    break;
                    
                case "reverse":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return StringFunctions.reverse(arg);
                    }
                    break;
                    
                case "printable":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return StringFunctions.printable(arg);
                    }
                    break;
                    
                case "contains":
                    if (args.length >= 2) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        String substr = toString(resolveValue(args[1].trim(), context));
                        return StringFunctions.contains(str, substr);
                    }
                    break;
                    
                case "startswith":
                    if (args.length >= 2) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        String prefix = toString(resolveValue(args[1].trim(), context));
                        return StringFunctions.startsWith(str, prefix);
                    }
                    break;
                    
                case "endswith":
                    if (args.length >= 2) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        String suffix = toString(resolveValue(args[1].trim(), context));
                        return StringFunctions.endsWith(str, suffix);
                    }
                    break;
                    
                case "submatch":
                    if (args.length >= 2) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        String regex = toString(resolveValue(args[1].trim(), context));
                        List<String> matches = StringFunctions.submatch(str, regex);
                        // 始终返回列表，即使为空
                        return matches;
                    }
                    break;
                    
                case "matches":
                    if (args.length >= 2) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        String regex = toString(resolveValue(args[1].trim(), context));
                        return StringFunctions.matches(str, regex);
                    }
                    break;
                    
                case "trim":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return StringFunctions.trim(arg);
                    }
                    break;
                    
                // ==================== 字节级函数 ====================
                case "bytes":
                    if (args.length >= 1) {
                        String arg = toString(resolveValue(args[0].trim(), context));
                        return BytesFunctions.bytes(arg);
                    }
                    break;
                    
                case "bcontains":
                    if (args.length >= 2) {
                        String data = toString(resolveValue(args[0].trim(), context));
                        String pattern = toString(resolveValue(args[1].trim(), context));
                        return BytesFunctions.bcontains(data, pattern);
                    }
                    break;
                    
                case "bmatches":
                    if (args.length >= 2) {
                        String data = toString(resolveValue(args[0].trim(), context));
                        String regex = toString(resolveValue(args[1].trim(), context));
                        return BytesFunctions.bmatches(data, regex);
                    }
                    break;
                    
                case "bsubmatch":
                    if (args.length >= 2) {
                        String data = toString(resolveValue(args[0].trim(), context));
                        String regex = toString(resolveValue(args[1].trim(), context));
                        List<String> matches = BytesFunctions.bsubmatch(data, regex);
                        // 始终返回列表，即使为空
                        return matches;
                    }
                    break;
                    
                // ==================== 工具函数 ====================
                case "unixtime":
                    return UtilityFunctions.unixtime();
                    
                case "sleep":
                    if (args.length >= 1) {
                        int seconds = parseIntArg(args[0].trim(), context);
                        return UtilityFunctions.sleep(seconds);
                    }
                    break;
                    
                case "string":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return UtilityFunctions.string(arg);
                    }
                    break;
                    
                case "int":
                case "parseint":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return UtilityFunctions.parseInt(arg);
                    }
                    break;
                    
                case "icontains":
                    if (args.length >= 2) {
                        String str = toString(resolveValue(args[0].trim(), context));
                        String substr = toString(resolveValue(args[1].trim(), context));
                        return UtilityFunctions.icontains(str, substr);
                    }
                    break;
                    
                case "ibcontains":
                    // 忽略大小写的字节级包含检查
                    if (args.length >= 2) {
                        String data = toString(resolveValue(args[0].trim(), context));
                        String pattern = toString(resolveValue(args[1].trim(), context));
                        // 移除 b"..." 或 b'...' 包装
                        if (pattern.startsWith("b\"") && pattern.endsWith("\"")) {
                            pattern = pattern.substring(2, pattern.length() - 1);
                        } else if (pattern.startsWith("b'") && pattern.endsWith("'")) {
                            pattern = pattern.substring(2, pattern.length() - 1);
                        }
                        return data.toLowerCase().contains(pattern.toLowerCase());
                    }
                    break;
                    
                case "resolve":
                    if (args.length >= 1) {
                        String domain = toString(resolveValue(args[0].trim(), context));
                        return UtilityFunctions.resolve(domain);
                    }
                    break;
                    
                case "uuid":
                    return UtilityFunctions.uuid();
                    
                // ==================== 反连平台 ====================
                case "newreverse":
                    return new ReverseObject();
                    
                // ==================== 高级函数 ====================
                case "size":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.size(arg);
                    }
                    break;
                    
                case "in":
                    if (args.length >= 2) {
                        Object item = resolveValue(args[0].trim(), context);
                        Object list = resolveValue(args[1].trim(), context);
                        return AdvancedFunctions.in(item, list);
                    }
                    break;
                    
                case "has":
                    if (args.length >= 2) {
                        Object map = resolveValue(args[0].trim(), context);
                        String key = toString(resolveValue(args[1].trim(), context));
                        return AdvancedFunctions.has(map, key);
                    }
                    break;
                    
                case "json_decode":
                case "jsondecode":
                    if (args.length >= 1) {
                        String jsonStr = toString(resolveValue(args[0].trim(), context));
                        return AdvancedFunctions.json_decode(jsonStr);
                    }
                    break;
                    
                case "json_encode":
                case "jsonencode":
                    if (args.length >= 1) {
                        Object obj = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.json_encode(obj);
                    }
                    break;
                    
                case "isstring":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.isString(arg);
                    }
                    break;
                    
                case "isnumber":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.isNumber(arg);
                    }
                    break;
                    
                case "islist":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.isList(arg);
                    }
                    break;
                    
                case "ismap":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.isMap(arg);
                    }
                    break;
                    
                case "first":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.first((List<?>) arg);
                        }
                    }
                    break;
                    
                case "last":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.last((List<?>) arg);
                        }
                    }
                    break;
                    
                case "unique":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.unique((List<?>) arg);
                        }
                    }
                    break;
                    
                case "reverselist":
                case "reverse_list":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.reverseList((List<?>) arg);
                        }
                    }
                    break;
                    
                case "sort":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.sort((List<?>) arg);
                        }
                    }
                    break;
                    
                case "keys":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof Map) {
                            return AdvancedFunctions.keys((Map<?, ?>) arg);
                        }
                    }
                    break;
                    
                case "values":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof Map) {
                            return AdvancedFunctions.values((Map<?, ?>) arg);
                        }
                    }
                    break;
                    
                case "get":
                    if (args.length >= 2) {
                        Object obj = resolveValue(args[0].trim(), context);
                        String path = toString(resolveValue(args[1].trim(), context));
                        return AdvancedFunctions.get(obj, path);
                    }
                    break;
                    
                case "toint":
                case "to_int":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.toInt(arg);
                    }
                    break;
                    
                case "todouble":
                case "to_double":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        return AdvancedFunctions.toDouble(arg);
                    }
                    break;
                    
                case "containsall":
                case "contains_all":
                    if (args.length >= 2) {
                        Object list1 = resolveValue(args[0].trim(), context);
                        Object list2 = resolveValue(args[1].trim(), context);
                        if (list1 instanceof List && list2 instanceof List) {
                            return AdvancedFunctions.containsAll((List<?>) list1, (List<?>) list2);
                        }
                    }
                    break;
                    
                case "containsany":
                case "contains_any":
                    if (args.length >= 2) {
                        Object list1 = resolveValue(args[0].trim(), context);
                        Object list2 = resolveValue(args[1].trim(), context);
                        if (list1 instanceof List && list2 instanceof List) {
                            return AdvancedFunctions.containsAny((List<?>) list1, (List<?>) list2);
                        }
                    }
                    break;
                    
                // ==================== 高级列表操作（扩展） ====================
                case "all":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.all((List<?>) arg);
                        }
                    }
                    break;
                    
                case "any":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.any((List<?>) arg);
                        }
                    }
                    break;
                    
                case "join":
                    if (args.length >= 2) {
                        Object arg = resolveValue(args[0].trim(), context);
                        String separator = toString(resolveValue(args[1].trim(), context));
                        if (arg instanceof List) {
                            return AdvancedFunctions.join((List<?>) arg, separator);
                        }
                    }
                    break;
                    
                case "flatten":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.flatten((List<?>) arg);
                        }
                    }
                    break;
                    
                case "distinct":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.distinct((List<?>) arg);
                        }
                    }
                    break;
                    
                case "max":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.max((List<?>) arg);
                        }
                    }
                    break;
                    
                case "min":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.min((List<?>) arg);
                        }
                    }
                    break;
                    
                case "sum":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.sum((List<?>) arg);
                        }
                    }
                    break;
                    
                case "avg":
                case "average":
                    if (args.length >= 1) {
                        Object arg = resolveValue(args[0].trim(), context);
                        if (arg instanceof List) {
                            return AdvancedFunctions.avg((List<?>) arg);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("函数调用失败: " + functionName);
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 智能提取函数参数（支持嵌套括号）
     * 
     * @param expression 完整表达式
     * @param firstParen 第一个左括号的位置
     * @return 参数字符串，不包含外层括号
     */
    private static String extractFunctionArguments(String expression, int firstParen) {
        if (firstParen < 0 || firstParen >= expression.length() - 1) {
            return null;
        }
        
        int parenCount = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        
        for (int i = firstParen; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            // 处理转义字符
            if (i > 0 && expression.charAt(i - 1) == '\\') {
                continue;
            }
            
            // 处理引号
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            
            // 在引号外处理括号
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenCount++;
                } else if (c == ')') {
                    parenCount--;
                    if (parenCount == 0) {
                        // 找到匹配的右括号
                        return expression.substring(firstParen + 1, i);
                    }
                }
            }
        }
        
        // 如果没有找到匹配的括号，返回null
        return null;
    }
    
    /**
     * 评估表达式并返回值
     */
    public static Object evaluateForValue(String expression, Map<String, Object> context) {
        return resolveValue(expression, context);
    }
    
    /**
     * 解析整数参数
     */
    private static int parseIntArg(String arg, Map<String, Object> context) {
        Object value = resolveValue(arg, context);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
    
    /**
     * 判断是否为数值
     */
    private static boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String) {
            try {
                Double.parseDouble((String) value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * 转换为 double
     */
    private static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
    
    /**
     * 转换为字符串
     */
    private static String toString(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
    
    /**
     * 处理字符串中的转义字符
     * 
     * @param str 包含转义字符的字符串
     * @return 处理后的字符串
     */
    private static String unescapeString(String str) {
        if (str == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case 'n':
                        result.append('\n');
                        i++;
                        break;
                    case 'r':
                        result.append('\r');
                        i++;
                        break;
                    case 't':
                        result.append('\t');
                        i++;
                        break;
                    case 'b':
                        result.append('\b');
                        i++;
                        break;
                    case 'f':
                        result.append('\f');
                        i++;
                        break;
                    case '\\':
                        result.append('\\');
                        i++;
                        break;
                    case '\'':
                        result.append('\'');
                        i++;
                        break;
                    case '\"':
                        result.append('\"');
                        i++;
                        break;
                    case 'x':
                        // 处理十六进制转义格式 (\\xHH)
                        if (i + 3 < str.length()) {
                            try {
                                String hex = str.substring(i + 2, i + 4);
                                int code = Integer.parseInt(hex, 16);
                                result.append((char) code);
                                i += 3;
                            } catch (NumberFormatException e) {
                                result.append(c);
                            }
                        } else {
                            result.append(c);
                        }
                        break;
                    case 'u':
                        // 处理 Unicode 转义格式 (\\uXXXX)
                        if (i + 5 < str.length()) {
                            try {
                                String hex = str.substring(i + 2, i + 6);
                                int code = Integer.parseInt(hex, 16);
                                result.append((char) code);
                                i += 5;
                            } catch (NumberFormatException e) {
                                result.append(c);
                            }
                        } else {
                            result.append(c);
                        }
                        break;
                    default:
                        // 不是标准转义字符，保留反斜杠和下一个字符
                        // 这对于正则表达式很重要，例如 \d, \w, \s 等
                        result.append(c);
                        result.append(next);
                        i++;
                        break;
                }
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}
