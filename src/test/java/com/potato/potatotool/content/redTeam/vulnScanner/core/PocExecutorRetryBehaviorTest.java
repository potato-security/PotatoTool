package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocExecutor 重试行为测试")
class PocExecutorRetryBehaviorTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("HTTP 步骤首跳 500 后重试成功应命中漏洞")
    void shouldRetryAndSucceedAfterTransientServerError() throws Exception {
        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/retry-once", exchange -> {
            int current = hitCount.incrementAndGet();
            if (current == 1) {
                write(exchange, 500, "first-failure");
                return;
            }
            write(exchange, 200, "retry-success");
        });
        server.start();

        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        PocExecutor executor = new PocExecutor(config);

        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("retry-once");
        poc.setName("retry-once");
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.MEDIUM);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath("/retry-once");
        step.setRetries(1);

        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList("retry-success"));
        step.setMatchers(Collections.singletonList(matcher));

        poc.setVerifySteps(Collections.singletonList(step));

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        ScanResult result = executor.execute(target, poc);

        assertTrue(result.isVulnerable());
        assertEquals(2, hitCount.get());
    }

    private void write(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
