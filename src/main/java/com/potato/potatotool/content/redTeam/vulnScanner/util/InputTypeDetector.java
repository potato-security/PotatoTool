package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;

import java.io.File;
import java.util.regex.Pattern;

/**
 * 输入类型检测器
 * 根据目标字符串自动检测输入类型
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class InputTypeDetector {
    
    // URL 正则（支持 http/https）
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$", Pattern.CASE_INSENSITIVE);
    
    // IP:端口 正则
    private static final Pattern IP_PORT_PATTERN = Pattern.compile(
        "^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})$");
    
    // IPv6:端口 正则
    private static final Pattern IPV6_PORT_PATTERN = Pattern.compile(
        "^\\[([a-fA-F0-9:]+)\\]:(\\d{1,5})$");
    
    // 纯 IP 正则
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    
    // 域名正则（不含协议）
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$");
    
    /**
     * 检测输入类型
     * 
     * @param target 目标字符串
     * @return 检测到的输入类型
     */
    public static InputType detect(String target) {
        if (target == null || target.trim().isEmpty()) {
            return InputType.URL; // 默认
        }
        
        target = target.trim();
        
        // 1. 检查是否为本地路径
        if (isLocalPath(target)) {
            File file = new File(target);
            if (file.isDirectory()) {
                return InputType.LOCAL_PATH;
            } else if (file.isFile()) {
                return InputType.LOCAL_FILE;
            }
            // 路径不存在，但格式像路径
            return InputType.LOCAL_PATH;
        }
        
        // 2. 检查是否为 URL
        if (URL_PATTERN.matcher(target).matches()) {
            return InputType.URL;
        }
        
        // 3. 检查是否为 IP:端口
        if (IP_PORT_PATTERN.matcher(target).matches() || 
            IPV6_PORT_PATTERN.matcher(target).matches()) {
            return InputType.IP_PORT;
        }
        
        // 4. 检查是否为纯 IP（没有端口）
        if (IP_PATTERN.matcher(target).matches()) {
            // 纯 IP 默认当作需要添加端口的情况
            // 根据 Nuclei 规则，纯 IP 会被当作 HTTP 处理
            return InputType.URL;
        }
        
        // 5. 检查是否为域名（不含协议）
        if (DOMAIN_PATTERN.matcher(target).matches()) {
            // 检查是否有端口
            if (target.contains(":")) {
                String[] parts = target.split(":");
                if (parts.length == 2 && isValidPort(parts[1])) {
                    return InputType.IP_PORT;
                }
            }
            
            // 纯域名，检查是否适合 DNS 扫描
            // 如果是顶级域名或二级域名，可能是 DNS 扫描
            int dotCount = target.length() - target.replace(".", "").length();
            if (dotCount == 1) {
                // 可能是 example.com 这样的格式，默认当作 URL
                return InputType.URL;
            }
            
            return InputType.DOMAIN;
        }
        
        // 6. 带端口的域名
        if (target.contains(":")) {
            String[] parts = target.split(":");
            if (parts.length == 2 && isValidPort(parts[1])) {
                return InputType.IP_PORT;
            }
        }
        
        // 默认当作 URL
        return InputType.URL;
    }
    
    /**
     * 检查是否为本地路径
     */
    private static boolean isLocalPath(String target) {
        // Unix 风格路径
        if (target.startsWith("/")) {
            return true;
        }
        
        // Windows 风格路径
        if (target.length() > 2 && target.charAt(1) == ':' && 
            (target.charAt(2) == '\\' || target.charAt(2) == '/')) {
            return true;
        }
        
        // 相对路径（以 ./ 或 ../ 开头）
        if (target.startsWith("./") || target.startsWith("../")) {
            return true;
        }
        
        // Windows 相对路径
        if (target.startsWith(".\\") || target.startsWith("..\\")) {
            return true;
        }
        
        // ~ 开头的路径（Unix 用户目录）
        if (target.startsWith("~")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 验证端口号是否有效
     */
    private static boolean isValidPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 规范化目标（根据输入类型添加必要的前缀）
     * 
     * @param target 原始目标
     * @param inputType 输入类型
     * @return 规范化后的目标
     */
    public static String normalizeTarget(String target, InputType inputType) {
        if (target == null || target.trim().isEmpty()) {
            return target;
        }
        
        target = target.trim();
        
        switch (inputType) {
            case URL:
                // 如果没有协议，添加 http://
                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                    return "http://" + target;
                }
                break;
                
            case LOCAL_PATH:
            case LOCAL_FILE:
                // 展开 ~ 路径
                if (target.startsWith("~")) {
                    String home = System.getProperty("user.home");
                    return home + target.substring(1);
                }
                break;
                
            default:
                break;
        }
        
        return target;
    }
    
    /**
     * 批量检测并规范化目标
     * 
     * @param targets 目标数组
     * @return 规范化后的目标数组及其类型
     */
    public static DetectionResult[] detectAndNormalize(String[] targets) {
        if (targets == null) {
            return new DetectionResult[0];
        }
        
        DetectionResult[] results = new DetectionResult[targets.length];
        
        for (int i = 0; i < targets.length; i++) {
            InputType type = detect(targets[i]);
            String normalized = normalizeTarget(targets[i], type);
            results[i] = new DetectionResult(targets[i], normalized, type);
        }
        
        return results;
    }
    
    /**
     * 检测结果
     */
    public static class DetectionResult {
        private final String original;
        private final String normalized;
        private final InputType inputType;
        
        public DetectionResult(String original, String normalized, InputType inputType) {
            this.original = original;
            this.normalized = normalized;
            this.inputType = inputType;
        }
        
        public String getOriginal() { return original; }
        public String getNormalized() { return normalized; }
        public InputType getInputType() { return inputType; }
    }
}
