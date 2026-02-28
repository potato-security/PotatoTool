package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * DSL 扩展函数集
 * 包含所有高级 DSL 函数的实现
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class DslFunctionExtended {

    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();

    // ==================== 字符串函数 ====================

    /**
     * replace - 字符串替换
     */
    public static String replace(String str, String old, String newStr) {
        if (str == null || old == null || newStr == null) {
            return str;
        }
        return str.replace(old, newStr);
    }

    /**
     * replace_regex - 正则替换
     */
    public static String replaceRegex(String str, String pattern, String replacement) {
        if (str == null || pattern == null || replacement == null) {
            return str;
        }
        return str.replaceAll(pattern, replacement);
    }

    /**
     * concat - 字符串拼接
     */
    public static String concat(String... strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s != null) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
     * reverse - 字符串反转
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * repeat - 字符串重复
     */
    public static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * split - 字符串分割（返回数组的JSON字符串）
     */
    public static String split(String str, String separator) {
        if (str == null || separator == null) {
            return "[]";
        }
        String[] parts = str.split(Pattern.quote(separator));
        return GSON.toJson(parts);
    }

    /**
     * join - 数组连接
     */
    public static String join(String separator, String... items) {
        if (items == null || items.length == 0) {
            return "";
        }
        return String.join(separator, items);
    }

    /**
     * trim_left - 去除左侧空白
     */
    public static String trimLeft(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("^\\s+", "");
    }

    /**
     * trim_right - 去除右侧空白
     */
    public static String trimRight(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\s+$", "");
    }

    /**
     * trim_prefix - 去除前缀
     */
    public static String trimPrefix(String str, String prefix) {
        if (str == null || prefix == null) {
            return str;
        }
        if (str.startsWith(prefix)) {
            return str.substring(prefix.length());
        }
        return str;
    }

    /**
     * trim_suffix - 去除后缀
     */
    public static String trimSuffix(String str, String suffix) {
        if (str == null || suffix == null) {
            return str;
        }
        if (str.endsWith(suffix)) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }

    /**
     * to_title - 标题格式（首字母大写）
     */
    public static String toTitle(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ==================== 随机函数 ====================

    /**
     * rand_text_alpha - 随机字母字符串
     */
    public static String randTextAlpha(int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return randBase(length, alphabet);
    }

    /**
     * rand_text_alphanumeric - 随机字母数字字符串
     */
    public static String randTextAlphanumeric(int length) {
        String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return randBase(length, charset);
    }

    /**
     * rand_text_numeric - 随机数字字符串
     */
    public static String randTextNumeric(int length) {
        String charset = "0123456789";
        return randBase(length, charset);
    }

    /**
     * rand_int - 随机整数
     */
    public static int randInt(int min, int max) {
        return RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * rand_base - 随机字符串（指定字符集）
     */
    public static String randBase(int length, String alphabet) {
        if (length <= 0 || alphabet == null || alphabet.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(alphabet.length());
            sb.append(alphabet.charAt(index));
        }
        return sb.toString();
    }

    /**
     * rand_char - 随机字符
     */
    public static String randChar(String charset) {
        return randBase(1, charset != null ? charset : "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    // ==================== 时间函数 ====================

    /**
     * to_unix_time - 转换为 Unix 时间戳
     * 支持两种输入：
     * 1. 格式化日期字符串（如 "2025-11-18 10:30:45"）
     * 2. Unix 时间戳字符串（如 "1763393518"）- 直接返回
     */
    public static long toUnixTime(String datetime) {
        if (datetime == null || datetime.trim().isEmpty()) {
            return System.currentTimeMillis() / 1000;
        }

        datetime = datetime.trim();

        // 检测是否为纯数字（Unix 时间戳）
        if (datetime.matches("^\\d+$")) {
            try {
                return Long.parseLong(datetime);
            } catch (NumberFormatException e) {
                System.err.println("Unix 时间戳解析失败: " + e.getMessage());
                return System.currentTimeMillis() / 1000;
            }
        }

        // 尝试多种日期格式解析
        String[] dateFormats = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm:ss"
        };

        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                Date date = sdf.parse(datetime);
                return date.getTime() / 1000;
            } catch (Exception ignored) {
                // 尝试下一个格式
            }
        }

        // 所有格式都失败，返回当前时间
        System.err.println("时间转换失败，无法识别格式: " + datetime);
        return System.currentTimeMillis() / 1000;
    }

    /**
     * date_time - 格式化日期时间
     */
    public static String dateTime(String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.format(new Date());
        } catch (Exception e) {
            return new Date().toString();
        }
    }

    /**
     * now - 当前时间（Unix 时间戳）
     */
    public static long now() {
        return System.currentTimeMillis() / 1000;
    }

    // ==================== JSON 函数 ====================

    /**
     * json_minify - JSON 压缩
     */
    public static String jsonMinify(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return GSON.toJson(element);
        } catch (Exception e) {
            System.err.println("JSON 压缩失败: " + e.getMessage());
            return json;
        }
    }

    /**
     * json_prettify - JSON 格式化
     */
    public static String jsonPrettify(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return GSON_PRETTY.toJson(element);
        } catch (Exception e) {
            System.err.println("JSON 格式化失败: " + e.getMessage());
            return json;
        }
    }

    /**
     * json_encode - JSON 编码
     */
    public static String jsonEncode(Object obj) {
        try {
            return GSON.toJson(obj);
        } catch (Exception e) {
            System.err.println("JSON 编码失败: " + e.getMessage());
            return null;
        }
    }

    // ==================== 加密函数 ====================

    /**
     * hmac_sha1 - HMAC-SHA1
     */
    public static String hmacSha1(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            System.err.println("HMAC-SHA1 计算失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * hmac_sha256 - HMAC-SHA256
     * @param data 待签名数据
     * @param key 签名密钥（不能为空）
     * @return HMAC-SHA256 十六进制字符串，失败返回 null
     */
    public static String hmacSha256(String data, String key) {
        // 参数验证
        if (data == null) {
            System.err.println("HMAC-SHA256 计算失败: 数据不能为 null");
            return null;
        }

        if (key == null || key.isEmpty()) {
            System.err.println("HMAC-SHA256 计算失败: 密钥不能为空（请检查 POC 是否正确配置签名密钥）");
            return null;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            System.err.println("HMAC-SHA256 计算失败: " + e.getMessage());
            System.err.println("  数据长度: " + data.length() + ", 密钥长度: " + key.length());
            return null;
        }
    }

    /**
     * hmac_sha512 - HMAC-SHA512
     * @param data 待签名数据
     * @param key 签名密钥
     * @return HMAC-SHA512 十六进制字符串
     */
    public static String hmacSha512(String data, String key) {
        if (data == null || key == null || key.isEmpty()) {
            System.err.println("HMAC-SHA512 计算失败: 参数不能为空");
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA512");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            System.err.println("HMAC-SHA512 计算失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * hmac_md5 - HMAC-MD5
     * @param data 待签名数据
     * @param key 签名密钥
     * @return HMAC-MD5 十六进制字符串
     */
    public static String hmacMd5(String data, String key) {
        if (data == null || key == null || key.isEmpty()) {
            System.err.println("HMAC-MD5 计算失败: 参数不能为空");
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacMD5");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            System.err.println("HMAC-MD5 计算失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * aes_gcm - AES-GCM 加密
     */
    public static String aesGcm(String plaintext, String key) {
        try {
            // 生成随机 IV
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            
            // 确保密钥长度为 16/24/32 字节
            byte[] keyBytes = padKey(key.getBytes("UTF-8"), 16);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            // 返回 IV + 密文的 Base64
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            System.err.println("AES-GCM 加密失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * aes_cbc - AES-CBC 加密
     */
    public static String aesCbc(String plaintext, String key, String ivStr) {
        try {
            byte[] keyBytes = padKey(key.getBytes("UTF-8"), 16);
            byte[] ivBytes = padKey(ivStr.getBytes("UTF-8"), 16);
            
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            System.err.println("AES-CBC 加密失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * generate_jwt - 生成 JWT（简化版）
     */
    public static String generateJwt(String payload, String secret) {
        try {
            // Header
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String headerB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.getBytes("UTF-8"));
            
            // Payload
            String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes("UTF-8"));
            
            // Signature
            String signInput = headerB64 + "." + payloadB64;
            String signature = hmacSha256(signInput, secret);
            
            return headerB64 + "." + payloadB64 + "." + signature;
        } catch (Exception e) {
            System.err.println("JWT 生成失败: " + e.getMessage());
            return null;
        }
    }

    // ==================== 工具函数 ====================

    /**
     * 填充密钥到指定长度
     */
    private static byte[] padKey(byte[] key, int length) {
        byte[] paddedKey = new byte[length];
        System.arraycopy(key, 0, paddedKey, 0, Math.min(key.length, length));
        return paddedKey;
    }

    /**
     * 字节数组转十六进制
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * to_number - 转换为数字
     */
    public static double toNumber(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * compare_versions - 版本比较
     * 返回: -1 (v1 < v2), 0 (v1 == v2), 1 (v1 > v2)
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0;
    }

    /**
     * line_starts_with - 检查是否有行以指定前缀开始
     */
    public static boolean lineStartsWith(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        String[] lines = str.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * line_ends_with - 检查是否有行以指定后缀结束
     */
    public static boolean lineEndsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        String[] lines = str.split("\\r?\\n");
        for (String line : lines) {
            if (line.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * print_debug - 调试输出
     */
    public static String printDebug(String message) {
        System.out.println("[DSL Debug] " + message);
        return message;
    }

    // ==================== 高级编码函数 ====================

    /**
     * unicode_encode - Unicode 编码
     */
    public static String unicodeEncode(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append(String.format("\\u%04x", (int) c));
        }
        return sb.toString();
    }

    /**
     * unicode_decode - Unicode 解码
     */
    public static String unicodeDecode(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == '\\' && i + 1 < str.length() && str.charAt(i + 1) == 'u') {
                String hex = str.substring(i + 2, Math.min(i + 6, str.length()));
                try {
                    int code = Integer.parseInt(hex, 16);
                    sb.append((char) code);
                    i += 6;
                } catch (Exception e) {
                    sb.append(str.charAt(i));
                    i++;
                }
            } else {
                sb.append(str.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * oct_to_dec - 八进制转十进制
     */
    public static String octToDec(String oct) {
        try {
            String octal = oct.replace("0o", "").replace("0O", "");
            int decimal = Integer.parseInt(octal, 8);
            return String.valueOf(decimal);
        } catch (Exception e) {
            return "0";
        }
    }

    /**
     * dec_to_oct - 十进制转八进制
     */
    public static String decToOct(String dec) {
        try {
            int decimal = Integer.parseInt(dec);
            return Integer.toOctalString(decimal);
        } catch (Exception e) {
            return "0";
        }
    }

    /**
     * hex_to_dec - 十六进制转十进制
     */
    public static String hexToDec(String hex) {
        try {
            String hexStr = hex.replace("0x", "").replace("0X", "");
            int decimal = Integer.parseInt(hexStr, 16);
            return String.valueOf(decimal);
        } catch (Exception e) {
            return "0";
        }
    }

    /**
     * dec_to_bin - 十进制转二进制
     */
    public static String decToBin(String dec) {
        try {
            int decimal = Integer.parseInt(dec);
            return Integer.toBinaryString(decimal);
        } catch (Exception e) {
            return "0";
        }
    }

    // ==================== 网络函数 ====================

    /**
     * resolve - DNS 解析（简化实现）
     */
    public static String resolve(String domain) {
        try {
            InetAddress address = InetAddress.getByName(domain);
            return address.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ip - 获取 IP 地址（占位实现）
     */
    public static String ip() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * host - 获取主机名（占位实现）
     */
    public static String host() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

    // ==================== 高级工具函数 ====================

    /**
     * zlib_compress - Zlib 压缩
     */
    public static String zlibCompress(String data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DeflaterOutputStream dos = new DeflaterOutputStream(baos);
            dos.write(data.getBytes("UTF-8"));
            dos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("Zlib 压缩失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * zlib_decompress - Zlib 解压
     */
    public static String zlibDecompress(String data) {
        try {
            byte[] compressed = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            InflaterInputStream iis = new InflaterInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = iis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toString("UTF-8");
        } catch (Exception e) {
            System.err.println("Zlib 解压失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * substring_after - 获取分隔符后的子串
     */
    public static String substringAfter(String str, String separator) {
        if (str == null || separator == null) {
            return str;
        }
        int index = str.indexOf(separator);
        if (index == -1) {
            return "";
        }
        return str.substring(index + separator.length());
    }

    /**
     * substring_before - 获取分隔符前的子串
     */
    public static String substringBefore(String str, String separator) {
        if (str == null || separator == null) {
            return str;
        }
        int index = str.indexOf(separator);
        if (index == -1) {
            return str;
        }
        return str.substring(0, index);
    }

    /**
     * wait_for - 等待指定时间（秒）
     */
    public static boolean waitFor(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * generate_java_gadget - 生成 Java 反序列化 payload（占位实现）
     */
    public static String generateJavaGadget(String gadgetType) {
        // 占位实现，实际应用中需要集成 ysoserial
        System.out.println("生成 Java Gadget: " + gadgetType);
        return "JAVA_GADGET_PLACEHOLDER";
    }

    /**
     * des - DES 加密
     */
    public static String des(String data, String key) {
        try {
            byte[] keyBytes = padKey(key.getBytes("UTF-8"), 8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
            
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            System.err.println("DES 加密失败: " + e.getMessage());
            return null;
        }
    }

}



