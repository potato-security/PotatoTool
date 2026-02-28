package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * 工具函数库
 * 对应 Xray 官方工具函数
 * 
 * 支持的函数：
 * - unixtime() - 获取当前Unix时间戳（秒）
 * - sleep(seconds) - 休眠指定秒数
 * - wait(reverse, timeout) - 等待反连（占位）
 * - string(value) - 转字符串
 * - int(value) - 转整数
 * - icontains(str, substr) - 不区分大小写的包含检查
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class UtilityFunctions {
    
    /**
     * 获取当前 Unix 时间戳（秒）
     * 
     * 示例:
     * - unixtime() -> 1730188800
     * 
     * @return 当前Unix时间戳（秒）
     */
    public static long unixtime() {
        return System.currentTimeMillis() / 1000;
    }
    
    /**
     * 获取当前 Unix 时间戳（毫秒）
     * 
     * 示例:
     * - unixtimeMilli() -> 1730188800000
     * 
     * @return 当前Unix时间戳（毫秒）
     */
    public static long unixtimeMilli() {
        return System.currentTimeMillis();
    }
    
    /**
     * 休眠指定秒数
     * 用于时间延迟检测
     * 
     * 示例:
     * - sleep(5) -> 休眠5秒
     * 
     * @param seconds 休眠秒数
     * @return 实际休眠时间（秒）
     */
    public static long sleep(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            Thread.sleep(seconds * 1000L);
            long endTime = System.currentTimeMillis();
            return (endTime - startTime) / 1000;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    /**
     * 等待反连
     * 
     * 示例:
     * - wait(reverse, 10) -> 等待 10 秒
     * - 推荐使用 reverse.wait(10) 语法
     * 
     * @param reverse 反连对象
     * @param timeout 超时时间（秒）
     * @return 是否收到反连
     */
    public static boolean wait(Object reverse, int timeout) {
        if (reverse == null) {
            System.err.println("wait() 函数需要有效的 Reverse 对象");
            return false;
        }
        
        // 如果是 ReverseObject 类型，调用其 waitFor 方法
        if (reverse.getClass().getSimpleName().equals("ReverseObject")) {
            try {
                java.lang.reflect.Method waitForMethod = reverse.getClass().getMethod("waitFor", int.class);
                Object result = waitForMethod.invoke(reverse, timeout);
                return result instanceof Boolean && (Boolean) result;
            } catch (Exception e) {
                System.err.println("调用 ReverseObject.waitFor() 失败: " + e.getMessage());
                return false;
            }
        }
        
        System.err.println("wait() 函数需要 ReverseObject 类型，当前类型: " + reverse.getClass().getName());
        return false;
    }
    
    /**
     * 转字符串
     * 将任意类型转为字符串
     * 
     * 示例:
     * - string(123) -> "123"
     * - string(true) -> "true"
     * 
     * @param value 待转换的值
     * @return 字符串表示
     */
    public static String string(Object value) {
        if (value == null) {
            return "";
        }
        // 处理浮点数：如果是整数值，去掉 .0 后缀
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
        }
        return value.toString();
    }
    
    /**
     * 转整数
     * 将字符串或数字转为整数
     * 
     * 示例:
     * - parseInt("123") -> 123
     * - parseInt(45.6) -> 45
     * 
     * @param value 待转换的值
     * @return 整数值
     */
    public static int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            System.err.println("转整数失败: " + value);
            return 0;
        }
    }
    
    /**
     * 不区分大小写的包含检查
     * 
     * 示例:
     * - icontains("Hello World", "WORLD") -> true
     * - icontains("test", "EST") -> true
     * 
     * @param str 原字符串
     * @param substr 子字符串
     * @return 是否包含（不区分大小写）
     */
    public static boolean icontains(String str, String substr) {
        if (str == null || substr == null) {
            return false;
        }
        return str.toLowerCase().contains(substr.toLowerCase());
    }
    
    /**
     * 不区分大小写的前缀检查
     * 
     * @param str 原字符串
     * @param prefix 前缀
     * @return 是否以指定前缀开头（不区分大小写）
     */
    public static boolean istartsWith(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        return str.toLowerCase().startsWith(prefix.toLowerCase());
    }
    
    /**
     * 不区分大小写的后缀检查
     * 
     * @param str 原字符串
     * @param suffix 后缀
     * @return 是否以指定后缀结尾（不区分大小写）
     */
    public static boolean iendsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        return str.toLowerCase().endsWith(suffix.toLowerCase());
    }
    
    /**
     * 域名解析
     * 将域名解析为 IP 地址
     * 
     * 示例:
     * - resolve("www.example.com") -> "93.184.216.34"
     * 
     * @param domain 域名
     * @return IP 地址（失败返回空字符串）
     */
    public static String resolve(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return "";
        }
        
        try {
            InetAddress address = InetAddress.getByName(domain);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("域名解析失败: " + domain);
            return "";
        }
    }
    
    /**
     * 反向 DNS 查询
     * 将 IP 地址解析为域名
     * 
     * 示例:
     * - reverseLookup("8.8.8.8") -> "dns.google"
     * 
     * @param ip IP 地址
     * @return 域名（失败返回空字符串）
     */
    public static String reverseLookup(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return "";
        }
        
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.getHostName();
        } catch (UnknownHostException e) {
            System.err.println("反向DNS查询失败: " + ip);
            return "";
        }
    }
    
    /**
     * 生成随机 UUID
     * 
     * 示例:
     * - uuid() -> "550e8400-e29b-41d4-a716-446655440000"
     * 
     * @return UUID 字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 判断字符串是否为空
     * 
     * @param str 字符串
     * @return 是否为空（null 或空字符串）
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 判断字符串是否非空
     * 
     * @param str 字符串
     * @return 是否非空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}

