package com.potato.potatotool.content.redTeam.portScanner.event;

public class PortScanStartedEvent extends PortScanEvent {
    private final long totalTasks;

    public PortScanStartedEvent(String scanId, long totalTasks) {
        super(scanId);
        this.totalTasks = totalTasks;
    }

    public long getTotalTasks() {
        return totalTasks;
    }
}
