package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import java.util.Map;

/**
 * 扫描历史记录
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class ScanHistory {
    private long id;
    private String scanTime;
    private int targetCount;
    private int pocCount;
    private int vulnCount;
    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private int infoCount;
    private long duration;
    private String status;
    private Map<String, Object> config;
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getScanTime() { return scanTime; }
    public void setScanTime(String scanTime) { this.scanTime = scanTime; }
    
    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }
    
    public int getPocCount() { return pocCount; }
    public void setPocCount(int pocCount) { this.pocCount = pocCount; }
    
    public int getVulnCount() { return vulnCount; }
    public void setVulnCount(int vulnCount) { this.vulnCount = vulnCount; }
    
    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
    
    public int getHighCount() { return highCount; }
    public void setHighCount(int highCount) { this.highCount = highCount; }
    
    public int getMediumCount() { return mediumCount; }
    public void setMediumCount(int mediumCount) { this.mediumCount = mediumCount; }
    
    public int getLowCount() { return lowCount; }
    public void setLowCount(int lowCount) { this.lowCount = lowCount; }
    
    public int getInfoCount() { return infoCount; }
    public void setInfoCount(int infoCount) { this.infoCount = infoCount; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
}


