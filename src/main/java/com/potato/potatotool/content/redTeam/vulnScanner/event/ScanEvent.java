package com.potato.potatotool.content.redTeam.vulnScanner.event;

import java.util.EventObject;

/**
 * 扫描事件基类
 * 所有扫描相关的事件都应该继承此类
 * 
 * @author Potato
 * @date 2024-10-28
 */
public abstract class ScanEvent extends EventObject {
    private static final long serialVersionUID = 1L;
    
    private final long timestamp;
    private final String scanId;
    
    public ScanEvent(Object source, String scanId) {
        super(source);
        this.scanId = scanId;
        this.timestamp = System.currentTimeMillis();
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getScanId() {
        return scanId;
    }
    
    /**
     * 获取事件类型描述
     */
    public abstract String getEventType();
}

