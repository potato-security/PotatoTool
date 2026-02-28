package com.potato.potatotool.content.redTeam.vulnScanner.exception;

/**
 * POC加载异常
 * 当POC文件加载、解析失败时抛出
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class PocLoadException extends VulnScanException {
    private static final long serialVersionUID = 1L;
    
    private String pocFile;
    
    public PocLoadException(String message) {
        super(message);
    }
    
    public PocLoadException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PocLoadException(String pocFile, String message, Throwable cause) {
        super(message, cause);
        this.pocFile = pocFile;
    }
    
    public String getPocFile() {
        return pocFile;
    }
    
    @Override
    public String getMessage() {
        if (pocFile != null) {
            return "POC文件加载失败 [" + pocFile + "]: " + super.getMessage();
        }
        return super.getMessage();
    }
}

