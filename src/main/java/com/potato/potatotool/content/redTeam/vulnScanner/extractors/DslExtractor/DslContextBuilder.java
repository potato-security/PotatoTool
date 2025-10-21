package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * DSL上下文构建器
 * 负责从HTTP响应和请求中提取信息并构建DSL评估上下文
 */
public class DslContextBuilder {

    /**
     * 创建DSL上下文
     * 从HTTP响应和请求中提取各种信息构建上下文映射
     */
    public static Map<String, Object> createDslContext(Object response, Object request) {
        Map<String, Object> context = new HashMap<>();
        
        try {
            // 提取响应信息
            if (response != null) {
                extractResponseInfo(response, context);
            }
            
            // 提取请求信息
            if (request != null) {
                extractRequestInfo(request, context);
            }
            
        } catch (Exception e) {
            System.err.println("创建DSL上下文时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return context;
    }

    /**
     * 提取响应信息
     */
    private static void extractResponseInfo(Object response, Map<String, Object> context) {
        try {
            // 检查是否为Map类型
            if (response instanceof Map) {
                extractFromMap((Map<?, ?>) response, context, true);
                return;
            }
            
            // 使用反射获取响应字段
            Class<?> responseClass = response.getClass();
            
            // 提取状态码
            try {
                Object statusCode = getFieldValue(response, "statusCode");
                if (statusCode == null) {
                    statusCode = getFieldValue(response, "status");
                }
                if (statusCode != null) {
                    context.put("status", statusCode);
                    context.put("status_code", statusCode);
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
            // 提取响应体
            try {
                Object body = getFieldValue(response, "body");
                if (body == null) {
                    body = getFieldValue(response, "content");
                }
                if (body != null) {
                    context.put("body", body.toString());
                    context.put("response", body.toString());
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
            // 提取响应头
            try {
                Object headers = getFieldValue(response, "headers");
                if (headers != null) {
                    context.put("headers", headers);
                    context.put("all_headers", headers);
                    
                    // 如果headers是Map类型，提取常用头部
                    if (headers instanceof Map) {
                        Map<?, ?> headerMap = (Map<?, ?>) headers;
                        extractCommonHeaders(headerMap, context);
                    }
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
            // 提取内容长度
            try {
                Object contentLength = getFieldValue(response, "contentLength");
                if (contentLength != null) {
                    context.put("content_length", contentLength);
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
            // 提取延迟时间
            try {
                Object latency = getFieldValue(response, "latency");
                if (latency == null) {
                    latency = getFieldValue(response, "time");
                }
                if (latency != null) {
                    context.put("latency", latency);
                    context.put("time", latency);
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
        } catch (Exception e) {
            System.err.println("提取响应信息时发生错误: " + e.getMessage());
        }
    }

    /**
     * 提取请求信息
     */
    private static void extractRequestInfo(Object request, Map<String, Object> context) {
        try {
            // 检查是否为Map类型
            if (request instanceof Map) {
                extractFromMap((Map<?, ?>) request, context, false);
                return;
            }
            
            // 提取URL信息
            try {
                Object url = getFieldValue(request, "url");
                if (url != null) {
                    context.put("url", url.toString());
                    parseUrlComponents(url.toString(), context);
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
            // 提取请求方法
            try {
                Object method = getFieldValue(request, "method");
                if (method != null) {
                    context.put("method", method.toString());
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
            // 提取请求头
            try {
                Object requestHeaders = getFieldValue(request, "headers");
                if (requestHeaders != null) {
                    context.put("request_headers", requestHeaders);
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
            // 提取请求体
            try {
                Object requestBody = getFieldValue(request, "body");
                if (requestBody == null) {
                    requestBody = getFieldValue(request, "data");
                }
                if (requestBody != null) {
                    context.put("request_body", requestBody.toString());
                    context.put("data", requestBody.toString());
                }
            } catch (Exception e) {
                // 忽略字段不存在的错误
            }
            
        } catch (Exception e) {
            System.err.println("提取请求信息时发生错误: " + e.getMessage());
        }
    }

    /**
     * 解析URL组件
     */
    private static void parseUrlComponents(String urlString, Map<String, Object> context) {
        try {
            URL url = new URL(urlString);
            
            context.put("scheme", url.getProtocol());
            context.put("host", url.getHost());
            context.put("port", url.getPort() == -1 ? ("https".equals(url.getProtocol()) ? 443 : 80) : url.getPort());
            context.put("path", url.getPath());
            
            // 解析查询参数
            String query = url.getQuery();
            if (query != null && !query.isEmpty()) {
                context.put("query", query);
                parseQueryParameters(query, context);
            }
            
        } catch (Exception e) {
            System.err.println("解析URL组件时发生错误: " + e.getMessage());
        }
    }

    /**
     * 解析查询参数
     */
    private static void parseQueryParameters(String query, Map<String, Object> context) {
        try {
            String[] pairs = query.split("&");
            Map<String, String> queryParams = new HashMap<>();
            
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                } else if (keyValue.length == 1) {
                    queryParams.put(keyValue[0], "");
                }
            }
            
            context.put("query_params", queryParams);
            
        } catch (Exception e) {
            System.err.println("解析查询参数时发生错误: " + e.getMessage());
        }
    }

    /**
     * 提取常用HTTP头部
     */
    private static void extractCommonHeaders(Map<?, ?> headerMap, Map<String, Object> context) {
        try {
            // 提取常用头部字段
            extractHeaderValue(headerMap, "content-type", "content_type", context);
            extractHeaderValue(headerMap, "server", "server", context);
            extractHeaderValue(headerMap, "location", "location", context);
            extractHeaderValue(headerMap, "set-cookie", "set_cookie", context);
            
        } catch (Exception e) {
            System.err.println("提取常用头部时发生错误: " + e.getMessage());
        }
    }

    /**
     * 提取特定头部值
     */
    private static void extractHeaderValue(Map<?, ?> headerMap, String headerName, String contextKey, Map<String, Object> context) {
        Object value = headerMap.get(headerName);
        if (value == null) {
            // 尝试大小写不敏感的查找
            for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().toString().equalsIgnoreCase(headerName)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        
        if (value != null) {
            context.put(contextKey, value.toString());
        }
    }

    /**
     * 使用反射获取字段值
     */
    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            
            // 尝试直接访问字段
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                // 字段不存在，尝试getter方法
            }
            
            // 尝试getter方法
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method getter = clazz.getMethod(getterName);
                return getter.invoke(obj);
            } catch (NoSuchMethodException e) {
                // getter方法不存在
            }
            
            // 尝试is方法（用于boolean类型）
            String isMethodName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method isMethod = clazz.getMethod(isMethodName);
                return isMethod.invoke(obj);
            } catch (NoSuchMethodException e) {
                // is方法不存在
            }
            
        } catch (Exception e) {
            // 忽略反射错误
        }
        
        return null;
    }

    /**
     * 从Map中提取信息到上下文
     */
    private static void extractFromMap(Map<?, ?> sourceMap, Map<String, Object> context, boolean isResponse) {
        try {
            for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();
                
                if (value != null) {
                    // 直接添加原始键值对
                    context.put(key, value);
                    
                    // 为响应数据添加特殊映射
                    if (isResponse) {
                        switch (key.toLowerCase()) {
                            case "body":
                            case "content":
                                context.put("body", value.toString());
                                context.put("response", value.toString());
                                break;
                            case "statuscode":
                            case "status_code":
                            case "status":
                                context.put("status", value);
                                context.put("status_code", value);
                                break;
                            case "headers":
                                context.put("headers", value);
                                context.put("all_headers", value);
                                // 如果headers是Map，提取常用头部
                                if (value instanceof Map) {
                                    extractCommonHeaders((Map<?, ?>) value, context);
                                }
                                break;
                            case "content_length":
                                context.put("content_length", value);
                                break;
                            case "content_type":
                                context.put("content_type", value);
                                break;
                        }
                    } else {
                        // 为请求数据添加特殊映射
                        switch (key.toLowerCase()) {
                            case "url":
                                context.put("url", value.toString());
                                parseUrlComponents(value.toString(), context);
                                break;
                            case "method":
                                context.put("method", value.toString());
                                break;
                            case "headers":
                                context.put("request_headers", value);
                                break;
                            case "body":
                            case "data":
                                context.put("request_body", value.toString());
                                context.put("data", value.toString());
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("从Map提取信息时发生错误: " + e.getMessage());
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private DslContextBuilder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}