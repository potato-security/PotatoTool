package com.potato.potatotool.utils.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestUtils 代理路由端到端测试")
class RequestUtilsProxyRoutingTest {

    private HttpServer targetServer;
    private ServerSocket proxyServer;
    private Thread proxyThread;
    private ProxySelector originalProxySelector;

    @AfterEach
    void tearDown() throws Exception {
        if (originalProxySelector != null) {
            ProxySelector.setDefault(originalProxySelector);
            originalProxySelector = null;
        }
        if (targetServer != null) {
            targetServer.stop(0);
            targetServer = null;
        }
        if (proxyServer != null && !proxyServer.isClosed()) {
            proxyServer.close();
            proxyServer = null;
        }
        if (proxyThread != null) {
            proxyThread.interrupt();
            proxyThread.join(1000);
            proxyThread = null;
        }
    }

    @Test
    @DisplayName("未设置代理时请求直连目标服务")
    void shouldSendRequestDirectlyWhenProxyIsNull() throws Exception {
        AtomicInteger targetHits = new AtomicInteger();
        AtomicInteger proxyHits = new AtomicInteger();
        HttpServer server = startTargetServer(targetHits);
        ServerSocket localProxy = startProxyServer(proxyHits, null);

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/direct";
        RequestObj requestObj = new RequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setProxies(null)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals("direct", response.getTextStr());
        }

        assertEquals(1, targetHits.get());
        assertEquals(0, proxyHits.get());
        assertFalse(localProxy.isClosed());
    }

    @Test
    @DisplayName("即使系统代理已设置，未配置业务代理时也应直连目标服务")
    void shouldIgnoreSystemProxyWhenBusinessProxyIsUnset() throws Exception {
        AtomicInteger targetHits = new AtomicInteger();
        AtomicInteger proxyHits = new AtomicInteger();
        HttpServer server = startTargetServer(targetHits);
        ServerSocket localProxy = startProxyServer(proxyHits, null);

        originalProxySelector = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Collections.singletonList(new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress("127.0.0.1", localProxy.getLocalPort())));
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            }
        });

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/system-proxy";
        RequestObj requestObj = new RequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setProxies(null)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals("direct", response.getTextStr());
        }

        assertEquals(1, targetHits.get());
        assertEquals(0, proxyHits.get());
        assertFalse(localProxy.isClosed());
    }

    @Test
    @DisplayName("设置 HTTP 代理时请求应经过代理")
    void shouldSendRequestThroughProxyWhenProxyConfigured() throws Exception {
        AtomicInteger targetHits = new AtomicInteger();
        AtomicInteger proxyHits = new AtomicInteger();
        CountDownLatch proxyObserved = new CountDownLatch(1);
        HttpServer server = startTargetServer(targetHits);
        ServerSocket localProxy = startProxyServer(proxyHits, proxyObserved);

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/proxied";
        String proxyAddress = "http://127.0.0.1:" + localProxy.getLocalPort();
        RequestObj requestObj = new RequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setProxies(proxyAddress)
                .setProxiesType("HTTP")
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals("proxied", response.getTextStr());
        }

        assertTrue(proxyObserved.await(1, TimeUnit.SECONDS));
        assertEquals(1, proxyHits.get());
        assertEquals(0, targetHits.get());
    }

    private HttpServer startTargetServer(AtomicInteger targetHits) throws IOException {
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetServer.createContext("/", exchange -> respond(exchange, targetHits, "direct"));
        targetServer.start();
        return targetServer;
    }

    private ServerSocket startProxyServer(AtomicInteger proxyHits, CountDownLatch observedLatch) throws IOException {
        proxyServer = new ServerSocket(0);
        proxyThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Socket socket = proxyServer.accept()) {
                    proxyHits.incrementAndGet();
                    if (observedLatch != null) {
                        observedLatch.countDown();
                    }
                    handleProxyRequest(socket);
                } catch (IOException e) {
                    if (proxyServer == null || proxyServer.isClosed()) {
                        return;
                    }
                }
            }
        }, "test-http-proxy");
        proxyThread.setDaemon(true);
        proxyThread.start();
        return proxyServer;
    }

    private void handleProxyRequest(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        String line = reader.readLine();
        if (line == null) {
            return;
        }

        // Drain remaining headers to finish the request cleanly.
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // no-op
        }

        byte[] body = "proxied".getBytes(StandardCharsets.UTF_8);
        OutputStream output = socket.getOutputStream();
        output.write(("HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        output.write(body);
        output.flush();
    }

    private void respond(HttpExchange exchange, AtomicInteger hitCounter, String bodyText) throws IOException {
        hitCounter.incrementAndGet();
        byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
