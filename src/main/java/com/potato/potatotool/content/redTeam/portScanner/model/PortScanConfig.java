package com.potato.potatotool.content.redTeam.portScanner.model;

import com.potato.potatotool.content.redTeam.portScanner.port.PortPresets;

public class PortScanConfig {
    private int[] ports = PortPresets.top100();
    private int connectTimeoutMs = 1500;
    private int probeTimeoutMs = 3000;
    private int batchSize = 1024;
    private int queueMultiplier = 4;
    private int publicRateLimit = 200;
    private boolean publicMode;
    private boolean serviceProbe = true;
    private boolean autoHandoff;
    private boolean saveClosed;
    private long maxHostsBeforeConfirm = 65536L;
    private long maxTasksBeforeConfirm = 10000000L;

    public int[] getPorts() {
        return ports;
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getProbeTimeoutMs() {
        return probeTimeoutMs;
    }

    public void setProbeTimeoutMs(int probeTimeoutMs) {
        this.probeTimeoutMs = probeTimeoutMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getQueueMultiplier() {
        return queueMultiplier;
    }

    public void setQueueMultiplier(int queueMultiplier) {
        this.queueMultiplier = queueMultiplier;
    }

    public int getPublicRateLimit() {
        return publicRateLimit;
    }

    public void setPublicRateLimit(int publicRateLimit) {
        this.publicRateLimit = publicRateLimit;
    }

    public boolean isPublicMode() {
        return publicMode;
    }

    public void setPublicMode(boolean publicMode) {
        this.publicMode = publicMode;
    }

    public boolean isServiceProbe() {
        return serviceProbe;
    }

    public void setServiceProbe(boolean serviceProbe) {
        this.serviceProbe = serviceProbe;
    }

    public boolean isAutoHandoff() {
        return autoHandoff;
    }

    public void setAutoHandoff(boolean autoHandoff) {
        this.autoHandoff = autoHandoff;
    }

    public boolean isSaveClosed() {
        return saveClosed;
    }

    public void setSaveClosed(boolean saveClosed) {
        this.saveClosed = saveClosed;
    }

    public long getMaxHostsBeforeConfirm() {
        return maxHostsBeforeConfirm;
    }

    public void setMaxHostsBeforeConfirm(long maxHostsBeforeConfirm) {
        this.maxHostsBeforeConfirm = maxHostsBeforeConfirm;
    }

    public long getMaxTasksBeforeConfirm() {
        return maxTasksBeforeConfirm;
    }

    public void setMaxTasksBeforeConfirm(long maxTasksBeforeConfirm) {
        this.maxTasksBeforeConfirm = maxTasksBeforeConfirm;
    }
}
