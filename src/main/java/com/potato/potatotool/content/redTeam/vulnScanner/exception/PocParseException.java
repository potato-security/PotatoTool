package com.potato.potatotool.content.redTeam.vulnScanner.exception;

/**
 * POC解析异常
 * 当POC文件格式不正确或内容解析失败时抛出
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class PocParseException extends PocLoadException {
    private static final long serialVersionUID = 1L;
    
    private String pocFormat;
    private int lineNumber;
    
    public PocParseException(String message) {
        super(message);
    }
    
    public PocParseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PocParseException(String pocFile, String pocFormat, String message) {
        super(pocFile, message, null);
        this.pocFormat = pocFormat;
    }
    
    public PocParseException(String pocFile, String pocFormat, String message, Throwable cause) {
        super(pocFile, message, cause);
        this.pocFormat = pocFormat;
    }
    
    public PocParseException(String pocFile, String pocFormat, int lineNumber, String message) {
        super(pocFile, message, null);
        this.pocFormat = pocFormat;
        this.lineNumber = lineNumber;
    }
    
    public String getPocFormat() {
        return pocFormat;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (pocFormat != null) {
            sb.append(" [格式: ").append(pocFormat).append("]");
        }
        if (lineNumber > 0) {
            sb.append(" [行号: ").append(lineNumber).append("]");
        }
        return sb.toString();
    }
}

