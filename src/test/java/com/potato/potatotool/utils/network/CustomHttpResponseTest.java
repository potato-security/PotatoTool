package com.potato.potatotool.utils.network;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CustomHttpResponse 流式断开测试")
class CustomHttpResponseTest {

    @Test
    @DisplayName("主动 disconnect 后的 stream closed 异常应被视为预期断流")
    void shouldSuppressExpectedCloseExceptionAfterManualDisconnect() {
        CustomHttpResponse response = new CustomHttpResponse(buildResponse());
        try {
            assertFalse(response.shouldSuppressStreamingException(new IOException("stream closed")));

            response.disconnect();

            assertTrue(response.shouldSuppressStreamingException(new IOException("stream closed")));
            assertTrue(response.shouldSuppressStreamingException(new IOException("Socket closed")));
            assertTrue(response.shouldSuppressStreamingException(new IOException("Canceled")));
            assertFalse(response.shouldSuppressStreamingException(new IOException("Read timed out")));
        } finally {
            response.disconnect();
        }
    }

    private Response buildResponse() {
        return new Response.Builder()
                .request(new Request.Builder().url("https://api.example.com/v1/chat/completions").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.parse("text/plain"), "ok"))
                .build();
    }
}
