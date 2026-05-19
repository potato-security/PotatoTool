package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JARM 本机集成测试")
class JarmLocalIntegrationTest {

    private ServerSocket serverSocket;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws Exception {
        JarmHandler.clearCache();
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
    @DisplayName("应能对本机伪 TLS 服务计算 JARM 指纹")
    void shouldComputeFingerprintAgainstLocalServer() throws Exception {
        JarmHandler.clearCache();
        CountDownLatch probeObserved = new CountDownLatch(10);
        byte[] serverHello = createMinimalServerHello();
        serverSocket = new ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
        int port = serverSocket.getLocalPort();

        serverThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed() && probeObserved.getCount() > 0) {
                    try (Socket socket = serverSocket.accept()) {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(serverHello);
                        outputStream.flush();
                        probeObserved.countDown();
                    }
                }
            } catch (SocketException ignored) {
            } catch (Exception ignored) {
            }
        }, "jarm-local-server");
        serverThread.setDaemon(true);
        serverThread.start();

        JarmHandler.JarmResult result = JarmHandler.compute("127.0.0.1", port);

        assertTrue(probeObserved.await(5, TimeUnit.SECONDS), "未观察到完整的 10 次 JARM 探测");
        assertTrue(result.isSuccess(), "预期 JARM 本机探测成功，实际: " + result.getError());
        assertNotNull(result.getFingerprint());
        assertEquals(62, result.getFingerprint().length());
        assertTrue(JarmHandler.isValidFingerprint(result.getFingerprint()));
        assertEquals(1, JarmFingerprinter.getCacheSize());
    }

    @Test
    @DisplayName("所有探测均连接失败时不应返回伪造指纹")
    void shouldReturnFailureWhenAllProbesCannotConnect() throws Exception {
        JarmHandler.clearCache();
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        JarmHandler.JarmResult result = JarmHandler.compute("127.0.0.1", port);

        assertFalse(result.isSuccess());
        assertEquals("", result.getFingerprint());
        assertNotNull(result.getError());
    }

    private byte[] createMinimalServerHello() {
        return new byte[] {
                0x16, 0x03, 0x03, 0x00, 0x2c,
                0x02, 0x00, 0x00, 0x28,
                0x03, 0x03,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
                0x00,
                (byte) 0xC0, 0x2F,
                0x00,
                0x00, 0x00
        };
    }
}
