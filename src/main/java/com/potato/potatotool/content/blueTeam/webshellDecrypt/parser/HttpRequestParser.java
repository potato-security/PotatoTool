package com.potato.potatotool.content.blueTeam.webshellDecrypt.parser;

import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.JsonUtils;
import com.potato.potatotool.utils.data.StrUtils;
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
            throw new IllegalArgumentException(I18nUtils.getString("webshell.error.post.header"));
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
            throw new IllegalArgumentException(I18nUtils.getString("webshell.error.get.header"));
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

        // JSON格式判断（优先级最高）
        if (postData.startsWith("{") && postData.endsWith("}") &&
            postData.contains("\"") && postData.contains(":")) {
            parseJsonFormat(postData, jsonData);
            return jsonData;
        }

        // Form-data格式判断
        if (isFormDataFormat(postData)) {
            parseFormDataFormat(postData, jsonData);
        }

        return jsonData;
    }

    /**
     * 判断是否是表单数据格式
     * <p>
     * 区分表单数据和纯数据（如Base64字符串）：
     * - 表单数据: data=xxx 或 data=xxx&foo=bar
     * - 纯数据: HQAAAAJuYW1lAAUAAAB0ZXN0ABBhZ2UAGQAAAAA= 或 fL1tMGI4YTljO/79NDQm7r9PZzBiOA==
     * </p>
     *
     * @param postData 待判断的数据
     * @return true表示是表单数据格式
     */
    private boolean isFormDataFormat(String postData) {
        // 1. 如果包含 & 符号，很可能是表单数据（多个参数）
        if (postData.contains("&")) {
            return true;
        }

        // 2. 如果不包含 =，肯定不是表单数据
        if (!postData.contains("=")) {
            return false;
        }

        // 3. 单个参数情况，需要判断是 key=value 还是纯数据
        int firstEqualIndex = postData.indexOf("=");

        // 3.1 如果 =在开头||参数名过长||=在结尾 ，不是表单数据
        if (firstEqualIndex == 0 || firstEqualIndex > 30 || firstEqualIndex == postData.length()-1 ) {
            return false;
        }

        // 3.2 检查 = 前面的内容
        String beforeEqual = postData.substring(0, firstEqualIndex);

        // 3.3 如果包含 Base64 特殊字符（/ + 等），很可能是纯数据而不是参数名
        if (beforeEqual.contains("/") || beforeEqual.contains("+")) {
            return false;
        }

        // 3.4 如果 = 在末尾附近（最后1-2个字符），可能是 Base64 填充符
        if (firstEqualIndex >= postData.length() - 2) {
            // 进一步检查是否符合 Base64 格式
            if (isLikelyBase64(postData)) {
                return false;
            }
        }

        // 3.5 检查是否匹配参数名模式（字母、数字、下划线、中划线、点号）
        return beforeEqual.matches("[a-zA-Z0-9_\\-\\.]+");
    }

    /**
     * 检查字符串是否像 Base64 格式
     * <p>
     * Base64 特征：
     * - 只包含 [A-Za-z0-9+/=]
     * - = 只能在末尾，最多2个
     * </p>
     */
    private boolean isLikelyBase64(String str) {
        // Base64 只包含 [A-Za-z0-9+/=]
        if (!str.matches("^[A-Za-z0-9+/]+=*$")) {
            return false;
        }

        // 检查 = 只在末尾
        int firstEqual = str.indexOf("=");
        if (firstEqual == -1) {
            return true; // 没有 =，也是合法的 Base64
        }

        // = 后面只能是 =
        String afterEqual = str.substring(firstEqual);
        return afterEqual.matches("^=+$") && afterEqual.length() <= 2;
    }
    
    /**
     * 解析表单格式数据
     */
    private void parseFormDataFormat(String postData, JSONObject jsonData) {
        String[] keyValuePairArray = postData.split("&");

        for (String keyValuePair : keyValuePairArray) {
            // 使用 split("=", 2) 限制分割次数，避免值中的 = 被分割
            String[] keyValue = keyValuePair.split("=", 2);

            if (keyValue.length == 2) {
                String key = StrUtils.urlDecode(keyValue[0]);
                String value = StrUtils.urlDecode(keyValue[1]);
                jsonData.put(key, value);
            } else if (keyValue.length == 1 && !keyValue[0].isEmpty()) {
                // 处理没有值的参数，如 data= 或 data
                String key = StrUtils.urlDecode(keyValue[0]);
                jsonData.put(key, "");
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