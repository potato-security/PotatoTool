package com.potato.potatotool.content.redTeam.vulnScanner.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 步骤执行记录
 * 用于记录POC执行过程中每个步骤的请求/响应详情
 *
 * @author Potato
 * @date 2025/01/07
 */
public class StepExecutionRecord {

    // 最大响应体长度限制（避免内存问题）
    private static final int MAX_RESPONSE_BODY_LENGTH = 5000;

    // ========== 步骤标识 ==========
    private int stepIndex;              // 步骤索引（从1开始）
    private String stepId;              // 步骤ID
    private String stepType;            // 步骤类型（http/dns/tcp/ssl/websocket/file/headless/code）

    // ========== 请求信息 ==========
    private String requestUrl;          // 请求URL（完整URL）
    private String requestMethod;       // 请求方法（GET/POST等）
    private Map<String, String> requestHeaders = new HashMap<>(); // 请求头
    private String requestBody;         // 请求体
    private String rawRequest;          // 原始HTTP请求文本

    // ========== 响应信息 ==========
    private int responseCode;           // 响应状态码
    private Map<String, String> responseHeaders = new HashMap<>(); // 响应头
    private String responseBody;        // 响应体（限制长度）
    private int responseBodyLength;     // 响应体原始长度
    private String rawResponse;         // 原始HTTP响应文本

    // ========== 执行结果 ==========
    private long responseTime;          // 响应时间（毫秒）
    private boolean matched;            // 是否匹配成功
    private List<String> matchedValues = new ArrayList<>(); // 匹配的值
    private Map<String, Object> extractedVariables = new HashMap<>(); // 提取的变量
    private String errorMessage;        // 错误信息

    // ========== 时间戳 ==========
    private long timestamp;             // 执行时间戳

    // ========== 构造方法 ==========

    public StepExecutionRecord() {
        this.timestamp = System.currentTimeMillis();
    }

    public StepExecutionRecord(int stepIndex, String stepId) {
        this();
        this.stepIndex = stepIndex;
        this.stepId = stepId;
    }

    // ========== 便捷方法 ==========

    /**
     * 构建原始HTTP请求文本
     */
    public void buildRawRequest() {
        StringBuilder sb = new StringBuilder();

        // 请求行
        if (requestMethod != null && requestUrl != null) {
            sb.append(requestMethod).append(" ").append(requestUrl).append(" HTTP/1.1\n");
        }

        // 请求头
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                sb.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
            }
        }

        // 请求体
        if (requestBody != null && !requestBody.isEmpty()) {
            sb.append("\n").append(requestBody);
        }

        this.rawRequest = sb.toString();
    }

    /**
     * 构建原始HTTP响应文本
     */
    public void buildRawResponse() {
        StringBuilder sb = new StringBuilder();

        // 状态行
        sb.append("HTTP/1.1 ").append(responseCode).append("\n");

        // 响应头（只保留关键头）
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
                String key = header.getKey();
                if (key != null && isImportantHeader(key)) {
                    sb.append(key).append(": ").append(header.getValue()).append("\n");
                }
            }
        }

        // 响应体
        if (responseBody != null && !responseBody.isEmpty()) {
            sb.append("\n").append(responseBody);
            if (responseBodyLength > responseBody.length()) {
                sb.append("\n... (truncated, total: ").append(responseBodyLength).append(" chars)");
            }
        }

        this.rawResponse = sb.toString();
    }

    /**
     * 判断是否为重要的响应头
     */
    private boolean isImportantHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.equals("content-type") ||
               lower.equals("server") ||
               lower.equals("set-cookie") ||
               lower.equals("location") ||
               lower.equals("x-powered-by") ||
               lower.equals("www-authenticate");
    }

    /**
     * 设置响应体（自动截断）
     */
    public void setResponseBodyWithLimit(String body) {
        if (body == null) {
            this.responseBody = null;
            this.responseBodyLength = 0;
            return;
        }

        this.responseBodyLength = body.length();
        if (body.length() > MAX_RESPONSE_BODY_LENGTH) {
            this.responseBody = body.substring(0, MAX_RESPONSE_BODY_LENGTH);
        } else {
            this.responseBody = body;
        }
    }

    /**
     * 添加匹配值
     */
    public void addMatchedValue(String value) {
        if (value != null && !value.isEmpty()) {
            this.matchedValues.add(value);
        }
    }

    /**
     * 添加提取的变量
     */
    public void addExtractedVariable(String name, Object value) {
        if (name != null && value != null) {
            this.extractedVariables.put(name, value);
        }
    }

    // ========== Getter/Setter ==========

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getRawRequest() {
        return rawRequest;
    }

    public void setRawRequest(String rawRequest) {
        this.rawRequest = rawRequest;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public int getResponseBodyLength() {
        return responseBodyLength;
    }

    public void setResponseBodyLength(int responseBodyLength) {
        this.responseBodyLength = responseBodyLength;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public List<String> getMatchedValues() {
        return matchedValues;
    }

    public void setMatchedValues(List<String> matchedValues) {
        this.matchedValues = matchedValues;
    }

    public Map<String, Object> getExtractedVariables() {
        return extractedVariables;
    }

    public void setExtractedVariables(Map<String, Object> extractedVariables) {
        this.extractedVariables = extractedVariables;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "StepExecutionRecord{" +
                "stepIndex=" + stepIndex +
                ", stepId='" + stepId + '\'' +
                ", stepType='" + stepType + '\'' +
                ", requestUrl='" + requestUrl + '\'' +
                ", responseCode=" + responseCode +
                ", matched=" + matched +
                ", responseTime=" + responseTime + "ms" +
                '}';
    }
}