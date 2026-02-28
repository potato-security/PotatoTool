package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.GobyJsonObj;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证处理器
 * 处理Goby POC中的各种认证方式
 * 
 * 支持的认证类型：
 * 1. Basic - HTTP Basic认证
 * 2. Bearer - Bearer Token认证
 * 3. Digest - HTTP Digest认证
 * 4. Cookie - Cookie认证
 * 
 * @author Potato
 * @date 2025-11-01
 */
public class AuthenticationHandler {

    /**
     * 应用认证配置到请求头
     * 
     * @param headers 请求头映射
     * @param auth 认证配置
     * @param variables 变量映射（用于替换变量引用）
     */
    public static void applyAuthentication(Map<String, String> headers, 
                                          GobyJsonObj.Authentication auth,
                                          Map<String, String> variables) {
        if (auth == null || headers == null) {
            return;
        }

        String authType = auth.getType();
        if (authType == null || authType.isEmpty()) {
            return;
        }

        try {
            switch (authType.toLowerCase()) {
                case "basic":
                    applyBasicAuth(headers, auth, variables);
                    break;
                    
                case "bearer":
                    applyBearerAuth(headers, auth, variables);
                    break;
                    
                case "digest":
                    // Digest认证比较复杂，需要challenge-response机制
                    // 这里提供基础支持
                    applyDigestAuth(headers, auth, variables);
                    break;
                    
                case "cookie":
                    applyCookieAuth(headers, auth, variables);
                    break;
                    
                default:
                    System.err.println("不支持的认证类型: " + authType);
            }
        } catch (Exception e) {
            System.err.println("应用认证配置失败: " + e.getMessage());
        }
    }

    /**
     * 应用Basic认证
     * 格式: Authorization: Basic base64(username:password)
     */
    private static void applyBasicAuth(Map<String, String> headers, 
                                       GobyJsonObj.Authentication auth,
                                       Map<String, String> variables) {
        String username = resolveValue(auth.getUsername(), variables);
        String password = resolveValue(auth.getPassword(), variables);

        if (username == null || password == null) {
            System.err.println("Basic认证缺少用户名或密码");
            return;
        }

        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(
            credentials.getBytes(StandardCharsets.UTF_8)
        );

        headers.put("Authorization", "Basic " + encoded);
    }

    /**
     * 应用Bearer Token认证
     * 格式: Authorization: Bearer <token>
     */
    private static void applyBearerAuth(Map<String, String> headers,
                                       GobyJsonObj.Authentication auth,
                                       Map<String, String> variables) {
        String token = resolveValue(auth.getToken(), variables);

        if (token == null || token.isEmpty()) {
            System.err.println("Bearer认证缺少token");
            return;
        }

        headers.put("Authorization", "Bearer " + token);
    }

    /**
     * 应用Digest认证
     * 注意：Digest认证通常需要先发一次请求获取challenge，这里仅提供基础支持
     */
    private static void applyDigestAuth(Map<String, String> headers,
                                       GobyJsonObj.Authentication auth,
                                       Map<String, String> variables) {
        // Digest认证比较复杂，需要服务器的challenge
        // 这里提供一个基础的占位实现
        // 实际使用中，Digest认证通常在HTTP客户端层面处理

        String username = resolveValue(auth.getUsername(), variables);
        String password = resolveValue(auth.getPassword(), variables);

        if (username != null && password != null) {
            // 简化处理：添加凭证信息到自定义头
            headers.put("X-Digest-Username", username);
            // 注意：密码不应明文传输，这里仅为示例
            System.out.println("提示: Digest认证需要在HTTP客户端层面实现challenge-response");
        }
    }

    /**
     * 应用Cookie认证
     * 将cookies添加到Cookie请求头
     */
    private static void applyCookieAuth(Map<String, String> headers,
                                       GobyJsonObj.Authentication auth,
                                       Map<String, String> variables) {
        Map<String, String> cookies = auth.getCookies();
        if (cookies == null || cookies.isEmpty()) {
            return;
        }

        StringBuilder cookieHeader = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (cookieHeader.length() > 0) {
                cookieHeader.append("; ");
            }
            String value = resolveValue(entry.getValue(), variables);
            cookieHeader.append(entry.getKey()).append("=").append(value);
        }

        headers.put("Cookie", cookieHeader.toString());
    }

    /**
     * 处理Cookie到请求头
     * 
     * @param headers 请求头
     * @param cookies Cookie映射
     * @param variables 变量映射
     */
    public static void applyCookies(Map<String, String> headers,
                                   Map<String, String> cookies,
                                   Map<String, String> variables) {
        if (cookies == null || cookies.isEmpty()) {
            return;
        }

        StringBuilder cookieHeader = new StringBuilder();
        
        // 如果已有Cookie头，先保留
        if (headers.containsKey("Cookie")) {
            cookieHeader.append(headers.get("Cookie"));
        }

        // 添加新的cookies
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (cookieHeader.length() > 0) {
                cookieHeader.append("; ");
            }
            String value = resolveValue(entry.getValue(), variables);
            cookieHeader.append(entry.getKey()).append("=").append(value);
        }

        headers.put("Cookie", cookieHeader.toString());
    }

    /**
     * 解析值（支持变量引用）
     * 
     * @param value 原始值
     * @param variables 变量映射
     * @return 解析后的值
     */
    private static String resolveValue(String value, Map<String, String> variables) {
        if (value == null) {
            return null;
        }

        // 如果是变量引用 {{{varName}}}
        if (value.contains("{{{") && value.contains("}}}")) {
            // 简单替换
            String result = value;
            if (variables != null) {
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    result = result.replace("{{{" + entry.getKey() + "}}}", entry.getValue());
                }
            }
            return result;
        }

        return value;
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        认证处理器测试                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        Map<String, String> headers = new LinkedHashMap<>();
        Map<String, String> variables = new HashMap<>();
        variables.put("user", "admin");
        variables.put("pass", "password123");
        variables.put("token", "abc123xyz");

        // 测试Basic认证
        System.out.println("=== Basic认证测试 ===");
        GobyJsonObj.Authentication basicAuth = new GobyJsonObj.Authentication();
        basicAuth.setType("basic");
        basicAuth.setUsername("{{{user}}}");
        basicAuth.setPassword("{{{pass}}}");
        applyAuthentication(headers, basicAuth, variables);
        System.out.println("Authorization: " + headers.get("Authorization"));

        // 测试Bearer认证
        System.out.println("\n=== Bearer认证测试 ===");
        headers.clear();
        GobyJsonObj.Authentication bearerAuth = new GobyJsonObj.Authentication();
        bearerAuth.setType("bearer");
        bearerAuth.setToken("{{{token}}}");
        applyAuthentication(headers, bearerAuth, variables);
        System.out.println("Authorization: " + headers.get("Authorization"));

        // 测试Cookie
        System.out.println("\n=== Cookie测试 ===");
        headers.clear();
        Map<String, String> cookies = new HashMap<>();
        cookies.put("session", "{{{token}}}");
        cookies.put("user_id", "123");
        applyCookies(headers, cookies, variables);
        System.out.println("Cookie: " + headers.get("Cookie"));
    }
}

