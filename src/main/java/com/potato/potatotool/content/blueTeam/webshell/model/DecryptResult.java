package com.potato.potatotool.content.blueTeam.webshell.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解密结果类
 * 封装解密操作的结果信息
 * 
 * @author Potato
 * @version 2.0
 */
public class DecryptResult {
    
    private final boolean success;
    private final String data;
    private final String errorMessage;
    private final List<List<String>> encodeModes;
    
    private DecryptResult(boolean success, String data, String errorMessage, List<List<String>> encodeModes) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.encodeModes = encodeModes != null ? encodeModes : new ArrayList<>();
    }
    
    /**
     * 创建成功结果
     */
    public static DecryptResult success(String data, List<List<String>> encodeModes) {
        return new DecryptResult(true, data, null, encodeModes);
    }
    
    /**
     * 创建失败结果
     */
    public static DecryptResult error(String errorMessage) {
        return new DecryptResult(false, null, errorMessage, null);
    }
    
    /**
     * 转换为Map格式（兼容原有接口）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("error", success ? 0 : 1);
        result.put("data", success ? data : errorMessage);
        result.put("encodeModeList", new ArrayList<>(encodeModes));
        return result;
    }
    
    // Getters
    public boolean isSuccess() {
        return success;
    }
    
    public String getData() {
        return data;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public List<List<String>> getEncodeModes() {
        return encodeModes;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "DecryptResult{success=true, encodeModes=" + encodeModes + "}";
        } else {
            return "DecryptResult{success=false, error='" + errorMessage + "'}";
        }
    }
}