package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DSL表达式解析器
 * 负责解析DSL表达式，将其转换为可执行的匹配器结构
 */
public class DslExpressionParser {

    /**
     * DSL匹配器类
     */
    public static class DslMatcher {
        private String type;
        private String expression;
        private String operator;
        private List<DslMatcher> subMatchers;

        public DslMatcher(String type, String expression) {
            this.type = type;
            this.expression = expression;
            this.subMatchers = new ArrayList<>();
        }

        public DslMatcher(String operator, List<DslMatcher> subMatchers) {
            this.operator = operator;
            this.subMatchers = subMatchers;
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public List<DslMatcher> getSubMatchers() { return subMatchers; }
        public void setSubMatchers(List<DslMatcher> subMatchers) { this.subMatchers = subMatchers; }
    }

    /**
     * 将DSL表达式解析为匹配器列表
     */
    public static List<DslMatcher> parseDslToMatchers(List<String> dslExpressions) {
        List<DslMatcher> matchers = new ArrayList<>();
        
        for (String expression : dslExpressions) {
            try {
                String normalizedExpression = DslUtils.normalizeExpression(expression);
                DslMatcher matcher = parseExpression(normalizedExpression);
                if (matcher != null) {
                    matchers.add(matcher);
                }
            } catch (Exception e) {
                System.err.println("解析DSL表达式失败: " + expression + ", 错误: " + e.getMessage());
                // 创建一个默认的DSL类型匹配器
                matchers.add(new DslMatcher("dsl", expression));
            }
        }
        
        return matchers;
    }

    /**
     * 解析单个表达式
     */
    private static DslMatcher parseExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        expression = stripEnclosingParentheses(expression.trim());

        // 检查是否包含逻辑操作符
        if (containsLogicalOperators(expression)) {
            return parseLogicalExpression(expression);
        }

        // 解析单个条件
        return parseSingleCondition(expression);
    }

    /**
     * 检查表达式是否包含逻辑操作符
     */
    private static boolean containsLogicalOperators(String expression) {
        return hasTopLevelLogicalOperator(expression) ||
               stripLeadingNot(expression) != null;
    }

    /**
     * 解析包含逻辑操作符的表达式
     */
    private static DslMatcher parseLogicalExpression(String expression) {
        expression = stripEnclosingParentheses(expression.trim());

        // 首先按 OR 分割
        List<String> orParts = splitTopLevelLogical(expression, "OR");
        if (orParts.size() > 1) {
            List<DslMatcher> orMatchers = new ArrayList<>();
            for (String part : orParts) {
                DslMatcher matcher = parseExpression(part.trim());
                if (matcher != null) {
                    orMatchers.add(matcher);
                }
            }
            return new DslMatcher("OR", orMatchers);
        }

        // 然后按 AND 分割
        List<String> andParts = splitTopLevelLogical(expression, "AND");
        if (andParts.size() > 1) {
            List<DslMatcher> andMatchers = new ArrayList<>();
            for (String part : andParts) {
                DslMatcher matcher = parseExpression(part.trim());
                if (matcher != null) {
                    andMatchers.add(matcher);
                }
            }
            return new DslMatcher("AND", andMatchers);
        }

        String withoutNot = stripLeadingNot(expression);
        if (withoutNot != null) {
            DslMatcher innerMatcher = parseExpression(withoutNot);
            if (innerMatcher != null) {
                List<DslMatcher> notMatchers = new ArrayList<>();
                notMatchers.add(innerMatcher);
                return new DslMatcher("NOT", notMatchers);
            }
        }

        // 如果没有找到逻辑操作符，解析为单个条件
        return parseSingleCondition(expression);
    }

    /**
     * 解析单个条件
     */
    private static DslMatcher parseSingleCondition(String expression) {
        expression = stripEnclosingParentheses(expression.trim());

        // 处理 NOT 操作符
        String withoutNot = stripLeadingNot(expression);
        if (withoutNot != null) {
            DslMatcher innerMatcher = parseExpression(withoutNot);
            if (innerMatcher != null) {
                List<DslMatcher> notMatchers = new ArrayList<>();
                notMatchers.add(innerMatcher);
                return new DslMatcher("NOT", notMatchers);
            }
        }

        // 检查函数调用
        if (isFunctionCall(expression)) {
            return parseFunctionCall(expression);
        }

        // 检查状态码比较
        if (isStatusComparison(expression)) {
            return new DslMatcher("status", expression);
        }

        // 检查比较表达式
        if (isComparisonExpression(expression)) {
            return new DslMatcher("comparison", expression);
        }

        // 默认作为DSL表达式处理
        return new DslMatcher("dsl", expression);
    }

