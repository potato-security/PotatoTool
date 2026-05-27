package com.potato.potatotool.content.redTeam.portScanner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PortScanResult {
    private String scanId;
    private long startTime;
    private long endTime;
    private long totalTasks;
    private long completedTasks;
    private final List<PortResult> results = new ArrayList<>();

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(long completedTasks) {
        this.completedTasks = completedTasks;
    }

    public synchronized void addOrUpdate(PortResult result) {
        for (int i = 0; i < results.size(); i++) {
            PortResult current = results.get(i);
            if (current.getHost().equals(result.getHost()) && current.getPort() == result.getPort()) {
                results.set(i, result);
                return;
            }
        }
        results.add(result);
    }

    public synchronized List<PortResult> getResults() {
        return Collections.unmodifiableList(new ArrayList<>(results));
    }

    public synchronized List<PortResult> getOpenResults() {
        List<PortResult> open = new ArrayList<>();
        for (PortResult result : results) {
            if (result.getState() == PortState.OPEN) {
                open.add(result);
            }
        }
        return open;
    }
}
