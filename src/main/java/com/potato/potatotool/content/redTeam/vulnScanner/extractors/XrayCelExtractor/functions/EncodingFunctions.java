package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 编码/解码函数库
 * 对应 Xray 官方编码函数
 * 
 * 支持的函数：
 * - base64(str) - Base64 编码
 * - base64Decode(str) - Base64 解码
 * - urlencode(str) - URL 编码
 * - urldecode(str) - URL 解码
 * - hexEncode(str) - 十六进制编码
 * - hexDecode(str) - 十六进制解码
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class EncodingFunctions {
    
    /**
     * Base64 编码
     * 
     * 示例:
     * - base64("test") -> "dGVzdA=="
     * - base64("admin:password") -> "YWRtaW46cGFzc3dvcmQ="
     * 
     * @param str 待编码字符串
     * @return Base64 编码后的字符串
     */
    public static String base64(String str) {
        if (str == null) {
            return "";
        }
        try {
            return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Base64 编码失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Base64 解码
     * 
     * 示例:
     * - base64Decode("dGVzdA==") -> "test"
     * - base64Decode("YWRtaW46cGFzc3dvcmQ=") -> "admin:password"
     * 
     * @param str Base64 编码的字符串
     * @return 解码后的字符串
     */
    public static String base64Decode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(str);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Base64 解码失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * URL 编码
     * 
     * 示例:
     * - urlencode("hello world") -> "hello+world"
     * - urlencode("test=1&key=2") -> "test%3D1%26key%3D2"
     * 
     * @param str 待编码字符串
     * @return URL 编码后的字符串
     */
    public static String urlencode(String str) {
        if (str == null) {
            return "";
        }
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            System.err.println("URL 编码失败: " + e.getMessage());
            return str;
        }
    }
    
    /**
     * URL 解码
     * 
     * 示例:
     * - urldecode("hello+world") -> "hello world"
     * - urldecode("test%3D1%26key%3D2") -> "test=1&key=2"
     * 
     * @param str URL 编码的字符串
     * @return 解码后的字符串
     */
    public static String urldecode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            System.err.println("URL 解码失败: " + e.getMessage());
            return str;
        }
    }
    
    /**
     * 十六进制编码
     * 
     * 示例:
     * - hexEncode("test") -> "74657374"
     * - hexEncode("admin") -> "61646d696e"
     * 
     * @param str 待编码字符串
     * @return 十六进制编码后的字符串
     */
    public static String hexEncode(String str) {
        if (str == null) {
            return "";
        }
        try {
            StringBuilder hex = new StringBuilder();
            for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            System.err.println("十六进制编码失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * 十六进制解码
     * 
     * 示例:
     * - hexDecode("74657374") -> "test"
     * - hexDecode("61646d696e") -> "admin"
     * 
     * @param str 十六进制编码的字符串
     * @return 解码后的字符串，如果输入无效则返回空字符串
     */
    public static String hexDecode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        // 检查长度是否为偶数
        if (str.length() % 2 != 0) {
            System.err.println("十六进制字符串长度必须为偶数");
            return "";
        }
        
        // 验证所有字符都是有效的十六进制字符
        if (!str.matches("[0-9a-fA-F]+")) {
            System.err.println("十六进制字符串包含非法字符: " + str);
            return "";
        }
        
        try {
            int len = str.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                int digit1 = Character.digit(str.charAt(i), 16);
                int digit2 = Character.digit(str.charAt(i+1), 16);
                if (digit1 == -1 || digit2 == -1) {
                    // 无效的十六进制字符
                    return "";
                }
                data[i / 2] = (byte) ((digit1 << 4) + digit2);
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("十六进制解码失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * HTML 实体编码
     * 转义特殊字符为 HTML 实体
     * 
     * 示例:
     * - htmlEscape("<script>") -> "&lt;script&gt;"
     * - htmlEscape("a & b") -> "a &amp; b"
     * 
     * @param str 待编码字符串
     * @return HTML 实体编码后的字符串
     */
    public static String htmlEscape(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                case '/':
                    sb.append("&#x2F;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * HTML 实体解码
     * 将 HTML 实体转回普通字符
     * 
     * 示例:
     * - htmlUnescape("&lt;script&gt;") -> "<script>"
     * - htmlUnescape("a &amp; b") -> "a & b"
     * 
     * @param str HTML 实体编码的字符串
     * @return 解码后的字符串
     */
    public static String htmlUnescape(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        return str.replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&amp;", "&")
                  .replace("&quot;", "\"")
                  .replace("&#x27;", "'")
                  .replace("&#x2F;", "/");
    }
}

