package com.potato.potatotool.content.redTeam.vulnScanner.model;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import java.util.Map;
import java.util.HashMap;

/**
 * 漏洞扫描配置类
 */
public class ScanConfig {
    private int threads = 20; // 默认线程数
    private String protocol; // 协议类型过滤
    private PocObj.Severity severity; // 严重程度过滤
    private boolean debug = false; // 是否开启调试模式
    private String proxy; // 全局代理
    private int timeout = 15; // HTTP请求超时时间（秒），用于连接和读取超时设置
    private int retries = 2; // 重试次数
    private boolean followRedirects = true; // 是否跟随重定向
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"; // 用户代理
    private Map<String, String> headers = new HashMap<>(); // 请求头
    private int maxResponseSize = 1024 * 1024; // 最大响应大小（字节），默认1MB

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public PocObj.Severity getSeverity() {
        return severity;
    }

    public void setSeverity(PocObj.Severity severity) {
        this.severity = severity;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public int getMaxResponseSize() {
        return maxResponseSize;
    }

    public void setMaxResponseSize(int maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
    }
}