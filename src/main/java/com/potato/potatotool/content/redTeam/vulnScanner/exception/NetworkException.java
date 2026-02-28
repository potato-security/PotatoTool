package com.potato.potatotool.content.redTeam.vulnScanner.exception;

/**
 * 网络异常
 * 当网络请求失败时抛出
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class NetworkException extends ScanExecutionException {
    private static final long serialVersionUID = 1L;
    
    public enum NetworkErrorType {
        CONNECTION_TIMEOUT("连接超时"),
        READ_TIMEOUT("读取超时"),
        CONNECTION_REFUSED("连接被拒绝"),
        UNKNOWN_HOST("未知主机"),
        SSL_ERROR("SSL错误"),
        PROXY_ERROR("代理错误"),
        OTHER("其他网络错误");
        
        private final String description;
        
        NetworkErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private NetworkErrorType errorType;
    
    public NetworkException(String message) {
        super(message);
        this.errorType = NetworkErrorType.OTHER;
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = determineErrorType(cause);
    }
    
    public NetworkException(NetworkErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public NetworkException(NetworkErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    public NetworkException(String target, NetworkErrorType errorType, String message) {
        super(target, null, message);
        this.errorType = errorType;
    }
    
    public NetworkErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * 根据异常类型判断网络错误类型
     */
    private static NetworkErrorType determineErrorType(Throwable cause) {
        if (cause == null) {
            return NetworkErrorType.OTHER;
        }
        
        String message = cause.getMessage();
        if (message == null) {
            return NetworkErrorType.OTHER;
        }
        
        message = message.toLowerCase();
        
        if (message.contains("connection timed out") || message.contains("connect timeout")) {
            return NetworkErrorType.CONNECTION_TIMEOUT;
        } else if (message.contains("read timed out") || message.contains("sockettimeoutexception")) {
            return NetworkErrorType.READ_TIMEOUT;
        } else if (message.contains("connection refused") || message.contains("connectexception")) {
            return NetworkErrorType.CONNECTION_REFUSED;
        } else if (message.contains("unknown host") || message.contains("unknownhostexception")) {
            return NetworkErrorType.UNKNOWN_HOST;
        } else if (message.contains("ssl") || message.contains("certificate")) {
            return NetworkErrorType.SSL_ERROR;
        } else if (message.contains("proxy")) {
            return NetworkErrorType.PROXY_ERROR;
        }
        
        return NetworkErrorType.OTHER;
    }
    
    @Override
    public String getMessage() {
        return errorType.getDescription() + ": " + super.getMessage();
    }
}

