package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * DSL函数评估器
 * 负责评估各种DSL函数调用
 */
public class DslFunctionEvaluator {

    /**
     * 评估contains函数
     */
    public static boolean evaluateContainsFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String searchValue = DslUtils.cleanStringValue(args[1]);

            if (target == null || searchValue == null) {
                return false;
            }

            boolean result = target.contains(searchValue);
            
            // 处理剩余的比较操作
            if (args.length > 2) {
                String remainingExpression = String.join(",", java.util.Arrays.copyOfRange(args, 2, args.length));
                return processComparisonOperator(String.valueOf(result), remainingExpression, context);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估contains函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估matches函数（正则匹配）
     */
    public static boolean evaluateMatchesFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String pattern = DslUtils.cleanStringValue(args[1]);

            if (target == null || pattern == null) {
                return false;
            }

            boolean result = Pattern.compile(pattern).matcher(target).find();
            
            // 处理剩余的比较操作
            if (args.length > 2) {
                String remainingExpression = String.join(",", java.util.Arrays.copyOfRange(args, 2, args.length));
                return processComparisonOperator(String.valueOf(result), remainingExpression, context);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估matches函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估len函数（长度）
     */
    public static boolean evaluateLenFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return false;
            }

            int length = target.length();
            context.put("__length_result", length);
            
            // 处理剩余的比较操作
            if (args.length > 1) {
                String remainingExpression = String.join(",", java.util.Arrays.copyOfRange(args, 1, args.length));
                return processComparisonOperator(String.valueOf(length), remainingExpression, context);
            }
            
            return length > 0;
            
        } catch (Exception e) {
            System.err.println("评估len函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估startsWith函数
     */
    public static boolean evaluateStartsWithFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String prefix = DslUtils.cleanStringValue(args[1]);

            if (target == null || prefix == null) {
                return false;
            }

            return target.startsWith(prefix);
            
        } catch (Exception e) {
            System.err.println("评估startsWith函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估endsWith函数
     */
    public static boolean evaluateEndsWithFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String suffix = DslUtils.cleanStringValue(args[1]);

            if (target == null || suffix == null) {
                return false;
            }

            return target.endsWith(suffix);
            
        } catch (Exception e) {
            System.err.println("评估endsWith函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估toLowerCase函数并返回字符串值
     */
    public static String evaluateToLowerCaseFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = target.toLowerCase();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估toLowerCase函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估toUpperCase函数并返回字符串值
     */
    public static String evaluateToUpperCaseFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = target.toUpperCase();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估toUpperCase函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估base64函数并返回字符串值
     */
    public static String evaluateBase64Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = Base64.getEncoder().encodeToString(target.getBytes(DslConstants.DEFAULT_CHARSET));
            return result;
            
        } catch (Exception e) {
            System.err.println("评估base64函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估md5函数并返回字符串值
     */
    public static String evaluateMd5Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(target.getBytes(DslConstants.DEFAULT_CHARSET));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            String result = sb.toString();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估md5函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估substr函数并返回字符串值
     */
    public static String evaluateSubstrFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            int start = DslUtils.safeParseInt(args[1], 0);
            
            // 处理负数索引
            if (start < 0) {
                start = Math.max(0, target.length() + start);
            }
            
            if (start >= target.length()) {
                return "";
            }

            String result;
            if (args.length >= 3) {
                int length = DslUtils.safeParseInt(args[2], target.length() - start);
                int endIndex = Math.min(start + length, target.length());
                result = target.substring(start, endIndex);
            } else {
                result = target.substring(start);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估substr函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估trim函数并返回字符串值
     */
    public static String evaluateTrimFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = target.trim();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估trim函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估unixtime函数
     */
    public static boolean evaluateUnixtimeFunction(String expression, Map<String, Object> context) {
        try {
            long unixTime = System.currentTimeMillis() / 1000;
            
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length > 0) {
                String remainingExpression = String.join(",", args);
                return processComparisonOperator(String.valueOf(unixTime), remainingExpression, context);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("评估unixtime函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估urldecode函数并返回字符串值
     */
    public static String evaluateUrlDecodeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result;
            try {
                result = URLDecoder.decode(target, DslConstants.DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                System.err.println("URL解码失败: " + e.getMessage());
                return null;
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估urldecode函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估regex函数
     */
    public static boolean evaluateRegexFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                System.err.println("regex函数需要至少2个参数: pattern, target");
                return false;
            }

            String pattern = args[0].trim();
            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
            
            if (target == null) {
                return false;
            }

            // 移除引号
            if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
                pattern = pattern.substring(1, pattern.length() - 1);
            }
            
            // 处理双重转义：将\\转换为\
            pattern = pattern.replace("\\\\", "\\");
            
            // 编译并匹配正则表达式
            Pattern regexPattern = Pattern.compile(pattern);
            boolean matches = regexPattern.matcher(target).find();
            
            return matches;
            
        } catch (Exception e) {
            System.err.println("评估regex函数失败: " + e.getMessage());
            return false;
        }
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

        // 检查是否是函数调用
        if (DslConstants.FUNCTION_PATTERN.matcher(value).find()) {
            return DslEvaluatorRefactored.evaluateFunctionForValue(value, context);
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
     * 统一的函数评估方法（优化版本 - 使用统一的函数类型管理器）
     */
    public static boolean evaluateFunction(String expression, Map<String, Object> context) {
        try {
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
            System.err.println("函数评估失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理比较操作符
     */
    private static boolean processComparisonOperator(String leftValue, String expression, Map<String, Object> context) {
        try {
            String[] parts = DslExpressionParser.parseComparisonExpression(expression);
            if (parts.length != 3 || parts[1].isEmpty()) {
                return true; // 没有比较操作符，默认返回true
            }

            String operator = parts[1];
            String rightValue = DslEvaluatorRefactored.resolveValueOrFunction(parts[2], context);

            return compareValues(leftValue, operator, rightValue);
            
        } catch (Exception e) {
            System.err.println("处理比较操作符失败: " + e.getMessage());
            return false;
        }
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
    private DslFunctionEvaluator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}