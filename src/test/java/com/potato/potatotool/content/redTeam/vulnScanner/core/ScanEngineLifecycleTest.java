package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanStartedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanState;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScanEngine 生命周期回归测试")
class ScanEngineLifecycleTest {

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
    @DisplayName("空目标与空 POC 应拒绝启动")
    void shouldRejectEmptyTargetsAndPocs(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanEngine engine = new ScanEngine(new ScanConfig());
        try {
            PocObj.Poc poc = buildHttpPoc("empty-guard", "/fast", "FAST-OK");

            IllegalArgumentException noTargets = assertThrows(
                    IllegalArgumentException.class,
                    () -> engine.startScan(Collections.<String>emptyList(), Collections.singletonList(poc)));
            assertTrue(noTargets.getMessage().contains("目标列表不能为空"));

            IllegalArgumentException noPocs = assertThrows(
                    IllegalArgumentException.class,
                    () -> engine.startScan(Collections.singletonList(BASE_URL), Collections.<PocObj.Poc>emptyList()));
            assertTrue(noPocs.getMessage().contains("POC列表不能为空"));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("运行中重复 startScan 应抛出 IllegalStateException")
    void shouldRejectDuplicateStartWhileScanning(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(2);
        ScanEngine engine = new ScanEngine(config);
        try {
            CountDownLatch started = new CountDownLatch(1);
            engine.addEventListener(new ScanEventListener() {
                @Override
                public void onScanStarted(ScanStartedEvent event) {
                    started.countDown();
                }
            });

            PocObj.Poc slowPoc = buildHttpPoc("duplicate-start", "/slow", "SLOW-OK");
            engine.startScan(Collections.singletonList(BASE_URL), Collections.singletonList(slowPoc));

            assertTrue(started.await(2, TimeUnit.SECONDS));
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> engine.startScan(Collections.singletonList(BASE_URL), Collections.singletonList(slowPoc)));
            assertTrue(exception.getMessage().contains("扫描任务正在进行中"));
        } finally {
            engine.stopScan();
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("pauseScan 后应落库为 PAUSED，resumeScan 后应继续完成")
    void shouldPausePersistAndResumeScan(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(3);
        ScanEngine engine = new ScanEngine(config);
        try {
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<ScanCompletedEvent> completedRef = new AtomicReference<ScanCompletedEvent>();
            CountDownLatch completed = new CountDownLatch(1);
            engine.addEventListener(new ScanEventListener() {
                @Override
                public void onScanStarted(ScanStartedEvent event) {
                    started.countDown();
                }

                @Override
                public void onScanCompleted(ScanCompletedEvent event) {
                    completedRef.set(event);
                    completed.countDown();
                }
            });

            PocObj.Poc slowPoc = buildHttpPoc("pause-resume", "/slow", "SLOW-OK");
            engine.startScan(Collections.singletonList(BASE_URL), Collections.singletonList(slowPoc));

            assertTrue(started.await(2, TimeUnit.SECONDS));
            engine.pauseScan();
            Thread.sleep(200);

            assertTrue(engine.isPaused());
            assertFalse(engine.isScanning());

            VulnScanDatabase database = VulnScanDatabase.getInstance();
            ScanState pausedState = database.loadScanState(engine.getCurrentScanId());
            assertNotNull(pausedState);
            assertEquals(ScanState.Status.PAUSED, pausedState.getStatus());
            assertEquals(1, pausedState.getTotalTasks());

            engine.resumeScan(pausedState, Collections.singletonList(slowPoc));

            assertTrue(completed.await(5, TimeUnit.SECONDS), "恢复后扫描未在预期时间内完成");
            assertFalse(engine.isScanning());
            assertFalse(engine.isPaused());
            assertNotNull(completedRef.get());
            assertTrue(completedRef.get().isSuccess());

            ScanState finalState = database.loadScanState(engine.getCurrentScanId());
            assertNotNull(finalState);
            assertEquals(ScanState.Status.COMPLETED, finalState.getStatus());
            assertEquals(1, finalState.getCompletedTasks());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("stopScan 后应结束运行并写入 STOPPED 状态")
    void shouldStopAndPersistStoppedState(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        ScanEngine engine = new ScanEngine(config);
        try {
            CountDownLatch started = new CountDownLatch(1);
            engine.addEventListener(new ScanEventListener() {
                @Override
                public void onScanStarted(ScanStartedEvent event) {
                    started.countDown();
                }
            });

            PocObj.Poc slowPoc = buildHttpPoc("stop-scan", "/slow", "SLOW-OK");
            engine.startScan(Collections.singletonList(BASE_URL), Collections.singletonList(slowPoc));
            assertTrue(started.await(2, TimeUnit.SECONDS));

            engine.stopScan();
            Thread.sleep(200);

            assertFalse(engine.isScanning());
            assertFalse(engine.isPaused());

            ScanState stoppedState = VulnScanDatabase.getInstance().loadScanState(engine.getCurrentScanId());
            assertNotNull(stoppedState);
            assertEquals(ScanState.Status.STOPPED, stoppedState.getStatus());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("多任务扫描应按线程配置并发执行")
    void shouldExecuteMultipleTasksConcurrently(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(3);
        config.setEnableClustering(false);
        ScanEngine engine = new ScanEngine(config);
        try {
            CountDownLatch completed = new CountDownLatch(1);
            engine.addEventListener(new ScanEventListener() {
                @Override
                public void onScanCompleted(ScanCompletedEvent event) {
                    completed.countDown();
                }
            });

            PocObj.Poc poc1 = buildHttpPoc("concurrent-1", "/slow?a=1", "SLOW-OK");
            PocObj.Poc poc2 = buildHttpPoc("concurrent-2", "/slow?a=2", "SLOW-OK");
            PocObj.Poc poc3 = buildHttpPoc("concurrent-3", "/slow?a=3", "SLOW-OK");

            long start = System.currentTimeMillis();
            engine.startScan(Collections.singletonList(BASE_URL), Arrays.asList(poc1, poc2, poc3));

            assertTrue(completed.await(5, TimeUnit.SECONDS), "并发扫描未在预期时间内完成");
            long duration = System.currentTimeMillis() - start;

            assertTrue(duration < 2200, "3 个慢请求应并发完成，实际耗时: " + duration + "ms");
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("扫描正常完成后线程池应关闭，避免长时间运行泄漏线程")
    void shouldShutdownExecutorAfterCompletion(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanConfig config = new ScanConfig();
        config.setThreads(2);
        config.setEnableClustering(false);
        ScanEngine engine = new ScanEngine(config);
        try {
            CountDownLatch completed = new CountDownLatch(1);
            engine.addEventListener(new ScanEventListener() {
                @Override
                public void onScanCompleted(ScanCompletedEvent event) {
                    completed.countDown();
                }
            });

            PocObj.Poc fastPoc = buildHttpPoc("executor-close", "/fast", "FAST-OK");
            engine.startScan(Collections.singletonList(BASE_URL), Collections.singletonList(fastPoc));

            assertTrue(completed.await(5, TimeUnit.SECONDS), "扫描未在预期时间内完成");

            Field executorField = ScanEngine.class.getDeclaredField("executorService");
            executorField.setAccessible(true);
            Object executor = executorField.get(engine);

            assertTrue(executor instanceof ExecutorService);
            assertTrue(((ExecutorService) executor).isShutdown(), "扫描完成后线程池应已关闭");
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("shutdown 后数据库写线程应及时退出")
    void shouldTerminateDbWriterThreadOnShutdown(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanEngine engine = new ScanEngine(new ScanConfig());
        try {
            assertTrue(countThreads("scan-db-writer") >= 1, "初始化后应存在数据库写线程");
        } finally {
            engine.shutdown();
        }

        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && countThreads("scan-db-writer") > 0) {
            Thread.sleep(50);
        }

        assertEquals(0, countThreads("scan-db-writer"), "shutdown 后数据库写线程应已退出");
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("scan-lifecycle.sqlite");
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

        PocObj.Matcher status = new PocObj.Matcher();
        status.setType(PocObj.MatcherType.STATUS);
        status.setValues(Collections.singletonList("200"));

        PocObj.Matcher body = new PocObj.Matcher();
        body.setType(PocObj.MatcherType.WORD);
        body.setPart("body");
        body.setValues(Collections.singletonList(expectedBody));

        step.setMatchers(Arrays.asList(status, body));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private static HttpServer createServer() {
        try {
            HttpServer server = HttpServer.create(new java.net.InetSocketAddress(0), 0);
            server.createContext("/fast", exchange -> write(exchange, 200, "FAST-OK"));
            server.createContext("/slow", exchange -> {
                sleepQuietly(1500);
                write(exchange, 200, "SLOW-OK");
            });
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("启动生命周期测试服务器失败", e);
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

    private static int countThreads(String prefix) {
        int count = 0;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread != null && thread.isAlive() && thread.getName() != null
                    && thread.getName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
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
