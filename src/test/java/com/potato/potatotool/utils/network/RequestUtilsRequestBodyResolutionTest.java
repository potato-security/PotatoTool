package com.potato.potatotool.utils.network;

import okhttp3.MediaType;
import okhttp3.Request;
import okio.Buffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private String readBody(Request request) throws Exception {
        assertNotNull(request.body());
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readString(StandardCharsets.UTF_8);
    }
}
