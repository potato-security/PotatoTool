package com.potato.potatotool.content.redTeam.vulnScanner.event;

/**
 * 扫描信息事件
 * 用于传递指纹识别、筛选统计、聚类优化等信息
 * 替代 UnifiedVulnScanService 内部的 ScanEvent + ScanEventType
 */
public class ScanInfoEvent extends ScanEvent {
    private static final long serialVersionUID = 1L;

    public enum InfoType {
        FINGERPRINT_STARTED, FINGERPRINT_COMPLETED,
        DEDUPLICATION, POC_SELECTION, CLUSTERING_STATS,
        INFO, WARNING
    }

    private final InfoType infoType;
    private final String message;

    public ScanInfoEvent(Object source, String scanId, InfoType infoType, String message) {
        super(source, scanId);
        this.infoType = infoType;
        this.message = message;
    }

    public InfoType getInfoType() {
        return infoType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getEventType() {
        return "SCAN_INFO";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", infoType, message);
    }
}
