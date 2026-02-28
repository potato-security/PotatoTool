package com.potato.potatotool.vulnScanner.testlab;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 底层 HTTP 服务器 - 保留原始 URL，不进行路径归一化
 * 
 * 用于测试路径遍历、URL 编码等特殊 POC
 * 
 * @author Potato
 */
public class RawHttpServer {
    
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // 路由表：前缀 -> Handler
    private final Map<String, RawHttpHandler> handlers = new ConcurrentHashMap<>();
    private RawHttpHandler defaultHandler;
    
    public RawHttpServer(int port) {
        this.port = port;
    }
    
    /**
     * 注册请求处理器
     * @param pathPrefix 路径前缀（会匹配以此开头的所有请求）
     * @param handler 处理器
     */
    public void createContext(String pathPrefix, RawHttpHandler handler) {
        handlers.put(pathPrefix, handler);
    }
    
    /**
     * 设置默认处理器
     */
    public void setDefaultHandler(RawHttpHandler handler) {
        this.defaultHandler = handler;
    }
    
    /**
     * 启动服务器
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(100); // 100ms 超时，用于优雅关闭
        executor = Executors.newFixedThreadPool(20);
        running.set(true);
        
        // 启动接受连接的线程
        Thread acceptThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleConnection(clientSocket));
                } catch (SocketTimeoutException e) {
                    // 正常超时，继续循环
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Accept error: " + e.getMessage());
                    }
                }
            }
        }, "RawHttpServer-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        
        System.out.println("底层 HTTP 服务器已启动: http://127.0.0.1:" + port);
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("底层 HTTP 服务器已停止");
    }
    
    /**
     * 处理连接
     */
    private void handleConnection(Socket socket) {
        try {
            socket.setSoTimeout(5000); // 5秒读取超时
            
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            
            // 解析请求行（不解码 URL）
            String requestLine = readLine(is);
            if (requestLine == null || requestLine.isEmpty()) {
                socket.close();
                return;
            }
            
            // 解析请求行: METHOD URI HTTP/1.1
            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                socket.close();
                return;
            }
            
            String method = parts[0];
            String rawUri = parts[1]; // 原始 URI，不解码
            
            // 解析请求头
            Map<String, String> headers = new LinkedHashMap<>();
            String headerLine;
            int contentLength = 0;
            while ((headerLine = readLine(is)) != null && !headerLine.isEmpty()) {
                int colonIdx = headerLine.indexOf(':');
                if (colonIdx > 0) {
                    String key = headerLine.substring(0, colonIdx).trim();
                    String value = headerLine.substring(colonIdx + 1).trim();
                    headers.put(key, value);
                    if ("Content-Length".equalsIgnoreCase(key)) {
                        try {
                            contentLength = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            contentLength = 0;
                        }
                    }
                }
            }
            
            // 读取请求体
            byte[] body = new byte[0];
            if (contentLength > 0) {
                body = new byte[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int n = is.read(body, read, contentLength - read);
                    if (n < 0) break;
                    read += n;
                }
            }
            
            // 创建请求对象
            RawHttpRequest request = new RawHttpRequest(method, rawUri, headers, body);
            RawHttpResponse response = new RawHttpResponse();
            
            // 查找匹配的 Handler
            RawHttpHandler handler = findHandler(rawUri);
            if (handler != null) {
                try {
                    handler.handle(request, response);
                } catch (Exception e) {
                    response.setStatus(500);
                    response.setBody("Internal Server Error: " + e.getMessage());
                }
            } else if (defaultHandler != null) {
                defaultHandler.handle(request, response);
            } else {
                response.setStatus(404);
                response.setBody("Not Found");
            }
            
            // 发送响应
            sendResponse(os, response);
            
        } catch (Exception e) {
            // 忽略连接错误
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
    
    /**
     * 查找匹配的 Handler（最长前缀匹配）
     */
    private RawHttpHandler findHandler(String rawUri) {
        // 提取路径部分（不解码）
        String path = rawUri;
        int queryIdx = rawUri.indexOf('?');
        if (queryIdx >= 0) {
            path = rawUri.substring(0, queryIdx);
        }
        
        // 最长前缀匹配
        String bestMatch = null;
        for (String prefix : handlers.keySet()) {
            if (path.startsWith(prefix)) {
                if (bestMatch == null || prefix.length() > bestMatch.length()) {
                    bestMatch = prefix;
                }
            }
        }
        
        return bestMatch != null ? handlers.get(bestMatch) : null;
    }
    
    /**
     * 读取一行（遇到 \r\n 结束）
     */
    private String readLine(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int prev = -1;
        int b;
        while ((b = is.read()) != -1) {
            if (b == '\n' && prev == '\r') {
                // 去掉末尾的 \r
                byte[] bytes = baos.toByteArray();
                if (bytes.length > 0) {
                    return new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8);
                }
                return "";
            }
            baos.write(b);
            prev = b;
        }
        return baos.size() > 0 ? new String(baos.toByteArray(), StandardCharsets.UTF_8) : null;
    }
    
