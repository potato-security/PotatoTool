package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TcpHandler 本机覆盖测试")
class TcpHandlerCoverageTest {

    private ServerSocket serverSocket;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread.join(1000);
        }
        serverSocket = null;
        serverThread = null;
    }

    @Test
    @DisplayName("应完成 TCP 连接、发送和读取响应")
    void shouldSendAndReceiveTcpPayload() throws Exception {
        CountDownLatch observed = new CountDownLatch(1);
        serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        serverThread = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                byte[] buffer = new byte[64];
                int length = inputStream.read(buffer);
                String received = new String(buffer, 0, length, StandardCharsets.UTF_8);
                if ("ping".equals(received)) {
                    observed.countDown();
                    outputStream.write("pong".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
            } catch (Exception ignored) {
            }
        }, "tcp-handler-test-server");
        serverThread.setDaemon(true);
        serverThread.start();

        TcpHandler.TcpResponse response = TcpHandler.send("127.0.0.1:" + port, "ping", 2000);

        assertTrue(observed.await(1, TimeUnit.SECONDS));
        assertTrue(response.isSuccess());
        assertEquals("pong", response.getDataString());
        assertEquals(4, response.getBytesSent());
        assertEquals(4, response.getBytesReceived());
    }

    @Test
    @DisplayName("连接被拒绝时应返回失败和错误原因")
    void shouldReportConnectionRefused() {
        TcpHandler.TcpResponse response = TcpHandler.send("127.0.0.1:1", "ping", 200);

        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("连接被拒绝") || response.getError().contains("IO错误"));
    }

    @Test
    @DisplayName("服务端不返回数据时应报告连接超时")
    void shouldReportTimeoutWhenServerDoesNotRespond() throws Exception {
        serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        serverThread = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                Thread.sleep(500);
                socket.close();
            } catch (Exception ignored) {
            }
        }, "tcp-handler-timeout-server");
        serverThread.setDaemon(true);
        serverThread.start();

        TcpHandler.TcpResponse response = TcpHandler.send("127.0.0.1:" + port, "ping", 100);

        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("连接超时"), "应返回超时错误，实际: " + response.getError());
    }
}
