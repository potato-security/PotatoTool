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
    private final ScanPhase phase;
    private final int phaseCompleted;
    private final int phaseTotal;
    private final double overallProgress;
    private final String message;
    
    public ScanProgressEvent(Object source, String scanId, int completedTasks, int totalTasks, int vulnerabilitiesFound) {
        this(source, scanId, completedTasks, totalTasks, vulnerabilitiesFound,
            ScanPhase.SCANNING, completedTasks, totalTasks,
            totalTasks > 0 ? (completedTasks * 1.0d / totalTasks) : 0.0d, null);
    }

    public ScanProgressEvent(Object source, String scanId, int completedTasks, int totalTasks,
                             int vulnerabilitiesFound, ScanPhase phase, int phaseCompleted,
                             int phaseTotal, double overallProgress, String message) {
        super(source, scanId);
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.vulnerabilitiesFound = vulnerabilitiesFound;
        this.phase = phase;
        this.phaseCompleted = phaseCompleted;
        this.phaseTotal = phaseTotal;
        this.overallProgress = clamp(overallProgress);
        this.message = message;
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

    public ScanPhase getPhase() {
        return phase;
    }

    public int getPhaseCompleted() {
        return phaseCompleted;
    }

    public int getPhaseTotal() {
        return phaseTotal;
    }

    public double getOverallProgress() {
        return overallProgress;
    }

    public String getMessage() {
        return message;
    }
    
    /**
     * 获取进度百分比（0-100）
     */
    public int getProgressPercentage() {
        return (int) (clamp(overallProgress) * 100);
    }
    
    @Override
    public String getEventType() {
        return "SCAN_PROGRESS";
    }
    
    @Override
    public String toString() {
        return String.format("ScanProgressEvent[scanId=%s, phase=%s, progress=%d/%d (%d%%), vulnerabilities=%d, message=%s]",
            getScanId(), phase, phaseCompleted, phaseTotal, getProgressPercentage(), vulnerabilitiesFound, message);
    }

    private double clamp(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }
        if (value > 1.0d) {
            return 1.0d;
        }
        return value;
    }
}
