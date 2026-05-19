package com.potato.potatotool.utils.network;

import com.potato.potatotool.vulnScanner.testlab.RawHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestUtils 原始 URL 保留测试")
class RequestUtilsRawUrlPreservationTest {

    private RawHttpServer rawHttpServer;

    @AfterEach
    void tearDown() {
        if (rawHttpServer != null) {
            rawHttpServer.stop();
            rawHttpServer = null;
        }
    }

    @Test
    @DisplayName("preserveRawUrl=true 时请求路径不应被归一化")
    void shouldKeepRawTraversalPathWhenPreserveRawUrlEnabled() throws Exception {
        AtomicReference<String> observedRawUri = new AtomicReference<String>();
        rawHttpServer = new RawHttpServer(18892);
        rawHttpServer.createContext("/safe/goby/nodejs-path-traversal", (request, response) -> {
            observedRawUri.set(request.getRawUri());
            byte[] body = "raw-path".getBytes(StandardCharsets.UTF_8);
            response.setStatus(200);
            response.setHeader("Content-Type", "text/plain; charset=utf-8");
            response.setBody(body);
        });
        rawHttpServer.start();

        String rawUrl = "http://127.0.0.1:18892/safe/goby/nodejs-path-traversal/static/../../../a/../../../../etc/passwd";
        RequestObj requestObj = new RequestObj()
                .setUrl(rawUrl)
                .setMethod("GET")
                .setPreserveRawUrl(true)
                .setRetries(0)
                .setTimeOut(3)
                .setReadTimeout(3);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals("raw-path", response.getTextStr());
        }

        assertEquals("/safe/goby/nodejs-path-traversal/static/../../../a/../../../../etc/passwd", observedRawUri.get());
        assertTrue(observedRawUri.get().contains("../"));
    }

    @Test
    @DisplayName("preserveRawUrl=true 的 POST 请求应携带完整请求体")
    void shouldSendPostBodyWithContentLengthWhenPreserveRawUrlEnabled() throws Exception {
        AtomicReference<String> observedRawUri = new AtomicReference<String>();
        AtomicReference<String> observedBody = new AtomicReference<String>();
        rawHttpServer = new RawHttpServer(18892);
        rawHttpServer.createContext("/vuln/goby/finereport-v9-overwrite", (request, response) -> {
            observedRawUri.set(request.getRawUri());
            observedBody.set(request.getBodyAsString());
            response.setStatus(200);
            response.setHeader("Content-Type", "application/json; charset=utf-8");
            response.setBody("{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8));
        });
        rawHttpServer.start();

        String rawUrl = "http://127.0.0.1:18892/vuln/goby/finereport-v9-overwrite/WebReport/ReportServer?op=svginit&cmd=design_save_svg&filePath=chartmapsvg/../../../../WebReport/a.svg.jsp";
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        RequestObj requestObj = new RequestObj()
                .setUrl(rawUrl)
                .setMethod("POST")
                .setPreserveRawUrl(true)
                .setHeaders(headers)
                .setPostMethod("RAW")
                .setPostData("{\"__CONTENT__\":\"abc123\",\"__CHARSET__\":\"UTF-8\"}")
                .setRetries(0)
                .setTimeOut(3)
                .setReadTimeout(3);

        try (CustomHttpResponse response = RequestUtils.requests(requestObj)) {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals("{\"status\":\"success\"}", response.getTextStr());
        }

        assertTrue(observedRawUri.get().contains("chartmapsvg/../../../../WebReport/a.svg.jsp"));
        assertEquals("{\"__CONTENT__\":\"abc123\",\"__CHARSET__\":\"UTF-8\"}", observedBody.get());
    }
}
