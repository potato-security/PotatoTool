package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 原始HTTP报文解析器
 * 用于解析Goby POC中的raw字段（原始HTTP请求报文）
 * 
 * 支持的格式:
 * GET /path HTTP/1.1
 * Host: example.com
 * User-Agent: Mozilla/5.0
 * Content-Length: 10
 * 
 * param=value
 * 
 * @author Potato
 * @date 2025-11-01
 */
public class RawHttpRequestParser {

    /**
     * 解析后的HTTP请求
     */
    public static class ParsedRequest {
        private String method;                          // HTTP方法
        private String path;                            // 请求路径
        private String protocol;                        // 协议版本
        private Map<String, String> headers;            // 请求头
        private String body;                            // 请求体

        public ParsedRequest(String method, String path, String protocol, 
                           Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.protocol = protocol;
            this.headers = headers;
            this.body = body;
        }

        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getProtocol() { return protocol; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody() { return body; }
        
        @Override
        public String toString() {
            return "ParsedRequest{" +
                    "method='" + method + '\'' +
                    ", path='" + path + '\'' +
                    ", protocol='" + protocol + '\'' +
                    ", headers=" + headers.size() +
                    ", bodyLength=" + (body != null ? body.length() : 0) +
                    '}';
        }
    }

    /**
     * 解析原始HTTP请求报文
     * 
     * @param rawRequest 原始HTTP报文
     * @return 解析后的请求对象
     * @throws IllegalArgumentException 如果报文格式无效
     */
    public static ParsedRequest parse(String rawRequest) {
        if (rawRequest == null || rawRequest.trim().isEmpty()) {
            throw new IllegalArgumentException("原始HTTP报文不能为空");
        }

        try {
            // 使用\r\n分割（标准HTTP换行符）
            String[] lines = rawRequest.split("\r\n");
            
            // 如果没有\r\n，尝试用\n分割
            if (lines.length == 1) {
                lines = rawRequest.split("\n");
            }

            if (lines.length == 0) {
                throw new IllegalArgumentException("无效的HTTP报文格式");
            }

            // 解析第一行：方法 路径 协议
            String firstLine = lines[0].trim();
            
            // 检查是否以HTTP方法开头（GET, POST, PUT, DELETE, HEAD, OPTIONS等）
            // 使用大小写不敏感匹配，允许任意方法名（因为某些POC可能使用自定义方法）
            // 宽松模式：只要是以字母开头，且后面有空格，就认为是有效的方法
            if (!firstLine.matches("(?i)^[A-Z]+\\s+.*")) {
                throw new IllegalArgumentException("请求行格式无效，必须以HTTP方法开头: " + firstLine);
            }
            
            String[] requestLine = firstLine.split("\\s+");
            if (requestLine.length < 2) {
                throw new IllegalArgumentException("请求行格式无效: " + firstLine);
            }

            String method = requestLine[0].toUpperCase();
            String path = requestLine[1];
            // 宽容模式：如果缺少协议版本，默认为HTTP/1.1
            String protocol = requestLine.length > 2 ? requestLine[2] : "HTTP/1.1";

            // 解析请求头
            Map<String, String> headers = new LinkedHashMap<>();
            int i = 1;
            for (; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // 空行表示请求头结束
                if (line.isEmpty()) {
                    i++;
                    break;
                }
                
                // 解析请求头（格式: Name: Value）
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex).trim();
                    String headerValue = line.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);
                }
            }

            // 解析请求体（剩余所有行）
            StringBuilder bodyBuilder = new StringBuilder();
            for (; i < lines.length; i++) {
                if (bodyBuilder.length() > 0) {
                    bodyBuilder.append("\n");
                }
                bodyBuilder.append(lines[i]);
            }
            String body = bodyBuilder.length() > 0 ? bodyBuilder.toString() : null;

            return new ParsedRequest(method, path, protocol, headers, body);

        } catch (Exception e) {
            throw new IllegalArgumentException("解析HTTP报文失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将解析后的请求重新组装为原始报文
     * 
     * @param request 解析后的请求
     * @return 原始HTTP报文
     */
    public static String build(ParsedRequest request) {
        if (request == null) {
            return null;
        }

        StringBuilder raw = new StringBuilder();

        // 请求行
        raw.append(request.getMethod())
           .append(" ")
           .append(request.getPath())
           .append(" ")
           .append(request.getProtocol())
           .append("\r\n");

        // 请求头
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                raw.append(header.getKey())
                   .append(": ")
                   .append(header.getValue())
                   .append("\r\n");
            }
        }

        // 空行（分隔请求头和请求体）
        raw.append("\r\n");

        // 请求体
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            raw.append(request.getBody());
        }

        return raw.toString();
    }

    /**
     * 验证原始HTTP报文格式
     * 
     * @param rawRequest 原始HTTP报文
     * @return 是否是有效格式
     */
    public static boolean isValidRawRequest(String rawRequest) {
        if (rawRequest == null || rawRequest.trim().isEmpty()) {
            return false;
        }

        try {
            parse(rawRequest);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        原始HTTP报文解析器测试                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // 测试1: GET请求
        String rawGet = "GET /api/users?id=123 HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "User-Agent: Mozilla/5.0\r\n" +
                "Accept: application/json\r\n" +
                "\r\n";

        System.out.println("=== 测试1: GET请求 ===");
        System.out.println("原始报文:\n" + rawGet);
        ParsedRequest req1 = parse(rawGet);
        System.out.println("解析结果: " + req1);
        System.out.println("  Method: " + req1.getMethod());
        System.out.println("  Path: " + req1.getPath());
        System.out.println("  Protocol: " + req1.getProtocol());
        System.out.println("  Headers: " + req1.getHeaders());
        System.out.println("  Body: " + req1.getBody());

        // 测试2: POST请求
        String rawPost = "POST /api/login HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "Content-Length: 27\r\n" +
                "\r\n" +
                "username=admin&password=123";

        System.out.println("\n=== 测试2: POST请求 ===");
        System.out.println("原始报文:\n" + rawPost);
        ParsedRequest req2 = parse(rawPost);
        System.out.println("解析结果: " + req2);
        System.out.println("  Method: " + req2.getMethod());
        System.out.println("  Path: " + req2.getPath());
        System.out.println("  Body: " + req2.getBody());

        // 测试3: 重新构建
        System.out.println("\n=== 测试3: 重新构建报文 ===");
        String rebuilt = build(req2);
        System.out.println("重新构建:\n" + rebuilt);

        // 测试4: 验证
        System.out.println("\n=== 测试4: 格式验证 ===");
        System.out.println("有效报文: " + isValidRawRequest(rawGet));
        System.out.println("无效报文: " + isValidRawRequest("invalid data"));
    }
}


