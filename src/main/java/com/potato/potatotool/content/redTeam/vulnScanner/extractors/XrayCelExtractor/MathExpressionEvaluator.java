package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数学表达式评估器
 * 支持带括号优先级的算术运算
 * 
 * 支持的运算符：
 * - 加法: +
 * - 减法: -
 * - 乘法: *
 * - 除法: /
 * - 取模: %
 * - 括号: ()
 * 
 * 示例:
 * - (5 + 3) * 2 => 16
 * - 10 - 3 * 2 => 4
 * - (response.latency - baseline) / 1000 => 计算延迟差（秒）
 * 
 * @author Potato
 * @date 2025-10-30
 */
public class MathExpressionEvaluator {
    
    /**
     * 评估数学表达式
     * 
     * @param expression 数学表达式
     * @param context CEL 上下文
     * @return 计算结果
     */
    public static double evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            // 预处理：替换变量
            String processed = preprocessExpression(expression, context);
            
            // 使用双栈算法评估表达式
            return evaluateWithStacks(processed);
            
        } catch (Exception e) {
            System.err.println("数学表达式评估失败: " + expression + " - " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 预处理表达式：替换变量为具体值
     */
    private static String preprocessExpression(String expression, Map<String, Object> context) {
        String result = expression.trim();
        
        // 替换变量（如 response.latency, baseline）
        // 使用正则匹配标识符
        Pattern pattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_.]*)\\b");
        Matcher matcher = pattern.matcher(result);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            
            // 尝试从上下文获取值
            Object value = resolveVariable(varName, context);
            if (value != null) {
                String replacement = String.valueOf(toNumber(value));
                matcher.appendReplacement(sb, replacement);
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 解析变量
     */
    private static Object resolveVariable(String varName, Map<String, Object> context) {
        if (context == null || varName == null) {
            return null;
        }
        
        // 简单变量
        if (context.containsKey(varName)) {
            return context.get(varName);
        }
        
        // 属性访问（如 response.latency）
        if (varName.contains(".")) {
            return XrayCelParser.evaluateForValue(varName, context);
        }
        
        return null;
    }
    
    /**
     * 转换为数值
     */
    private static double toNumber(Object obj) {
        if (obj == null) {
            return 0.0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * 使用双栈算法评估表达式
     * 
     * @param expression 预处理后的表达式（只包含数字和运算符）
     * @return 计算结果
     */
    private static double evaluateWithStacks(String expression) {
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();
        
        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            
            // 跳过空格
            if (c == ' ') {
                i++;
                continue;
            }
            
            // 数字（包括负数）
            if (Character.isDigit(c) || (c == '-' && (i == 0 || expression.charAt(i-1) == '('))) {
                StringBuilder num = new StringBuilder();
                if (c == '-') {
                    num.append(c);
                    i++;
                }
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    num.append(expression.charAt(i++));
                }
                numbers.push(Double.parseDouble(num.toString()));
                continue;
            }
            
            // 左括号
            if (c == '(') {
                operators.push(c);
                i++;
                continue;
            }
            
            // 右括号
            if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                if (!operators.isEmpty()) {
                    operators.pop(); // 弹出 '('
                }
                i++;
                continue;
            }
            
            // 运算符
            if (isOperator(c)) {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(c)) {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
                i++;
                continue;
            }
            
            i++;
        }
        
        // 处理剩余的运算符
        while (!operators.isEmpty()) {
            numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
        }
        
        return numbers.isEmpty() ? 0.0 : numbers.pop();
    }
    
    /**
     * 判断是否为运算符
     */
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
    }
    
    /**
     * 运算符优先级
     */
    private static int precedence(char operator) {
        switch (operator) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
            case '%':
                return 2;
            default:
                return 0;
        }
    }
    
    /**
     * 应用运算符
     * 注意：b 在前，a 在后（因为栈是后进先出）
     */
    private static double applyOperator(char operator, double a, double b) {
        switch (operator) {
            case '+':
                return b + a;
            case '-':
                return b - a;
            case '*':
                return b * a;
            case '/':
                if (a == 0) {
                    throw new ArithmeticException("除数不能为0");
                }
                return b / a;
            case '%':
                if (a == 0) {
                    throw new ArithmeticException("取模的除数不能为0");
                }
                return b % a;
            default:
                return 0;
        }
    }
    
    /**
     * 检测表达式是否为数学表达式
     * 
     * @param expression 表达式
     * @return 是否为数学表达式
     */
    public static boolean isMathExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        // 包含算术运算符（排除负号开头的情况）
        String trimmed = expression.trim();
        return (trimmed.contains(" + ") || 
                trimmed.contains(" - ") || 
                trimmed.contains(" * ") || 
                trimmed.contains(" / ") || 
                trimmed.contains(" % ") ||
                trimmed.contains("(") && (trimmed.contains("+") || trimmed.contains("*") || trimmed.contains("/")));
    }
}