    /**
     * 检查是否为函数调用
     */
    private static boolean isFunctionCall(String expression) {
        return DslConstants.FUNCTION_PATTERN.matcher(expression).find();
    }

    /**
     * 解析函数调用
     */
    private static DslMatcher parseFunctionCall(String expression) {
        Matcher matcher = DslConstants.FUNCTION_PATTERN.matcher(expression);
        if (matcher.find()) {
            String functionName = matcher.group(1);
            
            switch (functionName.toLowerCase()) {
                case "contains":
                case "bcontains":
                    return new DslMatcher("contains", expression);
                case "ignorecase":
                    return new DslMatcher("ignoreCase", expression);
                case "matches":
                case "bmatches":
                    return new DslMatcher("matches", expression);
                case "len":
                case "length":
                    return new DslMatcher("len", expression);
                case "startswith":
                    return new DslMatcher("startsWith", expression);
                case "endswith":
                    return new DslMatcher("endsWith", expression);
                default:
                    return new DslMatcher("function", expression);
            }
        }
        
        return new DslMatcher("dsl", expression);
    }

    /**
     * 判断表达式是否包含顶层逻辑操作符。
     */
    public static boolean hasTopLevelLogicalOperator(String expression) {
        String strippedExpression = stripEnclosingParentheses(expression);
        return splitTopLevelLogical(strippedExpression, "OR").size() > 1 ||
               splitTopLevelLogical(strippedExpression, "AND").size() > 1;
    }

    /**
     * 只在顶层拆分逻辑操作符，忽略字符串和括号内的 and/or、&&、||。
     */
    public static List<String> splitTopLevelLogical(String expression, String operator) {
        List<String> parts = new ArrayList<>();
        if (expression == null) {
            return parts;
        }

        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            parts.add(trimmed);
            return parts;
        }

