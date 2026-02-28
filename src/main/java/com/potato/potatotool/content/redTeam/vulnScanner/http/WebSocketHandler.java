package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 处理器
 * 用于执行 WebSocket 通信并接收响应
 * 支持 Nuclei WebSocket 协议的所有功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class WebSocketHandler {
    
    /**
     * WebSocket 响应结果
     */
    public static class WebSocketResponse {
        private String address;                 // WebSocket 地址
        private boolean connected;              // 是否连接成功
        private long duration;                  // 连接和通信总耗时（毫秒）
        private List<String> receivedMessages;  // 接收到的消息列表
        private String raw;                     // 原始响应（所有消息拼接）
        private boolean success;                // 操作是否成功
        private String error;                   // 错误信息
        private int statusCode;                 // HTTP 握手状态码
        
        public WebSocketResponse() {
            this.receivedMessages = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public List<String> getReceivedMessages() { return receivedMessages; }
        public void setReceivedMessages(List<String> receivedMessages) { this.receivedMessages = receivedMessages; }
        
        public String getRaw() { return raw; }
        public void setRaw(String raw) { this.raw = raw; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        
        @Override
        public String toString() {
            return "WebSocketResponse{" +
                    "address='" + address + '\'' +
                    ", connected=" + connected +
                    ", duration=" + duration + "ms" +
                    ", receivedMessages=" + receivedMessages.size() +
                    ", success=" + success +
                    '}';
        }
    }
    
    /**
     * 自定义 WebSocket 客户端
     */
    private static class CustomWebSocketClient extends WebSocketClient {
        private final List<String> receivedMessages = new ArrayList<>();
        private final CountDownLatch connectLatch = new CountDownLatch(1);
        private final CountDownLatch closeLatch = new CountDownLatch(1);
        private volatile boolean isConnected = false;
        private volatile String errorMessage = null;
        private volatile int httpStatusCode = 0;
        
        public CustomWebSocketClient(URI serverUri, Map<String, String> headers) {
            super(serverUri, headers);
        }
        
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            isConnected = true;
            httpStatusCode = handshakedata.getHttpStatus();
            System.out.println("✓ WebSocket 连接已建立: " + getURI());
            connectLatch.countDown();
        }
        
        @Override
        public void onMessage(String message) {
            receivedMessages.add(message);
            System.out.println("← 接收消息: " + message);
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            String source = remote ? "服务器" : "客户端";
            System.out.println("✗ WebSocket 连接已关闭 [" + source + "] - 代码: " + code + ", 原因: " + reason);
            closeLatch.countDown();
        }
        
        @Override
        public void onError(Exception ex) {
            errorMessage = ex.getMessage();
            System.err.println("✗ WebSocket 错误: " + ex.getMessage());
            ex.printStackTrace();
            connectLatch.countDown();
            closeLatch.countDown();
        }
        
        public List<String> getReceivedMessages() {
            return new ArrayList<>(receivedMessages);
        }
        
        public boolean waitForConnection(long timeout, TimeUnit unit) {
            try {
                return connectLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        public void waitForClose(long timeout, TimeUnit unit) {
            try {
                closeLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        public boolean isConnectedSuccessfully() {
            return isConnected;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public int getHttpStatusCode() {
            return httpStatusCode;
        }
    }
    
    /**
     * 执行 WebSocket 通信
     * 
     * @param address WebSocket 地址（如 "ws://echo.websocket.org"）
     * @param messages 要发送的消息列表
     * @return WebSocket 响应
     */
    public static WebSocketResponse communicate(String address, List<String> messages) {
        return communicate(address, messages, null, 10);
    }
    
    /**
     * 执行 WebSocket 通信（完整参数）
     * 
     * @param address WebSocket 地址
     * @param messages 要发送的消息列表
     * @param headers 自定义请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return WebSocket 响应
     */
    public static WebSocketResponse communicate(String address, List<String> messages, 
                                                Map<String, String> headers, int timeoutSeconds) {
        WebSocketResponse response = new WebSocketResponse();
        response.setAddress(address);
        
        long startTime = System.currentTimeMillis();
        CustomWebSocketClient client = null;
        
        try {
            // 解析 WebSocket URI
            URI serverUri = new URI(address);
            
            // 创建 WebSocket 客户端
            if (headers != null && !headers.isEmpty()) {
                client = new CustomWebSocketClient(serverUri, headers);
            } else {
                client = new CustomWebSocketClient(serverUri, new HashMap<String, String>());
            }
            
            // 设置连接超时
            client.setConnectionLostTimeout(timeoutSeconds);
            
            // 连接到 WebSocket 服务器
            System.out.println("→ 正在连接 WebSocket: " + address);
            client.connect();
            
            // 等待连接建立
            boolean connected = client.waitForConnection(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!connected || !client.isConnectedSuccessfully()) {
                response.setSuccess(false);
                response.setConnected(false);
                response.setError("连接超时或失败: " + 
                    (client.getErrorMessage() != null ? client.getErrorMessage() : "未知错误"));
                return response;
            }
            
            response.setConnected(true);
            response.setStatusCode(client.getHttpStatusCode());
            
            // 发送消息
            if (messages != null && !messages.isEmpty()) {
                for (String message : messages) {
                    System.out.println("→ 发送消息: " + message);
                    client.send(message);
                    
                    // 等待响应（给服务器一些时间处理）
                    Thread.sleep(500);
                }
            }
            
            // 等待一段时间以接收所有响应
            Thread.sleep(2000);
            
            // 获取接收到的消息
            List<String> receivedMessages = client.getReceivedMessages();
            response.setReceivedMessages(receivedMessages);
            
            // 生成原始响应
            if (!receivedMessages.isEmpty()) {
                response.setRaw(String.join("\n", receivedMessages));
            }
            
            response.setSuccess(true);
            
        } catch (URISyntaxException e) {
            response.setSuccess(false);
            response.setError("无效的 WebSocket URI: " + e.getMessage());
            System.err.println("WebSocket 通信失败 - 无效的 URI: " + address + " - " + e.getMessage());
        } catch (InterruptedException e) {
            response.setSuccess(false);
            response.setError("操作被中断: " + e.getMessage());
            System.err.println("WebSocket 通信被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("WebSocket 通信异常: " + e.getMessage());
            System.err.println("WebSocket 通信失败: " + address + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭连接
            if (client != null && client.isOpen()) {
                client.close();
                client.waitForClose(2, TimeUnit.SECONDS);
            }
            
            // 计算总耗时
            response.setDuration(System.currentTimeMillis() - startTime);
        }
        
        return response;
    }
    
    /**
     * 简单的 WebSocket 测试（只发送一条消息）
     */
    public static WebSocketResponse sendMessage(String address, String message) {
        return communicate(address, Arrays.asList(message), null, 10);
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试 WebSocket Echo 服务器
        System.out.println("=== 测试 WebSocket Echo 服务器 ===");
        
        String echoServer = "wss://echo.websocket.org/.ws";
        List<String> messages = Arrays.asList(
            "Hello WebSocket!",
            "Test message 1",
            "Test message 2"
        );
        
        WebSocketResponse response = communicate(echoServer, messages);
        
        System.out.println("\n=== 测试结果 ===");
        System.out.println(response);
        System.out.println("连接成功: " + response.isConnected());
        System.out.println("操作成功: " + response.isSuccess());
        System.out.println("HTTP 状态码: " + response.getStatusCode());
        System.out.println("耗时: " + response.getDuration() + "ms");
        System.out.println("接收到的消息数: " + response.getReceivedMessages().size());
        System.out.println("接收到的消息:");
        for (String msg : response.getReceivedMessages()) {
            System.out.println("  - " + msg);
        }
        
        // 测试简单发送
        System.out.println("\n=== 测试简单发送 ===");
        WebSocketResponse response2 = sendMessage(echoServer, "{\"action\":\"ping\"}");
        System.out.println(response2);
        System.out.println("接收到的消息: " + response2.getReceivedMessages());
    }
}



