package com.potato.potatotool.content.redTeam.vulnScanner.exception;

/**
 * 扫描执行异常
 * 当扫描过程中发生错误时抛出
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ScanExecutionException extends VulnScanException {
    private static final long serialVersionUID = 1L;
    
    private String target;
    private String pocId;
    
    public ScanExecutionException(String message) {
        super(message);
    }
    
    public ScanExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ScanExecutionException(String target, String pocId, String message) {
        super(message);
        this.target = target;
        this.pocId = pocId;
    }
    
    public ScanExecutionException(String target, String pocId, String message, Throwable cause) {
        super(message, cause);
        this.target = target;
        this.pocId = pocId;
    }
    
    public String getTarget() {
        return target;
    }
    
    public String getPocId() {
        return pocId;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("扫描执行异常");
        if (target != null) {
            sb.append(" [目标: ").append(target).append("]");
        }
        if (pocId != null) {
            sb.append(" [POC: ").append(pocId).append("]");
        }
        sb.append(": ").append(super.getMessage());
        return sb.toString();
    }
}

