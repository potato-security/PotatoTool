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
import java.nio.charset.StandardCharsets;
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

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 基于OkHttp3的网络请求工具类
 * @author Potato
 * @date 2024/12/19
 */
public class RequestUtils {

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
                        System.err.println("收到426状态码，尝试升级到HTTPS: " + requestUrl);
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
                                + requestUrl);
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
                        System.err.println("SSL异常，尝试降级到HTTP: " + requestUrl
                                + " - " + e.getMessage());
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
                                + requestUrl
                                + " - " + e.getMessage());
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
        Response response = null;
        CustomHttpResponse customResponse = null;

        try {
            if (shouldUseRawSocketHttp(requestObj)) {
                return sendRawSocketHttpRequest(requestObj, maxResponseSize);
            }

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
            
            throw new Exception("[×] 内存不足，无法完成请求: " + requestObj.getUrl(), oom);
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
                    System.err.println("资源未找到 (404): " + requestObj.getUrl());
                }
                throw new Exception("[×] 资源未找到: " + requestObj.getUrl(), e);
            }
            
            throw new Exception("[×] 请求失败: " + requestObj.getUrl()
                    + ", 错误: " + e.getMessage(), e);
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

    private static boolean shouldUseRawSocketHttp(RequestObj requestObj) {
        if (requestObj == null || !requestObj.getPreserveRawUrl()) {
            return false;
        }
        String url = requestObj.getUrl();
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.contains("/../")
                || url.contains(".//")
                || url.contains("/..%2f")
                || url.contains("/..%2F")
                || url.contains("%2e%2e")
                || url.contains("%2E%2E")
                || url.contains("%u");
    }

    private static CustomHttpResponse sendRawSocketHttpRequest(RequestObj requestObj, int maxResponseSize) throws Exception {
        RawHttpExchange exchange = executeRawSocketHttpRequest(requestObj, maxResponseSize);
        Response response = exchange.toOkHttpResponse();
        CustomHttpResponse customResponse = new CustomHttpResponse(response, maxResponseSize);
        customResponse.setResponseTime(exchange.getResponseTimeMillis());
        return customResponse;
    }

    private static RawHttpExchange executeRawSocketHttpRequest(RequestObj requestObj, int maxResponseSize) throws Exception {
        if (requestObj == null || requestObj.getUrl() == null || requestObj.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("[×] URL不能为空");
        }

        URL url = new URL(requestObj.getUrl());
        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
        if (port <= 0) {
            port = "https".equalsIgnoreCase(protocol) ? 443 : 80;
        }

        String requestTarget = resolveRawRequestTarget(requestObj.getUrl(), url);
        byte[] requestBody = buildRawRequestBodyBytes(requestObj);
        Map<String, String> headers = enrichRawRequestHeaders(
                buildRawRequestHeaders(requestObj, host, port, protocol),
                requestBody
        );
        int connectTimeoutMs = Math.max(1, requestObj.getTimeOut()) * 1000;
        int readTimeoutMs = Math.max(1, requestObj.getReadTimeout()) * 1000;
        int maxRetries = Math.max(0, requestObj.getRetries());

        Exception lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            Socket socket = null;
            long startTime = System.currentTimeMillis();
            try {
                socket = createRawSocket(protocol, host, port, connectTimeoutMs, readTimeoutMs, requestObj);
                OutputStream outputStream = socket.getOutputStream();
                writeRawHttpRequest(outputStream, requestObj.getMethod(), requestTarget, headers, requestBody);
                outputStream.flush();

                RawHttpExchange exchange = readRawHttpResponse(socket.getInputStream(), requestObj.getUrl(), requestObj,
                        protocol, maxResponseSize, System.currentTimeMillis() - startTime);
                return exchange;
            } catch (Exception e) {
                lastError = e;
                if (attempt >= maxRetries) {
                    break;
                }
                try {
                    Thread.sleep(1000L * Math.max(0, requestObj.getRetryWaitTime()));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("重试被中断", ie);
                }
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        if (lastError instanceof Exception) {
            throw lastError;
        }
        throw new IOException("[×] 请求失败: " + requestObj.getUrl());
    }

    private static Socket createRawSocket(String protocol, String host, int port,
                                          int connectTimeoutMs, int readTimeoutMs,
                                          RequestObj requestObj) throws Exception {
        Socket rawSocket = new Socket();
        rawSocket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        rawSocket.setSoTimeout(readTimeoutMs);

        if (!"https".equalsIgnoreCase(protocol)) {
            return rawSocket;
        }

        SSLSocketFactory socketFactory;
        if (requestObj != null && requestObj.hasTlsSni()) {
            socketFactory = new CustomSniSSLSocketFactory(trustAllSSLSocketFactory, requestObj.getTlsSni());
        } else {
            socketFactory = trustAllSSLSocketFactory;
        }

        SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(rawSocket, host, port, true);
        sslSocket.setSoTimeout(readTimeoutMs);
        sslSocket.startHandshake();
        return sslSocket;
    }

    private static void writeRawHttpRequest(OutputStream outputStream,
                                            String method,
                                            String requestTarget,
                                            Map<String, String> headers,
                                            byte[] requestBody) throws IOException {
        String requestMethod = method == null || method.trim().isEmpty() ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        builder.append(requestMethod).append(' ').append(requestTarget).append(" HTTP/1.1\r\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        builder.append("\r\n");

        outputStream.write(builder.toString().getBytes(StandardCharsets.ISO_8859_1));
        if (requestBody != null && requestBody.length > 0) {
            outputStream.write(requestBody);
        }
    }

    private static RawHttpExchange readRawHttpResponse(InputStream inputStream,
                                                       String requestUrl,
                                                       RequestObj requestObj,
                                                       String protocol,
                                                       int maxResponseSize,
                                                       long responseTimeMillis) throws Exception {
        String statusLine = readAsciiLine(inputStream);
        if (statusLine == null || statusLine.trim().isEmpty()) {
            throw new IOException("响应为空");
        }

        String[] statusParts = statusLine.split(" ", 3);
        int statusCode = statusParts.length > 1 ? Integer.parseInt(statusParts[1]) : 200;
        String message = statusParts.length > 2 ? statusParts[2] : "";

        Map<String, List<String>> headers = new LinkedHashMap<>();
        String headerLine;
        int contentLength = -1;
        while ((headerLine = readAsciiLine(inputStream)) != null) {
            if (headerLine.isEmpty()) {
                break;
            }
            int idx = headerLine.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String name = headerLine.substring(0, idx).trim();
            String value = headerLine.substring(idx + 1).trim();
            if (!headers.containsKey(name)) {
                headers.put(name, new ArrayList<String>());
            }
            headers.get(name).add(value);
            if ("Content-Length".equalsIgnoreCase(name)) {
                try {
                    contentLength = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                    contentLength = -1;
                }
            }
        }

        byte[] body = readRawResponseBody(inputStream, headers, contentLength, maxResponseSize);
        return new RawHttpExchange(requestUrl, requestObj, protocol, statusCode, message, headers, body, responseTimeMillis);
    }

    private static byte[] readRawResponseBody(InputStream inputStream,
                                              Map<String, List<String>> headers,
                                              int contentLength,
                                              int maxResponseSize) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }

        String transferEncoding = getHeaderValue(headers, "Transfer-Encoding");
        if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
            return readChunkedBody(inputStream, maxResponseSize);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int remaining = contentLength;
        while (true) {
            int bytesRead;
            if (contentLength >= 0) {
                if (remaining <= 0) {
                    break;
                }
                bytesRead = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
            } else {
                bytesRead = inputStream.read(buffer);
            }

            if (bytesRead == -1) {
                break;
            }

            int writable = bytesRead;
            if (maxResponseSize > 0 && outputStream.size() + bytesRead > maxResponseSize) {
                writable = Math.max(0, maxResponseSize - outputStream.size());
            }
            if (writable > 0) {
                outputStream.write(buffer, 0, writable);
            }
            if (contentLength >= 0) {
                remaining -= bytesRead;
            }
            if (maxResponseSize > 0 && outputStream.size() >= maxResponseSize) {
                break;
            }
        }
        return outputStream.toByteArray();
    }

    private static byte[] readChunkedBody(InputStream inputStream, int maxResponseSize) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (true) {
            String chunkSizeLine = readAsciiLine(inputStream);
            if (chunkSizeLine == null) {
                break;
            }
            String normalized = chunkSizeLine.trim();
            int semicolonIndex = normalized.indexOf(';');
            if (semicolonIndex >= 0) {
                normalized = normalized.substring(0, semicolonIndex);
            }
            int chunkSize = Integer.parseInt(normalized.trim(), 16);
            if (chunkSize == 0) {
                readAsciiLine(inputStream);
                break;
            }

            byte[] chunk = new byte[chunkSize];
            int offset = 0;
            while (offset < chunkSize) {
                int read = inputStream.read(chunk, offset, chunkSize - offset);
                if (read == -1) {
                    throw new EOFException("chunked body 提前结束");
                }
                offset += read;
            }
            readAsciiLine(inputStream);

            int writable = chunk.length;
            if (maxResponseSize > 0 && outputStream.size() + chunk.length > maxResponseSize) {
                writable = Math.max(0, maxResponseSize - outputStream.size());
            }
            if (writable > 0) {
                outputStream.write(chunk, 0, writable);
            }
            if (maxResponseSize > 0 && outputStream.size() >= maxResponseSize) {
                break;
            }
        }
        return outputStream.toByteArray();
    }

    private static String resolveRawRequestTarget(String rawUrl, URL parsedUrl) {
        String fallbackPath = "/";
        if (parsedUrl != null && parsedUrl.getFile() != null && !parsedUrl.getFile().isEmpty()) {
            fallbackPath = parsedUrl.getFile();
        }

        try {
            URI uri = new URI(rawUrl);
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            if (rawPath == null || rawPath.isEmpty()) {
                rawPath = "/";
            }
            return rawQuery == null || rawQuery.isEmpty() ? rawPath : rawPath + "?" + rawQuery;
        } catch (Exception ignored) {
            int schemeIdx = rawUrl.indexOf("://");
            int pathIdx = schemeIdx >= 0 ? rawUrl.indexOf('/', schemeIdx + 3) : rawUrl.indexOf('/');
            if (pathIdx < 0) {
                return fallbackPath;
            }
            return rawUrl.substring(pathIdx);
        }
    }

    private static Map<String, String> buildRawRequestHeaders(RequestObj requestObj, String host, int port, String protocol) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        if (!requestObj.getNoUserAgent()) {
            if (requestObj.getRandomUserAgent()) {
                headers.put("User-Agent", StrUtils.RandomUserAgent());
            } else {
                headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.1 Safari/537.36");
            }
        }
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("Connection", "close");
        headers.put("Host", buildHostHeader(host, port, protocol));

        String bearerToken = requestObj.getBearerToken();
        if (bearerToken != null && !bearerToken.isEmpty()) {
            headers.put("Authorization", "Bearer " + bearerToken.replace("Bearer ", ""));
        }

        Map<String, String> customHeaders = requestObj.getHeaders();
        if (customHeaders != null) {
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                String headerName = entry.getKey();
                String headerValue = entry.getValue();
                if (headerName == null || headerValue == null || headerName.trim().isEmpty() || headerValue.trim().isEmpty()) {
                    continue;
                }
                if (headerValue.contains("{{randomAgent}}")) {
                    headerValue = headerValue.replace("{{randomAgent}}", StrUtils.RandomUserAgent());
                }
                if (headerValue.contains("{{UUID}}")) {
                    headerValue = headerValue.replace("{{UUID}}", UUID.randomUUID().toString().replace("-", ""));
                }
                headers.put(headerName, headerValue);
            }
        }
        return headers;
    }

    private static Map<String, String> enrichRawRequestHeaders(Map<String, String> headers, byte[] requestBody) {
        Map<String, String> enriched = new LinkedHashMap<String, String>();
        if (headers != null) {
            enriched.putAll(headers);
        }
        if (requestBody != null && requestBody.length > 0 && !containsHeaderIgnoreCase(enriched, "Content-Length")) {
            enriched.put("Content-Length", String.valueOf(requestBody.length));
        }
        return enriched;
    }

    private static boolean containsHeaderIgnoreCase(Map<String, String> headers, String headerName) {
        if (headers == null || headerName == null) {
            return false;
        }
        for (String key : headers.keySet()) {
            if (key != null && key.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }

    private static String buildHostHeader(String host, int port, String protocol) {
        boolean defaultPort = "https".equalsIgnoreCase(protocol) ? port == 443 : port == 80;
        return defaultPort ? host : host + ":" + port;
    }

    private static byte[] buildRawRequestBodyBytes(RequestObj requestObj) throws Exception {
        String method = requestObj.getMethod();
        if (method == null) {
            method = "GET";
        }
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method) || "TRACE".equalsIgnoreCase(method)) {
            return new byte[0];
        }

        Request request = buildRequestForRawSocket(requestObj);
        RequestBody body = request.body();
        if (body == null) {
            return new byte[0];
        }

        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readByteArray();
    }

    private static Request buildRequestForRawSocket(RequestObj requestObj) throws Exception {
        Request.Builder builder = new Request.Builder().url(requestObj.getUrl());
        setRequestBody(builder, requestObj);
        return builder.build();
    }

    private static String getHeaderValue(Map<String, List<String>> headers, String headerName) {
        if (headers == null || headerName == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    private static String readAsciiLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = inputStream.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                byte[] bytes = outputStream.toByteArray();
                return bytes.length == 0 ? "" : new String(bytes, 0, bytes.length - 1, StandardCharsets.ISO_8859_1);
            }
            outputStream.write(current);
            previous = current;
        }
        if (outputStream.size() == 0) {
            return null;
        }
        return new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static final class RawHttpExchange {
        private final String requestUrl;
        private final RequestObj requestObj;
        private final String protocol;
        private final int statusCode;
        private final String message;
        private final Map<String, List<String>> headers;
        private final byte[] body;
        private final long responseTimeMillis;

        private RawHttpExchange(String requestUrl, RequestObj requestObj, String protocol, int statusCode,
                                String message, Map<String, List<String>> headers, byte[] body,
                                long responseTimeMillis) {
            this.requestUrl = requestUrl;
            this.requestObj = requestObj;
            this.protocol = protocol;
            this.statusCode = statusCode;
            this.message = message == null ? "" : message;
            this.headers = headers == null ? new LinkedHashMap<String, List<String>>() : headers;
            this.body = body == null ? new byte[0] : body;
            this.responseTimeMillis = responseTimeMillis;
        }

        private long getResponseTimeMillis() {
            return responseTimeMillis;
        }

        private Response toOkHttpResponse() {
            Request.Builder requestBuilder = new Request.Builder().url(requestUrl);
            String method = requestObj != null && requestObj.getMethod() != null ? requestObj.getMethod() : "GET";
            requestBuilder.method(method, buildRawSocketResponseRequestBody(method, requestObj));
            Response.Builder responseBuilder = new Response.Builder()
                    .request(requestBuilder.build())
                    .protocol("https".equalsIgnoreCase(protocol) ? Protocol.HTTP_1_1 : Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(message)
                    .body(ResponseBody.create(resolveResponseMediaType(), body));

            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                for (String value : entry.getValue()) {
                    if (value != null) {
                        responseBuilder.addHeader(entry.getKey(), value);
                    }
                }
            }
            return responseBuilder.build();
        }

        private MediaType resolveResponseMediaType() {
            String contentType = getHeaderValue(headers, "Content-Type");
            MediaType mediaType = contentType == null ? null : MediaType.parse(contentType);
            return mediaType != null ? mediaType : MediaType.parse("application/octet-stream");
        }
    }

    private static RequestBody buildRawSocketResponseRequestBody(String method, RequestObj requestObj) {
        String normalizedMethod = method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        if ("GET".equals(normalizedMethod) || "HEAD".equals(normalizedMethod)) {
            return null;
        }

        byte[] requestBodyBytes = null;
        if (requestObj != null) {
            requestBodyBytes = requestObj.getPostData();
        }
        if (requestBodyBytes == null) {
            requestBodyBytes = new byte[0];
        }

        MediaType mediaType = null;
        if (requestObj != null && requestObj.getHeaders() != null) {
            String contentType = requestObj.getHeaders().get("Content-Type");
            if (contentType == null) {
                for (Map.Entry<String, String> entry : requestObj.getHeaders().entrySet()) {
                    if (entry.getKey() != null && "Content-Type".equalsIgnoreCase(entry.getKey())) {
                        contentType = entry.getValue();
                        break;
                    }
                }
            }
            mediaType = contentType == null ? null : MediaType.parse(contentType);
        }
        if (mediaType == null) {
            mediaType = MediaType.parse("application/octet-stream");
        }
        return RequestBody.create(mediaType, requestBodyBytes);
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
        ProxyAddressParser.ParsedProxyAddress parsedProxy = null;
        if (proxies != null && !proxies.isEmpty()) {
            parsedProxy = ProxyAddressParser.parse(proxies, proxiesType);
            if (parsedProxy != null && parsedProxy.hasCredentials()) {
                requestObj.setProxyUsername(parsedProxy.getUsername());
                requestObj.setProxyPassword(parsedProxy.getPassword());
            }
            Proxy proxy = createProxy(parsedProxy, proxiesType);
            if (proxy != null) {
                effectiveProxy = proxy;
            }
        }
        builder.proxy(effectiveProxy);
        configureProxyAuthenticator(builder, requestObj, parsedProxy);

        return builder.build();
    }

    /**
     * 创建代理对象
     */
    private static Proxy createProxy(ProxyAddressParser.ParsedProxyAddress parsedProxy, String proxiesType) {
        try {
            if (parsedProxy == null) {
                return null;
            }

            Proxy.Type proxyType = parsedProxy.toJavaProxyType(proxiesType);
            return new Proxy(proxyType, new InetSocketAddress(parsedProxy.getHost(), parsedProxy.getPort()));
        } catch (Exception e) {
            System.err.println("创建代理失败: " + e.getMessage());
            return null;
        }
    }

    private static void configureProxyAuthenticator(OkHttpClient.Builder builder,
                                                    RequestObj requestObj,
                                                    ProxyAddressParser.ParsedProxyAddress parsedProxy) {
        if (builder == null || requestObj == null || parsedProxy == null) {
            return;
        }

        final String username = requestObj.getProxyUsername();
        if (username == null || username.isEmpty()) {
            return;
        }

        if (parsedProxy.toJavaProxyType(requestObj.getProxiesType()) == Proxy.Type.SOCKS) {
            return;
        }

        final String password = requestObj.getProxyPassword() == null ? "" : requestObj.getProxyPassword();
        builder.proxyAuthenticator((route, response) -> {
            if (response.request().header("Proxy-Authorization") != null) {
                return null;
            }
            String credential = Credentials.basic(username, password, StandardCharsets.UTF_8);
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        });
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

    private static MediaType resolveRequestBodyMediaType(RequestObj requestObj, String fallbackContentType) {
        String contentType = getHeaderIgnoreCase(requestObj == null ? null : requestObj.getHeaders(), "Content-Type");
        if (contentType == null || contentType.trim().isEmpty()) {
            contentType = fallbackContentType;
        }
        MediaType mediaType = contentType == null || contentType.trim().isEmpty()
                ? null
                : MediaType.parse(contentType.trim());
        return mediaType != null ? mediaType : MediaType.parse(fallbackContentType);
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
        MediaType mediaType = resolveRequestBodyMediaType(requestObj, "application/octet-stream");
        
        if (file != null) {
            return RequestBody.create(file, mediaType);
        } else if (postData != null && postData.length > 0) {
            return RequestBody.create(postData, mediaType);
        } else {
            return RequestBody.create("", resolveRequestBodyMediaType(requestObj, "text/plain"));
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
