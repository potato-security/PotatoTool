package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Goby POC 特殊函数处理器
 * 处理 Goby POC 中的 @@ 前缀特殊函数
 * 
 * 支持的函数：
 * - @@random(length) - 生成随机字符串
 * - @@base64(str) - Base64编码
 * - @@md5(str) - MD5哈希
 * - @@sha1(str) - SHA1哈希
 * - @@sha256(str) - SHA256哈希
 * - @@sha512(str) - SHA512哈希
 * - @@dnslog() - 生成DNSLog域名（集成dnslog.cn服务）
 * - @@httplog() - 生成HTTP反连URL（集成interactsh等服务）
 * - @@reverse_shell(ip, port, type) - 生成反弹Shell Payload
 * - @@urlencode(str) - URL编码
 * - @@urldecode(str) - URL解码
 * - @@timestamp() - 当前Unix时间戳（毫秒）
 * - @@date(format) - 格式化当前日期时间
 * - @@uuid() - 生成UUID
 * - @@file(path) - 读取文件内容
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class GobyFunctionProcessor {
    
    private static final Random RANDOM = new Random();
    
    // 文件读取的根目录限制（安全考虑）
    private static final String FILE_BASE_DIR = System.getProperty("user.dir") + "/payload_files";
    
    // 函数调用模式：@@functionName(args)
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("@@(\\w+)\\(([^)]*)\\)");
    
    /**
     * 处理字符串中的所有 @@ 函数调用
     * 
     * @param input 输入字符串
     * @param variables 变量映射（用于解析函数参数中的变量引用）
     * @return 处理后的字符串
     */
    public static String processGobyFunctions(String input, Map<String, String> variables) {
        if (input == null || !input.contains("@@")) {
            return input;
        }
        
        String result = input;
        Matcher matcher = FUNCTION_PATTERN.matcher(result);
        
        // 替换所有函数调用
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String functionName = matcher.group(1).toLowerCase();
            String args = matcher.group(2).trim();
            
            try {
                String replacement = executeFunction(functionName, args, variables);
                if (replacement != null) {
                    // 转义特殊字符以避免替换问题
                    replacement = Matcher.quoteReplacement(replacement);
                    matcher.appendReplacement(sb, replacement);
                } else {
                    // 如果函数执行失败，保留原始文本
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            } catch (Exception e) {
                System.err.println("执行Goby函数失败: @@" + functionName + "(" + args + ")");
                System.err.println("错误: " + e.getMessage());
                // 保留原始文本
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 执行指定的函数
     * 
     * @param functionName 函数名
     * @param args 函数参数
     * @param variables 变量映射
     * @return 函数执行结果
     */
    private static String executeFunction(String functionName, String args, Map<String, String> variables) {
        switch (functionName) {
            case "random":
                return executeRandom(args);
            case "base64":
                return executeBase64(args, variables);
            case "md5":
                return executeMd5(args, variables);
            case "sha1":
                return executeSha1(args, variables);
            case "sha256":
                return executeSha256(args, variables);
            case "sha512":
                return executeSha512(args, variables);
            case "dnslog":
                return executeDnslog();
            case "httplog":
                return executeHttplog();
            case "reverse_shell":
            case "reverseshell":
                return executeReverseShell(args, variables);
            case "urlencode":
                return executeUrlEncode(args, variables);
            case "urldecode":
                return executeUrlDecode(args, variables);
            case "timestamp":
                return executeTimestamp();
            case "date":
                return executeDate(args, variables);
            case "uuid":
                return executeUuid();
            case "file":
                return executeFile(args, variables);
            default:
                System.err.println("未知的Goby函数: @@" + functionName);
                return null;
        }
    }
    
    /**
     * 生成随机字符串
     * @@random(4) -> 生成4位随机字母数字字符串
     */
    private static String executeRandom(String args) {
        try {
            int length = Integer.parseInt(args.trim());
            if (length <= 0 || length > 1000) {
                System.err.println("随机字符串长度必须在1-1000之间: " + length);
                return null;
            }
            
            String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder result = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                result.append(chars.charAt(RANDOM.nextInt(chars.length())));
            }
            return result.toString();
            
        } catch (NumberFormatException e) {
            System.err.println("随机字符串长度必须是数字: " + args);
            return null;
        }
    }
    
    /**
     * Base64编码
     * @@base64(password) -> cGFzc3dvcmQ=
     */
    private static String executeBase64(String args, Map<String, String> variables) {
        String value = resolveArgument(args, variables);
        if (value == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * MD5哈希
     * @@md5(password) -> 5f4dcc3b5aa765d61d8327deb882cf99
     */
    private static String executeMd5(String args, Map<String, String> variables) {
        String value = resolveArgument(args, variables);
        if (value == null) {
            return null;
        }
        return calculateHash(value, "MD5");
    }
    
    /**
     * SHA1哈希
     * @@sha1(password) -> 5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8
     */
    private static String executeSha1(String args, Map<String, String> variables) {
        String value = resolveArgument(args, variables);
        if (value == null) {
            return null;
        }
        return calculateHash(value, "SHA-1");
    }
    
    /**
     * SHA256哈希
     */
    private static String executeSha256(String args, Map<String, String> variables) {
        String value = resolveArgument(args, variables);
        if (value == null) {
            return null;
        }
        return calculateHash(value, "SHA-256");
    }
    
    /**
     * SHA512哈希
     */
    private static String executeSha512(String args, Map<String, String> variables) {
        String value = resolveArgument(args, variables);
        if (value == null) {
            return null;
        }
        return calculateHash(value, "SHA-512");
    }
    
    /**
     * DNSLog功能
     * 生成并返回一个唯一的DNSLog域名，用于DNS外带检测
     */
    private static String executeDnslog() {
        // 调用DNSLog服务生成唯一域名
        String domain = DnsLogService.generateDnsLogDomain();
        if (DnsLogService.isFallbackDomain(domain)) {
            System.err.println("dnslog函数返回fallback域名，DNS OOB当前不可用: " + domain);
        }
        return domain;
    }
    
    /**
     * HTTPLog功能
     * 生成并返回一个唯一的HTTP反连URL，用于HTTP外带检测
     */
    private static String executeHttplog() {
        // 调用HTTPLog服务生成唯一URL
        return HttpLogService.generateHttpLogUrl();
    }
    
    /**
     * 反弹Shell生成
     * @@reverse_shell(ip, port) -> bash反弹Shell
     * @@reverse_shell(ip, port, type) -> 指定类型的反弹Shell
     * 
     * 支持的类型：bash, python, nc, powershell, php, perl, ruby, java
     */
    private static String executeReverseShell(String args, Map<String, String> variables) {
        if (args == null || args.isEmpty()) {
            System.err.println("reverse_shell函数需要参数: ip, port [, type]");
            return null;
        }
        
        // 解析参数
        String[] params = args.split(",");
        if (params.length < 2) {
            System.err.println("reverse_shell函数至少需要2个参数: ip, port");
            return null;
        }
        
        try {
            String ip = resolveArgument(params[0].trim(), variables);
            int port = Integer.parseInt(resolveArgument(params[1].trim(), variables));
            
            // 默认类型是bash
            ReverseShellGenerator.ShellType type = ReverseShellGenerator.ShellType.BASH;
            
            // 如果提供了第三个参数（Shell类型）
            if (params.length >= 3) {
                String typeStr = resolveArgument(params[2].trim(), variables).toLowerCase();
                try {
                    type = ReverseShellGenerator.ShellType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // 尝试按名称匹配
                    for (ReverseShellGenerator.ShellType t : ReverseShellGenerator.ShellType.values()) {
                        if (t.getName().equalsIgnoreCase(typeStr)) {
                            type = t;
                            break;
                        }
                    }
                }
            }
            
            // 生成Payload
            return ReverseShellGenerator.generate(ip, port, type);
            
        } catch (NumberFormatException e) {
            System.err.println("端口号必须是数字: " + params[1]);
            return null;
        } catch (Exception e) {
            System.err.println("生成反弹Shell失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * URL编码
     * @@urlencode(test value) -> test+value
     */
    private static String executeUrlEncode(String args, Map<String, String> variables) {
        String value = resolveArgument(args, variables);
        if (value == null) {
            return null;
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("URL编码失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * URL解码
     * @@urldecode(test+value) -> test value
     */
    private static String executeUrlDecode(String args, Map<String, String> variables) {
        String value = resolveArgument(args, variables);
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("URL解码失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析函数参数
     * 如果参数是变量引用（如 str2），则从变量映射中获取值
     * 否则直接返回参数字符串
     * 
     * @param arg 参数字符串
     * @param variables 变量映射
     * @return 解析后的值
     */
    private static String resolveArgument(String arg, Map<String, String> variables) {
        if (arg == null) {
            return null;
        }
        
        arg = arg.trim();
        
        // 如果参数是变量引用（不含引号），尝试从变量映射中获取
        if (!arg.startsWith("\"") && !arg.startsWith("'")) {
            if (variables != null && variables.containsKey(arg)) {
                return variables.get(arg);
            }
        }
        
        // 去除引号（如果有）
        if ((arg.startsWith("\"") && arg.endsWith("\"")) || 
            (arg.startsWith("'") && arg.endsWith("'"))) {
            return arg.substring(1, arg.length() - 1);
        }
        
        return arg;
    }
    
    /**
     * 获取当前Unix时间戳（毫秒）
     * @@timestamp() -> 1698765432000
     */
    private static String executeTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
    
    /**
     * 格式化当前日期时间
     * @@date() -> 使用默认格式 yyyy-MM-dd HH:mm:ss
     * @@date(yyyy-MM-dd) -> 2025-11-01
     * @@date(yyyyMMddHHmmss) -> 20251101143025
     * 
     * 支持的格式占位符：
     * - yyyy: 4位年份
     * - yy: 2位年份
     * - MM: 2位月份（01-12）
     * - dd: 2位日期（01-31）
     * - HH: 24小时制小时（00-23）
     * - mm: 分钟（00-59）
     * - ss: 秒（00-59）
     * - SSS: 毫秒（000-999）
     */
    private static String executeDate(String args, Map<String, String> variables) {
        try {
            String format = resolveArgument(args, variables);
            
            // 如果没有指定格式或格式为空，使用默认格式
            if (format == null || format.isEmpty()) {
                format = "yyyy-MM-dd HH:mm:ss";
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.format(new Date());
            
        } catch (IllegalArgumentException e) {
            System.err.println("日期格式错误: " + args + " - " + e.getMessage());
            // 降级使用默认格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date());
        } catch (Exception e) {
            System.err.println("日期格式化失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 生成UUID
     * @@uuid() -> 550e8400-e29b-41d4-a716-446655440000
     */
    private static String executeUuid() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 读取文件内容
     * @@file(path/to/file.txt) -> 文件内容
     * @@file(/absolute/path/file.txt) -> 文件内容
     * 
     * 安全限制：
     * - 相对路径限制在 payload_files 目录内
     * - 绝对路径需谨慎使用
     * - 限制文件大小为10MB
     * 
     * @param args 文件路径
     * @param variables 变量映射
     * @return 文件内容
     */
    private static String executeFile(String args, Map<String, String> variables) {
        String filePath = resolveArgument(args, variables);
        
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("文件路径不能为空");
            return null;
        }
        
        try {
            File file;

            // 判断是否是绝对路径
            if (filePath.startsWith("/") || filePath.matches("^[A-Za-z]:.*")) {
                // 绝对路径
                file = new File(filePath);
            } else {
                // 相对路径，限制在payload_files目录内
                file = new File(FILE_BASE_DIR, filePath);
            }
            
            // 安全检查
            if (!file.exists()) {
                System.err.println("文件不存在: " + file.getAbsolutePath());
                return null;
            }
            
            if (!file.isFile()) {
                System.err.println("不是有效的文件: " + file.getAbsolutePath());
                return null;
            }
            
            // 限制文件大小为10MB
            long maxSize = 10 * 1024 * 1024;
            if (file.length() > maxSize) {
                System.err.println("文件过大 (限制10MB): " + file.getAbsolutePath());
                return null;
            }
            
            // 读取文件内容
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (content.length() > 0) {
                        content.append("\n");
                    }
                    content.append(line);
                }
            }
            
            System.out.println("成功读取文件: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
            return content.toString();
            
        } catch (IOException e) {
            System.err.println("读取文件失败: " + filePath + " - " + e.getMessage());
            return null;
        } catch (SecurityException e) {
            System.err.println("文件访问被拒绝: " + filePath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 计算哈希值
     * 
     * @param input 输入字符串
     * @param algorithm 哈希算法（MD5, SHA-1, SHA-256等）
     * @return 十六进制哈希字符串
     */
    private static String calculateHash(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("不支持的哈希算法: " + algorithm);
            return null;
        }
    }
}

