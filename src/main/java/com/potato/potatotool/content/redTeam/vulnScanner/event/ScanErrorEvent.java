package com.potato.potatotool.content.redTeam.vulnScanner.event;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslConstants;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 扫描错误事件
 * 当扫描过程中发生错误时触发
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ScanErrorEvent extends ScanEvent {
    private static final long serialVersionUID = 1L;
    
    private final String errorMessage;
    private final Throwable exception;
    private final String target;
    private final String pocId;
    
    public ScanErrorEvent(Object source, String scanId, String errorMessage, Throwable exception) {
        this(source, scanId, null, null, errorMessage, exception);
    }
    
    public ScanErrorEvent(Object source, String scanId, String target, String pocId, 
                         String errorMessage, Throwable exception) {
        super(source, scanId);
        this.target = target;
        this.pocId = pocId;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public String getTarget() {
        return target;
    }
    
    public String getPocId() {
        return pocId;
    }
    
    @Override
    public String getEventType() {
        return "SCAN_ERROR";
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ScanErrorEvent[scanId=").append(getScanId());
        if (target != null) {
            sb.append(", target=").append(target);
        }
        if (pocId != null) {
            sb.append(", pocId=").append(pocId);
        }
        sb.append(", error=").append(errorMessage).append("]");
        return sb.toString();
    }

    public static void logUnrecognizedExpression(String expression) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DslConstants.ERROR_LOG_FILE, true))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            writer.println("[" + timestamp + "] 扫描错误: " + expression);
        } catch (IOException e) {
            System.err.println("无法写入错误日志文件: " + e.getMessage());
        }
    }
}

