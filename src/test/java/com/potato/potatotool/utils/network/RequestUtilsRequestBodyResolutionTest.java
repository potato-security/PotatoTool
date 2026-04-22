package com.potato.potatotool.utils.network;

import okhttp3.MediaType;
import okhttp3.Request;
import okio.Buffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestUtils 请求体模式端到端测试")
class RequestUtilsRequestBodyResolutionTest {

    @Test
    @DisplayName("urlencoded raw body 保持原始透传，不走 FORM")
    void urlEncodedRawBodyShouldKeepRawPayload() throws Exception {
        RequestObj requestObj = new RequestObj()
                .setMethod("POST")
                .setUrl("http://example.com/test")
                .setHeaders(Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded"))
                .setPostData("a=1&b=2");

        Request request = RequestUtils.buildRequest(requestObj);
        String payload = readBody(request);

        assertEquals("POST", request.method());
        assertEquals("a=1&b=2", payload);
        assertFalse(payload.contains("form-data"));
        assertFalse(payload.contains("----"));
    }

    @Test
    @DisplayName("JSON header + body 走 JSON 编码，避免 octet-stream")
    void jsonHeaderWithBodyShouldUseJsonRequestBody() throws Exception {
        RequestObj requestObj = new RequestObj()
                .setMethod("POST")
                .setUrl("http://example.com/test")
                .setHeaders(Collections.singletonMap("Content-Type", "application/json; charset=utf-8"))
                .setPostData("{\"name\":\"potato\"}");

        Request request = RequestUtils.buildRequest(requestObj);
        String payload = readBody(request);
        MediaType mediaType = request.body() == null ? null : request.body().contentType();

        assertEquals("POST", request.method());
        assertEquals("{\"name\":\"potato\"}", payload);
        assertNotNull(mediaType);
        assertEquals("application/json; charset=utf-8", mediaType.toString());
    }

    @Test
    @DisplayName("显式 RAW + JSON header 时保持 RAW")
    void explicitRawShouldNotBeOverriddenByJsonHeader() throws Exception {
        RequestObj requestObj = new RequestObj()
                .setMethod("POST")
                .setUrl("http://example.com/test")
                .setHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .setPostData("{\"name\":\"potato\"}")
                .setPostMethod("RAW");

        Request request = RequestUtils.buildRequest(requestObj);
        String payload = readBody(request);
        MediaType mediaType = request.body() == null ? null : request.body().contentType();

        assertEquals("{\"name\":\"potato\"}", payload);
        assertNotNull(mediaType);
        assertEquals("application/octet-stream", mediaType.toString());
    }

    @Test
    @DisplayName("FORM 文件 part 保留显式文件名和 MIME")
    void customFormFilePartShouldKeepExplicitFileNameAndMime() throws Exception {
        Path tempFile = Files.createTempFile("potatotool-request-body", ".tmp");
        Files.write(tempFile, "hello-file".getBytes(StandardCharsets.UTF_8));
        try {
            Map<String, Object> formParameters = new LinkedHashMap<String, Object>();
            formParameters.put("purpose", "user_data");
            formParameters.put("file", new RequestObj.FormFilePart(tempFile.toFile(), "evidence.log", "text/plain"));

            RequestObj requestObj = new RequestObj()
                    .setMethod("POST")
                    .setUrl("http://example.com/upload")
                    .setFormParameters(formParameters);

            Request request = RequestUtils.buildRequest(requestObj);
            String payload = readBody(request);

            assertEquals("POST", request.method());
            assertTrue(payload.contains("name=\"purpose\""));
            assertTrue(payload.contains("user_data"));
            assertTrue(payload.contains("name=\"file\"; filename=\"evidence.log\""));
            assertTrue(payload.contains("Content-Type: text/plain"));
            assertTrue(payload.contains("hello-file"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String readBody(Request request) throws Exception {
        assertNotNull(request.body());
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readString(StandardCharsets.UTF_8);
    }
}
