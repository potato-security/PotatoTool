package com.potato.potatotool.content.redTeam.vulnScanner.exception;

/**
 * 配置异常
 * 当配置错误或缺失时抛出
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ConfigurationException extends VulnScanException {
    private static final long serialVersionUID = 1L;
    
    private String configKey;
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ConfigurationException(String configKey, String message) {
        super(message);
        this.configKey = configKey;
    }
    
    public String getConfigKey() {
        return configKey;
    }
    
    @Override
    public String getMessage() {
        if (configKey != null) {
            return "配置错误 [" + configKey + "]: " + super.getMessage();
        }
        return super.getMessage();
    }
}

