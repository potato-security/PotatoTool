package com.potato.potatotool.content.redTeam.vulnScanner.model;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

/**
 * 漏洞扫描配置类
 */
public class ScanConfig {
    private int threads = 10; // 默认线程数
    private String protocol; // 协议类型过滤
    private PocObj.Severity severity; // 严重程度过滤
    private boolean debug = false; // 是否开启调试模式
    private String proxy; // 全局代理
    private int timeOut = 30; // 默认超时时间（秒）

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

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }
} 