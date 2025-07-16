package com.potato.potatotool.utils.network;

import com.google.gson.JsonObject;
import com.potato.potatotool.utils.core.Constants;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Potato
 * @date 2023/4/13 14:03
 */

/**
 * url               请求URL
 * method            请求方式（默：GET）
 * headers           请求头
 * followRedirects   是否允许重定向（默：否）
 * randomUserAgent   是否随机UA头（默：是）
 * noUserAgent       是否不设置UA头（默：否）
 * proxies           代理（如携带代理类型会自动提取）
 * proxiesType       代理类型（默：HTTP）
 * timeOut           请求和读取超时时间
 * maxRetries        服务器异常/无响应时重放次数
 * retryWaitTime     重放间隔（秒）
 * postMethod        POST请求传输模式（默：Raw）
 * postData          POST数据byte[]
 * formParameters    form表单Map格式
 * file              上传的文件File
 */
public class RequestObj {
    private String method = "GET";
    private String url;
    private Map<String, String> headers;
    private String bearerToken = "";
    private boolean randomUserAgent = true;
    private boolean followRedirects = false;
    private String postMethod = "Raw";
    private byte[] postData;
    private String proxiesType = "HTTP";
    private String proxies;
    private int timeOut = 10;
    private File file;
    private Map<String, Object> formParameters;
    private int retries = 1;
    private int retryWaitTime = 1;
    private boolean noUserAgent = false;
    private int maxResponseSize = Integer.MAX_VALUE;

    public RequestObj(){
        initializeProxySettings();
    }

    private void initializeProxySettings() {
        //  初始化默认代理配置
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Proxy");
        boolean enable = tmpJsonObj.getAsJsonPrimitive("enable").getAsBoolean();
        String address = tmpJsonObj.getAsJsonPrimitive("address").getAsString();
        if(enable && address.length() > 0){
            setProxies(address);
        }
    }


    public RequestObj setMethod(String method) {
        validateMethod(method);
        this.method = method.toUpperCase();
        return this;
    }

    private static final String[] VALID_METHODS = {"GET", "POST", "OPTIONS", "PUT", "DELETE", "HEAD"};
    
    private void validateMethod(String method) {
        if (!Arrays.asList(VALID_METHODS).contains(method.toUpperCase())) {
            throw new IllegalArgumentException("[×] 不支持的请求方法: " + method);
        }
    }

    public String getMethod() {
        return this.method;
    }

    public RequestObj setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUrl() {
        return this.url;
    }

    public RequestObj setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public RequestObj setRandomUserAgent(boolean randomUserAgent) {
        this.randomUserAgent = randomUserAgent;
        return this;
    }

    public boolean getRandomUserAgent() {
        return this.randomUserAgent;
    }

    public RequestObj setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public boolean getFollowRedirects() {
        return this.followRedirects;
    }

    public RequestObj setProxies(String proxies) {
        this.proxies = proxies;
        return this;
    }

    public String getProxies() {
        return this.proxies;
    }

    public RequestObj setProxiesType(String proxiesType) {
        validateProxiesType(proxiesType);
        this.proxiesType = proxiesType;
        return this;
    }

    private void validateProxiesType(String proxiesType) {
        if (!Arrays.asList("HTTP", "HTTPS", "SOCKS").contains(proxiesType.toUpperCase())) {
            throw new IllegalArgumentException("[×] 代理模式支持'HTTP'、'HTTPS'、'SOCKS'，不应为" + proxiesType);
        }
    }

    public String getProxiesType() {
        return this.proxiesType;
    }


    public RequestObj setPostMethod(String postMethod) {
        validatePostMethod(postMethod);
        this.postMethod = postMethod;
        return this;
    }

    private void validatePostMethod(String postMethod) {
        if (!Arrays.asList("FORM", "RAW", "CHUNKED", "JSON").contains(postMethod.toUpperCase())) {
            throw new IllegalArgumentException("[×] POST模式支持'Form'、'Raw'、'Chunked'、'Json'，不应为" + postMethod);
        }
    }

    public String getPostMethod() {
        return this.postMethod;
    }


    public RequestObj setPostData(byte[] data) {
        this.postData = data;
        return this;
    }
    public RequestObj setPostData(String postData) {
        this.postData = postData.getBytes(StandardCharsets.UTF_8);
        return this;
    }
    public RequestObj setPostData(File postDataFile) {
        try {
            this.file = postDataFile;
            this.postData = Files.readAllBytes(postDataFile.toPath());
        } catch (IOException e) {
            System.err.println("无法读取文件：" + e.getMessage());
        }
        return this;
    }

    public RequestObj setPostData(JSONObject postJsonData) {
        this.postMethod = "Json";
        this.postData = postJsonData.toString().getBytes(StandardCharsets.UTF_8);
        return this;
    }
    public RequestObj setPostData(JsonObject postJsonData) {
        this.postMethod = "Json";
        this.postData = postJsonData.toString().getBytes(StandardCharsets.UTF_8);
        return this;
    }
    public byte[] getPostData() {
        return this.postData;
    }

    public File getPostFile() {
        return this.file;
    }

    public RequestObj setFormParameters(Map<String, Object> formParameters) {
        this.postMethod = "Form";
        this.formParameters = formParameters;
        return this;
    }

    public Map<String, Object> getFormParameters() {
        return this.formParameters;
    }

    public RequestObj setTimeOut(int timeOut) {
        this.timeOut = timeOut;
        return this;
    }

    public int getTimeOut() {
        return this.timeOut;
    }


    public int getRetries() {
        return retries;
    }

    public RequestObj setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public int getRetryWaitTime() {
        return retryWaitTime;
    }

    public RequestObj setRetryWaitTime(int retryWaitTime) {
        this.retryWaitTime = retryWaitTime;
        return this;
    }

    public RequestObj setNoUserAgent(boolean noUserAgent) {
        this.noUserAgent = noUserAgent;
        if(noUserAgent) this.randomUserAgent = false;
        return this;
    }
    public boolean getNoUserAgent() {
        return noUserAgent;
    }

    public RequestObj setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return this;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public RequestObj setMaxResponseSize(int maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
        return this;
    }

    public int getMaxResponseSize() {
        return maxResponseSize;
    }
}
