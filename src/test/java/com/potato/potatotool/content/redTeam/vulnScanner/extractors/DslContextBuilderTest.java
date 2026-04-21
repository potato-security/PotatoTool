package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslContextBuilder;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DslContextBuilderTest {

    @Test
    public void createDslContext_shouldExtractRequestInfoFromCustomHttpResponseWithoutJavaFxReflection() {
        Request request = new Request.Builder()
                .url("https://example.com/api/data?id=1")
                .get()
                .addHeader("X-Test", "demo")
                .build();
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.parse("application/json"), "{\"status\":\"ok\"}"))
                .build();

        CustomHttpResponse customHttpResponse = new CustomHttpResponse(response);
        Map<String, Object> context = DslContextBuilder.createDslContext(customHttpResponse, customHttpResponse);

        assertEquals("https://example.com/api/data?id=1", context.get("url"));
        assertEquals("GET", context.get("method"));
        assertEquals("/api/data", context.get("path"));
        assertEquals("id=1", context.get("query"));
        assertTrue(context.containsKey("request_headers"));
        assertEquals("{\"status\":\"ok\"}", context.get("body"));
    }
}
