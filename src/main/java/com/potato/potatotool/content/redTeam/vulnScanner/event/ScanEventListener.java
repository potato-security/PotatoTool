package com.potato.potatotool.content.redTeam.vulnScanner.event;

import java.util.EventListener;

/**
 * 扫描事件监听器接口
 * 
 * @author Potato
 * @date 2024-10-28
 */
public interface ScanEventListener extends EventListener {
    
    /**
     * 扫描开始事件处理
     */
    default void onScanStarted(ScanStartedEvent event) {}
    
    /**
     * 扫描进度事件处理
     */
    default void onScanProgress(ScanProgressEvent event) {}
    
    /**
     * 漏洞发现事件处理
     */
    default void onVulnerabilityFound(VulnerabilityFoundEvent event) {}
    
    /**
     * 扫描完成事件处理
     */
    default void onScanCompleted(ScanCompletedEvent event) {}
    
    /**
     * 扫描错误事件处理
     */
    default void onScanError(ScanErrorEvent event) {}

    /**
     * 扫描信息事件处理（指纹识别、筛选统计等）
     */
    default void onScanInfo(ScanInfoEvent event) {}
}

