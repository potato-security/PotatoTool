package com.potato.potatotool.utils.network;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestUtils HTTP 请求覆盖测试")
class RequestUtilsHttpCoverageTest {

    private static final Gson GSON = new Gson();

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("GET 请求应发送 Cookie、自定义 Header 和调用方 User-Agent")
    void shouldSendGetWithCookieHeaderAndUserAgent() throws Exception {
        Map<String, String> observed = new HashMap<String, String>();
        startServer(exchange -> {
            observed.put("cookie", exchange.getRequestHeaders().getFirst("Cookie"));
            observed.put("custom", exchange.getRequestHeaders().getFirst("X-Test"));
            observed.put("userAgent", exchange.getRequestHeaders().getFirst("User-Agent"));
            writeJson(exchange, 200, "{\"ok\":true}");
        });

        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Cookie", "sid=abc; mode=test");
        headers.put("X-Test", "hello");
        headers.put("User-Agent", "PotatoTool-Test-UA");
        RequestObj requestObj = new RequestObj()
                .setUrl(baseUrl("/get"))
                .setMethod("GET")
                .setHeaders(headers)
                .setRandomUserAgent(false)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals("{\"ok\":true}", response.getTextStr());
        }

        assertEquals("sid=abc; mode=test", observed.get("cookie"));
        assertEquals("hello", observed.get("custom"));
        assertEquals("PotatoTool-Test-UA", observed.get("userAgent"));
    }

    @Test
    @DisplayName("POST JSON 请求应保留 Content-Type 并发送请求体")
    void shouldSendJsonBody() throws Exception {
        Map<String, String> observed = new HashMap<String, String>();
        startServer(exchange -> {
            observed.put("method", exchange.getRequestMethod());
            observed.put("contentType", exchange.getRequestHeaders().getFirst("Content-Type"));
            observed.put("body", readBody(exchange));
            writeJson(exchange, 200, "{\"status\":\"json\"}");
        });

        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        RequestObj requestObj = new RequestObj()
                .setUrl(baseUrl("/json"))
                .setMethod("POST")
                .setHeaders(headers)
                .setPostData("{\"name\":\"potato\"}")
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertEquals(200, response.getResponseCode());
            assertEquals("{\"status\":\"json\"}", response.getTextStr());
        }

        assertEquals("POST", observed.get("method"));
        assertEquals("application/json; charset=utf-8", observed.get("contentType"));
        assertEquals("{\"name\":\"potato\"}", observed.get("body"));
    }

    @Test
    @DisplayName("FORM 请求应构造 multipart 并包含文本字段")
    void shouldSendMultipartFormField() throws Exception {
        Map<String, String> observed = new HashMap<String, String>();
        startServer(exchange -> {
            observed.put("contentType", exchange.getRequestHeaders().getFirst("Content-Type"));
            observed.put("body", readBody(exchange));
            writeJson(exchange, 200, "{\"status\":\"form\"}");
        });

        Map<String, Object> form = new LinkedHashMap<String, Object>();
        form.put("username", "admin");
        form.put("mode", "baseline");
        RequestObj requestObj = new RequestObj()
                .setUrl(baseUrl("/form"))
                .setMethod("POST")
                .setFormParameters(form)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertEquals(200, response.getResponseCode());
            assertEquals("{\"status\":\"form\"}", response.getTextStr());
        }

        assertTrue(observed.get("contentType").startsWith("multipart/form-data;"));
        assertTrue(observed.get("body").contains("name=\"username\""));
        assertTrue(observed.get("body").contains("admin"));
        assertTrue(observed.get("body").contains("name=\"mode\""));
    }

    @Test
    @DisplayName("FORM 请求应包含 multipart 文件上传内容")
    void shouldSendMultipartFileUpload(@TempDir Path tempDir) throws Exception {
        Map<String, String> observed = new HashMap<String, String>();
        startServer(exchange -> {
            observed.put("contentType", exchange.getRequestHeaders().getFirst("Content-Type"));
            observed.put("body", readBody(exchange));
            writeJson(exchange, 200, "{\"status\":\"file\"}");
        });

        Path uploadFile = tempDir.resolve("payload.txt");
        Files.write(uploadFile, "file-content".getBytes(StandardCharsets.UTF_8));
        Map<String, Object> form = new LinkedHashMap<String, Object>();
        form.put("file", new RequestObj.FormFilePart(uploadFile.toFile(), "payload.txt", "text/plain"));
        RequestObj requestObj = new RequestObj()
                .setUrl(baseUrl("/upload"))
                .setMethod("POST")
                .setFormParameters(form)
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertEquals(200, response.getResponseCode());
            assertEquals("{\"status\":\"file\"}", response.getTextStr());
        }

        assertTrue(observed.get("contentType").startsWith("multipart/form-data;"));
        assertTrue(observed.get("body").contains("filename=\"payload.txt\""));
        assertTrue(observed.get("body").contains("file-content"));
    }

    @Test
    @DisplayName("Bearer Token 应转换为 Authorization 请求头")
    void shouldApplyBearerTokenHeader() throws Exception {
        Map<String, String> observed = new HashMap<String, String>();
        startServer(exchange -> {
            observed.put("auth", exchange.getRequestHeaders().getFirst("Authorization"));
            writeJson(exchange, 200, "{\"status\":\"bearer\"}");
        });

        RequestObj requestObj = new RequestObj()
                .setUrl(baseUrl("/auth"))
                .setMethod("GET")
                .setBearerToken("token-123")
                .setRetries(0);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertEquals(200, response.getResponseCode());
            assertEquals("{\"status\":\"bearer\"}", response.getTextStr());
        }

        assertEquals("Bearer token-123", observed.get("auth"));
    }

    private void startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> handler.handle(exchange));
        server.start();
    }

    private String baseUrl(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private interface HttpHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
