package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Headless 本机执行回归测试")
class HeadlessLocalExecutionTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("应可通过本机浏览器运行时访问本地页面并读取标题")
    void shouldNavigateLocalPageAndReadTitle() throws Exception {
        HeadlessHandler.HeadlessCompatibilityResult compatibility = HeadlessHandler.checkCompatibility();
        assertTrue(compatibility.isCompatible(), compatibility.buildDisplayMessage());

        startLocalServer("<html><head><title>PotatoTool Headless Local</title></head><body>ok</body></html>");
        HeadlessHandler.HeadlessResponse response = HeadlessHandler.navigateAndGetTitle(baseUrl());

        assertNotNull(response);
        assertTrue(response.isSuccess(), String.valueOf(response.getError()));
        assertEquals("PotatoTool Headless Local", response.getPageTitle());
    }

    @Test
    @DisplayName("应可执行本地页面脚本并返回结果")
    void shouldExecuteScriptAgainstLocalPage() throws Exception {
        HeadlessHandler.HeadlessCompatibilityResult compatibility = HeadlessHandler.checkCompatibility();
        assertTrue(compatibility.isCompatible(), compatibility.buildDisplayMessage());

        startLocalServer("<html><head><title>Local Script</title></head><body><h1 id='x'>hello</h1></body></html>");
        HeadlessHandler.HeadlessResponse response = HeadlessHandler.executeScript(baseUrl(), "document.querySelector('#x').textContent");

        assertNotNull(response);
        assertTrue(response.isSuccess(), String.valueOf(response.getError()));
        assertNotNull(response.getScriptResults());
        assertEquals("hello", response.getScriptResults().get("script_2"));
    }

    private void startLocalServer(String html) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }
}