        String normalizedOperator = operator == null ? "" : operator.trim().toUpperCase();
        int start = 0;
        int parenthesesLevel = 0;
        boolean inQuotes = false;
        char quoteChar = '\0';

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
                continue;
            }
            if (inQuotes) {
                if (c == quoteChar && !isEscaped(trimmed, i)) {
                    inQuotes = false;
                }
                continue;
            }

            if (c == '(') {
                parenthesesLevel++;
                continue;
            }
            if (c == ')') {
                if (parenthesesLevel > 0) {
                    parenthesesLevel--;
                }
                continue;
            }

            if (parenthesesLevel == 0) {
                int operatorLength = topLevelLogicalOperatorLength(trimmed, i, normalizedOperator);
                if (operatorLength > 0) {
                    parts.add(trimmed.substring(start, i).trim());
                    start = i + operatorLength;
                    i = start - 1;
                }
            }
        }

        if (start == 0) {
            parts.add(trimmed);
        } else {
            parts.add(trimmed.substring(start).trim());
        }
        return parts;
    }

    private static int topLevelLogicalOperatorLength(String expression, int index, String operator) {
        if ("OR".equals(operator)) {
            if (expression.startsWith("||", index)) {
                return 2;
            }
            return wordLogicalOperatorLength(expression, index, "or");
        }
        if ("AND".equals(operator)) {
            if (expression.startsWith("&&", index)) {
                return 2;
            }
            return wordLogicalOperatorLength(expression, index, "and");
        }
        return 0;
    }

    private static int wordLogicalOperatorLength(String expression, int index, String operator) {
        int end = index + operator.length();
        if (end > expression.length()) {
            return 0;
        }
        if (!expression.regionMatches(true, index, operator, 0, operator.length())) {
            return 0;
        }
        boolean leftBoundary = index == 0 || !isIdentifierChar(expression.charAt(index - 1));
        boolean rightBoundary = end == expression.length() || !isIdentifierChar(expression.charAt(end));
        return leftBoundary && rightBoundary ? operator.length() : 0;
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * 移除完整包裹表达式的一层或多层括号。
     */
    public static String stripEnclosingParentheses(String expression) {
        if (expression == null) {
            return null;
        }

        String result = expression.trim();
        while (result.length() >= 2 && result.charAt(0) == '(' && result.charAt(result.length() - 1) == ')'
                && closingParenthesisMatchesEnd(result)) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private static boolean closingParenthesisMatchesEnd(String expression) {
        int parenthesesLevel = 0;
        boolean inQuotes = false;
        char quoteChar = '\0';

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
                continue;
            }
            if (inQuotes) {
                if (c == quoteChar && !isEscaped(expression, i)) {
                    inQuotes = false;
                }
                continue;
            }

            if (c == '(') {
                parenthesesLevel++;
            } else if (c == ')') {
                parenthesesLevel--;
                if (parenthesesLevel == 0 && i < expression.length() - 1) {
                    return false;
                }
                if (parenthesesLevel < 0) {
                    return false;
                }
            }
        }

        return parenthesesLevel == 0;
    }

    /**
     * 移除表达式开头的顶层 not / !，不处理 !=。
     */
    public static String stripLeadingNot(String expression) {
        if (expression == null) {
            return null;
        }

        String trimmed = expression.trim();
        if (trimmed.startsWith("!") && !trimmed.startsWith("!=")) {
            return stripEnclosingParentheses(trimmed.substring(1).trim());
        }
        if (trimmed.length() >= 3 && trimmed.regionMatches(true, 0, "not", 0, 3)
                && (trimmed.length() == 3 || !isIdentifierChar(trimmed.charAt(3)))) {
            return stripEnclosingParentheses(trimmed.substring(3).trim());
        }
        return null;
    }

    /**
     * 检查是否为状态码比较
     */
    private static boolean isStatusComparison(String expression) {
        return expression.toLowerCase().contains("status") && 
               DslUtils.findComparisonOperator(expression) != null;
    }

    /**
     * 检查是否为比较表达式
     */
    private static boolean isComparisonExpression(String expression) {
        return DslUtils.findComparisonOperator(expression) != null;
    }

    /**
     * 解析contains函数的参数
     * 改进：支持嵌套括号的正确匹配，正确处理引号内的括号字符
     */
    public static String[] parseContainsFunction(String expression) {
        try {
            int startIndex = expression.indexOf('(');

            if (startIndex < 0 || startIndex >= expression.length() - 1) {
                return new String[0];
            }

            // 从第一个左括号开始，匹配对应的右括号（支持嵌套括号和引号）
            int parenthesesLevel = 0;
            int endIndex = -1;
            boolean inQuotes = false;
            char quoteChar = '\0';

            for (int i = startIndex; i < expression.length(); i++) {
                char c = expression.charAt(i);

                // 处理引号状态
                if (!inQuotes && (c == '"' || c == '\'')) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (inQuotes && c == quoteChar) {
                    // 检查是否为转义引号
                    if (i > 0 && expression.charAt(i - 1) != '\\') {
                        inQuotes = false;
                    }
                } else if (!inQuotes) {
                    // 只在非引号状态下处理括号
                    if (c == '(') {
                        parenthesesLevel++;
                    } else if (c == ')') {
                        parenthesesLevel--;

                        if (parenthesesLevel == 0) {
                            // 找到匹配的右括号
                            endIndex = i;
                            break;
                        }
                    }
                }
            }

            if (endIndex <= startIndex) {
                // 降低日志级别，这通常不是错误
                // System.err.println("[警告] 未找到匹配的右括号: " + expression);
                return new String[0];
            }

            // 提取括号内的参数字符串
            String argsString = expression.substring(startIndex + 1, endIndex);
            return parseArguments(argsString);

        } catch (Exception e) {
            System.err.println("解析contains函数参数失败: " + expression + " - " + e.getMessage());
            return new String[0];
        }
    }

    /**
     * 解析函数参数
     */
    public static String[] parseArguments(String argsString) {
        if (argsString == null || argsString.trim().isEmpty()) {
            return new String[0];
        }

        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\0';
        int parenthesesLevel = 0;

        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
                currentArg.append(c);
            } else if (inQuotes && c == quoteChar) {
                if (!isEscaped(argsString, i)) {
                    inQuotes = false;
                }
                currentArg.append(c);
            } else if (!inQuotes && c == '(') {
                parenthesesLevel++;
                currentArg.append(c);
            } else if (!inQuotes && c == ')') {
                parenthesesLevel--;
                currentArg.append(c);
            } else if (!inQuotes && c == ',' && parenthesesLevel == 0) {
                args.add(currentArg.toString().trim());
                currentArg = new StringBuilder();
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            args.add(currentArg.toString().trim());
        }

        return args.toArray(new String[0]);
    }

    private static boolean isEscaped(String value, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    /**
     * 提取比较操作符和操作数
     */
    public static String[] parseComparisonExpression(String expression) {
        String operator = DslUtils.findComparisonOperator(expression);
        if (operator == null) {
            return new String[]{expression, "", ""};
        }

        String[] parts = expression.split(Pattern.quote(operator), 2);
        if (parts.length == 2) {
            return new String[]{parts[0].trim(), operator, parts[1].trim()};
        }

        return new String[]{expression, "", ""};
    }

    /**
     * 私有构造函数，防止实例化
     */
    private DslExpressionParser() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
