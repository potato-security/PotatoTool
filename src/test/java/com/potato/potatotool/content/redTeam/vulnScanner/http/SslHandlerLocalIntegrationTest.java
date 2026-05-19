package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SslHandler 本机集成测试")
class SslHandlerLocalIntegrationTest {

    private SSLServerSocket sslServerSocket;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws Exception {
        if (sslServerSocket != null && !sslServerSocket.isClosed()) {
            sslServerSocket.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread.join(1000);
        }
        sslServerSocket = null;
        serverThread = null;
    }

    @Test
    @DisplayName("应能连接本机 SSL 服务并提取证书信息")
    void shouldInspectLocalSslServer() throws Exception {
        CountDownLatch handshakeObserved = new CountDownLatch(1);
        SSLContext serverContext = createServerContext();
        sslServerSocket = (SSLServerSocket) serverContext.getServerSocketFactory()
                .createServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
        int port = sslServerSocket.getLocalPort();

        serverThread = new Thread(() -> {
            try (SSLSocket socket = (SSLSocket) sslServerSocket.accept()) {
                socket.setUseClientMode(false);
                socket.startHandshake();
                handshakeObserved.countDown();
                Thread.sleep(100);
            } catch (Exception ignored) {
            }
        }, "ssl-handler-local-server");
        serverThread.setDaemon(true);
        serverThread.start();

        SslHandler.SslResponse response = SslHandler.check("127.0.0.1:" + port, 3000);

        assertTrue(handshakeObserved.await(3, TimeUnit.SECONDS), "本机 SSL 握手未完成");
        assertTrue(response.isSuccess(), "预期本机 SSL 检测成功，实际: " + response.getError());
        assertTrue(response.isConnected());
        assertNotNull(response.getProtocol());
        assertNotNull(response.getCipherSuite());
        assertNotNull(response.getSubjectDN());
        assertNotNull(response.getIssuerDN());
        assertTrue(response.getChainLength() >= 1);
        assertTrue(response.isCertificateValid());
        assertFalse(response.isExpired());
        assertFalse(response.isSelfSigned());
    }

    @Test
    @DisplayName("连接已关闭端口时应返回失败")
    void shouldReturnFailureForClosedLocalPort() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        SslHandler.SslResponse response = SslHandler.check("127.0.0.1:" + port, 1000);

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertTrue(response.getError().contains("连接失败") || response.getError().contains("refused"));
    }

    private SSLContext createServerContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("certs/ai-gateway-client.p12")) {
            if (inputStream == null) {
                throw new IllegalStateException("未找到测试证书 certs/ai-gateway-client.p12");
            }
            keyStore.load(inputStream, "potato".toCharArray());
        }

        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "potato".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
        return sslContext;
    }
}
