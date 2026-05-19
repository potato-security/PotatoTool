package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanErrorEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase;
import com.potato.potatotool.utils.core.Constants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScanEngine 异常链路回归测试")
class ScanEngineErrorPathTest {

    private static final String HOST = "127.0.0.1";
    private static final HttpServer SERVER = createServer();
    private static final String BASE_URL = "http://" + HOST + ":" + SERVER.getAddress().getPort();

    private String originalUserHome;

    static {
        SERVER.start();
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @AfterEach
    void tearDown() throws Exception {
        resetSingleton(VulnScanDatabase.class, "instance", true);
        resetSingleton(com.potato.potatotool.storage.PathManager.class, "instance", false);
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.class, "instance", false);
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    @DisplayName("目标超时应派发 READ_TIMEOUT 错误事件")
    void shouldDispatchTimeoutError(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(1);
        config.setDebug(true);
        config.setEnableClustering(false);
        ScanEngine engine = new ScanEngine(config);
        try {
            PocObj.Poc poc = buildHttpPoc("timeout-path", "/timeout", "never");
            AtomicReference<ScanErrorEvent> errorRef = new AtomicReference<ScanErrorEvent>();
            AtomicReference<ScanCompletedEvent> completedRef = new AtomicReference<ScanCompletedEvent>();
            CountDownLatch completed = new CountDownLatch(1);
            attachListeners(engine, errorRef, completedRef, completed);

            engine.startScan(Collections.singletonList(BASE_URL), Collections.singletonList(poc));

            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertNotNull(completedRef.get());
            assertTrue(completedRef.get().isSuccess(), "网络异常应被吞掉并继续完成扫描");
            assertNotNull(errorRef.get());
            assertNotNull(errorRef.get().getException());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("连接拒绝应派发 CONNECTION_REFUSED 错误事件")
    void shouldDispatchConnectionRefusedError(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(1);
        config.setDebug(true);
        config.setEnableClustering(false);
        ScanEngine engine = new ScanEngine(config);
        try {
            PocObj.Poc poc = buildHttpPoc("refused-path", "/", "never");
            AtomicReference<ScanErrorEvent> errorRef = new AtomicReference<ScanErrorEvent>();
            CountDownLatch completed = new CountDownLatch(1);
            attachListeners(engine, errorRef, new AtomicReference<ScanCompletedEvent>(), completed);

            engine.startScan(Collections.singletonList("http://" + HOST + ":" + port), Collections.singletonList(poc));

            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertNotNull(errorRef.get());
            assertNotNull(errorRef.get().getException());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("DNS 解析失败应派发 UNKNOWN_HOST 错误事件")
    void shouldDispatchUnknownHostError(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(1);
        config.setDebug(true);
        config.setEnableClustering(false);
        ScanEngine engine = new ScanEngine(config);
        try {
            PocObj.Poc poc = buildHttpPoc("unknown-host", "/", "never");
            AtomicReference<ScanErrorEvent> errorRef = new AtomicReference<ScanErrorEvent>();
            CountDownLatch completed = new CountDownLatch(1);
            attachListeners(engine, errorRef, new AtomicReference<ScanCompletedEvent>(), completed);

            engine.startScan(Collections.singletonList("http://potatotool-does-not-exist.invalid"), Collections.singletonList(poc));

            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertNotNull(errorRef.get());
            assertNotNull(errorRef.get().getException());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("无效 raw HTTP 报文应派发 handler 异常事件")
    void shouldDispatchHandlerExceptionForInvalidRawRequest(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setDebug(true);
        config.setEnableClustering(false);
        ScanEngine engine = new ScanEngine(config);
        try {
            PocObj.Poc poc = new PocObj.Poc();
            poc.setId("raw-handler-exception");
            poc.setName("raw-handler-exception");
            poc.setProtocol("http");
            poc.setSeverity(PocObj.Severity.LOW);

            PocObj.PocStep step = new PocObj.PocStep();
            step.setStepId("http_1");
            step.setRaw(Collections.singletonList("BAD_REQUEST_WITHOUT_HTTP_METHOD"));
            poc.setVerifySteps(Collections.singletonList(step));

            AtomicReference<ScanErrorEvent> errorRef = new AtomicReference<ScanErrorEvent>();
            AtomicReference<ScanCompletedEvent> completedRef = new AtomicReference<ScanCompletedEvent>();
            CountDownLatch completed = new CountDownLatch(1);
            attachListeners(engine, errorRef, completedRef, completed);

            engine.startScan(Collections.singletonList(BASE_URL), Collections.singletonList(poc));

            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertNotNull(completedRef.get());
            assertTrue(completedRef.get().isSuccess());
            assertNotNull(errorRef.get());
            assertNotNull(errorRef.get().getException());
        } finally {
            engine.shutdown();
        }
    }

    private void attachListeners(ScanEngine engine,
                                 AtomicReference<ScanErrorEvent> errorRef,
                                 AtomicReference<ScanCompletedEvent> completedRef,
                                 CountDownLatch completed) {
        engine.addEventListener(new ScanEventListener() {
            @Override
            public void onScanError(ScanErrorEvent event) {
                errorRef.compareAndSet(null, event);
            }

            @Override
            public void onScanCompleted(ScanCompletedEvent event) {
                completedRef.set(event);
                completed.countDown();
            }
        });
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("scan-error-path.sqlite");
        Files.createDirectories(dbPath.getParent());

        String json = "{\n" +
                "  \"VulnScan\": {\n" +
                "    \"dbPath\": \"" + escapeForJson(dbPath.toString()) + "\"\n" +
                "  }\n" +
                "}";
        Files.write(configDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;
        resetSingleton(VulnScanDatabase.class, "instance", true);
        resetSingleton(com.potato.potatotool.storage.PathManager.class, "instance", false);
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.class, "instance", false);
    }

    private static PocObj.Poc buildHttpPoc(String id, String path, String expectedBody) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.MEDIUM);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath(path);
        step.setTimeout(1);

        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(expectedBody));
        step.setMatchers(Collections.singletonList(matcher));
        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private static HttpServer createServer() {
        try {
            HttpServer server = HttpServer.create(new java.net.InetSocketAddress(0), 0);
            server.createContext("/timeout", exchange -> {
                sleepQuietly(2500);
                write(exchange, 200, "timeout");
            });
            server.createContext("/", exchange -> write(exchange, 200, "root"));
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("启动异常链路测试服务器失败", e);
        }
    }

    private static void write(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void resetSingleton(Class<?> type, String fieldName, boolean closeDatabase) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object existing = field.get(null);
        if (closeDatabase && existing instanceof VulnScanDatabase) {
            ((VulnScanDatabase) existing).close();
        }
        field.set(null, null);
    }

    private String escapeForJson(String value) {
        return value.replace("\\", "\\\\");
    }
}
