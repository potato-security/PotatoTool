package com.potato.potatotool.content.redTeam.portScanner.event;

public class PortScanProgressEvent extends PortScanEvent {
    private final long completedTasks;
    private final long totalTasks;
    private final int openCount;

    public PortScanProgressEvent(String scanId, long completedTasks, long totalTasks, int openCount) {
        super(scanId);
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.openCount = openCount;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public int getOpenCount() {
        return openCount;
    }

    public double getProgressPercentage() {
        if (totalTasks <= 0) {
            return 0;
        }
        return Math.min(100.0, completedTasks * 100.0 / totalTasks);
    }
}
