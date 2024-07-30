package com.potato.potatotool.utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;

public class requestUtils {

    /**
     * 信任所有SSL证书(默认开启)
     * @throws Exception
     */
    public static void TrustAllSSL() throws Exception {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

    }

    /**
     *    忽略SSL告警
     * @throws Exception
     */
    public static void ignoreSSLError() throws Exception {

        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    }


    /**
     * @param requestObj-url               请求URL
     * @param requestObj-headers           请求头
     * @param requestObj-followRedirects   是否允许重定向
     * @param requestObj-proxies           代理
     * @return                             CustomHttpResponse对象(对象方法详情请转跳)
     * @throws Exception
     */
    public static CustomHttpResponse requests(RequestObj requestObj) throws Exception {

        // 初始化入参
        String method = requestObj.getMethod();
        String url = requestObj.getUrl();
        Map<String, String> headers = requestObj.getHeaders();
        Boolean  randomUserAgent = requestObj.getRandomUserAgent();
        boolean followRedirects = requestObj.getFollowRedirects();
        String proxies = requestObj.getProxies();
        String postMethod = requestObj.getPostMethod();
        byte[] postData = requestObj.getPostData();
        int timeOut = requestObj.getTimeOut();
        String boundary = null;

        // 信任所有SSL证书
        TrustAllSSL();
        // 创建URL对象
        URL obj = new URL(url);
        // 创建HttpURLConnection对象
        HttpURLConnection con;
        if (proxies != null && !proxies.isEmpty()) {

            String[] tmpProxyList = proxies.toLowerCase().replace("http://","").replace("https://","").replace("socks://","").split(":");

            Proxy.Type proxyType = Proxy.Type.HTTP;     //  默认HTTP代理模式
            if( requestObj.getProxiesType().equalsIgnoreCase("SOCKS") || proxies.toLowerCase().startsWith("socks://") ){ //  兼容SOCKS代理模式
                proxyType = Proxy.Type.SOCKS;
            }

            Proxy proxy = new Proxy(proxyType, new InetSocketAddress( tmpProxyList[0], Integer.parseInt(tmpProxyList[1]) ));
            con = (HttpURLConnection) obj.openConnection(proxy);

        }else {
            con = (HttpURLConnection) obj.openConnection();
        }

        // 设置请求方法
        con.setRequestMethod(method);

        // 设置头部信息
        con.setRequestProperty("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.1 Safari/537.36");
        con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        con.setRequestProperty("Connection", "close");
        con.setRequestProperty("Tool-Test", "Potato-Test");
        if(randomUserAgent){
            con.setRequestProperty("User-Agent", strUtils.RandomUserAgent());
        }
        // 设置自定义头部信息
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        //根据post模式设置头部
        File file = requestObj.getPostFile();
        if(postMethod.equalsIgnoreCase("Raw") && file != null){

            con.setRequestProperty("Content-Type", "application/octet-stream");

        } else if(postMethod.equalsIgnoreCase("Chunked")){

            con.setRequestProperty("Transfer-Encoding", "chunked");
            con.setChunkedStreamingMode(0);

        } else if(postMethod.equalsIgnoreCase("Form")){
            boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");

            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        }
        // 设置是否重定向
        con.setInstanceFollowRedirects(followRedirects);

        // 超时时间设置 秒转换毫秒
        con.setConnectTimeout(timeOut * 1000);
        con.setReadTimeout(timeOut * 1000);

        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE") || method.equalsIgnoreCase("PUT")) {
            // 启用输出流
            con.setDoOutput(true);
            // 获取输出流
            // 写入请求参数
            try (OutputStream os = con.getOutputStream()) {
                if(postMethod.equalsIgnoreCase("Form")){

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

                }else{
                    if(postData.length == 0){
                        throw new Exception("[×] 未setPostData，请检查");
                    }
                    os.write(postData);
                }

                os.flush();
            }
        }

        return new CustomHttpResponse(con);
    }


}

