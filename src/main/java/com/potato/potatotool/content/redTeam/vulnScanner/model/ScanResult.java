package com.potato.potatotool.content.redTeam.vulnScanner.model;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.util.HashMap;
import java.util.Map;

/**
 * 漏洞扫描结果类
 */
public class ScanResult {
    private String target; // 目标URL
    private PocObj.Poc poc; // 匹配的POC
    private boolean vulnerable; // 是否存在漏洞
    private long timestamp; // 扫描时间戳
    private Map<String, Object> details = new HashMap<>(); // 详细信息

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public PocObj.Poc getPoc() {
        return poc;
    }

    public void setPoc(PocObj.Poc poc) {
        this.poc = poc;
    }

    public boolean isVulnerable() {
        return vulnerable;
    }

    public void setVulnerable(boolean vulnerable) {
        this.vulnerable = vulnerable;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "target='" + target + '\'' +
                ", poc=" + (poc != null ? poc.getName() : "null") +
                ", vulnerable=" + vulnerable +
                ", timestamp=" + timestamp +
                '}';
    }
} 