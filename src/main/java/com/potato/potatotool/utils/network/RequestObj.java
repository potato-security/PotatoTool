package com.potato.potatotool.utils.network;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 更新为基于OkHttp3的请求对象
 * @author Potato
 * @date 2024/12/19
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
 * timeOut           连接超时时间
 * readTimeout       读取超时时间
 * writeTimeout      写入超时时间
 * callTimeout       整体调用超时时间
 * maxRetries        服务器异常/无响应时重放次数
 * retryWaitTime     重放间隔（秒）
 * postMethod        POST请求传输模式（默：Raw）
 * postData          POST数据byte[]
 * formParameters    form表单Map格式
 * file              上传的文件File
 * strictSslValidation 严格的SSL校验（默：否） 一般校验协议是否适用SSL使用
 * bearerToken       Bearer令牌
 * maxResponseSize   最大响应大小（字节）
 * connectionPool    连接池-用于复用连接的池
 * dispatcher        调度器-用于控制线程池
 */
public class RequestObj {
    private String method = "GET";
    private String url;
    private Map<String, String> headers;
    private String bearerToken = "";
    private boolean randomUserAgent = true;
    private boolean followRedirects = false;
    private String postMethod = "Raw";
    private boolean postMethodExplicit = false;
    private byte[] postData;
    private String proxiesType = "HTTP";
    private String proxies;
    private int timeOut = 10;   // connectTimeout
    private int readTimeout = 30;
    private int writeTimeout = 30;
    private int callTimeout = 40;
    private File file;
    private Map<String, Object> formParameters;
    private int retries = 1;
    private int retryWaitTime = 1;
    private boolean noUserAgent = false;
    private int maxResponseSize = Integer.MAX_VALUE;
    private ConnectionPool connectionPool;
    private Dispatcher dispatcher;
    /** 是否保留原始URL编码（不进行归一化），用于POC扫描时保留路径遍历等特殊编码 */
    private boolean preserveRawUrl = false;
    /** 自定义 TLS SNI 主机名（用于 SSRF 检测），为 null 时使用默认行为 */
    private String tlsSni;
    /** 是否为内置 AI 请求（用于日志/异常地址脱敏） */
    private boolean internalAiRequest = false;

    public RequestObj(){
        initializeProxySettings();
        initializeHeaderSettings();
    }

    private void initializeHeaderSettings() {
        try {
            Map<String, String> globalHeaders = HeaderManager.getInstance().getCustomHeaders();
            if (!globalHeaders.isEmpty()) {
                this.headers = new LinkedHashMap<>(globalHeaders);
            }
        } catch (Exception ignored) {
        }
    }

    private void initializeProxySettings() {
        //  初始化默认代理配置
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
        boolean enable = tmpJsonObj.getAsJsonPrimitive(ConfigConstants.PROXY_ENABLE).getAsBoolean();
        String address = tmpJsonObj.getAsJsonPrimitive(ConfigConstants.PROXY_ADDRESS).getAsString();
        if(enable && address.length() > 0){
            setProxies(address);
        }
    }

    public RequestObj setMethod(String method) {
        validateMethod(method);
        this.method = method.toUpperCase();
        return this;
    }

    private static final String[] VALID_METHODS = {
        "GET", "POST", "OPTIONS", "PUT", "DELETE", "HEAD",
        "PATCH",  // RESTful API 常用
        "TRACE",  // HTTP 调试方法
        "CONNECT",  // HTTP 隧道
        "PURGE",  // Varnish 缓存清除
        "DEBUG",  // ASP.NET 调试
        "PROPFIND", "PROPPATCH", "MKCOL", "COPY", "MOVE", "LOCK", "UNLOCK",  // WebDAV 方法
        "TRACK"  // 类似 TRACE
    };
    