    /**
     * 发送响应
     */
    private void sendResponse(OutputStream os, RawHttpResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(response.getStatus()).append(" ").append(getStatusText(response.getStatus())).append("\r\n");
        
        // 添加响应头
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        
        byte[] bodyBytes = response.getBodyBytes();
        if (!response.getHeaders().containsKey("Content-Length")) {
            sb.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        }
        if (!response.getHeaders().containsKey("Content-Type")) {
            sb.append("Content-Type: text/plain\r\n");
        }
        sb.append("Connection: close\r\n");
        sb.append("\r\n");
        
        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        os.write(bodyBytes);
        os.flush();
    }
    
    private String getStatusText(int status) {
        switch (status) {
            case 200: return "OK";
            case 201: return "Created";
            case 302: return "Found";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "Unknown";
        }
    }
    
    /**
     * HTTP 请求（保留原始 URL）
     */
    public static class RawHttpRequest {
        private final String method;
        private final String rawUri;      // 原始 URI（未解码）
        private final Map<String, String> headers;
        private final byte[] body;
        
        public RawHttpRequest(String method, String rawUri, Map<String, String> headers, byte[] body) {
            this.method = method;
            this.rawUri = rawUri;
            this.headers = headers;
            this.body = body;
        }
        
        public String getMethod() { return method; }
        
        /** 获取原始 URI（未解码，保留 %XX 编码） */
        public String getRawUri() { return rawUri; }
        
        /** 获取原始路径部分（未解码） */
        public String getRawPath() {
            int queryIdx = rawUri.indexOf('?');
            return queryIdx >= 0 ? rawUri.substring(0, queryIdx) : rawUri;
        }
        
        /** 获取查询字符串 */
        public String getQuery() {
            int queryIdx = rawUri.indexOf('?');
            return queryIdx >= 0 ? rawUri.substring(queryIdx + 1) : null;
        }
        
        public Map<String, String> getHeaders() { return headers; }
        public String getHeader(String name) { return headers.get(name); }
        public byte[] getBody() { return body; }
        public String getBodyAsString() { return new String(body, StandardCharsets.UTF_8); }
    }
    
    /**
     * HTTP 响应
     */
    public static class RawHttpResponse {
        private int status = 200;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private byte[] body = new byte[0];
        
        public void setStatus(int status) { this.status = status; }
        public int getStatus() { return status; }
        
        public void setHeader(String name, String value) { headers.put(name, value); }
        public Map<String, String> getHeaders() { return headers; }
        
        public void setBody(String body) { this.body = body.getBytes(StandardCharsets.UTF_8); }
        public void setBody(byte[] body) { this.body = body; }
        public byte[] getBodyBytes() { return body; }
        
        public void setContentType(String contentType) { setHeader("Content-Type", contentType); }
    }
    
    /**
     * 请求处理器接口
     */
    public interface RawHttpHandler {
        void handle(RawHttpRequest request, RawHttpResponse response) throws Exception;
    }
}
