package com.potato.potatotool.content.redTeam.portScanner.event;

public abstract class PortScanEvent {
    private final String scanId;
    private final long timestamp;

    protected PortScanEvent(String scanId) {
        this.scanId = scanId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getScanId() {
        return scanId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
