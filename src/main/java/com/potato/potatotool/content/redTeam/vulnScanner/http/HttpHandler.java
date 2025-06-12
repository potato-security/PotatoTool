package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.utils.network.RequestObj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP请求处理类，负责解析和处理HTTP请求
 */
public class HttpHandler {

    /**
     * 处理原始HTTP请求
     * @param requestObj 请求对象
     * @param rawLines 原始请求行列表
     * @param target 目标URL
     * @param variables 变量映射
     */
    public static void processRawRequest(RequestObj requestObj, List<String> rawLines, String target, Map<String, String> variables) {
        if (requestObj == null || rawLines == null || rawLines.isEmpty()) {
            return;
        }

        // 解析第一行以获取请求方法和路径
        String firstLine = rawLines.get(0);
        String[] parts = firstLine.split("\\s+");
        if (parts.length >= 2) {
            // 设置请求方法
            requestObj.setMethod(parts[0]);
            
            // 设置请求路径（可能需要替换变量）
            String path = parts[1];
            if (variables != null && !variables.isEmpty()) {
                path = replaceVariables(path, variables);
            }
            
            // 如果路径以http开头，则是完整URL，需要提取路径部分
            if (path.toLowerCase().startsWith("http")) {
                try {
                    java.net.URL url = new java.net.URL(path);
                    path = url.getPath();
                    if (url.getQuery() != null && !url.getQuery().isEmpty()) {
                        path += "?" + url.getQuery();
                    }
                } catch (java.net.MalformedURLException e) {
                    // 忽略URL解析错误
                }
            }
            
            requestObj.setUrl(path);
        }

        Map<String, String> headers = new HashMap<>();
        StringBuilder body = new StringBuilder();
        boolean isBody = false;

        // 解析其余行以获取请求头和请求体
        for (int i = 1; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            
            // 空行标志着请求头的结束和请求体的开始
            if (line.trim().isEmpty()) {
                isBody = true;
                continue;
            }
            
            if (!isBody) {
                // 解析请求头
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex).trim();
                    String headerValue = line.substring(colonIndex + 1).trim();
                    
                    // 替换变量
                    if (variables != null && !variables.isEmpty()) {
                        headerValue = replaceVariables(headerValue, variables);
                    }
                    
                    headers.put(headerName, headerValue);
                }
            } else {
                // 累积请求体
                body.append(line).append("\n");
            }
        }

        // 设置请求头和请求体
        requestObj.setHeaders(headers);
        
        String bodyContent = body.toString().trim();
        if (!bodyContent.isEmpty()) {
            // 替换请求体中的变量
            if (variables != null && !variables.isEmpty()) {
                bodyContent = replaceVariables(bodyContent, variables);
            }
            requestObj.setPostData(bodyContent);
        }

        // 根据Content-Type设置postMethod
        if (headers != null) {
            String contentType = headers.get("Content-Type");
            if (contentType != null) {
                if (contentType.contains("application/json")) {
                    requestObj.setPostMethod("JSON");
                } else if (contentType.contains("multipart/form-data")) {
                    requestObj.setPostMethod("FORM");
                } else {
                    requestObj.setPostMethod("RAW");
                }
            }
        }
    }

    /**
     * 替换字符串中的变量引用
     * @param input 输入字符串
     * @param variables 变量映射
     * @return 替换后的字符串
     */
    public static String replaceVariables(String input, Map<String, String> variables) {
        if (input == null || variables == null || variables.isEmpty()) {
            return input;
        }

        String result = input;
        
        // 替换形如 {{variable}} 的变量引用
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String varName = entry.getKey();
            String varValue = entry.getValue();
            
            if (varName != null && varValue != null) {
                result = result.replace("{{" + varName + "}}", varValue);
            }
        }
        
        return result;
    }

    /**
     * 处理特殊变量
     * @param requestObj 请求对象
     * @param specialLine 特殊变量行
     */
    public void processSpecialVariable(RequestObj requestObj, String specialLine) {
        if (requestObj == null || specialLine == null || specialLine.isEmpty()) {
            return;
        }

        // 分割特殊变量行，格式如：@timeout=10 @proxy=127.0.0.1:8080 @followRedirect=true
        String[] variables = specialLine.split("\\s+");

        for (String variable : variables) {
            if (!variable.startsWith("@")) {
                continue;
            }

            // 去掉@前缀
            String varWithoutPrefix = variable.substring(1);

            // 分割变量名和值
            String[] parts = varWithoutPrefix.split("=", 2);
            if (parts.length != 2) {
                System.err.println("特殊变量格式错误: " + variable + "，应为 @name=value 格式");
                continue;
            }

            String name = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            // 根据变量名设置请求对象的属性
            switch (name) {
                case "timeout":
                    try {
                        int timeout = Integer.parseInt(value);
                        if (timeout > 0) {
                            requestObj.setTimeOut(timeout);
                        } else {
                            System.err.println("超时值必须大于0: " + value);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("无效的超时值: " + value);
                    }
                    break;

                case "proxy":
                    requestObj.setProxies(value);
                    break;

                case "followredirect":
                case "followredirects":
                    requestObj.setFollowRedirects(Boolean.parseBoolean(value));
                    break;

                case "retries":
                    try {
                        int retries = Integer.parseInt(value);
                        requestObj.setRetries(retries);
                    } catch (NumberFormatException e) {
                        System.err.println("无效的重试次数: " + value);
                    }
                    break;

                case "retrywaittime":
                    try {
                        int retryWaitTime = Integer.parseInt(value);
                        requestObj.setRetryWaitTime(retryWaitTime);
                    } catch (NumberFormatException e) {
                        System.err.println("无效的重试等待时间: " + value);
                    }
                    break;

                case "randomuseragent":
                    requestObj.setRandomUserAgent(Boolean.parseBoolean(value));
                    break;

                case "nouseragent":
                    requestObj.setNoUserAgent(Boolean.parseBoolean(value));
                    break;

                case "strictsslvalidation":
                    requestObj.setStrictSslValidation(Boolean.parseBoolean(value));
                    break;

                case "proxiestype":
                    try {
                        requestObj.setProxiesType(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.err.println(e.getMessage());
                    }
                    break;

                default:
                    System.out.println("未知的特殊变量: " + name + "=" + value);
                    break;
            }
        }
    }
} 