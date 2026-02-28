package com.potato.potatotool.content.redTeam.vulnScanner.exception;

/**
 * 漏洞扫描基础异常类
 * 所有漏洞扫描相关的异常都应该继承此类
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class VulnScanException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private String errorCode;
    private Object[] args;
    
    public VulnScanException(String message) {
        super(message);
    }
    
    public VulnScanException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public VulnScanException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public VulnScanException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public VulnScanException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getArgs() {
        return args;
    }
}

