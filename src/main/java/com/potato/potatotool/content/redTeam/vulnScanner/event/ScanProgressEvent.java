package com.potato.potatotool.content.redTeam.vulnScanner.event;

/**
 * 扫描进度事件
 * 定期触发，报告扫描进度
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ScanProgressEvent extends ScanEvent {
    private static final long serialVersionUID = 1L;
    
    private final int completedTasks;
    private final int totalTasks;
    private final int vulnerabilitiesFound;
    
    public ScanProgressEvent(Object source, String scanId, int completedTasks, int totalTasks, int vulnerabilitiesFound) {
        super(source, scanId);
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.vulnerabilitiesFound = vulnerabilitiesFound;
    }
    
    public int getCompletedTasks() {
        return completedTasks;
    }
    
    public int getTotalTasks() {
        return totalTasks;
    }
    
    public int getVulnerabilitiesFound() {
        return vulnerabilitiesFound;
    }
    
    /**
     * 获取进度百分比（0-100）
     */
    public int getProgressPercentage() {
        if (totalTasks == 0) {
            return 0;
        }
        return (int) ((completedTasks * 100.0) / totalTasks);
    }
    
    @Override
    public String getEventType() {
        return "SCAN_PROGRESS";
    }
    
    @Override
    public String toString() {
        return String.format("ScanProgressEvent[scanId=%s, progress=%d/%d (%d%%), vulnerabilities=%d]", 
            getScanId(), completedTasks, totalTasks, getProgressPercentage(), vulnerabilitiesFound);
    }
}

