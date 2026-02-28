package com.potato.potatotool.content.redTeam.vulnScanner.event;

/**
 * 扫描开始事件
 * 当扫描任务开始时触发
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ScanStartedEvent extends ScanEvent {
    private static final long serialVersionUID = 1L;
    
    private final int totalTargets;
    private final int totalPocs;
    private final int totalTasks;
    
    public ScanStartedEvent(Object source, String scanId, int totalTargets, int totalPocs, int totalTasks) {
        super(source, scanId);
        this.totalTargets = totalTargets;
        this.totalPocs = totalPocs;
        this.totalTasks = totalTasks;
    }
    
    public int getTotalTargets() {
        return totalTargets;
    }
    
    public int getTotalPocs() {
        return totalPocs;
    }
    
    public int getTotalTasks() {
        return totalTasks;
    }
    
    @Override
    public String getEventType() {
        return "SCAN_STARTED";
    }
    
    @Override
    public String toString() {
        return String.format("ScanStartedEvent[scanId=%s, targets=%d, pocs=%d, tasks=%d]", 
            getScanId(), totalTargets, totalPocs, totalTasks);
    }
}

