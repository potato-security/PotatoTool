package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Xray CEL 随机值生成函数
 * 
 * 支持的函数：
 * - randomInt(min, max): 生成随机整数
 * - randomLowercase(length): 生成随机小写字符串
 * - randomUppercase(length): 生成随机大写字符串
 * - rand_text_alpha(length): 生成随机字母字符串
 * - rand_text_numeric(length): 生成随机数字字符串
 * - rand_text_alphanumeric(length): 生成随机字母数字字符串
 * - rand_int(min, max): randomInt 的别名
 * - rand_char(length): 生成随机可见字符
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class RandomFunctions {
    
    // 字符集常量
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMERIC_CHARS = "0123456789";
    private static final String ALPHA_CHARS = LOWERCASE_CHARS + UPPERCASE_CHARS;
    private static final String ALPHANUMERIC_CHARS = ALPHA_CHARS + NUMERIC_CHARS;
    private static final String VISIBLE_CHARS = ALPHANUMERIC_CHARS + "!@#$%^&*()_+-=[]{}|;:,.<>?";
    
    // 使用 ThreadLocalRandom 提高性能
    private static final Random RANDOM = new SecureRandom();
    
    /**
     * 生成随机整数（闭区间）
     * 
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 随机整数
     * 
     * 示例：
     * randomInt(1, 10) -> 返回 1 到 10 之间的随机数
     * randomInt(5, 8) -> 返回 5, 6, 7, 或 8
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        
        if (min == max) {
            return min;
        }
        
        // 使用 ThreadLocalRandom 生成范围内的随机数
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    /**
     * 生成随机小写字符串
     * 
     * @param length 字符串长度
     * @return 随机小写字符串
     * 
     * 示例：
     * randomLowercase(8) -> "abcdefgh" (随机)
     */
    public static String randomLowercase(int length) {
        return randomString(LOWERCASE_CHARS, length);
    }
    
    /**
     * 生成随机大写字符串
     * 
     * @param length 字符串长度
     * @return 随机大写字符串
     * 
     * 示例：
     * randomUppercase(8) -> "ABCDEFGH" (随机)
     */
    public static String randomUppercase(int length) {
        return randomString(UPPERCASE_CHARS, length);
    }
    
    /**
     * 生成随机字母字符串（大小写混合）
     * 
     * @param length 字符串长度
     * @return 随机字母字符串
     * 
     * 示例：
     * rand_text_alpha(10) -> "aBcDeFgHij" (随机)
     */
    public static String rand_text_alpha(int length) {
        return randomString(ALPHA_CHARS, length);
    }
    
    /**
     * 生成随机数字字符串
     * 
     * @param length 字符串长度
     * @return 随机数字字符串
     * 
     * 示例：
     * rand_text_numeric(6) -> "123456" (随机)
     */
    public static String rand_text_numeric(int length) {
        return randomString(NUMERIC_CHARS, length);
    }
    
    /**
     * 生成随机字母数字字符串
     * 
     * @param length 字符串长度
     * @return 随机字母数字字符串
     * 
     * 示例：
     * rand_text_alphanumeric(12) -> "a1B2c3D4e5F6" (随机)
     */
    public static String rand_text_alphanumeric(int length) {
        return randomString(ALPHANUMERIC_CHARS, length);
    }
    
    /**
     * 生成随机整数（别名）
     * 
     * @param min 最小值
     * @param max 最大值
     * @return 随机整数
     */
    public static int rand_int(int min, int max) {
        return randomInt(min, max);
    }
    
    /**
     * 生成随机可见字符字符串
     * 包括字母、数字和特殊符号
     * 
     * @param length 字符串长度
     * @return 随机字符串
     * 
     * 示例：
     * rand_char(10) -> "aB!3$xY#9@" (随机)
     */
    public static String rand_char(int length) {
        return randomString(VISIBLE_CHARS, length);
    }
    
    /**
     * 生成随机十六进制字符串
     * 
     * @param length 字符串长度
     * @return 随机十六进制字符串
     * 
     * 示例：
     * rand_text_hex(8) -> "a1b2c3d4" (随机)
     */
    public static String rand_text_hex(int length) {
        return randomString("0123456789abcdef", length);
    }
    
    /**
     * 生成随机 Base64 字符串（不含填充符）
     * 
     * @param length 字符串长度
     * @return 随机 Base64 字符串
     */
    public static String rand_base64(int length) {
        return randomString(ALPHANUMERIC_CHARS + "+/", length);
    }
    
    /**
     * 核心随机字符串生成方法
     * 
     * @param charset 字符集
     * @param length 字符串长度
     * @return 随机字符串
     */
    private static String randomString(String charset, int length) {
        if (length <= 0) {
            return "";
        }
        
        if (charset == null || charset.isEmpty()) {
            charset = ALPHANUMERIC_CHARS;
        }
        
        StringBuilder result = new StringBuilder(length);
        int charsetLength = charset.length();
        
        for (int i = 0; i < length; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(charsetLength);
            result.append(charset.charAt(randomIndex));
        }
        
        return result.toString();
    }
    
    /**
     * 生成随机 UUID（标准格式，含连字符）
     * 
     * @return 随机 UUID 字符串
     * 
     * 示例：
     * randomUUID() -> "a1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890"
     * 
     * 注意：与Xray官方行为一致，UUID包含连字符
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成随机 UUID（不含连字符）
     * 
     * @return 随机 UUID 字符串
     * 
     * 示例：
     * randomUUIDNoDash() -> "a1b2c3d4e5f67890a1b2c3d4e5f67890"
     */
    public static String randomUUIDNoDash() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成随机字节数组
     * 
     * @param length 字节数组长度
     * @return 随机字节数组
     */
    public static byte[] randomBytes(int length) {
        if (length <= 0) {
            return new byte[0];
        }
        
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
    
    /**
     * 生成随机布尔值
     * 
     * @return 随机布尔值
     */
    public static boolean randomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }
    
    /**
     * 生成随机浮点数
     * 
     * @param min 最小值
     * @param max 最大值
     * @return 随机浮点数
     */
    public static double randomDouble(double min, double max) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }
}

