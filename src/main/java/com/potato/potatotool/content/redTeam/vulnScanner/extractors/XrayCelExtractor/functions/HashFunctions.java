package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 哈希函数库
 * 对应 Xray 官方哈希函数
 * 
 * 支持的函数：
 * - md5(str) - MD5 哈希
 * - sha1(str) - SHA1 哈希
 * - sha256(str) - SHA256 哈希
 * - sha512(str) - SHA512 哈希
 * - mmh3(str) - MurmurHash3 32位
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class HashFunctions {
    
    /**
     * MD5 哈希
     * 
     * 示例:
     * - md5("test") -> "098f6bcd4621d373cade4e832627b4f6"
     * - md5("admin") -> "21232f297a57a5a743894a0e4a801fc3"
     * 
     * @param str 待哈希字符串
     * @return MD5 哈希值（32位十六进制字符串）
     */
    public static String md5(String str) {
        return hash(str, "MD5");
    }
    
    /**
     * SHA1 哈希
     * 
     * 示例:
     * - sha1("test") -> "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
     * - sha1("admin") -> "d033e22ae348aeb5660fc2140aec35850c4da997"
     * 
     * @param str 待哈希字符串
     * @return SHA1 哈希值（40位十六进制字符串）
     */
    public static String sha1(String str) {
        return hash(str, "SHA-1");
    }
    
    /**
     * SHA256 哈希
     * 
     * 示例:
     * - sha256("test") -> "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
     * 
     * @param str 待哈希字符串
     * @return SHA256 哈希值（64位十六进制字符串）
     */
    public static String sha256(String str) {
        return hash(str, "SHA-256");
    }
    
    /**
     * SHA512 哈希
     * 
     * 示例:
     * - sha512("test") -> "ee26b0dd4af7e749aa1a8ee3c10ae9923f618980..."
     * 
     * @param str 待哈希字符串
     * @return SHA512 哈希值（128位十六进制字符串）
     */
    public static String sha512(String str) {
        return hash(str, "SHA-512");
    }
    
    /**
     * 通用哈希函数
     * 
     * @param str 待哈希字符串
     * @param algorithm 哈希算法名称
     * @return 哈希值（十六进制字符串）
     */
    private static String hash(String str, String algorithm) {
        if (str == null) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println(algorithm + " 哈希失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * MurmurHash3 32位哈希
     * 用于 Favicon 哈希计算等场景
     * 
     * 示例:
     * - mmh3("test") -> "3127628307"
     * - mmh3("admin") -> "1819897183"
     * 
     * @param str 待哈希字符串
     * @return MurmurHash3 哈希值（整数字符串）
     */
    public static String mmh3(String str) {
        if (str == null) {
            return "";
        }
        
        try {
            int hash = murmur3_32(str.getBytes(StandardCharsets.UTF_8), 0);
            return String.valueOf(hash);
        } catch (Exception e) {
            System.err.println("MurmurHash3 哈希失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * MurmurHash3 32位实现
     * 参考 Xray/Fofa 的实现
     * 
     * @param data 待哈希数据
     * @param seed 种子值
     * @return 32位哈希值
     */
    private static int murmur3_32(byte[] data, int seed) {
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;
        final int r1 = 15;
        final int r2 = 13;
        final int m = 5;
        final int n = 0xe6546b64;
        
        int hash = seed;
        int len = data.length;
        int rounded = (len >>> 2) << 2;
        
        // 处理完整的4字节块
        for (int i = 0; i < rounded; i += 4) {
            int k = (data[i] & 0xff) | 
                    ((data[i + 1] & 0xff) << 8) | 
                    ((data[i + 2] & 0xff) << 16) | 
                    (data[i + 3] << 24);
            
            k *= c1;
            k = (k << r1) | (k >>> (32 - r1));
            k *= c2;
            
            hash ^= k;
            hash = (hash << r2) | (hash >>> (32 - r2));
            hash = hash * m + n;
        }
        
        // 处理剩余字节
        int k = 0;
        int val = len & 0x03;
        
        if (val == 3) {
            k = (data[rounded + 2] & 0xff) << 16;
        }
        if (val >= 2) {
            k |= (data[rounded + 1] & 0xff) << 8;
        }
        if (val >= 1) {
            k |= data[rounded] & 0xff;
            k *= c1;
            k = (k << r1) | (k >>> (32 - r1));
            k *= c2;
            hash ^= k;
        }
        
        // 最终混合
        hash ^= len;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        
        return hash;
    }
    
    /**
     * Base64 编码后的 MurmurHash3
     * 用于 Fofa Favicon 哈希
     * 
     * @param base64Data Base64 编码的数据
     * @return MurmurHash3 哈希值
     */
    public static String mmh3Base64(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            return "";
        }
        
        try {
            byte[] data = Base64.getDecoder().decode(base64Data);
            int hash = murmur3_32(data, 0);
            return String.valueOf(hash);
        } catch (Exception e) {
            System.err.println("MurmurHash3 Base64 哈希失败: " + e.getMessage());
            return "";
        }
    }
}

