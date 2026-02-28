package com.potato.potatotool.content.redTeam.vulnScanner.storage;

/**
 * 漏洞详情
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class VulnDetail {
    private long id;
    private long scanId;
    private String target;
    private String pocId;
    private String pocName;
    private String pocFormat;
    private String severity;
    private String vulnType;
    private String protocol;
    private String description;
    private String foundTime;
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getScanId() { return scanId; }
    public void setScanId(long scanId) { this.scanId = scanId; }
    
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public String getPocId() { return pocId; }
    public void setPocId(String pocId) { this.pocId = pocId; }
    
    public String getPocName() { return pocName; }
    public void setPocName(String pocName) { this.pocName = pocName; }
    
    public String getPocFormat() { return pocFormat; }
    public void setPocFormat(String pocFormat) { this.pocFormat = pocFormat; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getVulnType() { return vulnType; }
    public void setVulnType(String vulnType) { this.vulnType = vulnType; }
    
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getFoundTime() { return foundTime; }
    public void setFoundTime(String foundTime) { this.foundTime = foundTime; }
}


