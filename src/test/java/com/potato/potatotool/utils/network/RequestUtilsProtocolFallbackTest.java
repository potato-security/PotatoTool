package com.potato.potatotool.utils.network;

import com.sun.net.httpserver.HttpServer;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("RequestUtils 协议 fallback 无副作用测试")
class RequestUtilsProtocolFallbackTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("HTTPS 降级 fallback 不应污染原始 RequestObj.url")
    void httpsFallbackShouldNotMutateOriginalRequestUrl() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/fallback", exchange -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        int port = server.getAddress().getPort();
        String originalUrl = "https://127.0.0.1:" + port + "/fallback";
        String fallbackUrl = "http://127.0.0.1:" + port + "/fallback";

        RequestObj requestObj = new RequestObj()
                .setUrl(originalUrl)
                .setRetries(0);

        OkHttpClient client = RequestUtils.createOkHttpClient(requestObj).newBuilder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if ("https".equalsIgnoreCase(request.url().scheme())) {
                            throw new SSLHandshakeException("mock handshake failure");
                        }
                        return chain.proceed(request);
                    }
                })
                .build();

        CustomHttpResponse response = RequestUtils.requests(requestObj, client);
        try {
            assertNotNull(response);
            assertEquals(200, response.getResponseCode());
            assertEquals("ok", response.getTextStr());
            assertEquals(originalUrl, requestObj.getUrl());
            assertFalse(requestObj.getUrl().startsWith("http://"));
            assertEquals(fallbackUrl, response.getURL().toString());
        } finally {
            response.disconnect();
        }
    }
}
