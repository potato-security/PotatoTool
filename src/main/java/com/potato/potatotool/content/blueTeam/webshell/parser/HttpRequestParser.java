package com.potato.potatotool.content.blueTeam.webshell.parser;

import com.potato.potatotool.utils.data.JsonUtils;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * HTTP请求解析器
 * 负责从HTTP请求中提取需要解密的数据
 * 
 * @author Potato
 * @version 2.0
 */
public class HttpRequestParser {
    
    /**
     * 从HTTP请求中提取数据
     * 
     * @param content HTTP请求内容
     * @return 提取的数据
     */
    public String extractDataFromRequest(String content) {
        // 处理POST请求
        if (content.startsWith("POST ") && content.contains("Host")) {
            return extractPostData(content);
        }
        
        // 处理GET请求
        if (content.startsWith("GET ") && content.contains("Host")) {
            return extractGetData(content);
        }

        // 直接返回原内容
        return content;
    }
    
    /**
     * 提取POST请求数据
     */
    private String extractPostData(String content) {
        int startIndex = content.indexOf("\n\n") + 2;
        if (startIndex == 1) {
            startIndex = content.indexOf("\r\n\r\n") + 4;
        }
        
        if (startIndex == 3) {
            throw new IllegalArgumentException("请求头部与PostData衔接处换行字符存在问题，请修改");
        }
        
        return content.substring(startIndex);
    }
    
    /**
     * 提取GET请求数据
     */
    private String extractGetData(String content) {
        int startIndex = content.indexOf("?") + 1;
        int endIndex = content.indexOf(" HTTP/");
        
        if (startIndex == 0 || endIndex == -1) {
            throw new IllegalArgumentException("请求头部未检测到GetData开始和结束，请修改或手动提出");
        }
        
        return content.substring(startIndex, endIndex);
    }
    
    /**
     * 提取Shiro RememberMe值
     */
    public String extractShiroRememberMe(String content) {
        Pattern pattern = Pattern.compile("rememberMe=(\\S+)");
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "";
    }
    
    /**
     * 从HTTP请求中提取数据并解析为JSON对象
     * 
     * @param content HTTP请求内容
     * @return 包含提取数据和解析后JSON对象的结果
     */
    public RequestParseResult extractAndParseRequest(String content) {
        // 1. 提取数据
        String extractedData = extractDataFromRequest(content);
        
        // 2. 解析为JSON对象
        JSONObject jsonData = parseFormData(extractedData);
        
        return new RequestParseResult(extractedData, jsonData);
    }
    
    /**
     * 解析表单数据为JSON对象
     * 
     * @param postData 表单数据
     * @return JSON对象
     */
    public JSONObject parseFormData(String postData) {
        JSONObject jsonData = new JsonUtils.OrderedJSONObject();
        
        if (postData.contains("&") && !(postData.startsWith("{") && postData.endsWith("}"))) {
            // Form-data格式
            parseFormDataFormat(postData, jsonData);
        } else if (postData.startsWith("{") && postData.endsWith("}") && 
                   postData.contains("\"") && postData.contains(":")) {
            // JSON格式
            parseJsonFormat(postData, jsonData);
        }
        
        return jsonData;
    }
    
    /**
     * 解析表单格式数据
     */
    private void parseFormDataFormat(String postData, JSONObject jsonData) {
        String[] keyValuePairArray = postData.split("&");
        
        for (String keyValuePair : keyValuePairArray) {
            String[] keyValue = keyValuePair.split("=");
            
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                jsonData.put(key, value);
            } else {
                if (debugMode) {
                    System.out.println("分割存在异常：" + Arrays.toString(keyValue));
                    System.out.println("分割存在异常，该组参数等号个数：" + keyValue.length);
                }
                jsonData.clear();
                break;
            }
        }
    }
    
    /**
     * 解析JSON格式数据
     */
    private void parseJsonFormat(String postData, JSONObject jsonData) {
        try {
            JSONObject parsed = new JsonUtils.OrderedJSONObject(postData);
            // 复制所有键值对
            for (String key : parsed.keySet()) {
                jsonData.put(key, parsed.get(key));
            }
        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
                System.out.println("json格式错误，确定是json格式么？");
            }
        }
    }
}