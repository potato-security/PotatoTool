package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.utils.CustomHttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DSL表达式评估器，用于解析和评估各种DSL表达式
 */
public class DslEvaluatorNew {

    /**
     * 使用DSL表达式提取内容
     * @param response HTTP响应
     * @param expressions DSL表达式列表
     * @return 提取的内容
     */
    public static String extractDsl(CustomHttpResponse response, List<String> dslExpressions) {
        if (response == null || dslExpressions == null || dslExpressions.isEmpty()) {
            return null;
        }

        try {
            // 遍历所有DSL表达式
            for (String expression : dslExpressions) {
                expression = expression.trim();
                
                // 提取正则表达式内容 - regex函数格式: regex("pattern", body)
                Pattern regexPattern = Pattern.compile("regex\\(\"(.*?)\",\\s*(.*?)\\)");
                Matcher regexMatcher = regexPattern.matcher(expression);
                if (regexMatcher.find()) {
                    String pattern = regexMatcher.group(1);
                    String sourcePart = regexMatcher.group(2).trim();
                    
                    String sourceContent = getResponsePart(response, sourcePart);
                    if (sourceContent != null) {
                        Pattern p = Pattern.compile(pattern);
                        Matcher m = p.matcher(sourceContent);
                        if (m.find()) {
                            return m.group(0);
                        }
                    }
                    continue;
                }
                
                // 检查contains函数 - contains(body, "searchString")
                Pattern containsPattern = Pattern.compile("contains\\((.*?),\\s*\"(.*?)\"\\)");
                Matcher containsMatcher = containsPattern.matcher(expression);
                if (containsMatcher.find()) {
                    String sourcePart = containsMatcher.group(1).trim();
                    String searchString = containsMatcher.group(2);
                    
                    String sourceContent = getResponsePart(response, sourcePart);
                    if (sourceContent != null && sourceContent.contains(searchString)) {
                        return searchString;
                    }
                    continue;
                }

                // 如果表达式直接指向响应的某个部分，直接返回该部分内容
                if (expression.equals("body") || expression.equals("header") || expression.equals("status") || expression.equals("all")) {
                    return getResponsePart(response, expression);
                }
            }
        } catch (Exception e) {
            System.err.println("DSL表达式提取内容失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * 使用DSL表达式判断是否匹配
     * @param response HTTP响应
     * @param dslExpressions DSL表达式列表
     * @return 是否匹配成功
     */
    public static boolean matchDslWithNestedMatchers(CustomHttpResponse response, List<String> dslExpressions) {
        if (response == null || dslExpressions == null || dslExpressions.isEmpty()) {
            return false;
        }

        try {
            // 遍历所有DSL表达式
            for (String expression : dslExpressions) {
                expression = expression.trim();
                
                // 如果为空，则跳过
                if (expression.isEmpty()) {
                    continue;
                }
                
                boolean result = evaluateDslExpression(response, expression);
                if (result) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("DSL表达式匹配失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * 评估单个DSL表达式
     * @param response HTTP响应
     * @param expression DSL表达式
     * @return 是否匹配成功
     */
    private static boolean evaluateDslExpression(CustomHttpResponse response, String expression) {
        try {
            // 处理逻辑运算符 && (AND)
            if (expression.contains("&&")) {
                String[] conditions = expression.split("&&");
                for (String condition : conditions) {
                    if (!evaluateDslExpression(response, condition.trim())) {
                        return false;
                    }
                }
                return true;
            }
            
            // 处理逻辑运算符 || (OR)
            if (expression.contains("||")) {
                String[] conditions = expression.split("\\|\\|");
                for (String condition : conditions) {
                    if (evaluateDslExpression(response, condition.trim())) {
                        return true;
                    }
                }
                return false;
            }
            
            // 处理逻辑运算符 ! (NOT)
            if (expression.startsWith("!")) {
                return !evaluateDslExpression(response, expression.substring(1).trim());
            }

            // 处理相等判断 ==
            if (expression.contains("==")) {
                String[] parts = expression.split("==");
                String left = evaluateOperand(response, parts[0].trim());
                String right = evaluateOperand(response, parts[1].trim());
                return left != null && right != null && left.equals(right);
            }
            
            // 处理不等判断 !=
            if (expression.contains("!=")) {
                String[] parts = expression.split("!=");
                String left = evaluateOperand(response, parts[0].trim());
                String right = evaluateOperand(response, parts[1].trim());
                return left != null && right != null && !left.equals(right);
            }
            
            // 处理大于判断 >
            if (expression.contains(" > ")) {
                String[] parts = expression.split(" > ");
                try {
                    double left = Double.parseDouble(evaluateOperand(response, parts[0].trim()));
                    double right = Double.parseDouble(evaluateOperand(response, parts[1].trim()));
                    return left > right;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            // 处理小于判断 <
            if (expression.contains(" < ")) {
                String[] parts = expression.split(" < ");
                try {
                    double left = Double.parseDouble(evaluateOperand(response, parts[0].trim()));
                    double right = Double.parseDouble(evaluateOperand(response, parts[1].trim()));
                    return left < right;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            // 处理大于等于判断 >=
            if (expression.contains(">=")) {
                String[] parts = expression.split(">=");
                try {
                    double left = Double.parseDouble(evaluateOperand(response, parts[0].trim()));
                    double right = Double.parseDouble(evaluateOperand(response, parts[1].trim()));
                    return left >= right;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            // 处理小于等于判断 <=
            if (expression.contains("<=")) {
                String[] parts = expression.split("<=");
                try {
                    double left = Double.parseDouble(evaluateOperand(response, parts[0].trim()));
                    double right = Double.parseDouble(evaluateOperand(response, parts[1].trim()));
                    return left <= right;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            // 处理contains函数 - contains(body, "searchString")
            Pattern containsPattern = Pattern.compile("contains\\((.*?),\\s*\"(.*?)\"\\)");
            Matcher containsMatcher = containsPattern.matcher(expression);
            if (containsMatcher.find()) {
                String sourcePart = containsMatcher.group(1).trim();
                String searchString = containsMatcher.group(2);
                
                String sourceContent = getResponsePart(response, sourcePart);
                return sourceContent != null && sourceContent.contains(searchString);
            }
            
            // 处理contains_all函数 - contains_all(body, "str1", "str2", ...)
            Pattern containsAllPattern = Pattern.compile("contains_all\\((.*?),\\s*(.+)\\)");
            Matcher containsAllMatcher = containsAllPattern.matcher(expression);
            if (containsAllMatcher.find()) {
                String sourcePart = containsAllMatcher.group(1).trim();
                String searchStrings = containsAllMatcher.group(2).trim();
                
                String[] stringsToSearch = searchStrings.split(",");
                String sourceContent = getResponsePart(response, sourcePart);
                
                if (sourceContent == null) {
                    return false;
                }
                
                for (String str : stringsToSearch) {
                    str = str.trim();
                    if (str.startsWith("\"") && str.endsWith("\"")) {
                        str = str.substring(1, str.length() - 1);
                    }
                    if (!sourceContent.contains(str)) {
                        return false;
                    }
                }
                return true;
            }
            
            // 处理contains_any函数 - contains_any(body, "str1", "str2", ...)
            Pattern containsAnyPattern = Pattern.compile("contains_any\\((.*?),\\s*(.+)\\)");
            Matcher containsAnyMatcher = containsAnyPattern.matcher(expression);
            if (containsAnyMatcher.find()) {
                String sourcePart = containsAnyMatcher.group(1).trim();
                String searchStrings = containsAnyMatcher.group(2).trim();
                
                String[] stringsToSearch = searchStrings.split(",");
                String sourceContent = getResponsePart(response, sourcePart);
                
                if (sourceContent == null) {
                    return false;
                }
                
                for (String str : stringsToSearch) {
                    str = str.trim();
                    if (str.startsWith("\"") && str.endsWith("\"")) {
                        str = str.substring(1, str.length() - 1);
                    }
                    if (sourceContent.contains(str)) {
                        return true;
                    }
                }
                return false;
            }
            
            // 处理regex函数 - regex("pattern", body)
            Pattern regexPattern = Pattern.compile("regex\\(\"(.*?)\",\\s*(.*?)\\)");
            Matcher regexMatcher = regexPattern.matcher(expression);
            if (regexMatcher.find()) {
                String pattern = regexMatcher.group(1);
                String sourcePart = regexMatcher.group(2).trim();
                
                String sourceContent = getResponsePart(response, sourcePart);
                if (sourceContent != null) {
                    Pattern p = Pattern.compile(pattern);
                    Matcher m = p.matcher(sourceContent);
                    return m.find();
                }
                return false;
            }
            
            // 处理status_code
            if (expression.startsWith("status_code")) {
                try {
                    int statusCode = response.getResponseCode();
                    
                    // status_code == 200
                    if (expression.contains("==")) {
                        int compareCode = Integer.parseInt(expression.split("==")[1].trim());
                        return statusCode == compareCode;
                    }
                    
                    // status_code != 404
                    if (expression.contains("!=")) {
                        int compareCode = Integer.parseInt(expression.split("!=")[1].trim());
                        return statusCode != compareCode;
                    }
                    
                    // status_code < 400
                    if (expression.contains("<")) {
                        int compareCode = Integer.parseInt(expression.split("<")[1].trim());
                        return statusCode < compareCode;
                    }
                    
                    // status_code > 200
                    if (expression.contains(">")) {
                        int compareCode = Integer.parseInt(expression.split(">")[1].trim());
                        return statusCode > compareCode;
                    }
                    
                    // 默认返回状态码
                    return statusCode > 0;
                } catch (Exception e) {
                    return false;
                }
            }
            
            // 如果表达式只是简单的body, header, status等，检查它们是否非空
            String content = getResponsePart(response, expression);
            return content != null && !content.isEmpty();
            
        } catch (Exception e) {
            System.err.println("评估DSL表达式失败: " + expression + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 评估操作数，如body, header, status等
     * @param response HTTP响应
     * @param operand 操作数
     * @return 操作数的值
     */
    private static String evaluateOperand(CustomHttpResponse response, String operand) {
        // 如果是字符串常量
        if (operand.startsWith("\"") && operand.endsWith("\"")) {
            return operand.substring(1, operand.length() - 1);
        }
        
        // 如果是数字常量
        try {
            Double.parseDouble(operand);
            return operand;
        } catch (NumberFormatException e) {
            // 不是数字，继续检查其他可能性
        }
        
        // 如果是函数调用
        if (operand.contains("(") && operand.contains(")")) {
            // MD5 函数 - md5(body)
            if (operand.startsWith("md5(")) {
                String sourcePart = operand.substring(4, operand.length() - 1).trim();
                String sourceContent = getResponsePart(response, sourcePart);
                if (sourceContent != null) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        byte[] digest = md.digest(sourceContent.getBytes(StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digest) {
                            sb.append(String.format("%02x", b));
                        }
                        return sb.toString();
                    } catch (Exception e) {
                        System.err.println("计算MD5失败: " + e.getMessage());
                    }
                }
            }
            
            // 其他函数调用可以在此处扩展
        }
        
        // 否则，假设这是响应的一部分（body, header等）
        return getResponsePart(response, operand);
    }

    /**
     * 获取响应的指定部分
     * @param response HTTP响应
     * @param part 部分名称（body, header, status等）
     * @return 响应内容
     */
    public static String getResponsePart(CustomHttpResponse response, String part) {
        if (response == null || part == null || part.isEmpty()) {
            return null;
        }

        try {
            switch (part.toLowerCase()) {
                case "body":
                    return response.getTextStr();
                case "header":
                    return response.getHeaderFieldsText();
                case "status":
                case "status_code":
                    return String.valueOf(response.getResponseCode());
                case "all":
                    return response.getAllResponseText();
                case "duration":
                    return String.valueOf(response.getResponseTime() / 1000.0); // 转换为秒
                case "content_type":
                    return response.getContentType();
                case "content_length":
                    return String.valueOf(response.getContentLength());
                default:
                    // 检查是否为带下标的请求部分，例如 body_1, header_2 等
                    if (part.contains("_") && part.split("_").length == 2) {
                        String basePart = part.split("_")[0];
                        // 这里返回基本响应部分，因为下标在实际使用时可能需要根据不同请求保存
                        return getResponsePart(response, basePart);
                    }
                    
                    // 尝试获取特定的响应头
                    Map<String, List<String>> fields = response.getHeaderFields();
                    if (fields != null) {
                        List<String> headerValues = fields.get(part);
                        if (headerValues != null && !headerValues.isEmpty()) {
                            return String.join("; ", headerValues);
                        }
                    }
                    return null;
            }
        } catch (Exception e) {
            System.err.println("获取响应部分失败: " + part + ", 错误: " + e.getMessage());
            return null;
        }
    }
} 