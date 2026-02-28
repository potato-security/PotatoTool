package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串操作函数库
 * 对应 Xray 官方字符串函数
 * 
 * 支持的函数：
 * - len(obj) - 获取长度
 * - substr(str, start, length) - 提取子字符串
 * - replaceAll(str, old, new) - 替换所有匹配
 * - toUpper(str) - 转大写
 * - toLower(str) - 转小写
 * - repeat(str, n) - 重复字符串
 * - reverse(str) - 反转字符串
 * - printable(str) - 转为可打印字符
 * - contains(str, substr) - 包含检查
 * - startsWith(str, prefix) - 前缀检查
 * - endsWith(str, suffix) - 后缀检查
 * - indexOf(str, substr) - 查找位置
 * - submatch(str, regex) - 正则提取
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class StringFunctions {
    
    /**
     * 获取长度
     * 支持字符串、列表、Map 等类型
     * 
     * 示例:
     * - len("hello") -> 5
     * - len([1,2,3]) -> 3
     * - len({a:1, b:2}) -> 2
     * 
     * @param obj 对象
     * @return 长度
     */
    public static int len(Object obj) {
        if (obj == null) {
            return 0;
        }
        
        if (obj instanceof String) {
            return ((String) obj).length();
        }
        
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).size();
        }
        
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).size();
        }
        
        if (obj instanceof Object[]) {
            return ((Object[]) obj).length;
        }
        
        // 默认返回字符串表示的长度
        return obj.toString().length();
    }
    
    /**
     * 提取子字符串
     * 
     * 示例:
     * - substr("hello", 0, 3) -> "hel"
     * - substr("world", 1, 3) -> "orl"
     * - substr("test", 2, 10) -> "st" (自动截断)
     * 
     * @param str 原字符串
     * @param start 起始位置（从0开始）
     * @param length 长度
     * @return 子字符串
     */
    public static String substr(String str, int start, int length) {
        if (str == null || start < 0 || length <= 0) {
            return "";
        }
        
        if (start >= str.length()) {
            return "";
        }
        
        int end = Math.min(start + length, str.length());
        return str.substring(start, end);
    }
    
    /**
     * 替换所有匹配
     * 
     * 示例:
     * - replaceAll("hello world", "o", "0") -> "hell0 w0rld"
     * - replaceAll("test test", "test", "TEST") -> "TEST TEST"
     * 
     * @param str 原字符串
     * @param oldStr 待替换字符串
     * @param newStr 新字符串
     * @return 替换后的字符串
     */
    public static String replaceAll(String str, String oldStr, String newStr) {
        if (str == null) {
            return "";
        }
        
        if (oldStr == null || oldStr.isEmpty()) {
            return str;
        }
        
        return str.replace(oldStr, newStr != null ? newStr : "");
    }
    
    /**
     * 转大写
     * 
     * 示例:
     * - toUpper("hello") -> "HELLO"
     * - toUpper("Test123") -> "TEST123"
     * 
     * @param str 原字符串
     * @return 大写字符串
     */
    public static String toUpper(String str) {
        return str != null ? str.toUpperCase() : "";
    }
    
    /**
     * 转小写
     * 
     * 示例:
     * - toLower("HELLO") -> "hello"
     * - toLower("Test123") -> "test123"
     * 
     * @param str 原字符串
     * @return 小写字符串
     */
    public static String toLower(String str) {
        return str != null ? str.toLowerCase() : "";
    }
    
    /**
     * 重复字符串
     * 
     * 示例:
     * - repeat("ab", 3) -> "ababab"
     * - repeat("x", 5) -> "xxxxx"
     * 
     * @param str 原字符串
     * @param times 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int times) {
        if (str == null || times <= 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(str.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 反转字符串
     * 
     * 示例:
     * - reverse("hello") -> "olleh"
     * - reverse("12345") -> "54321"
     * 
     * @param str 原字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        return new StringBuilder(str).reverse().toString();
    }
    
    /**
     * 转为可打印字符
     * 非打印字符转为 \xHH 格式
     * 
     * 示例:
     * - printable("hello") -> "hello"
     * - printable("test\n\r") -> "test\x0a\x0d"
     * 
     * @param str 原字符串
     * @return 可打印字符串
     */
    public static String printable(String str) {
        if (str == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            // 可打印ASCII字符范围: 32-126
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                // 转为 \xHH 格式
                sb.append(String.format("\\x%02x", (int) c));
            }
        }
        return sb.toString();
    }
    
    /**
     * 包含检查
     * 
     * 示例:
     * - contains("hello world", "world") -> true
     * - contains("test", "abc") -> false
     * 
     * @param str 原字符串
     * @param substr 子字符串
     * @return 是否包含
     */
    public static boolean contains(String str, String substr) {
        if (str == null || substr == null) {
            return false;
        }
        return str.contains(substr);
    }
    
    /**
     * 前缀检查
     * 
     * 示例:
     * - startsWith("hello world", "hello") -> true
     * - startsWith("test", "abc") -> false
     * 
     * @param str 原字符串
     * @param prefix 前缀
     * @return 是否以指定前缀开头
     */
    public static boolean startsWith(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        return str.startsWith(prefix);
    }
    
    /**
     * 后缀检查
     * 
     * 示例:
     * - endsWith("hello world", "world") -> true
     * - endsWith("test", "abc") -> false
     * 
     * @param str 原字符串
     * @param suffix 后缀
     * @return 是否以指定后缀结尾
     */
    public static boolean endsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        return str.endsWith(suffix);
    }
    
    /**
     * 查找位置
     * 
     * 示例:
     * - indexOf("hello world", "world") -> 6
     * - indexOf("test", "abc") -> -1
     * 
     * @param str 原字符串
     * @param substr 子字符串
     * @return 位置（从0开始），未找到返回-1
     */
    public static int indexOf(String str, String substr) {
        if (str == null || substr == null) {
            return -1;
        }
        return str.indexOf(substr);
    }
    
    /**
     * 正则提取（捕获组）
     * 提取正则表达式的所有捕获组
     * 
     * 示例:
     * - submatch("token=abc123", "token=(\\w+)") -> ["abc123"]
     * - submatch("user:admin pass:123", "user:(\\w+) pass:(\\w+)") -> ["admin", "123"]
     * - submatch("no match", "\\d+") -> []
     * 
     * @param str 原字符串
     * @param regex 正则表达式（包含捕获组）
     * @return 捕获组列表
     */
    public static List<String> submatch(String str, String regex) {
        List<String> result = new ArrayList<>();
        
        if (str == null || regex == null) {
            return result;
        }
        
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(str);
            
            if (matcher.find()) {
                // 提取所有捕获组（不包括组0，即完整匹配）
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String group = matcher.group(i);
                    if (group != null) {
                        result.add(group);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("正则提取失败: " + regex + " - " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 正则匹配检查
     * 使用 Matcher.find() 进行模式匹配（部分匹配），而不是完全匹配
     * 
     * 示例:
     * - matches("test123", "\\w+") -> true
     * - matches("abc", "\\d+") -> false
     * - matches("hello world", "world") -> true (部分匹配)
     * 
     * @param str 原字符串
     * @param regex 正则表达式
     * @return 是否匹配
     */
    public static boolean matches(String str, String regex) {
        if (str == null || regex == null) {
            return false;
        }
        
        try {
            // 使用 Matcher.find() 而不是 String.matches()
            // 这样可以实现部分匹配，与 Xray CEL 的语义一致
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(str);
            return matcher.find();
        } catch (Exception e) {
            System.err.println("正则匹配失败: " + regex + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 去除首尾空白
     * 
     * 示例:
     * - trim("  hello  ") -> "hello"
     * - trim("\n\ttest\r\n") -> "test"
     * 
     * @param str 原字符串
     * @return 去除空白后的字符串
     */
    public static String trim(String str) {
        return str != null ? str.trim() : "";
    }
    
    /**
     * 分割字符串
     * 
     * 示例:
     * - split("a,b,c", ",") -> ["a", "b", "c"]
     * - split("hello world", " ") -> ["hello", "world"]
     * 
     * @param str 原字符串
     * @param separator 分隔符
     * @return 分割后的列表
     */
    public static List<String> split(String str, String separator) {
        List<String> result = new ArrayList<>();
        
        if (str == null || separator == null) {
            return result;
        }
        
        String[] parts = str.split(Pattern.quote(separator));
        for (String part : parts) {
            result.add(part);
        }
        
        return result;
    }
}

