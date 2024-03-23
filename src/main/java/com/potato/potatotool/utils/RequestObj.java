package com.potato.potatotool.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static com.potato.potatotool.utils.Constants.getResourceString;

/**
 * @author Potato
 * @date 2023/4/13 14:03
 */


public class RequestObj {
    private String method = "GET";
    private String url;
    private Map<String, String> headers;
    private boolean randomUserAgent = true;
    private boolean followRedirects = false;
    private String postMethod = "Raw";
    private byte[] postData;
    private String proxiesType = "HTTP";
    private String proxies;
    private int timeOut = 10;
    private File file;
    private Map formParameters;

    public RequestObj(){
        //  初始化默认代理配置
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Proxy");
        boolean enable = tmpJsonObj.getAsJsonPrimitive("enable").getAsBoolean();
        String address = tmpJsonObj.getAsJsonPrimitive("address").getAsString();
        if(enable && address.length() > 0){
            setProxies(address);
        }

    }


    public RequestObj setMethod(String method) {

        if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("POST") && !method.equalsIgnoreCase("OPTIONS") && !method.equalsIgnoreCase("PUT") && !method.equalsIgnoreCase("DELETE")) {
            try {
                throw new Exception("[×] 请求方法不应为" + method);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.method = method;
        return this;
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

        if (!proxiesType.equalsIgnoreCase("HTTP") && !proxiesType.equalsIgnoreCase("HTTPS") && !proxiesType.equalsIgnoreCase("SOCKS")) {
            try {
                throw new Exception("[×] 代理模式支持'HTTP'、'HTTPS'、'SOCKS'，不应为" + proxiesType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.proxiesType = proxiesType;
        return this;
    }

    public String getProxiesType() {
        return this.proxiesType;
    }


    public RequestObj setPostMethod(String postMethod) {

        if (!postMethod.equalsIgnoreCase("Form") && !postMethod.equalsIgnoreCase("Raw") && !postMethod.equalsIgnoreCase("Chunked")) {
            try {
                throw new Exception("[×] POST模式支持'Form'、'Raw'、'Chunked'，不应为" + postMethod);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.postMethod = postMethod;
        return this;
    }
    public String getPostMethod() {
        return this.postMethod;
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
        this.postData = postJsonData.toString().getBytes(StandardCharsets.UTF_8);
        return this;
    }
    public RequestObj setPostData(JsonObject postJsonData) {
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

}
