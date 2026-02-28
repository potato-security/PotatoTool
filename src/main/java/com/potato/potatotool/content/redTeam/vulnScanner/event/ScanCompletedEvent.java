package com.potato.potatotool.content.redTeam.vulnScanner.event;

import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import java.util.List;

/**
 * 扫描完成事件
 * 当扫描任务完成时触发
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ScanCompletedEvent extends ScanEvent {
    private static final long serialVersionUID = 1L;
    
    private final List<ScanResult> results;
    private final long duration;
    private final boolean success;
    
    public ScanCompletedEvent(Object source, String scanId, List<ScanResult> results, long duration, boolean success) {
        super(source, scanId);
        this.results = results;
        this.duration = duration;
        this.success = success;
    }
    
    public List<ScanResult> getResults() {
        return results;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取发现的漏洞数量
     */
    public int getVulnerabilityCount() {
        if (results == null) {
            return 0;
        }
        return (int) results.stream().filter(r -> r.isVulnerable()).count();
    }
    
    @Override
    public String getEventType() {
        return "SCAN_COMPLETED";
    }
    
    @Override
    public String toString() {
        return String.format("ScanCompletedEvent[scanId=%s, results=%d, vulnerabilities=%d, duration=%dms, success=%s]", 
            getScanId(), results != null ? results.size() : 0, getVulnerabilityCount(), duration, success);
    }
}

