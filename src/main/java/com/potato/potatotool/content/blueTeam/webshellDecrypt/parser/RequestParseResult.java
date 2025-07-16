package com.potato.potatotool.content.blueTeam.webshellDecrypt.parser;

import org.json.JSONObject;

/**
 * HTTP请求解析结果
 * 封装提取的数据和解析后的JSON对象
 * 
 * @author Potato
 * @version 2.0
 */
public class RequestParseResult {
    private final String extractedData;
    private final JSONObject jsonData;
    
    public RequestParseResult(String extractedData, JSONObject jsonData) {
        this.extractedData = extractedData;
        this.jsonData = jsonData;
    }
    
    /**
     * 获取提取的原始数据
     * 
     * @return 提取的数据
     */
    public String getExtractedData() {
        return extractedData;
    }
    
    /**
     * 获取解析后的JSON对象
     * 
     * @return JSON对象
     */
    public JSONObject getJsonData() {
        return jsonData;
    }
    
    /**
     * 检查是否有JSON数据
     * 
     * @return 如果JSON对象不为空且有数据则返回true
     */
    public boolean hasJsonData() {
        return jsonData != null && jsonData.length() > 0;
    }
}