package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import java.util.Map;

/**
 * 数据库统计信息
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class DatabaseStatistics {
    private int totalScans;
    private int totalVulns;
    private Map<String, Integer> severityStats;
    
    public int getTotalScans() { return totalScans; }
    public void setTotalScans(int totalScans) { this.totalScans = totalScans; }
    
    public int getTotalVulns() { return totalVulns; }
    public void setTotalVulns(int totalVulns) { this.totalVulns = totalVulns; }
    
    public Map<String, Integer> getSeverityStats() { return severityStats; }
    public void setSeverityStats(Map<String, Integer> severityStats) { 
        this.severityStats = severityStats; 
    }
}


