package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import com.potato.potatotool.content.redTeam.vulnScanner.util.RegexCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字节级操作函数库
 * 对应 Xray 官方字节级函数
 * 
 * 支持的函数：
 * - bytes(str) - 字符串转字节数组
 * - string(bytes) - 字节数组转字符串
 * - bcontains(data, pattern) - 字节级包含检查
 * - bmatches(data, regex) - 字节级正则匹配
 * - bsubmatch(data, regex) - 字节级正则提取
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class BytesFunctions {
    
    /**
     * 字符串转字节数组
     * 
     * 示例:
     * - bytes("hello") -> [104, 101, 108, 108, 111]
     * - bytes("测试") -> UTF-8 字节数组
     * 
     * @param str 字符串
     * @return 字节数组
     */
    public static byte[] bytes(String str) {
        if (str == null) {
            return new byte[0];
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * 字节数组转字符串
     * 
     * 示例:
     * - string([104, 101, 108, 108, 111]) -> "hello"
     * 
     * @param bytes 字节数组
     * @return 字符串
     */
    public static String string(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 字节级包含检查
     * 检查 data 中是否包含 pattern
     * 
     * 示例:
     * - bcontains(bytes("hello world"), bytes("world")) -> true
     * - bcontains(bytes("test"), bytes("abc")) -> false
     * 
     * @param data 待检查的数据
     * @param pattern 待查找的模式
     * @return 是否包含
     */
    public static boolean bcontains(byte[] data, byte[] pattern) {
        if (data == null || pattern == null) {
            return false;
        }
        
        if (pattern.length == 0) {
            return true;
        }
        
        if (data.length < pattern.length) {
            return false;
        }
        
        // KMP 算法优化的字节匹配
        for (int i = 0; i <= data.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 字节级包含检查（字符串重载）
     * 便捷方法，自动将字符串转为字节数组
     * 
     * @param data 待检查的数据字符串
     * @param pattern 待查找的模式字符串
     * @return 是否包含
     */
    public static boolean bcontains(String data, String pattern) {
        return bcontains(bytes(data), bytes(pattern));
    }
    
    /**
     * 字节级正则匹配
     * 将字节数组转为字符串后进行正则匹配
     * 注意：使用 find() 而非 matches()，即检查字符串中是否包含匹配的部分
     * 
     * 示例:
     * - bmatches(bytes("test123"), "\\w+\\d+") -> true
     * - bmatches(bytes("abc"), "\\d+") -> false
     * - bmatches(bytes("root:x:0:0:..."), "root:.*?:[0-9]*:") -> true
     * 
     * @param data 待匹配的数据
     * @param regex 正则表达式
     * @return 是否包含匹配的内容
     */
    public static boolean bmatches(byte[] data, String regex) {
        if (data == null || regex == null) {
            return false;
        }
        
        try {
            String str = string(data);
            // 使用 find() 检查是否包含匹配的部分，而不是 matches() 要求整个字符串匹配
            Pattern pattern = RegexCompat.compile(regex);
            return pattern.matcher(str).find();
        } catch (Exception e) {
            System.err.println("字节级正则匹配失败: " + regex + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 字节级正则匹配（字符串重载）
     * 
     * @param data 待匹配的数据字符串
     * @param regex 正则表达式
     * @return 是否匹配
     */
    public static boolean bmatches(String data, String regex) {
        return bmatches(bytes(data), regex);
    }
    
    /**
     * 字节级正则提取（捕获组）
     * 将字节数组转为字符串后提取正则捕获组
     * 
     * 示例:
     * - bsubmatch(bytes("token=abc123"), "token=(\\w+)") -> ["abc123"]
     * - bsubmatch(bytes("user:admin pass:123"), "user:(\\w+) pass:(\\w+)") -> ["admin", "123"]
     * 
     * @param data 待提取的数据
     * @param regex 正则表达式（包含捕获组）
     * @return 捕获组列表
     */
    public static List<String> bsubmatch(byte[] data, String regex) {
        List<String> result = new ArrayList<>();
        
        if (data == null || regex == null) {
            return result;
        }
        
        try {
            String str = string(data);
            Pattern pattern = RegexCompat.compile(regex);
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
            System.err.println("字节级正则提取失败: " + regex + " - " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 字节级正则提取（字符串重载）
     * 
     * @param data 待提取的数据字符串
     * @param regex 正则表达式（包含捕获组）
     * @return 捕获组列表
     */
    public static List<String> bsubmatch(String data, String regex) {
        return bsubmatch(bytes(data), regex);
    }
    
    /**
     * 字节数组拼接
     * 
     * 示例:
     * - bconcat(bytes("hello"), bytes(" world")) -> bytes("hello world")
     * 
     * @param arrays 待拼接的字节数组列表
     * @return 拼接后的字节数组
     */
    public static byte[] bconcat(byte[]... arrays) {
        if (arrays == null || arrays.length == 0) {
            return new byte[0];
        }
        
        // 计算总长度
        int totalLength = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                totalLength += array.length;
            }
        }
        
        // 拼接
        byte[] result = new byte[totalLength];
        int pos = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                System.arraycopy(array, 0, result, pos, array.length);
                pos += array.length;
            }
        }
        
        return result;
    }
    
    /**
     * 获取字节数组长度
     * 
     * @param data 字节数组
     * @return 长度
     */
    public static int blen(byte[] data) {
        return data != null ? data.length : 0;
    }
    
    /**
     * 字节数组切片
     * 
     * 示例:
     * - bslice(bytes("hello"), 0, 3) -> bytes("hel")
     * 
     * @param data 字节数组
     * @param start 起始位置
     * @param end 结束位置（不包含）
     * @return 切片结果
     */
    public static byte[] bslice(byte[] data, int start, int end) {
        if (data == null || start < 0 || end <= start) {
            return new byte[0];
        }
        
        start = Math.max(0, start);
        end = Math.min(data.length, end);
        
        if (start >= end) {
            return new byte[0];
        }
        
        byte[] result = new byte[end - start];
        System.arraycopy(data, start, result, 0, result.length);
        return result;
    }
}
