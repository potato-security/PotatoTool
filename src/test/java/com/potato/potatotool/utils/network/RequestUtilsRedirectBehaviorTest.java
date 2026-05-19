package com.potato.potatotool.utils.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestUtils 重定向行为测试")
class RequestUtilsRedirectBehaviorTest {

    private HttpServer primaryServer;
    private HttpServer secondaryServer;

    @AfterEach
    void tearDown() {
        if (primaryServer != null) {
            primaryServer.stop(0);
            primaryServer = null;
        }
        if (secondaryServer != null) {
            secondaryServer.stop(0);
            secondaryServer = null;
        }
    }

    @Test
    @DisplayName("未开启 followRedirects 时应保留 302 初始响应")
    void shouldKeepInitial302WhenFollowRedirectsDisabled() throws Exception {
        primaryServer = HttpServer.create(new InetSocketAddress(0), 0);
        primaryServer.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/landing");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        primaryServer.createContext("/landing", exchange -> write(exchange, 200, "landing"));
        primaryServer.start();

        int port = primaryServer.getAddress().getPort();
        RequestObj requestObj = new RequestObj()
                .setUrl("http://127.0.0.1:" + port + "/redirect")
                .setMethod("GET")
                .setFollowRedirects(false)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(302, response.getResponseCode());
            assertEquals(302, response.getInitialResponseCode());
            assertTrue(response.getHeaderField("Location").contains("/landing"));
        }
    }

    @Test
    @DisplayName("开启 followRedirects 时应跟随同端口 302 跳转")
    void shouldFollow302RedirectWhenEnabled() throws Exception {
        primaryServer = HttpServer.create(new InetSocketAddress(0), 0);
        int port;
        primaryServer.start();
        port = primaryServer.getAddress().getPort();
        primaryServer.createContext("/redirect", exchange -> {
            writeRedirect(exchange, 302, "http://127.0.0.1:" + port + "/landing");
        });
        primaryServer.createContext("/landing", exchange -> write(exchange, 200, "followed"));
        RequestObj requestObj = new RequestObj()
                .setUrl("http://127.0.0.1:" + port + "/redirect")
                .setMethod("GET")
                .setFollowRedirects(true)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals(302, response.getInitialResponseCode());
            assertEquals("followed", response.getTextStr());
            assertEquals("/landing", response.getURL().getPath());
        }
    }

    @Test
    @DisplayName("开启 followRedirects 时应跟随跨端口 307 跳转")
    void shouldFollowCrossPort307RedirectWhenEnabled() throws Exception {
        secondaryServer = HttpServer.create(new InetSocketAddress(0), 0);
        secondaryServer.createContext("/landing", exchange -> write(exchange, 200, "cross-port"));
        secondaryServer.start();

        int secondaryPort = secondaryServer.getAddress().getPort();
        primaryServer = HttpServer.create(new InetSocketAddress(0), 0);
        primaryServer.createContext("/redirect307", exchange -> {
            writeRedirect(exchange, 307, "http://127.0.0.1:" + secondaryPort + "/landing");
        });
        primaryServer.start();

        int primaryPort = primaryServer.getAddress().getPort();
        RequestObj requestObj = new RequestObj()
                .setUrl("http://127.0.0.1:" + primaryPort + "/redirect307")
                .setMethod("GET")
                .setFollowRedirects(true)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals(307, response.getInitialResponseCode());
            assertEquals("cross-port", response.getTextStr());
            assertEquals(secondaryPort, response.getURL().getPort());
        }
    }

    @Test
    @DisplayName("开启 followRedirects 时应跟随 301 永久重定向")
    void shouldFollow301RedirectWhenEnabled() throws Exception {
        primaryServer = HttpServer.create(new InetSocketAddress(0), 0);
        primaryServer.start();
        int port = primaryServer.getAddress().getPort();
        primaryServer.createContext("/redirect301", exchange -> {
            writeRedirect(exchange, 301, "http://127.0.0.1:" + port + "/final301");
        });
        primaryServer.createContext("/final301", exchange -> write(exchange, 200, "permanent"));

        RequestObj requestObj = new RequestObj()
                .setUrl("http://127.0.0.1:" + port + "/redirect301")
                .setMethod("GET")
                .setFollowRedirects(true)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals(301, response.getInitialResponseCode());
            assertEquals("permanent", response.getTextStr());
            assertEquals("/final301", response.getURL().getPath());
        }
    }

    @Test
    @DisplayName("循环重定向应以异常结束而不是静默成功")
    void shouldFailOnRedirectLoop() throws Exception {
        primaryServer = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicInteger hops = new AtomicInteger();
        primaryServer.createContext("/loop-a", exchange -> {
            hops.incrementAndGet();
            writeRedirect(exchange, 302, "/loop-b");
        });
        primaryServer.createContext("/loop-b", exchange -> {
            hops.incrementAndGet();
            writeRedirect(exchange, 302, "/loop-a");
        });
        primaryServer.start();

        int port = primaryServer.getAddress().getPort();
        RequestObj requestObj = new RequestObj()
                .setUrl("http://127.0.0.1:" + port + "/loop-a")
                .setMethod("GET")
                .setFollowRedirects(true)
                .setRetries(0)
                .setTimeOut(3)
                .setReadTimeout(3);

        try {
            RequestUtils.requests(requestObj);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            assertTrue(message.contains("Too many follow-up requests") || message.contains("请求失败"));
            assertTrue(hops.get() >= 2, "循环重定向至少应发生两跳");
            return;
        }

        assertFalse(true, "循环重定向不应返回成功响应");
    }

    private void write(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void writeRedirect(HttpExchange exchange, int statusCode, String location) throws IOException {
        byte[] bytes = "redirect".getBytes("UTF-8");
        exchange.getResponseHeaders().set("Location", location);
        exchange.getResponseHeaders().set("Connection", "close");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