    private void validateMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("[×] 请求方法不能为空");
        }
        // 不再严格验证方法名，允许任意方法（用于安全测试）
        // 只检查是否包含非法字符
        if (method.contains(" ") || method.contains("\n") || method.contains("\r")) {
            throw new IllegalArgumentException("[×] 请求方法包含非法字符: " + method);
        }
    }

    public String getMethod() {
        return this.method;
    }

    public RequestObj setUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("[×] URL不能为空");
        }
        this.url = url;
        return this;
    }

    public String getUrl() {
        return this.url;
    }

    public RequestObj setHeaders(Map<String, String> headers) {
        if (headers == null) {
            this.headers = null;
            return this;
        }
        if (this.headers == null) {
            this.headers = new LinkedHashMap<>(headers);
        } else {
            this.headers.putAll(headers);
        }
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
        this.postMethod = postMethod.toUpperCase();
        this.postMethodExplicit = true;
        return this;
    }

    private void validatePostMethod(String postMethod) {
        if (postMethod == null || postMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("[×] POST方法不能为空");
        }
        if (!Arrays.asList("FORM", "RAW", "CHUNKED", "JSON").contains(postMethod.toUpperCase())) {
            throw new IllegalArgumentException("[×] POST模式支持'Form'、'Raw'、'Chunked'、'Json'，不应为" + postMethod);
        }
    }

    public String getPostMethod() {
        return this.postMethod;
    }

    public boolean isPostMethodExplicit() {
        return postMethodExplicit;
    }

    public RequestObj setPostData(byte[] data) {
        this.postData = data;
        return this;
    }
    
    public RequestObj setPostData(String postData) {
        if (postData != null) {
            this.postData = postData.getBytes(StandardCharsets.UTF_8);
        } else {
            this.postData = null;
        }
        return this;
    }
    
    public RequestObj setPostData(File postDataFile) {
        this.file = postDataFile;
        // 不立即读取文件，只在需要时流式处理
        this.postData = null; 
        return this;
    }

    public RequestObj setPostData(JSONObject postJsonData) {
        this.postMethod = "JSON";
        this.postMethodExplicit = true;
        this.postData = postJsonData.toString().getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public RequestObj setPostData(JsonObject postJsonData) {
        this.postMethod = "JSON";
        this.postMethodExplicit = true;
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
        this.postMethod = "FORM";
        this.postMethodExplicit = true;
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


    public int getReadTimeout() {
        return readTimeout;
    }

    public RequestObj setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public RequestObj setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    public int getCallTimeout() {
        return callTimeout;
    }

    public RequestObj setCallTimeout(int callTimeout) {
        this.callTimeout = callTimeout;
        return this;
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

    /**
     * 设置是否保留原始URL编码（不进行归一化）
     * 用于POC扫描时保留路径遍历等特殊编码（如 %2e%2e、%u002e 等）
     * @param preserveRawUrl true-保留原始编码，false-允许归一化（默认）
     * @return RequestObj
     */
    public RequestObj setPreserveRawUrl(boolean preserveRawUrl) {
        this.preserveRawUrl = preserveRawUrl;
        return this;
    }

    /**
     * 获取是否保留原始URL编码
     */
    public boolean getPreserveRawUrl() {
        return preserveRawUrl;
    }

    /**
     * 设置自定义连接池
     * @param connectionPool 连接池实例
     * @return OkHttpRequestObj
     */
    public RequestObj setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        return this;
    }

    /**
     * 获取连接池配置
     * @return ConnectionPool实例，如果未设置则返回null
     */
    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    /**
     * 设置自定义调度器
     * @param dispatcher 调度器实例
     * @return OkHttpRequestObj
     */
    public RequestObj setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    /**
     * 获取调度器配置
     * @return Dispatcher实例，如果未设置则返回null
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * 标记是否为内置 AI 请求
     * @param internalAiRequest true-内置 AI 请求
     * @return RequestObj
     */
    public RequestObj setInternalAiRequest(boolean internalAiRequest) {
        this.internalAiRequest = internalAiRequest;
        return this;
    }

    /**
     * 是否为内置 AI 请求
     */
    public boolean isInternalAiRequest() {
        return internalAiRequest;
    }

    /**
     * 设置自定义 TLS SNI 主机名
     * 用于 SSRF 检测，在 TLS 握手时使用指定的主机名而非实际连接的主机
     * @param tlsSni 自定义 SNI 主机名，设为 null 或空字符串表示不启用
     * @return RequestObj
     */
    public RequestObj setTlsSni(String tlsSni) {
        this.tlsSni = (tlsSni != null && !tlsSni.trim().isEmpty()) ? tlsSni.trim() : null;
        return this;
    }

    /**
     * 获取自定义 TLS SNI 主机名
     * @return SNI 主机名，null 表示未启用自定义 SNI
     */
    public String getTlsSni() {
        return tlsSni;
    }

    /**
     * 检查是否启用了自定义 TLS SNI
     * @return true 表示启用了自定义 SNI
     */
    public boolean hasTlsSni() {
        return tlsSni != null;
    }

}
