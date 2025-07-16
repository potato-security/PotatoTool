package com.potato.potatotool.utils.network;

import com.potato.potatotool.utils.data.StrUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;

import static com.potato.potatotool.ToStart.debugMode;

public class RequestUtils {

    /**
     * 信任所有SSL证书(默认开启) 非全局模式，存在线程隔离，支持多线程
     * @throws Exception
     */
    private static SSLSocketFactory createTrustAllSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final SSLSocketFactory trustAllSSLSocketFactory = createTrustAllSSLSocketFactory();

    public static SSLSocketFactory getTrustAllSSLSocketFactory() {
        return trustAllSSLSocketFactory;
    }

    static {
        HttpsURLConnection.setDefaultSSLSocketFactory(getTrustAllSSLSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
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
     *
     * @return                             CustomHttpResponse对象(对象方法详情请转跳)
     * @throws Exception
     */
    public static CustomHttpResponse requests(RequestObj requestObj) throws Exception {

        int maxResponseSize = requestObj.getMaxResponseSize();
        int maxRetries = requestObj.getRetries();
        int retryWaitTime = requestObj.getRetryWaitTime();
        int retryCount = 0;

        Exception lastException = null;
        
        while (retryCount <= maxRetries) {

            // 创建HttpURLConnection对象 防止重复创建
            HttpURLConnection con = null;

            try {
                // 初始化入参
                String method = requestObj.getMethod();
                String url = requestObj.getUrl();
                Map<String, String> headers = requestObj.getHeaders();
                String bearerToken = requestObj.getBearerToken();
                Boolean randomUserAgent = requestObj.getRandomUserAgent();
                boolean followRedirects = requestObj.getFollowRedirects();
                String proxies = requestObj.getProxies();
                String postMethod = requestObj.getPostMethod();
                byte[] postData = requestObj.getPostData();
                int timeOut = requestObj.getTimeOut();
                String boundary = null;

                // 创建URL对象
                URL obj = new URL(url);
                if (proxies != null && !proxies.isEmpty()) {

                    String[] tmpProxyList = proxies.toLowerCase().replace("http://", "").replace("https://", "").replace("socks://", "").split(":");

                    Proxy.Type proxyType = Proxy.Type.HTTP;     //  默认HTTP代理模式
                    if (requestObj.getProxiesType().equalsIgnoreCase("SOCKS") || proxies.toLowerCase().startsWith("socks://")) { //  兼容SOCKS代理模式
                        proxyType = Proxy.Type.SOCKS;
                    }

                    Proxy proxy = new Proxy(proxyType, new InetSocketAddress(tmpProxyList[0], Integer.parseInt(tmpProxyList[1])));
                    con = (HttpURLConnection) obj.openConnection(proxy);

                } else {
                    con = (HttpURLConnection) obj.openConnection(Proxy.NO_PROXY);
                }

                // 设置请求方法
                con.setRequestMethod(method);

                // 设置头部信息
                if(!requestObj.getNoUserAgent()) {
                    con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.1 Safari/537.36");
                }
                con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                con.setRequestProperty("Tool-Test", "Potato-Test");
                if (randomUserAgent) {
                    con.setRequestProperty("User-Agent", StrUtils.RandomUserAgent());
                }
                if (!bearerToken.isEmpty()){
                    con.setRequestProperty("Authorization", "Bearer " + bearerToken.replace("Bearer ", ""));
                }
                // 设置自定义头部信息
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        con.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                //根据post模式设置头部
                File file = requestObj.getPostFile();
                if (postMethod.equalsIgnoreCase("Raw") || file != null) {

                    con.setRequestProperty("Content-Type", "application/octet-stream");

                } else if (postMethod.equalsIgnoreCase("Chunked")) {

                    con.setRequestProperty("Transfer-Encoding", "chunked");
                    con.setChunkedStreamingMode(0);

                } else if (postMethod.equalsIgnoreCase("Form")) {
                    boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");

                    con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                } else if (postMethod.equalsIgnoreCase("Json")) {
                    con.setRequestProperty("Content-Type", "application/json");
                }
                // 设置是否重定向
                con.setInstanceFollowRedirects(followRedirects);

                // 超时时间设置 秒转换毫秒
                con.setConnectTimeout(timeOut * 1000);
                con.setReadTimeout((timeOut + 10) * 1000);

                if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE") || method.equalsIgnoreCase("PUT")) {
                    // 启用输出流
                    con.setDoOutput(true);
                    // 获取输出流
                    // 写入请求参数
                    try (OutputStream os = con.getOutputStream()) {
                        if (postMethod.equalsIgnoreCase("Form")) {

                            Map<String, Object> formParameters = requestObj.getFormParameters();

                            // 构建表单参数
                            if (formParameters != null) {
                                for (Map.Entry<String, Object> entry : formParameters.entrySet()) {
                                    String paramName = entry.getKey();
                                    Object paramValue = entry.getValue();

                                    if (paramValue instanceof String) {
                                        // 字符串参数
                                        String paramStr = (String) paramValue;

                                        StringBuilder paramBuilder = new StringBuilder();
                                        paramBuilder.append("--").append(boundary).append("\r\n");
                                        paramBuilder.append("Content-Disposition: form-data; name=\"").append(paramName).append("\"\r\n");
                                        paramBuilder.append("\r\n");
                                        paramBuilder.append(paramStr).append("\r\n");

                                        byte[] paramBytes = paramBuilder.toString().getBytes(StandardCharsets.UTF_8);
                                        os.write(paramBytes);
                                    } else if (paramValue instanceof File) {
                                        // 文件参数
                                        File fileData = (File) paramValue;

                                        StringBuilder fileBuilder = new StringBuilder();
                                        fileBuilder.append("--").append(boundary).append("\r\n");
                                        fileBuilder.append("Content-Disposition: form-data; name=\"").append(paramName)
                                                .append("\"; filename=\"").append(fileData.getName()).append("\"\r\n");
                                        fileBuilder.append("Content-Type: ").append(HttpURLConnection.guessContentTypeFromName(fileData.getName()))
                                                .append("\r\n");
                                        fileBuilder.append("\r\n");

                                        byte[] fileHeaderBytes = fileBuilder.toString().getBytes(StandardCharsets.UTF_8);
                                        os.write(fileHeaderBytes);

                                        // 写入文件内容
                                        try (InputStream fileInputStream = new FileInputStream(fileData)) {
                                            byte[] buffer = new byte[4096];
                                            int bytesRead;
                                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                                os.write(buffer, 0, bytesRead);
                                            }
                                            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            // 添加结束标识
                            byte[] boundaryBytes = ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
                            os.write(boundaryBytes);

                        } else {
                            if (postData==null || postData.length == 0) {
                                if (debugMode) {
                                    System.err.println("[警告] 未设置postData，将发送空请求体");
                                }
                                // 继续执行，不抛出异常
                            } else {
                                os.write(postData);
                            }
                        }

                        os.flush();
                    }
                }

                // 开始计时 - 在获取响应码前开始计时以准确测量服务器响应时间
                long startTime = System.currentTimeMillis();
                
                int responseCode = con.getResponseCode();

                // 创建响应对象
                CustomHttpResponse response = new CustomHttpResponse(con, maxResponseSize);
                long endTime = System.currentTimeMillis();
                response.setResponseTime(endTime - startTime);
                
                // 对于5xx服务器错误，可以考虑重试；对于4xx客户端错误，不应重试
                if (responseCode >= 500) {
                    if (debugMode) {
                        System.err.println("服务器错误 (" + responseCode + "): " + requestObj.getUrl());
                    }
                    throw new IOException("Server error: " + responseCode);
                }
                
                return response;

            } catch (IOException e) {
                if (con != null) {
                    con.disconnect();
                }
                String errorMsg = e.toString();

                // 处理SSL证书相关问题
                if(errorMsg.contains("Unsupported or unrecognized SSL message")){
                    if (debugMode) {
                        System.err.println("不支持SSL，尝试使用HTTP: " + requestObj.getUrl());
                    }
                    requestObj.setUrl(requestObj.getUrl().replaceAll("https://","http://"));
                    continue;
                }
                
                // 检查是否是FileNotFoundException（通常是404错误）
                if (e instanceof java.io.FileNotFoundException) {
                    if (debugMode) {
                        System.err.println("资源未找到 (404): " + requestObj.getUrl());
                    }
                    // 对于404错误，不重试，直接抛出异常让调用者处理
                    throw new Exception("[×] 资源未找到: " + requestObj.getUrl(), e);
                }
                
                // 记录详细的错误信息
                if (debugMode) {
                    System.err.println("网络请求异常 (重试 " + retryCount + "/" + maxRetries + "): " + 
                        requestObj.getUrl() + " - " + e.getMessage());
                    e.printStackTrace();
                }
                
                // 记录最后一次异常
                lastException = e;
                
                // 处理其他IO异常（包括 5xx 响应、读取超时、网络连接问题、连接超时等），根据情况重试
                if (retryCount < maxRetries) {
                    retryCount++;
                    // 适度增加超时时间，避免过度增长
                    int newTimeout = Math.min(requestObj.getTimeOut() + 5, 20);
                    requestObj.setTimeOut(newTimeout);
                    
                    if (debugMode) {
                        System.err.println("重试请求 (" + retryCount + "/" + maxRetries + "): " + requestObj.getUrl() + ", 错误: " + e.getMessage());
                    }
                    
                    Thread.sleep(retryWaitTime * 1000); // 重试间隔时间
                    continue; // 继续重试
                } else {
                    // 已达到最大重试次数，跳出循环
                    break;
                }
            }

        }
        
        // 如果循环结束仍未成功，抛出最后一次的异常信息
        String errorMsg = "请求失败，超出重试次数 (" + maxRetries + "): " + requestObj.getUrl();
        if (lastException != null && lastException.getMessage() != null) {
            errorMsg += ", 最后错误: " + lastException.getMessage();
        }
        throw new Exception("[×] " + errorMsg, lastException);
    }
}

