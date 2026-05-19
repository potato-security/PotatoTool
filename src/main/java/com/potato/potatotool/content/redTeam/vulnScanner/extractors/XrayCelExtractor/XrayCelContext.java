package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import com.potato.potatotool.utils.network.CustomHttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Xray CEL 上下文构建器
 * 构建符合 Xray 规范的执行上下文
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class XrayCelContext {
    
    /**
     * 创建标准 CEL 上下文
     * 包含 response 和 request 对象
     * 
     * @param response HTTP 响应对象
     * @param request HTTP 请求对象
     * @return CEL 上下文
     */
    public static Map<String, Object> createContext(Object response, Object request) {
        Map<String, Object> context = new HashMap<>();
        
        // 添加 response 对象
        if (response != null) {
            addResponseToContext(context, response);
        }
        
        // 添加 request 对象
        if (request != null) {
            addRequestToContext(context, request);
        }
        
        return context;
    }
    
    /**
     * 创建规则上下文
     * 用于评估规则调用表达式（如 r0() && r1()）
     * 
     * @param ruleResults 规则结果映射
     * @return CEL 上下文
     */
    public static Map<String, Object> createRuleContext(Map<String, Boolean> ruleResults) {
        Map<String, Object> context = new HashMap<>();
        
        if (ruleResults != null) {
            // 将规则结果添加到上下文
            for (Map.Entry<String, Boolean> entry : ruleResults.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        
        return context;
    }

    public static Map<String, Object> createRuleContext(Map<String, Boolean> ruleResults,
                                                         Map<String, ?> variables) {
        Map<String, Object> context = createRuleContext(ruleResults);
        if (variables != null) {
            for (Map.Entry<String, ?> entry : variables.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List && !((List<?>) value).isEmpty()) {
                    value = ((List<?>) value).get(0);
                }
                context.put(entry.getKey(), value);
            }
        }
        return context;
    }
    
    /**
     * 添加自定义变量到上下文
     * 
     * @param context 现有上下文
     * @param variables 变量映射
     */
    public static void addVariables(Map<String, Object> context, Map<String, String> variables) {
        if (context != null && variables != null) {
            context.putAll(variables);
        }
    }
    
    /**
     * 将 HTTP 响应添加到上下文
     */
    private static void addResponseToContext(Map<String, Object> context, Object responseObj) {
        try {
            if (responseObj instanceof CustomHttpResponse) {
                CustomHttpResponse response = (CustomHttpResponse) responseObj;
                
                // 创建 response 对象
                Map<String, Object> responseMap = new HashMap<>();
                
                // 基本属性
                responseMap.put("status", response.getResponseCode());
                responseMap.put("status_code", response.getResponseCode()); // 别名
                String bodyText = response.getTextStr() != null ? response.getTextStr() : "";
                responseMap.put("body", bodyText);
                responseMap.put("body_string", bodyText);
                responseMap.put("raw", bodyText);
                
                // Headers
                Map<String, String> headers = new HashMap<>();
                Map<String, List<String>> headerFields = response.getHeaderFields();
                if (headerFields != null) {
                    headerFields.forEach((key, values) -> {
                        if (values != null && !values.isEmpty() && key != null) {
                            headers.put(key, values.get(0));
                        }
                    });
                }
                responseMap.put("headers", headers);
                
                // 常用响应头
                responseMap.put("content_type", headers.getOrDefault("Content-Type", ""));
                responseMap.put("content_length", headers.getOrDefault("Content-Length", "0"));
                responseMap.put("server", headers.getOrDefault("Server", ""));
                responseMap.put("location", headers.getOrDefault("Location", ""));
                
                // 响应时间
                responseMap.put("latency", response.getResponseTime());
                
                // 响应 URL（最终 URL，可能经过重定向）
                String finalUrl = response.getURL() != null ? response.getURL().toString() : "";
                responseMap.put("url", finalUrl != null ? finalUrl : "");
                responseMap.put("time", response.getResponseTime());
                
                // 将 response 对象添加到上下文
                context.put("response", new ResponseWrapper(responseMap));
                
                // 同时添加扁平化的属性（方便访问）
                context.put("status", response.getResponseCode());
                context.put("body", bodyText);
                context.put("headers", headers);
                context.put("latency", response.getResponseTime());
                
            } else {
                // 兜底：使用原始对象
                context.put("response", responseObj);
            }
        } catch (Exception e) {
            System.err.println("添加响应对象到上下文失败: " + e.getMessage());
        }
    }
    
    /**
     * 将 HTTP 请求添加到上下文
     */
    private static void addRequestToContext(Map<String, Object> context, Object requestObj) {
        try {
            // 创建 request 对象（简化版）
            Map<String, Object> requestMap = new HashMap<>();
            
            // 基本属性（可根据实际 request 对象扩展）
            requestMap.put("method", "GET");
            requestMap.put("url", "");
            requestMap.put("path", "");
            requestMap.put("headers", new HashMap<String, String>());
            requestMap.put("body", "");
            
            context.put("request", new RequestWrapper(requestMap));
            
        } catch (Exception e) {
            System.err.println("添加请求对象到上下文失败: " + e.getMessage());
        }
    }
    
    /**
     * Response 包装器
     * 提供方便的属性访问
     * 
     * 支持 Xray 官方所有 response 属性：
     * - response.status / response.status_code
     * - response.body / response.body_string / response.raw
     * - response.headers
     * - response.latency / response.time
     * - response.content_type / response.content_length
     * - response.server / response.location
     */
    public static class ResponseWrapper {
        private final Map<String, Object> data;
        
        public ResponseWrapper(Map<String, Object> data) {
            this.data = data;
        }
        
        public Object get(String key) {
            return data.get(key);
        }
        
        // 状态码
        public int getStatus() {
            Object status = data.get("status");
            return status instanceof Integer ? (Integer) status : 0;
        }
        
        public int getStatusCode() {
            return getStatus();
        }
        
        // 响应体
        public String getBody() {
            Object body = data.get("body");
            return body != null ? body.toString() : "";
        }
        
        public String getBodyString() {
            return getBody();
        }
        
        public String getRaw() {
            Object raw = data.get("raw");
            return raw != null ? raw.toString() : "";
        }
        
        public byte[] getBodyBytes() {
            return getBody().getBytes(StandardCharsets.UTF_8);
        }
        
        // 响应头
        @SuppressWarnings("unchecked")
        public Map<String, String> getHeaders() {
            Object headers = data.get("headers");
            if (headers instanceof Map) {
                return (Map<String, String>) headers;
            }
            return new HashMap<>();
        }
        
        public String getHeader(String name) {
            return getHeaders().getOrDefault(name, "");
        }
        
        /**
         * 响应的最终 URL（可能经过重定向）
         */
        public String getUrl() {
            Object url = data.get("url");
            return url != null ? url.toString() : "";
        }
        
        /**
         * 响应 URL 的路径部分
         */
        public String getUrlPath() {
            String url = getUrl();
            if (url.isEmpty()) {
                return "";
            }
            try {
                // 解析 URL 获取路径
                int protocolEnd = url.indexOf("://");
                if (protocolEnd != -1) {
                    int pathStart = url.indexOf('/', protocolEnd + 3);
                    if (pathStart != -1) {
                        int queryStart = url.indexOf('?', pathStart);
                        if (queryStart != -1) {
                            return url.substring(pathStart, queryStart);
                        }
                        return url.substring(pathStart);
                    }
                }
                return "/";
            } catch (Exception e) {
                return "";
            }
        }
        
        // 常用响应头
        public String getContentType() {
            Object ct = data.get("content_type");
            if (ct != null && !ct.toString().isEmpty()) {
                return ct.toString();
            }
            return getHeader("Content-Type");
        }
        
        public String getContentLength() {
            Object cl = data.get("content_length");
            if (cl != null && !cl.toString().isEmpty()) {
                return cl.toString();
            }
            return getHeader("Content-Length");
        }
        
        public String getServer() {
            Object server = data.get("server");
            if (server != null && !server.toString().isEmpty()) {
                return server.toString();
            }
            return getHeader("Server");
        }
        
        public String getLocation() {
            Object location = data.get("location");
            if (location != null && !location.toString().isEmpty()) {
                return location.toString();
            }
            return getHeader("Location");
        }
        
        public String getSetCookie() {
            return getHeader("Set-Cookie");
        }
        
        public String getCookie() {
            return getHeader("Cookie");
        }
        
        // 响应时间
        public long getLatency() {
            Object latency = data.get("latency");
            if (latency instanceof Number) {
                return ((Number) latency).longValue();
            }
            return 0L;
        }
        
        public long getTime() {
            return getLatency();
        }
        
        // 方法调用（用于 CEL 表达式）
        public boolean contains(String str) {
            return getBody().contains(str);
        }
        
        public boolean startsWith(String str) {
            return getBody().startsWith(str);
        }
        
        public boolean endsWith(String str) {
            return getBody().endsWith(str);
        }
        
        public int length() {
            return getBody().length();
        }
        
        public boolean matches(String regex) {
            try {
                return getBody().matches(regex);
            } catch (Exception e) {
                return false;
            }
        }
        
        public String toLowerCase() {
            return getBody().toLowerCase();
        }
        
        public String toUpperCase() {
            return getBody().toUpperCase();
        }
        
        public String trim() {
            return getBody().trim();
        }
        
        @Override
        public String toString() {
            return getBody();
        }
    }
    
    /**
     * Request 包装器
     * 完整支持 Xray request 对象的所有属性和方法
     */
    public static class RequestWrapper {
        private final Map<String, Object> data;
        
        public RequestWrapper(Map<String, Object> data) {
            this.data = data;
        }
        
        public Object get(String key) {
            return data.get(key);
        }
        
        /**
         * HTTP 方法（GET, POST, etc.）
         */
        public String getMethod() {
            Object method = data.get("method");
            return method != null ? method.toString() : "";
        }
        
        /**
         * 完整的 URL
         */
        public String getUrl() {
            Object url = data.get("url");
            return url != null ? url.toString() : "";
        }
        
        /**
         * URI 路径部分
         */
        public String getPath() {
            Object path = data.get("path");
            return path != null ? path.toString() : "";
        }
        
        /**
         * URL 参数字符串（query string）
         */
        public String getQuery() {
            Object query = data.get("query");
            return query != null ? query.toString() : "";
        }
        
        /**
         * 协议（http/https）
         */
        public String getProto() {
            Object proto = data.get("proto");
            return proto != null ? proto.toString() : "http";
        }
        
        /**
         * 主机名（不含端口）
         */
        public String getHost() {
            Object host = data.get("host");
            if (host != null) {
                return host.toString();
            }
            // 从 URL 中提取
            String url = getUrl();
            if (url.contains("://")) {
                String[] parts = url.split("://", 2);
                if (parts.length > 1) {
                    String hostPart = parts[1].split("/")[0];
                    return hostPart.split(":")[0];
                }
            }
            return "";
        }
        
        /**
         * 端口号
         */
        public int getPort() {
            Object port = data.get("port");
            if (port instanceof Number) {
                return ((Number) port).intValue();
            }
            // 默认根据协议推断
            return "https".equalsIgnoreCase(getProto()) ? 443 : 80;
        }
        
        /**
         * 所有请求头（Map）
         */
        @SuppressWarnings("unchecked")
        public Map<String, String> getHeaders() {
            Object headers = data.get("headers");
            if (headers instanceof Map) {
                return (Map<String, String>) headers;
            }
            return new HashMap<>();
        }
        
        /**
         * 获取单个请求头（便捷方法）
         */
        public String getHeader(String name) {
            Map<String, String> headers = getHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return "";
        }
        
        /**
         * Content-Type 请求头
         */
        public String getContentType() {
            return getHeader("Content-Type");
        }
        
        /**
         * User-Agent 请求头
         */
        public String getUserAgent() {
            return getHeader("User-Agent");
        }
        
        /**
         * 请求体
         */
        public String getBody() {
            Object body = data.get("body");
            return body != null ? body.toString() : "";
        }
        
        /**
         * 请求体（字节数组）
         */
        public byte[] getBodyBytes() {
            String body = getBody();
            return body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        }
        
        /**
         * 请求体长度
         */
        public int getContentLength() {
            return getBody().length();
        }
        
        /**
         * 原始请求内容（用于某些特殊场景）
         */
        public String getRaw() {
            Object raw = data.get("raw");
            return raw != null ? raw.toString() : "";
        }
        
        /**
         * 字符串表示（返回 URL）
         */
        @Override
        public String toString() {
            return getUrl();
        }
    }
}
