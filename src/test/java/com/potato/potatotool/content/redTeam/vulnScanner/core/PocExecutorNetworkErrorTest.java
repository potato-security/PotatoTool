package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.NetworkException;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PocExecutor 网络异常分类测试")
class PocExecutorNetworkErrorTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("连接拒绝应归类为 CONNECTION_REFUSED")
    void shouldClassifyConnectionRefused() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        PocExecutor executor = new PocExecutor(new ScanConfig());
        PocObj.Poc poc = buildHttpPoc("refused", "/", "never", 1);

        NetworkException exception = assertThrows(NetworkException.class,
                () -> executor.execute("http://127.0.0.1:" + port, poc));

        assertEquals(NetworkException.NetworkErrorType.CONNECTION_REFUSED, exception.getErrorType());
    }

    @Test
    @DisplayName("DNS 解析失败应归类为 UNKNOWN_HOST")
    void shouldClassifyUnknownHost() {
        PocExecutor executor = new PocExecutor(new ScanConfig());
        PocObj.Poc poc = buildHttpPoc("unknown-host", "/", "never", 1);

        NetworkException exception = assertThrows(NetworkException.class,
                () -> executor.execute("http://potatotool-does-not-exist.invalid", poc));

        assertEquals(NetworkException.NetworkErrorType.UNKNOWN_HOST, exception.getErrorType());
    }

    @Test
    @DisplayName("读取超时应归类为 READ_TIMEOUT")
    void shouldClassifyReadTimeout() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/timeout", exchange -> {
            sleepQuietly(2500);
            write(exchange, 200, "late");
        });
        server.start();

        PocExecutor executor = new PocExecutor(new ScanConfig());
        PocObj.Poc poc = buildHttpPoc("timeout", "/timeout", "never", 1);

        NetworkException exception = assertThrows(NetworkException.class,
                () -> executor.execute("http://127.0.0.1:" + server.getAddress().getPort(), poc));

        assertEquals(NetworkException.NetworkErrorType.READ_TIMEOUT, exception.getErrorType());
    }

    private PocObj.Poc buildHttpPoc(String id, String path, String expectedBody, int timeoutSeconds) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.MEDIUM);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath(path);
        step.setTimeout(timeoutSeconds);

        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(expectedBody));
        step.setMatchers(Collections.singletonList(matcher));

        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private void write(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
