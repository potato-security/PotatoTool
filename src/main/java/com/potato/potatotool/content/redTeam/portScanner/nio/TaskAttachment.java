package com.potato.potatotool.content.redTeam.portScanner.nio;

import com.potato.potatotool.content.redTeam.portScanner.model.ScanTask;

import java.nio.channels.SelectionKey;

public class TaskAttachment {
    private final ScanTask task;
    private final long startTime;
    private final long deadlineMs;
    private SelectionKey key;
    long deadlineKey;

    public TaskAttachment(ScanTask task, long startTime, long deadlineMs) {
        this.task = task;
        this.startTime = startTime;
        this.deadlineMs = deadlineMs;
    }

    public ScanTask getTask() {
        return task;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDeadlineMs() {
        return deadlineMs;
    }

    public SelectionKey getKey() {
        return key;
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }
}
