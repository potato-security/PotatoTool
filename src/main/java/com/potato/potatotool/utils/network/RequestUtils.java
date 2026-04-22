package com.potato.potatotool.utils.network;

import com.potato.potatotool.utils.data.StrUtils;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 基于OkHttp3的网络请求工具类
 * @author Potato
 * @date 2024/12/19
 */
public class RequestUtils {

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)https?://[^\\s,，;；)]+");
    private static final Pattern LEADING_HOST_PORT_PATTERN = Pattern.compile("(?i)/[^\\s/:]+:\\d+");
    private static final Pattern IP_PORT_PATTERN = Pattern.compile("(?i)(?<![a-z0-9_.-])(?:\\d{1,3}\\.){3}\\d{1,3}:\\d+");
    private static final Pattern DOMAIN_PORT_PATTERN = Pattern.compile("(?i)(?<![a-z0-9_.-])[a-z0-9.-]+\\.[a-z]{2,}:\\d+");

    private static String maskInternalAiAddress(String value, boolean internalAiRequest) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        if (!internalAiRequest) {
            return value;
        }

        String result = URL_PATTERN.matcher(value).replaceAll("内置AI服务地址");
        result = LEADING_HOST_PORT_PATTERN.matcher(result).replaceAll("/内置AI服务主机");
        result = IP_PORT_PATTERN.matcher(result).replaceAll("内置AI服务主机");
        result = DOMAIN_PORT_PATTERN.matcher(result).replaceAll("内置AI服务主机");
        return result;
    }

    /**
     * 创建信任所有证书的TrustManager
     */
    private static final X509TrustManager trustAllTrustManager = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    };

    /**
     * 创建信任所有SSL证书的SSLSocketFactory
     */
    private static SSLSocketFactory createTrustAllSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllTrustManager}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final SSLSocketFactory trustAllSSLSocketFactory = createTrustAllSSLSocketFactory();
    
    /**
     * 自定义 SNI 的 SSLSocketFactory 包装类
     * 用于在 TLS 握手时设置自定义的 Server Name Indication
     * 仅在需要时实例化，避免不启用时的冗余操作
     */
    private static class CustomSniSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        private final String customSni;
        
        public CustomSniSSLSocketFactory(SSLSocketFactory delegate, String customSni) {
            this.delegate = delegate;
            this.customSni = customSni;
        }
        
        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }
        
        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }
        
        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(socket, host, port, autoClose);
            applySni(sslSocket);
            return sslSocket;
        }
        
        @Override
        public Socket createSocket(String host, int port) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port);
            applySni(sslSocket);
            return sslSocket;
        }
        
        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port, localHost, localPort);
            applySni(sslSocket);
            return sslSocket;
        }
        
        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port);
            applySni(sslSocket);
            return sslSocket;
        }
        
        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(address, port, localAddress, localPort);
            applySni(sslSocket);
            return sslSocket;
        }
        
        /**
         * 应用自定义 SNI 到 SSLSocket
         * 使用 SSLParameters.setServerNames() 设置 TLS SNI 扩展
         */
        private void applySni(SSLSocket sslSocket) {
            if (customSni == null || customSni.isEmpty()) {
                return;
            }
            try {
                SSLParameters params = sslSocket.getSSLParameters();
                // SNIHostName 从 Java 8 开始支持
                params.setServerNames(Collections.singletonList(new SNIHostName(customSni)));
                sslSocket.setSSLParameters(params);
            } catch (Exception e) {
                // 如果设置失败（如无效的主机名），记录警告但不中断请求
                System.err.println("[WARN] 设置自定义 SNI 失败: " + customSni + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * 协议升级/降级拦截器，处理HTTP/HTTPS协议切换
     * 使用静态WeakHashMap确保所有拦截器实例共享请求状态，并通过垃圾回收自动清理
     */
    private static class ProtocolFallbackInterceptor implements Interceptor {
        private final int maxRetries;
        private final RequestObj requestObj;
        // 使用静态WeakHashMap确保所有拦截器实例共享请求状态，并自动清理
        private static final Map<String, RequestState> requestStateMap = 
            Collections.synchronizedMap(new WeakHashMap<>());
        
        // 请求状态类，记录重试次数和是否已尝试协议切换
        private static class RequestState {
            int retryCount = 0;
            boolean protocolSwitched = false;
            // 保留时间戳用于调试目的
            long timestamp = System.currentTimeMillis();
        }
        
        public ProtocolFallbackInterceptor(int maxRetries, RequestObj requestObj) {
            this.maxRetries = maxRetries;
            this.requestObj = requestObj;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            String originalScheme = originalRequest.url().scheme();
            String requestUrl = String.valueOf(originalRequest.url());
            boolean internalAiRequest = requestObj != null && requestObj.isInternalAiRequest();

            // 使用完整的请求信息作为key，确保唯一性
            String requestKey = originalRequest.method() + ":" + 
                    originalRequest.url().toString() + ":" +
                    Thread.currentThread().getId();
            
            // 获取或创建请求状态
            RequestState state = requestStateMap.computeIfAbsent(requestKey, k -> new RequestState());
            // 更新时间戳
            state.timestamp = System.currentTimeMillis();
            
            try {
                Response response = chain.proceed(originalRequest);
                
                // 检查是否需要协议升级（HTTP 426状态码）
                if ("http".equalsIgnoreCase(originalScheme) && response.code() == 426 && !state.protocolSwitched) {
                    if (debugMode) {
                        System.err.println("收到426状态码，尝试升级到HTTPS: " + maskInternalAiAddress(requestUrl, internalAiRequest));
                    }
                    response.close();
                    state.protocolSwitched = true;
                    return handleProtocolUpgrade(chain, originalRequest);
                }
                
                // 检查5xx服务器错误，进行重试
                if (response.code() >= 500 && state.retryCount < maxRetries) {
                    // 检查线程是否被中断
                    if (Thread.currentThread().isInterrupted()) {
                        response.close();
                        requestStateMap.remove(requestKey);
                        throw new IOException("请求被中断");
                    }

                    if (debugMode) {
                        System.err.println("服务器错误 (" + response.code() + "), 重试 " + (state.retryCount + 1) + "/" + maxRetries + ": "
                                + maskInternalAiAddress(requestUrl, internalAiRequest));
                    }
                    response.close();

                    // 重试前等待
                    try {
                        Thread.sleep(1000 * (requestObj.getRetryWaitTime() + state.retryCount)); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        requestStateMap.remove(requestKey); // 使用正确的key清理状态
                        throw new IOException("重试被中断", ie);
                    }
                    state.retryCount++;

                    return chain.proceed(originalRequest);
                }
                
                // 请求成功，清理状态
                requestStateMap.remove(requestKey); // 使用正确的key清理状态
                return response;
                
            } catch (IOException e) {
                // 检查是否需要协议降级（HTTPS SSL异常）
                if ("https".equalsIgnoreCase(originalScheme) && isSslOrProtocolException(e) && !state.protocolSwitched) {
                    if (debugMode) {
                        System.err.println("SSL异常，尝试降级到HTTP: " + maskInternalAiAddress(requestUrl, internalAiRequest)
                                + " - " + maskInternalAiAddress(e.getMessage(), internalAiRequest));
                    }
                    state.protocolSwitched = true;
                    Request newRequest = downgradeToHttp(originalRequest);
                    return chain.proceed(newRequest);
                }
                
                // 检查是否可以重试（非协议切换的普通重试）
                if (state.retryCount < maxRetries) {
                    // 检查线程是否被中断
                    if (Thread.currentThread().isInterrupted()) {
                        requestStateMap.remove(requestKey);
                        throw new IOException("请求被中断", e);
                    }

                    if (debugMode) {
                        System.err.println("网络异常，重试 " + (state.retryCount + 1) + "/" + maxRetries + ": "
                                + maskInternalAiAddress(requestUrl, internalAiRequest)
                                + " - " + maskInternalAiAddress(e.getMessage(), internalAiRequest));
                    }

                    // 重试前等待
                    try {
                        Thread.sleep(1000 * (requestObj.getRetryWaitTime() + state.retryCount)); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        requestStateMap.remove(requestKey); // 使用正确的key清理状态
                        throw new IOException("重试被中断", ie);
                    }
                    state.retryCount++;

                    return chain.proceed(originalRequest);
                }
                
                // 重试次数用尽，清理状态并抛出异常
                requestStateMap.remove(requestKey); // 使用正确的key清理状态
                throw e;
            }
        }
        
        // 协议升级逻辑（HTTP→HTTPS）
        private Response handleProtocolUpgrade(Chain chain, Request request) throws IOException {
            HttpUrl newUrl = request.url().newBuilder()
                    .scheme("https")
                    .port(request.url().port() == 80 ? 443 : request.url().port())
                    .build();
            Request newRequest = request.newBuilder().url(newUrl).build();
            return chain.proceed(newRequest);
        }
        
        // 协议降级逻辑（HTTPS→HTTP）
        private Request downgradeToHttp(Request request) {
            HttpUrl httpUrl = request.url().newBuilder()
                    .scheme("http")
                    .port(request.url().port() == 443 ? 80 : request.url().port())
                    .build();
            return request.newBuilder().url(httpUrl).build();
        }
        
        // 判断是否为 SSL/协议不匹配等导致的异常，可降级场景
        private boolean isSslOrProtocolException(IOException e) {
            return e instanceof SSLHandshakeException ||
            e instanceof SSLProtocolException ||
            e instanceof SSLPeerUnverifiedException ||
            e instanceof SSLException; // 包含所有SSL相关异常的基类
        }
    }

    /**
     * @param requestObj-url               请求URL
     * @param requestObj-method            请求方式（默：GET）
     * @param requestObj-headers           请求头
     * @param requestObj-followRedirects   是否允许重定向（默：否）
     * @param requestObj-randomUserAgent   是否随机UA头（默：是）
     * @param requestObj-proxies           代理（如携带代理类型会自动提取）
     * @param requestObj-proxiesType       代理类型（默：HTTP）
     * @param requestObj-timeOut           请求和读取超时时间
     * @param requestObj-maxRetries        服务器异常(code:5xx)/无响应(读取超时)时重放次数
     * @param requestObj-retryWaitTime     重放间隔（秒）
     * @param requestObj-postMethod        POST请求传输模式（默：Raw）
     * @param requestObj-postData          POST数据byte[]
     * @param requestObj-formParameters    form表单Map格式
     * @param requestObj-file              上传的文件File
     * @param requestObj-maxResponseSize   读取最大响应字节数
     * @param requestObj-getStrictSslValidation严格的SSL校验
     *
     * 网络请求主方法
     * @param requestObj 请求对象
     * @return CustomHttpResponse响应对象
     * @throws Exception 请求异常
     */
    public static CustomHttpResponse requests(RequestObj requestObj) throws Exception {
        return requests(requestObj, null);
    }
    
    /**
     * 发送网络请求 - 支持传入自定义OkHttpClient
     * @param requestObj 请求对象
     * @param client 可选的OkHttpClient，如果为null则创建新的客户端
     */
    public static CustomHttpResponse requests(RequestObj requestObj, OkHttpClient client) throws Exception {
        int maxResponseSize = requestObj.getMaxResponseSize();
        boolean internalAiRequest = requestObj != null && requestObj.isInternalAiRequest();
        Response response = null;
        CustomHttpResponse customResponse = null;

        try {
            // 如果没有传入客户端，则创建新的
            if (client == null) {
                client = createOkHttpClient(requestObj);
            }
            
            // 构建请求
            Request request = buildRequest(requestObj);
            
            // 开始计时
            long startTime = System.currentTimeMillis();
            
            // 执行请求
            response = client.newCall(request).execute();
            
            // 创建响应对象
            customResponse = new CustomHttpResponse(response, maxResponseSize);
            long endTime = System.currentTimeMillis();
            customResponse.setResponseTime(endTime - startTime);
            
            return customResponse;
            
        } catch (OutOfMemoryError oom) {
            // 处理OOM异常
            System.err.println("内存不足，无法完成请求: " + oom.getMessage());
            System.gc(); // 尝试回收内存
            
            // 确保释放资源
            if (customResponse != null) {
                customResponse.disconnect();
            } else if (response != null) {
                response.close();
                response = null;
            }
            
            throw new Exception("[×] 内存不足，无法完成请求: " + maskInternalAiAddress(requestObj.getUrl(), internalAiRequest), oom);
        } catch (IOException e) {
            // 确保在异常情况下释放响应资源
            if (customResponse != null) {
                customResponse.disconnect();
            } else if (response != null) {
                response.close();
                response = null;
            }
            
            // 检查是否是FileNotFoundException（通常是404错误）
            if (e instanceof FileNotFoundException) {
                if (debugMode) {
                    System.err.println("资源未找到 (404): " + maskInternalAiAddress(requestObj.getUrl(), internalAiRequest));
                }
                throw new Exception("[×] 资源未找到: " + maskInternalAiAddress(requestObj.getUrl(), internalAiRequest), e);
            }
            
            throw new Exception("[×] 请求失败: " + maskInternalAiAddress(requestObj.getUrl(), internalAiRequest)
                    + ", 错误: " + maskInternalAiAddress(e.getMessage(), internalAiRequest), e);
        } catch (Exception e) {
            // 确保在异常情况下释放响应资源
            if (customResponse != null) {
                customResponse.disconnect();
            } else if (response != null) {
                response.close();
                response = null;
            }
            throw e;
        }
    }

    /**
     * 创建OkHttpClient
     */
    // 默认连接池 - 最大空闲连接数200，保持连接5分钟
    private static final ConnectionPool DEFAULT_CONNECTION_POOL = 
            new ConnectionPool(200, 5, TimeUnit.MINUTES);
    
    // 默认调度器 - 最大并发请求64，每个主机最大并发请求5
    private static final Dispatcher DEFAULT_DISPATCHER = createDefaultDispatcher();
    
    /**
     * 创建默认的调度器，优化并发请求性能
     */
    private static Dispatcher createDefaultDispatcher() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(64); // 默认64个并发请求
        dispatcher.setMaxRequestsPerHost(5); // 每个主机最多5个并发请求
        return dispatcher;
    }
    
    /**
     * 创建OkHttpClient，优化多线程并发性能
     */
    public static OkHttpClient createOkHttpClient(RequestObj requestObj) {
        // 初始化入参
        boolean followRedirects = requestObj.getFollowRedirects();
        String proxies = requestObj.getProxies();
        String proxiesType = requestObj.getProxiesType();
        int timeOut = requestObj.getTimeOut();
        int readTimeout = requestObj.getReadTimeout();
        int writeTimeout = requestObj.getWriteTimeout();
        int callTimeout = requestObj.getCallTimeout();
        int maxRetries = requestObj.getRetries();
        
        // 获取连接池 - 优先使用自定义连接池，否则使用默认连接池
        ConnectionPool connectionPool = requestObj.getConnectionPool() != null ? 
                requestObj.getConnectionPool() : DEFAULT_CONNECTION_POOL;
        
        // 获取调度器 - 优先使用自定义调度器，否则使用默认调度器
        Dispatcher dispatcher = requestObj.getDispatcher() != null ? 
                requestObj.getDispatcher() : DEFAULT_DISPATCHER;
        
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        
        builder.followRedirects(followRedirects)
               .connectTimeout(timeOut, TimeUnit.SECONDS)
               .readTimeout(readTimeout, TimeUnit.SECONDS)
               .writeTimeout(writeTimeout, TimeUnit.SECONDS)
               .callTimeout(callTimeout, TimeUnit.SECONDS) // 添加整体调用超时，防止请求长时间挂起
               .hostnameVerifier((hostname, session) -> true)
               .addInterceptor(new ProtocolFallbackInterceptor(maxRetries, requestObj))
               .retryOnConnectionFailure(false)
               .connectionPool(connectionPool) // 始终设置连接池
               .dispatcher(dispatcher); // 始终设置调度器

        // 设置 SSL Socket Factory
        // 仅在启用自定义 SNI 时创建包装类，否则使用默认的 trustAllSSLSocketFactory
        if (requestObj.hasTlsSni()) {
            // 启用自定义 SNI - 创建包装 SSLSocketFactory
            SSLSocketFactory sniFactory = new CustomSniSSLSocketFactory(trustAllSSLSocketFactory, requestObj.getTlsSni());
            builder.sslSocketFactory(sniFactory, trustAllTrustManager);
        } else {
            // 默认行为 - 使用静态共享的 trustAllSSLSocketFactory，无额外开销
            builder.sslSocketFactory(trustAllSSLSocketFactory, trustAllTrustManager);
        }

        // 显式指定代理路由，避免回退到 JVM/IDE/系统默认代理。
        Proxy effectiveProxy = Proxy.NO_PROXY;
        if (proxies != null && !proxies.isEmpty()) {
            Proxy proxy = createProxy(proxies, proxiesType);
            if (proxy != null) {
                effectiveProxy = proxy;
            }
        }
        builder.proxy(effectiveProxy);

        return builder.build();
    }

    /**
     * 创建代理对象
     */
    private static Proxy createProxy(String proxies, String proxiesType) {
        try {
            String[] tmpProxyList = proxies.toLowerCase()
                .replace("http://", "")
                .replace("https://", "")
                .replace("socks://", "")
                .split(":");
            
            if (tmpProxyList.length != 2) {
                System.err.println("代理格式错误: " + proxies);
                return null;
            }
            
            Proxy.Type proxyType = Proxy.Type.HTTP; // 默认HTTP代理模式
            if (proxiesType.equalsIgnoreCase("SOCKS") || proxies.toLowerCase().startsWith("socks://")) {
                proxyType = Proxy.Type.SOCKS;
            }
            
            return new Proxy(proxyType, new InetSocketAddress(tmpProxyList[0], Integer.parseInt(tmpProxyList[1])));
        } catch (Exception e) {
            System.err.println("创建代理失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 构建请求对象
     */
    public static Request buildRequest(RequestObj requestObj) throws Exception {
        Request.Builder builder = new Request.Builder();
        
        // 设置URL
        if (requestObj.getPreserveRawUrl()) {
            // 保留原始URL编码（用于POC扫描），不进行归一化
            builder.url(buildRawHttpUrl(requestObj.getUrl()));
        } else {
            // 默认行为：允许OkHttp进行URL归一化
            builder.url(requestObj.getUrl());
        }
        
        // 设置请求头
        setHeaders(builder, requestObj);
        
        // 设置请求体
        setRequestBody(builder, requestObj);
        
        return builder.build();
    }
    
    /**
     * 构建保留原始编码的HttpUrl
     * 用于POC扫描时保留路径遍历等特殊编码（如 %2e%2e、%u002e 等）
     */
    private static HttpUrl buildRawHttpUrl(String rawUrl) throws Exception {
        try {
            // 使用URI解析，getRawPath()可以获取原始未解码的路径
            URI uri = new URI(rawUrl);
            String protocol = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String rawPath = uri.getRawPath();      // 保留原始编码
            String rawQuery = uri.getRawQuery();    // 保留原始编码
            
            // 如果解析失败或host为空，回退到默认方式
            if (host == null || host.isEmpty()) {
                return HttpUrl.parse(rawUrl);
            }
            
            // 如果路径为空，使用根路径
            if (rawPath == null || rawPath.isEmpty()) {
                rawPath = "/";
            }
            
            // 使用HttpUrl.Builder构建URL，保留原始编码
            HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme(protocol != null ? protocol : "http")
                .host(host);
            
            // 设置端口（如果指定了）
            if (port != -1) {
                urlBuilder.port(port);
            }
            
            // 使用encodedPath保留原始编码（不会被二次编码或归一化）
            urlBuilder.encodedPath(rawPath);
            
            // 设置查询参数（如果有）
            if (rawQuery != null && !rawQuery.isEmpty()) {
                urlBuilder.encodedQuery(rawQuery);
            }
            
            return urlBuilder.build();
        } catch (Exception e) {
            // URI 解析失败时，回退到默认方式（允许归一化）
            HttpUrl result = HttpUrl.parse(rawUrl);
            if (result == null) {
                throw new IllegalArgumentException("无法解析URL: " + rawUrl, e);
            }
            return result;
        }
    }

    /**
     * 设置请求头
     */
    private static void setHeaders(Request.Builder builder, RequestObj requestObj) {
        // 设置默认头部
        if (!requestObj.getNoUserAgent()) {
            if (requestObj.getRandomUserAgent()) {
                builder.header("User-Agent", StrUtils.RandomUserAgent());
            } else {
                builder.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.1 Safari/537.36");
            }
        }
        
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

        // 设置Bearer Token
        String bearerToken = requestObj.getBearerToken();
        if (!bearerToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + bearerToken.replace("Bearer ", ""));
        }

        // 设置自定义头部
        Map<String, String> headers = requestObj.getHeaders();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                String headerValue = entry.getValue();

                // 验证 header name 是否合法（不包含空格、换行等非法字符）
                if (headerName == null || headerName.trim().isEmpty() || headerValue == null || headerValue.trim().isEmpty()) {
                    System.err.println("[警告] 跳过空的 header name/value");
                    continue;
                }

                // HTTP header name 不能包含空格、换行符、制表符等控制字符
                if (headerName.contains(" ") || headerName.contains("\t") ||
                    headerName.contains("\n") || headerName.contains("\r") ||
                    headerName.contains(":") || headerName.contains("/")) {
                    System.err.println("[警告] 跳过包含非法字符的 header name: " +
                        headerName.substring(0, Math.min(50, headerName.length())).replace("\n", "\\n").replace("\r", "\\r"));
                    continue;
                }

                try {
                    // 处理占位符替换
                    if (headerValue.contains("{{randomAgent}}")) {
                        headerValue = headerValue.replace("{{randomAgent}}", StrUtils.RandomUserAgent());
                    }
                    if (headerValue.contains("{{UUID}}")) {
                        headerValue = headerValue.replace("{{UUID}}",UUID.randomUUID().toString().replace("-", ""));
                    }
                    builder.header(headerName, headerValue);
                } catch (IllegalArgumentException e) {
                    System.err.println("[错误] 设置 header 失败 [" + headerName + "]: " + e.getMessage());
                }
            }
        }
    }

    private static String getHeaderIgnoreCase(Map<String, String> headers, String headerName) {
        if (headers == null || headers.isEmpty() || headerName == null || headerName.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && headerName.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String resolveEffectivePostMethod(RequestObj requestObj) {
        if (requestObj == null) {
            return "RAW";
        }

        String configuredPostMethod = requestObj.getPostMethod();
        if (requestObj.isPostMethodExplicit()) {
            return configuredPostMethod == null ? "RAW" : configuredPostMethod.toUpperCase();
        }

        Map<String, Object> formParameters = requestObj.getFormParameters();
        if (formParameters != null && !formParameters.isEmpty()) {
            return "FORM";
        }

        byte[] postData = requestObj.getPostData();
        boolean hasPostData = postData != null && postData.length > 0;
        if (hasPostData) {
            String contentType = getHeaderIgnoreCase(requestObj.getHeaders(), "Content-Type");
            if (contentType != null) {
                String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
                if (normalizedContentType.contains("application/json") || normalizedContentType.contains("+json")) {
                    return "JSON";
                }
            }
        }

        return "RAW";
    }

    /**
     * 设置请求体
     */
    private static void setRequestBody(Request.Builder builder, RequestObj requestObj) throws Exception {
        String method = requestObj.getMethod();
        String postMethod = resolveEffectivePostMethod(requestObj);
        
        // GET、HEAD、OPTIONS、TRACE等方法不应包含请求体
        if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD") ||
            method.equalsIgnoreCase("OPTIONS") || method.equalsIgnoreCase("TRACE")) {
            builder.method(method, null);
            return;
        }
        
        RequestBody requestBody = null;
        
        if (postMethod.equalsIgnoreCase("FORM")) {
            requestBody = createFormRequestBody(requestObj);
        } else if (postMethod.equalsIgnoreCase("JSON")) {
            requestBody = createJsonRequestBody(requestObj);
        } else if (postMethod.equalsIgnoreCase("RAW")) {
            requestBody = createRawRequestBody(requestObj);
        } else if (postMethod.equalsIgnoreCase("CHUNKED")) {
            requestBody = createChunkedRequestBody(requestObj);
        } else {
            // 默认处理：如果有数据则作为原始数据发送，否则发送空内容
            byte[] postData = requestObj.getPostData();
            File file = requestObj.getPostFile();
        
            if (file != null) {
                String contentType = guessContentType(file.getName());
                requestBody = RequestBody.create(file, MediaType.parse(contentType));
            } else if (postData != null && postData.length > 0) {
                requestBody = RequestBody.create(postData, MediaType.parse("application/octet-stream"));
            } else {
                requestBody = RequestBody.create("", MediaType.parse("text/plain"));
            }
        }
        
        builder.method(method, requestBody);
    }

    /**
     * 创建表单请求体
     */
    private static RequestBody createFormRequestBody(RequestObj requestObj) throws Exception {
        Map<String, Object> formParameters = requestObj.getFormParameters();
        
        if (formParameters == null || formParameters.isEmpty()) {
            return RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded"));
        }
        
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM);
        
        for (Map.Entry<String, Object> entry : formParameters.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            
            if (paramValue instanceof String) {
                multipartBuilder.addFormDataPart(paramName, (String) paramValue);
            } else if (paramValue instanceof RequestObj.FormFilePart) {
                RequestObj.FormFilePart filePart = (RequestObj.FormFilePart) paramValue;
                File fileData = filePart.getFile();
                if (fileData == null) {
                    continue;
                }
                String contentType = filePart.getContentType();
                if (contentType == null || contentType.trim().isEmpty()) {
                    contentType = guessContentType(filePart.getFileName());
                }
                RequestBody fileBody = RequestBody.create(fileData, MediaType.parse(contentType));
                multipartBuilder.addFormDataPart(paramName, filePart.getFileName(), fileBody);
            } else if (paramValue instanceof File) {
                File fileData = (File) paramValue;
                String contentType = guessContentType(fileData.getName());
                RequestBody fileBody = RequestBody.create(fileData, MediaType.parse(contentType));
                multipartBuilder.addFormDataPart(paramName, fileData.getName(), fileBody);
            }
        }
        
        return multipartBuilder.build();
    }

    /**
     * 创建JSON请求体
     */
    private static RequestBody createJsonRequestBody(RequestObj requestObj) {
        byte[] postData = requestObj.getPostData();
        if (postData != null && postData.length > 0) {
            return RequestBody.create(postData, MediaType.parse("application/json; charset=utf-8"));
        } else {
            return RequestBody.create("{}", MediaType.parse("application/json; charset=utf-8"));
        }
    }

    /**
     * 创建原始请求体（RAW模式）
     * 适用于已知大小的数据，使用固定Content-Length
     */
    private static RequestBody createRawRequestBody(RequestObj requestObj) {
        byte[] postData = requestObj.getPostData();
        File file = requestObj.getPostFile();
        
        if (file != null) {
            String contentType = guessContentType(file.getName());
            return RequestBody.create(file, MediaType.parse(contentType));
        } else if (postData != null && postData.length > 0) {
            return RequestBody.create(postData, MediaType.parse("application/octet-stream"));
        } else {
            return RequestBody.create("", MediaType.parse("text/plain"));
        }
    }

    /**
     * 创建分块传输请求体（CHUNKED模式）
     * 适用于流式传输或未知大小的数据，OkHttp会自动设置Transfer-Encoding: chunked
     */
    private static RequestBody createChunkedRequestBody(RequestObj requestObj) {
        byte[] postData = requestObj.getPostData();
        File file = requestObj.getPostFile();
        
        if (file != null) {
            String contentType = guessContentType(file.getName());
            // 对于文件，创建流式RequestBody以启用chunked传输
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse(contentType);
                }
                
                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try (FileInputStream fis = new FileInputStream(file);
                         Source source = Okio.source(fis)) {
                        sink.writeAll(source);
                    } catch (FileNotFoundException e) {
                        throw new IOException("File not found: " + file.getPath(), e);
                    }
                }
                
            };
        } else if (postData != null && postData.length > 0) {
            // 对于字节数组，也可以使用流式方式
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse("application/octet-stream");
                }
                
                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try (Buffer buffer = new Buffer()) {
                        buffer.write(postData);
                        sink.writeAll(buffer);
                    }
                }
                
            };
        } else {
            return RequestBody.create("", MediaType.parse("text/plain"));
        }
    }

    /**
     * 猜测文件的Content-Type
     */
    private static String guessContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerName.endsWith(".log")) {
            return "text/plain";
        } else if (lowerName.endsWith(".md")) {
            return "text/markdown";
        } else if (lowerName.endsWith(".csv")) {
            return "text/csv";
        } else if (lowerName.endsWith(".json")) {
            return "application/json";
        } else if (lowerName.endsWith(".xml")) {
            return "application/xml";
        } else if (lowerName.endsWith(".yaml") || lowerName.endsWith(".yml")) {
            return "application/x-yaml";
        } else if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerName.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerName.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            return "text/html";
        } else if (lowerName.endsWith(".css")) {
            return "text/css";
        } else if (lowerName.endsWith(".js")) {
            return "application/javascript";
        } else if (lowerName.endsWith(".java")) {
            return "text/x-java-source";
        } else if (lowerName.endsWith(".ts")) {
            return "text/plain";
        } else if (lowerName.endsWith(".py")) {
            return "text/x-python";
        } else if (lowerName.endsWith(".go")) {
            return "text/x-go";
        } else if (lowerName.endsWith(".php")) {
            return "application/x-httpd-php";
        } else if (lowerName.endsWith(".rb")) {
            return "application/x-ruby";
        } else if (lowerName.endsWith(".c") || lowerName.endsWith(".h")) {
            return "text/x-c";
        } else if (lowerName.endsWith(".cpp") || lowerName.endsWith(".hpp")) {
            return "text/x-c++";
        } else if (lowerName.endsWith(".sh")) {
            return "application/x-sh";
        } else if (lowerName.endsWith(".bat") || lowerName.endsWith(".ps1")) {
            return "text/plain";
        } else if (lowerName.endsWith(".sql")) {
            return "application/sql";
        } else if (lowerName.endsWith(".properties") || lowerName.endsWith(".ini") || lowerName.endsWith(".conf")) {
            return "text/plain";
        } else if (lowerName.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * 异步批量请求方法 - 支持实时处理每个请求结果的回调
     * 适用于大规模扫描场景，可以实时处理每个请求的结果
     * 
     * @param requestObjs 请求对象列表
     * @param executor 外部提供的线程池执行器
     * @param clientConfigObj 用于创建共享客户端的配置对象
     * @param resultCallback 结果回调函数，每个请求完成后立即调用
     * @param batchSize 批处理大小，控制同时进行的请求数量
     * @return CompletableFuture<Void> 所有请求完成的Future
     */
    public static CompletableFuture<Void> requestsAsyncWithCallback(
            List<RequestObj> requestObjs,
            Executor executor,
            RequestObj clientConfigObj,
            Consumer<AsyncRequestResult> resultCallback,
            int batchSize) {
            
        if (requestObjs == null || requestObjs.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // 如果没有提供执行器，使用默认的ForkJoinPool
        Executor actualExecutor = executor != null ? executor : ForkJoinPool.commonPool();
        
        // 创建共享的OkHttpClient - 使用指定的配置对象或第一个请求对象的配置
        RequestObj configObj = clientConfigObj != null ? clientConfigObj : requestObjs.get(0);
        OkHttpClient sharedClient = createOkHttpClient(configObj);
        
        // 创建信号量来控制并发
        Semaphore semaphore = new Semaphore(batchSize);
        
        // 创建所有任务的Future列表
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // 为每个请求创建异步任务
        for (int i = 0; i < requestObjs.size(); i++) {
            final int index = i;
            final RequestObj requestObj = requestObjs.get(i);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                AsyncRequestResult result = new AsyncRequestResult();
                result.setIndex(index);
                result.setRequestObj(requestObj);
                result.setStartTime(System.currentTimeMillis());
                CustomHttpResponse response = null;
                
                try {
                    // 获取信号量许可，控制并发
                    semaphore.acquire();
                    
                    try {
                        // 使用共享客户端调用requests方法
                        response = requests(requestObj, sharedClient);
                        result.setResponse(response);
                        result.setSuccess(true);
                    } catch (Exception e) {
                        result.setException(e);
                        result.setSuccess(false);
                        if (debugMode) {
                            System.err.println("异步请求失败 [" + index + "]: " + requestObj.getUrl() + " - " + e.getMessage());
                        }
                    } finally {
                        result.setEndTime(System.currentTimeMillis());
                        
                        // 调用结果回调函数，实时处理结果
                        if (resultCallback != null) {
                            try {
                                resultCallback.accept(result);
                            } catch (Exception e) {
                                // 忽略回调中的异常，确保释放资源
                                System.err.println("结果回调异常: " + e.getMessage());
                            }
                        }
                        
                        // 释放信号量许可
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.setException(e);
                    result.setSuccess(false);
                    result.setEndTime(System.currentTimeMillis());
                    
                    // 即使中断也尝试调用回调
                    if (resultCallback != null) {
                        try {
                            resultCallback.accept(result);
                        } catch (Exception callbackEx) {
                            // 忽略
                        }
                    }
                }
            }, actualExecutor);
            
            futures.add(future);
        }
        
        // 返回所有任务完成的Future
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * 异步批量请求方法 - 使用默认线程池和默认批处理大小
     * 
     * @param requestObjs 请求对象列表
     * @param resultCallback 结果回调函数
     * @return CompletableFuture<Void> 所有请求完成的Future
     */
    public static CompletableFuture<Void> requestsAsyncWithCallback(
            List<RequestObj> requestObjs,
            Consumer<AsyncRequestResult> resultCallback) {
        return requestsAsyncWithCallback(requestObjs, null, null, resultCallback, 50);
    }

    /**
     * 异步请求结果类
     */
    public static class AsyncRequestResult {
        private int index;                    // 请求在列表中的索引
        private RequestObj requestObj;  // 原始请求对象
        private CustomHttpResponse response; // 响应对象（成功时）
        private Exception exception;          // 异常对象（失败时）
        private boolean success;              // 是否成功
        private long startTime;              // 开始时间
        private long endTime;                // 结束时间

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public RequestObj getRequestObj() {
            return requestObj;
        }

        public void setRequestObj(RequestObj requestObj) {
            this.requestObj = requestObj;
        }

        public CustomHttpResponse getResponse() {
            return response;
        }

        public void setResponse(CustomHttpResponse response) {
            this.response = response;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        /**
         * 获取请求耗时（毫秒）
         */
        public long getDuration() {
            return endTime - startTime;
        }

        /**
         * 获取请求URL
         */
        public String getUrl() {
            return requestObj != null ? requestObj.getUrl() : "";
        }
        
        /**
         * 释放响应资源
         * 在不再需要响应数据时调用此方法释放资源
         */
        public void releaseResources() {
            if (response != null) {
                response.disconnect();
                response = null;
            }
        }

        @Override
        public String toString() {
            return "AsyncRequestResult{" +
                    "index=" + index +
                    ", url='" + getUrl() + '\'' +
                    ", success=" + success +
                    ", duration=" + getDuration() + "ms" +
                    (exception != null ? ", error='" + exception.getMessage() + '\'' : "") +
                    '}'; 
        }
        
        /**
         * 确保在对象被垃圾回收前释放资源
         */
        @Override
        protected void finalize() throws Throwable {
            try {
                releaseResources();
            } finally {
                super.finalize();
            }
        }
    }
}
